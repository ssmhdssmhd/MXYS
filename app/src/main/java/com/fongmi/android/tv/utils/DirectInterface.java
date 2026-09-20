package com.fongmi.android.tv.utils;

import android.text.TextUtils;

import com.fongmi.android.tv.setting.Setting;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.net.OkHttp;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 直走接口（默认关闭）：开启后，所有 .m3u8 播放都转发给配置的接口地址，
 * 由接口完成去广告后返回清洗清单；接口不可用时自动回退本地 HLS 清洗。
 *
 * 兼容两种接口返回：
 * 1. 直接返回以 #EXTM3U 开头的 m3u8 文本 —— 直接使用；
 * 2. 返回 JSON 封装（如 {"code":200,"url":...,"link":...}）—— 解析出 url/link 等字段，
 *    逐级拉取其指向的清单（最多 3 层），直到得到 #EXTM3U 文本。任一层失败均回退本地清洗。
 *
 * 性能保护：接口请求使用独立短超时（5s），并带失败熔断——连续失败 3 次后暂停使用接口 60s，
 * 期间直接走本地清洗，避免接口慢/不可用时把每次播放起播都拖住。
 */
public final class DirectInterface {

    private static final String TAG = "direct-if";
    private static final int MAX_REDIRECT = 3;
    /** 接口请求独立超时（秒）：接口响应慢时不至于把播放起播拖住 30s（OkHttp.player 默认超时较长）。 */
    private static final int TIMEOUT_SEC = 5;
    /** 熔断：连续失败达到该次数后，暂停使用直走接口一段时间，期间直接走本地清洗。 */
    private static final int MAX_FAILS = 3;
    /** 熔断冷却时长（毫秒）。 */
    private static final long COOLDOWN_MS = 60_000L;

    private static volatile OkHttpClient sClient;
    private static final AtomicInteger sFailCount = new AtomicInteger(0);
    private static final AtomicLong sCooldownUntil = new AtomicLong(0L);
    private static final AtomicBoolean sReporting = new AtomicBoolean(false);

    private DirectInterface() {}

    public static boolean isEnabled() {
        return Setting.isDirectInterface();
    }

    public static String endpoint() {
        return Setting.getDirectInterfaceUrl();
    }

    /** 是否已配置可用接口。 */
    public static boolean isConfigured() {
        return !TextUtils.isEmpty(endpoint());
    }

    /**
     * 调用直走接口获取去广告后的清单文本。
     *
     * @param playlistUrl 原始 m3u8 URL
     * @return 接口返回的清单文本；未配置/网络失败/递归解析仍拿不到非清单内容返回 null（由调用方回退本地清洗）
     */
    public static String fetch(String playlistUrl) {
        if (!isEnabled() || TextUtils.isEmpty(playlistUrl)) return null;
        String endpoint = endpoint();
        if (TextUtils.isEmpty(endpoint)) return null;
        if (inCooldown()) return null;
        String separator = endpoint.contains("?") ? "&" : "?";
        String url = endpoint + separator + "url=" + URLEncoder.encode(playlistUrl, StandardCharsets.UTF_8);
        String text = httpGet(url);
        if (TextUtils.isEmpty(text)) {
            SpiderDebug.log(TAG, "interface http fail url=%s", shortUrl(playlistUrl));
            recordFail();
            return null;
        }
        String playlist = resolveToPlaylist(text, url, 0);
        if (playlist != null) {
            SpiderDebug.log(TAG, "interface ok bytes=%s url=%s", playlist.length(), shortUrl(playlistUrl));
            resetFails();
            return playlist;
        }
        SpiderDebug.log(TAG, "interface invalid playlist url=%s", shortUrl(playlistUrl));
        recordFail();
        return null;
    }

    /** 熔断：连续失败达到阈值后暂停使用接口一段时间，避免接口慢/不可用时每个播源都被拖住。 */
    private static boolean inCooldown() {
        long cooldown = sCooldownUntil.get();
        long now = System.currentTimeMillis();
        if (cooldown <= now) return false;
        if (sReporting.compareAndSet(false, true)) {
            SpiderDebug.log(TAG, "interface cooling down, skip until+%sms", cooldown - now);
            sReporting.set(false);
        }
        return true;
    }

    private static void recordFail() {
        int fails = sFailCount.incrementAndGet();
        if (fails >= MAX_FAILS) {
            sFailCount.set(0);
            sCooldownUntil.set(System.currentTimeMillis() + COOLDOWN_MS);
            SpiderDebug.log(TAG, "interface failed %s times, cooling down for %ss", MAX_FAILS, COOLDOWN_MS / 1000);
        }
    }

    private static void resetFails() {
        sFailCount.set(0);
        sCooldownUntil.set(0L);
    }

    /**
     * 把接口返回的文本解析成最终 m3u8 清单：直接为 #EXTM3U 即返回，否则尝试作为 JSON 逐级下钻。
     */
    private static String resolveToPlaylist(String text, String baseUrl, int depth) {
        if (depth > MAX_REDIRECT) return null;
        if (text != null && text.trim().startsWith("#EXTM3U")) return text;
        return resolveJson(text, baseUrl, depth);
    }

    /** 尝试把 JSON 里的缓存清单地址取出来递归拉取。 */
    private static String resolveJson(String text, String baseUrl, int depth) {
        String target;
        try {
            JSONObject obj = new JSONObject(text);
            target = firstNonEmpty(
                    obj.optString("url"),
                    obj.optString("link"),
                    obj.optString("playUrl"),
                    obj.optString("play_url"),
                    obj.optString("m3u8"),
                    obj.optString("playlist"));
        } catch (Throwable e) {
            SpiderDebug.log(TAG, "interface json parse fail base=%s err=%s", shortUrl(baseUrl), e.toString());
            return null;
        }
        if (TextUtils.isEmpty(target)) return null;
        String next = httpGet(target);
        if (TextUtils.isEmpty(next)) return null;
        try {
            return resolveToPlaylist(next, target, depth + 1);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static OkHttpClient client() {
        OkHttpClient client = sClient;
        if (client == null) {
            synchronized (DirectInterface.class) {
                client = sClient;
                if (client == null) {
                    client = OkHttp.player().newBuilder()
                            .connectTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
                            .readTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
                            .writeTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
                            .build();
                    sClient = client;
                }
            }
        }
        return client;
    }

    private static String httpGet(String url) {
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "MoxiTVBox")
                .header("Accept", "application/vnd.apple.mpegurl")
                .build();
        try (Response response = client().newCall(request).execute()) {
            if (!response.isSuccessful()) return null;
            ResponseBody body = response.body();
            return body == null ? null : body.string();
        } catch (IOException e) {
            SpiderDebug.log(TAG, "interface error %s url=%s", e.toString(), shortUrl(url));
            return null;
        } catch (Throwable e) {
            SpiderDebug.log(TAG, "interface throw %s", e.toString());
            return null;
        }
    }

    private static String firstNonEmpty(String... candidates) {
        for (String c : candidates) if (!TextUtils.isEmpty(c)) return c;
        return "";
    }

    private static String shortUrl(String url) {
        return url == null ? "" : (url.length() <= 160 ? url : url.substring(0, 160) + "...");
    }
}