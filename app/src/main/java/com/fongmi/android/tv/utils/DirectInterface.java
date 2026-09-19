package com.fongmi.android.tv.utils;

import android.text.TextUtils;

import com.fongmi.android.tv.setting.Setting;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.net.OkHttp;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 直走接口（默认关闭）：开启后，所有 .m3u8 播放都转发给配置的接口地址，
 * 由接口完成去广告后返回清洗清单；接口不可用时自动回退本地 HLS 清洗。
 */
public final class DirectInterface {

    private static final String TAG = "direct-if";
    private static final int TIMEOUT_SEC = 20;

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
     * @return 接口返回的清单文本；未配置/网络失败/非清单内容返回 null（由调用方回退本地清洗）
     */
    public static String fetch(String playlistUrl) {
        if (!isEnabled() || TextUtils.isEmpty(playlistUrl)) return null;
        String endpoint = endpoint();
        if (TextUtils.isEmpty(endpoint)) return null;
        String separator = endpoint.contains("?") ? "&" : "?";
        String url = endpoint + separator + "url=" + URLEncoder.encode(playlistUrl, StandardCharsets.UTF_8);
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "MoxiTVBox")
                .header("Accept", "application/vnd.apple.mpegurl")
                .build();
        try (Response response = OkHttp.player().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                SpiderDebug.log(TAG, "interface http %s url=%s", response.code(), shortUrl(playlistUrl));
                return null;
            }
            ResponseBody body = response.body();
            if (body == null) return null;
            String text = body.string();
            if (TextUtils.isEmpty(text) || !text.trim().startsWith("#EXTM3U")) {
                SpiderDebug.log(TAG, "interface invalid playlist url=%s", shortUrl(playlistUrl));
                return null;
            }
            SpiderDebug.log(TAG, "interface ok bytes=%s url=%s", text.length(), shortUrl(playlistUrl));
            return text;
        } catch (IOException e) {
            SpiderDebug.log(TAG, "interface error %s url=%s", e.toString(), shortUrl(playlistUrl));
            return null;
        } catch (Throwable e) {
            SpiderDebug.log(TAG, "interface throw %s", e.toString());
            return null;
        }
    }

    private static String shortUrl(String url) {
        return url == null ? "" : (url.length() <= 160 ? url : url.substring(0, 160) + "...");
    }
}
