package com.fongmi.android.tv.bean;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * 资源站规则（直走接口的资源站级去广告配置）。
 *
 * 一个规则绑定一个资源站（域名），配置检测/索引/码率等策略：
 * - detectType  检测解析类型：both(指纹+哈希)/duration(仅时长指纹)/hash(仅内容哈希)/direct(直链播放)
 * - detectMethod 检测方式：auto/sequence/blockcut/slicebatch/shortblock/fingerprint/both/hash
 * - indexMode   索引方式：auto(跟随主索引子列表)/single(本地址即片段列表)/fixed(固定子路径)
 * - bitrate     码率选择：first/highest/lowest
 * - fixedPath   索引模式=fixed 时的固定子路径（如 3000k/hls/mixed.m3u8）
 * - proxyUrl    站点级出网代理（留空=按代理池/直连，填 - =强制直连）
 * - urlPrefixes URL 匹配前缀（站点路由：只对主机名前缀匹配，任一命中即匹配该站点）
 * - urlRegexes  URL 匹配正则（站点路由补充：默认只对主机名；以 ^ 开头或含 :// 时匹配整条 URL）
 * - cacheDir    缓存子目录（保存时自动同步目录名）
 * - sortCode    排序码
 * - fingerprints 广告时长指纹（一行一组，如 "5.567, 3.2" / "3, 3, 3, 3"，任一命中即剔除）
 * - adRegexes   广告过滤正则列表（片段级广告剔除，与站点路由无关）
 */
public class SiteRule {

    public static final String DETECT_BOTH = "both";
    public static final String DETECT_DURATION = "duration";
    public static final String DETECT_HASH = "hash";
    public static final String DETECT_DIRECT = "direct";

    public static final String METHOD_AUTO = "auto";
    public static final String METHOD_SEQUENCE = "sequence";
    public static final String METHOD_BLOCKCUT = "blockcut";
    public static final String METHOD_SLICEBATCH = "slicebatch";
    public static final String METHOD_SHORTBLOCK = "shortblock";
    public static final String METHOD_FINGERPRINT = "fingerprint";
    public static final String METHOD_BOTH = "both";
    public static final String METHOD_HASH = "hash";

    public static final String INDEX_AUTO = "auto";
    public static final String INDEX_SINGLE = "single";
    public static final String INDEX_FIXED = "fixed";

    public static final String BITRATE_FIRST = "first";
    public static final String BITRATE_HIGHEST = "highest";
    public static final String BITRATE_LOWEST = "lowest";

    @SerializedName("id")
    private String id;
    @SerializedName("name")
    private String name;
    @SerializedName("enabled")
    private boolean enabled = true;
    @SerializedName("domain")
    private String domain;
    @SerializedName("detectType")
    private String detectType = DETECT_BOTH;
    @SerializedName("detectMethod")
    private String detectMethod = METHOD_AUTO;
    @SerializedName("indexMode")
    private String indexMode = INDEX_AUTO;
    @SerializedName("bitrate")
    private String bitrate = BITRATE_FIRST;
    @SerializedName("fixedPath")
    private String fixedPath;
    @SerializedName("proxyUrl")
    private String proxyUrl;
    @SerializedName("cacheDir")
    private String cacheDir;
    @SerializedName("sortCode")
    private int sortCode;
    @SerializedName("urlPrefixes")
    private List<String> urlPrefixes = new ArrayList<>();
    @SerializedName("urlRegexes")
    private List<String> urlRegexes = new ArrayList<>();
    @SerializedName("fingerprints")
    private List<String> fingerprints = new ArrayList<>();
    @SerializedName("adRegexes")
    private List<String> adRegexes = new ArrayList<>();

    public static SiteRule create() {
        SiteRule rule = new SiteRule();
        rule.id = java.util.UUID.randomUUID().toString();
        return rule;
    }

    public String getId() { return id == null ? "" : id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name == null ? "" : name; }
    public void setName(String name) { this.name = name; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getDomain() { return domain == null ? "" : domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public String getDetectType() { return detectType == null ? DETECT_BOTH : detectType; }
    public void setDetectType(String detectType) { this.detectType = detectType; }

    public String getDetectMethod() { return detectMethod == null ? METHOD_AUTO : detectMethod; }
    public void setDetectMethod(String detectMethod) { this.detectMethod = detectMethod; }

    public String getIndexMode() { return indexMode == null ? INDEX_AUTO : indexMode; }
    public void setIndexMode(String indexMode) { this.indexMode = indexMode; }

    public String getBitrate() { return bitrate == null ? BITRATE_FIRST : bitrate; }
    public void setBitrate(String bitrate) { this.bitrate = bitrate; }

    public String getFixedPath() { return fixedPath == null ? "" : fixedPath; }
    public void setFixedPath(String fixedPath) { this.fixedPath = fixedPath; }

    public String getProxyUrl() { return proxyUrl == null ? "" : proxyUrl; }
    public void setProxyUrl(String proxyUrl) { this.proxyUrl = proxyUrl; }

    public String getCacheDir() { return cacheDir == null ? "" : cacheDir; }
    public void setCacheDir(String cacheDir) { this.cacheDir = cacheDir; }

    public int getSortCode() { return sortCode; }
    public void setSortCode(int sortCode) { this.sortCode = sortCode; }

    public List<String> getUrlPrefixes() { return urlPrefixes == null ? new ArrayList<>() : urlPrefixes; }
    public void setUrlPrefixes(List<String> urlPrefixes) { this.urlPrefixes = urlPrefixes == null ? new ArrayList<>() : urlPrefixes; }

    public List<String> getUrlRegexes() { return urlRegexes == null ? new ArrayList<>() : urlRegexes; }
    public void setUrlRegexes(List<String> urlRegexes) { this.urlRegexes = urlRegexes == null ? new ArrayList<>() : urlRegexes; }

    public List<String> getFingerprints() { return fingerprints == null ? new ArrayList<>() : fingerprints; }
    public void setFingerprints(List<String> fingerprints) { this.fingerprints = fingerprints == null ? new ArrayList<>() : fingerprints; }

    public List<String> getAdRegexes() { return adRegexes == null ? new ArrayList<>() : adRegexes; }
    public void setAdRegexes(List<String> adRegexes) { this.adRegexes = adRegexes == null ? new ArrayList<>() : adRegexes; }

    /** 站点路由：URL 是否命中本规则（前缀或正则任一命中即匹配）。 */
    public boolean matches(String url) {
        if (url == null) return false;
        String host = hostOf(url);
        for (String prefix : getUrlPrefixes()) {
            String p = prefix == null ? "" : prefix.trim();
            if (!p.isEmpty() && (host.startsWith(p) || host.contains(p))) return true;
        }
        for (String regex : getUrlRegexes()) {
            String r = regex == null ? "" : regex.trim();
            if (r.isEmpty()) continue;
            String target = (r.startsWith("^") || r.contains("://")) ? url : host;
            try {
                if (java.util.regex.Pattern.compile(r).matcher(target).find()) return true;
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    /** 特征码：解析 "3, 3, 3, 3" 为时长秒序列（可含小数）。 */
    public static List<Double> parseFingerprint(String group) {
        List<Double> values = new ArrayList<>();
        if (group == null) return values;
        for (String token : group.split("[,\\s]+")) {
            if (token.isEmpty()) continue;
            try {
                values.add(Double.parseDouble(token));
            } catch (Throwable ignored) {
            }
        }
        return values;
    }

    /** 特征码转 HLS 清洗规则：每组特征码生成一条「广告时长」规则（限定本站点域名）。 */
    public List<com.fongmi.android.tv.utils.HlsManifestCleaner.Rule> toFingerprintRules() {
        List<com.fongmi.android.tv.utils.HlsManifestCleaner.Rule> out = new ArrayList<>();
        String host = getDomain();
        if (host.isEmpty()) return out;
        for (String group : getFingerprints()) {
            List<Double> seq = parseFingerprint(group);
            if (seq.isEmpty()) continue;
            double min = seq.stream().mapToDouble(v -> v).min().orElse(0);
            double max = seq.stream().mapToDouble(v -> v).max().orElse(min);
            double pad = Math.max(0.5, max * 0.15);
            try {
                out.add(com.fongmi.android.tv.utils.HlsManifestCleaner.Rule.builder()
                        .playlistHostSuffixes(List.of(host))
                        .hostSuffixes(List.of(host))
                        .durationRange(Math.max(0, min - pad), max + pad)
                        .minimumSignals(1)
                        .build());
            } catch (Throwable ignored) {
            }
        }
        return out;
    }

    private static String hostOf(String url) {
        try {
            java.net.URI uri = java.net.URI.create(url);
            String host = uri.getHost();
            return host == null ? "" : host;
        } catch (Throwable e) {
            return "";
        }
    }
}
