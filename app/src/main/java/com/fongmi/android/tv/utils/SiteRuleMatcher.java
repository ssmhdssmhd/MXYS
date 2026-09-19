package com.fongmi.android.tv.utils;

import android.text.TextUtils;

import com.fongmi.android.tv.api.config.SiteRuleConfig;
import com.fongmi.android.tv.bean.SiteRule;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 资源站规则匹配引擎：
 * - compileRules()：把全部已启用站点规则编译为 HLS 清洗规则（特征码→时长指纹、广告正则→段URL正则、检测方式→信号组合）
 * - selectVariant()：master 清单按码率选择（first/highest/lowest）挑选变体
 * - applyIndexMode()：索引方式 fixed 时把播放 URL 重定位到固定子路径
 */
public final class SiteRuleMatcher {

    private static final Pattern STREAM_INF = Pattern.compile("#EXT-X-STREAM-INF:([^\\n]*)");
    private static final Pattern BANDWIDTH = Pattern.compile("BANDWIDTH=(\\d+)");
    private static final Pattern RESOLUTION = Pattern.compile("RESOLUTION=(\\d+)x(\\d+)");

    private SiteRuleMatcher() {}

    /** 编译全部已启用规则 → HLS 清洗规则列表（与内置/接口规则合并使用）。 */
    public static List<HlsManifestCleaner.Rule> compileRules() {
        List<HlsManifestCleaner.Rule> out = new ArrayList<>();
        for (SiteRule rule : SiteRuleConfig.enabled()) {
            try {
                out.addAll(compile(rule));
            } catch (Throwable ignored) {
            }
        }
        return out;
    }

    private static List<HlsManifestCleaner.Rule> compile(SiteRule rule) {
        List<HlsManifestCleaner.Rule> out = new ArrayList<>();
        String host = rule.getDomain();
        if (TextUtils.isEmpty(host)) return out;

        // 特征码 → 时长指纹规则（限定本站点）
        out.addAll(rule.toFingerprintRules());

        // 广告过滤正则 → 段 URL 正则规则
        List<String> adRegexes = rule.getAdRegexes();
        List<String> segmentPatterns = new ArrayList<>();
        for (String r : adRegexes) if (r != null && !r.trim().isEmpty()) segmentPatterns.add(r.trim());
        if (!segmentPatterns.isEmpty()) {
            try {
                HlsManifestCleaner.Rule.Builder builder = HlsManifestCleaner.Rule.builder()
                        .playlistHostSuffixes(List.of(host))
                        .segmentUrlPatterns(segmentPatterns)
                        .minimumSignals(1);
                // sequence/blockcut/shortblock 依赖 DISCONTINUITY 边界信号
                String method = rule.getDetectMethod();
                if (SiteRule.METHOD_SEQUENCE.equals(method) || SiteRule.METHOD_BLOCKCUT.equals(method)
                        || SiteRule.METHOD_SHORTBLOCK.equals(method)) {
                    builder.requireDiscontinuity(true);
                }
                out.add(builder.build());
            } catch (Throwable ignored) {
            }
        }
        return out;
    }

    /**
     * master 清单按码率选择变体。
     *
     * @param master 主清单文本（含 #EXT-X-STREAM-INF）
     * @param bitrate first/highest/lowest
     * @return 选中变体的 URL；非 master 或无法解析返回 null
     */
    public static String selectVariant(String master, String bitrate) {
        if (TextUtils.isEmpty(master) || !master.contains("#EXT-X-STREAM-INF")) return null;
        String[] lines = master.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        List<long[]> candidates = new ArrayList<>(); // {bandwidth, lineIndex}
        String pendingInf = null;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("#EXT-X-STREAM-INF:")) {
                pendingInf = line;
                continue;
            }
            if (pendingInf != null && !line.isEmpty() && !line.startsWith("#")) {
                Matcher bw = BANDWIDTH.matcher(pendingInf);
                long value = bw.find() ? Long.parseLong(bw.group(1)) : 0;
                candidates.add(new long[]{value, i});
                pendingInf = null;
            }
        }
        if (candidates.isEmpty()) return null;
        int pick = 0;
        if (SiteRule.BITRATE_HIGHEST.equals(bitrate)) {
            for (int i = 1; i < candidates.size(); i++) if (candidates.get(i)[0] > candidates.get(pick)[0]) pick = i;
        } else if (SiteRule.BITRATE_LOWEST.equals(bitrate)) {
            for (int i = 1; i < candidates.size(); i++) if (candidates.get(i)[0] < candidates.get(pick)[0]) pick = i;
        }
        return lines[(int) candidates.get(pick)[1]].trim();
    }

    /**
     * 索引方式 fixed：把 URL 的路径段重定位到固定子路径。
     * 例如固定子路径 "3000k/hls/mixed.m3u8" 时，返回同 scheme/host 下该路径的完整 URL。
     */
    public static String applyIndexMode(String url, SiteRule rule) {
        if (rule == null || !SiteRule.INDEX_FIXED.equals(rule.getIndexMode())) return url;
        String fixed = rule.getFixedPath();
        if (TextUtils.isEmpty(fixed)) return url;
        try {
            okhttp3.HttpUrl base = okhttp3.HttpUrl.parse(url);
            if (base == null) return url;
            String path = fixed.startsWith("/") ? fixed : "/" + fixed;
            return base.newBuilder().encodedPath(path).build().toString();
        } catch (Throwable e) {
            return url;
        }
    }

    /** 对某条播放 URL 是否应用索引重定位（存在匹配站点规则且索引方式=fixed）。 */
    public static String reindexIfNeeded(String url) {
        SiteRule rule = SiteRuleConfig.match(url);
        if (rule == null) return url;
        return applyIndexMode(url, rule);
    }
}
