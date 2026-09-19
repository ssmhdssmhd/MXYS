package com.fongmi.android.tv.utils;

import android.text.TextUtils;

import com.fongmi.android.tv.setting.Setting;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.net.OkHttp;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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
 */
public final class DirectInterface {

    private static final String TAG = "direct-if";
    private static final int MAX_REDIRECT = 3;

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
        String separator = endpoint.contains("?") ? "&" : "?";
        String url = endpoint + separator + "url=" + URLEncoder.encode(playlistUrl, StandardCharsets.UTF_8);
        String text = httpGet(url);
        if (TextUtils.isEmpty(text)) {
            SpiderDebug.log(TAG, "interface http fail url=%s", shortUrl(playlistUrl));
            return null;
        }
        String playlist = resolveToPlaylist(text, url, 0);
        if (playlist != null) {
            SpiderDebug.log(TAG, "interface ok bytes=%s url=%s", playlist.length(), shortUrl(playlistUrl));
            return playlist;
        }
        SpiderDebug.log(TAG, "interface invalid playlist url=%s", shortUrl(playlistUrl));
        return null;
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

    private static String httpGet(String url) {
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "MoxiTVBox")
                .header("Accept", "application/vnd.apple.mpegurl")
                .build();
        try (Response response = OkHttp.player().newCall(request).execute()) {
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