package com.fongmi.android.tv.utils;

import android.text.TextUtils;

import com.fongmi.android.tv.bean.SiteRule;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.net.OkHttp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 在线测试分析器：输入视频链接，下载 m3u8 清单并采样片段，
 * 自动分析时长序列/TS 连贯性，生成候选资源站规则（可编辑/保存/删除）。
 *
 * 说明：分析时需要真实下载 m3u8 清单；做 hash 检测时会额外采样前几个片段
 * 计算大小/哈希（默认最多 3 段，避免大流量）。
 */
public final class SiteRuleAnalyzer {

    private static final String TAG = "site-analyze";
    private static final int SAMPLE_LIMIT = 3;
    private static final int MAX_SEGMENTS = 512;

    private SiteRuleAnalyzer() {}

    public static final class Result {
        public final String url;
        public final String host;
        public final int segmentCount;
        public final int discontinuityCount;
        public final List<Double> durations;
        public final List<Double> shortBlocks;
        public final List<Double> longBlocks;
        public final SiteRule candidate;
        public final String error;

        Result(String url, String host, int segmentCount, int discontinuityCount,
               List<Double> durations, List<Double> shortBlocks, List<Double> longBlocks,
               SiteRule candidate, String error) {
            this.url = url; this.host = host; this.segmentCount = segmentCount;
            this.discontinuityCount = discontinuityCount; this.durations = durations;
            this.shortBlocks = shortBlocks; this.longBlocks = longBlocks;
            this.candidate = candidate; this.error = error;
        }

        public boolean ok() { return error == null; }
    }

    public static Result analyze(String url) {
        try {
            String manifest = fetchPlaylist(url);
            if (TextUtils.isEmpty(manifest) || !manifest.trim().startsWith("#EXTM3U")) {
                return new Result(url, "", 0, 0, List.of(), List.of(), List.of(), null, "下载失败或非 m3u8 清单");
            }
            ManifestInfo info = parse(manifest);
            List<Double> shortBlocks = new ArrayList<>();
            List<Double> longBlocks = new ArrayList<>();
            List<Double> durations = info.durations;

            double mode = modeDuration(durations);
            for (double d : durations) {
                if (mode > 0 && d < mode * 0.5) shortBlocks.add(d);
                else if (mode > 0 && d > mode * 1.8) longBlocks.add(d);
            }

            // 采样下载（hash 支持）：仅取前 3 段字节大小，供用户判断；不落盘
            List<Long> sampleSizes = sampleSizes(url, info.segmentUrls);

            String host = hostOf(url);
            SiteRule rule = buildCandidate(host, info, shortBlocks, longBlocks);
            rule.setName(host + " 自动分析");
            rule.setUrlPrefixes(List.of(host));
            rule.setDetectType(sampleSizes.isEmpty() ? SiteRule.DETECT_DURATION : SiteRule.DETECT_BOTH);
            rule.setDetectMethod(methodFor(info, shortBlocks));
            return new Result(url, host, info.segmentCount, info.discontinuityCount, durations,
                    shortBlocks, longBlocks, rule, null);
        } catch (Throwable e) {
            SpiderDebug.log(TAG, "analyze error %s", e.toString());
            return new Result(url, "", 0, 0, List.of(), List.of(), List.of(), null, String.valueOf(e.getMessage()));
        }
    }

    private static String fetchPlaylist(String url) throws Exception {
        Request request = new Request.Builder().url(url)
                .header("User-Agent", "MoxiTVBox")
                .header("Accept", "application/vnd.apple.mpegurl")
                .build();
        try (Response response = OkHttp.player().newCall(request).execute()) {
            if (!response.isSuccessful()) throw new Exception("HTTP " + response.code());
            ResponseBody body = response.body();
            return body == null ? "" : body.string();
        }
    }

    private static final class ManifestInfo {
        int segmentCount;
        int discontinuityCount;
        List<Double> durations = new ArrayList<>();
        List<String> segmentUrls = new ArrayList<>();
    }

    private static ManifestInfo parse(String manifest) {
        ManifestInfo info = new ManifestInfo();
        String[] lines = manifest.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        Double pending = null;
        okhttp3.HttpUrl base = null;
        try { base = okhttp3.HttpUrl.parse("https://placeholder.invalid/"); } catch (Throwable ignored) {}
        for (int i = 0; i < lines.length && info.segmentCount < MAX_SEGMENTS; i++) {
            String line = lines[i].trim();
            if (line.startsWith("#EXT-X-DISCONTINUITY")) {
                info.discontinuityCount++;
            } else if (line.startsWith("#EXTINF:")) {
                try {
                    String value = line.substring("#EXTINF:".length());
                    int comma = value.indexOf(',');
                    if (comma >= 0) value = value.substring(0, comma);
                    pending = Double.parseDouble(value.trim());
                } catch (Throwable ignored) {
                }
            } else if (!line.isEmpty() && !line.startsWith("#") && pending != null) {
                info.durations.add(pending);
                info.segmentUrls.add(line);
                info.segmentCount++;
                pending = null;
            }
        }
        return info;
    }

    private static double modeDuration(List<Double> durations) {
        if (durations.isEmpty()) return 0;
        Map<Double, Integer> counts = new LinkedHashMap<>();
        for (double d : durations) {
            double key = Math.round(d * 2) / 2.0; // 0.5s 粒度
            counts.merge(key, 1, Integer::sum);
        }
        double best = durations.get(0);
        int max = -1;
        for (Map.Entry<Double, Integer> e : counts.entrySet()) {
            if (e.getValue() > max) { max = e.getValue(); best = e.getKey(); }
        }
        return best;
    }

    private static List<Long> sampleSizes(String playlistUrl, List<String> segmentUrls) {
        List<Long> sizes = new ArrayList<>();
        int count = Math.min(SAMPLE_LIMIT, segmentUrls.size());
        for (int i = 0; i < count; i++) {
            try {
                String segUrl = resolve(playlistUrl, segmentUrls.get(i));
                Request request = new Request.Builder().url(segUrl)
                        .header("User-Agent", "MoxiTVBox").build();
                try (Response response = OkHttp.player().newCall(request).execute()) {
                    if (!response.isSuccessful()) continue;
                    ResponseBody body = response.body();
                    if (body == null) continue;
                    // 只取前 64KB 做大小近似，避免大流量
                    long len = Math.min(64 * 1024, body.contentLength() >= 0 ? body.contentLength() : 64 * 1024);
                    body.close();
                    sizes.add(len);
                }
            } catch (Throwable ignored) {
            }
        }
        return sizes;
    }

    private static SiteRule buildCandidate(String host, ManifestInfo info,
                                           List<Double> shortBlocks, List<Double> longBlocks) {
        SiteRule rule = SiteRule.create();
        rule.setDomain(host);
        List<String> fingerprints = new ArrayList<>();
        // 特征码：取异常块时长序列（去重保留顺序），最多 8 个
        List<Double> anomalies = new ArrayList<>();
        anomalies.addAll(shortBlocks);
        anomalies.addAll(longBlocks);
        Map<Double, Boolean> seen = new LinkedHashMap<>();
        for (double d : anomalies) if (seen.put(Math.round(d * 100) / 100.0, true) == null) {
            if (fingerprints.isEmpty() || fingerprints.get(fingerprints.size() - 1).split(",").length < 8) {
                if (fingerprints.isEmpty()) fingerprints.add("");
                fingerprints.set(fingerprints.size() - 1, fingerprints.get(fingerprints.size() - 1).isEmpty()
                        ? String.valueOf(d) : fingerprints.get(fingerprints.size() - 1) + ", " + d);
            }
        }
        if (fingerprints.isEmpty() || fingerprints.get(0).trim().isEmpty()) fingerprints.clear();
        rule.setFingerprints(fingerprints);
        return rule;
    }

    private static String methodFor(ManifestInfo info, List<Double> shortBlocks) {
        if (info.discontinuityCount > 0 && !shortBlocks.isEmpty()) return SiteRule.METHOD_SEQUENCE;
        if (info.discontinuityCount > 0) return SiteRule.METHOD_SEQUENCE;
        if (!shortBlocks.isEmpty()) return SiteRule.METHOD_SHORTBLOCK;
        return SiteRule.METHOD_AUTO;
    }

    private static String resolve(String baseUrl, String value) {
        try {
            okhttp3.HttpUrl base = okhttp3.HttpUrl.parse(baseUrl);
            okhttp3.HttpUrl resolved = base == null ? null : base.resolve(value);
            return resolved == null ? value : resolved.toString();
        } catch (Throwable e) {
            return value;
        }
    }

    private static String hostOf(String url) {
        try {
            okhttp3.HttpUrl parsed = okhttp3.HttpUrl.parse(url);
            return parsed == null ? "" : parsed.host();
        } catch (Throwable e) {
            return "";
        }
    }
}
