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

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 特殊播放（默认关闭）：播放设置里可配置接口，开启后对播放请求先经
 * 「接口 + 视频链接」解析，接口返回的 url 作为实际播放地址（通常是一个
 * 网页播放器地址，由调用方用 WebView 打开）。任何失败自动回退原播放流程。
 *
 * 性能保护：接口请求使用独立短超时（5s），并带失败熔断——连续失败 3 次后
 * 暂停使用接口 60s，避免接口慢/不可用时把每次播放都拖住。
 */
public final class SpecialPlay {

    private static final String TAG = "special-play";
    private static final int TIMEOUT_SEC = 5;
    private static final int MAX_FAILS = 3;
    private static final long COOLDOWN_MS = 60_000L;

    private static volatile OkHttpClient sClient;
    private static int failCount;
    private static long cooldownUntil;

    private SpecialPlay() {}

    public static boolean isEnabled() {
        return Setting.isSpecialPlay();
    }

    public static String endpoint() {
        return Setting.getSpecialPlayUrl();
    }

    public static boolean isConfigured() {
        return !TextUtils.isEmpty(endpoint());
    }

    /**
     * 调用特殊接口解析并返回实际播放地址。
     *
     * @param playUrl 待播放的视频链接
     * @return 接口返回的播放地址（http 网页播放器 / m3u8 等）；未配置、失败或熔断中返回 null
     */
    public static String parse(String playUrl) {
        if (!isEnabled() || TextUtils.isEmpty(playUrl)) return null;
        String endpoint = endpoint();
        if (TextUtils.isEmpty(endpoint)) return null;
        if (inCooldown()) return null;
        // 接口地址若本身已带空 url= 参数，直接拼值即可，避免再拼 &url= 产生重复的空 url 覆盖真实链接
        String separator = endpoint.contains("?") ? "&" : "?";
        String url = endpoint.endsWith("url=")
                ? endpoint + URLEncoder.encode(playUrl, StandardCharsets.UTF_8)
                : endpoint + separator + "url=" + URLEncoder.encode(playUrl, StandardCharsets.UTF_8);
        String text = httpGet(url);
        String resolved = TextUtils.isEmpty(text) ? null : resolve(text);
        if (resolved != null) {
            resetFails();
            SpiderDebug.log(TAG, "ok url=%s", shortUrl(resolved));
            return resolved;
        }
        if (TextUtils.isEmpty(text)) SpiderDebug.log(TAG, "http fail url=%s", shortUrl(playUrl));
        recordFail();
        return null;
    }

    /** 直接返回 JSON 里的 url/link 字段，或直接是 http 地址则原样返回。 */
    private static String resolve(String text) {
        String t = text.trim();
        // 直接是 http 地址
        if (t.startsWith("http")) return t;
        // mm3u8 明文
        if (t.startsWith("#EXTM3U")) return t;
        try {
            JSONObject obj = new JSONObject(t);
            String target = firstNonEmpty(
                    obj.optString("url"),
                    obj.optString("link"),
                    obj.optString("playUrl"),
                    obj.optString("play_url"),
                    obj.optString("m3u8"));
            if (!TextUtils.isEmpty(target) && target.startsWith("http")) return target;
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static boolean inCooldown() {
        long now = System.currentTimeMillis();
        if (cooldownUntil <= now) return false;
        SpiderDebug.log(TAG, "cooling down, skip for %sms", cooldownUntil - now);
        return true;
    }

    private static synchronized void recordFail() {
        failCount++;
        if (failCount >= MAX_FAILS) {
            failCount = 0;
            cooldownUntil = System.currentTimeMillis() + COOLDOWN_MS;
            SpiderDebug.log(TAG, "failed %s times, cool down %ss", MAX_FAILS, COOLDOWN_MS / 1000);
        }
    }

    private static synchronized void resetFails() {
        failCount = 0;
        cooldownUntil = 0L;
    }

    private static OkHttpClient client() {
        OkHttpClient client = sClient;
        if (client == null) {
            synchronized (SpecialPlay.class) {
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
                .header("Accept", "application/json, application/vnd.apple.mpegurl")
                .build();
        try (Response response = client().newCall(request).execute()) {
            if (!response.isSuccessful()) return null;
            ResponseBody body = response.body();
            return body == null ? null : body.string();
        } catch (IOException e) {
            SpiderDebug.log(TAG, "error %s", e.toString());
            return null;
        } catch (Throwable e) {
            SpiderDebug.log(TAG, "throw %s", e.toString());
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