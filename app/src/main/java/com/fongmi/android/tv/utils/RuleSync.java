package com.fongmi.android.tv.utils;

import android.text.TextUtils;

import com.fongmi.android.tv.setting.Setting;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.net.OkHttp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import org.json.JSONObject;

/**
 * 资源站规则远程同步：把规则 JSON 上传到公开仓库的 GZ 文件夹（GitHub Contents API）。
 * 仓库地址与 Token 在设置中配置（不硬编码、不入库）。
 */
public final class RuleSync {

    private static final String TAG = "rule-sync";
    private static final String DEFAULT_PATH = "GZ/rules.json";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private RuleSync() {}

    public static boolean isConfigured() {
        String repo = Setting.getRuleSyncRepo();
        String token = Setting.getRuleSyncToken();
        return !TextUtils.isEmpty(repo) && repo.contains("/") && !TextUtils.isEmpty(token);
    }

    /**
     * 上传规则 JSON 到 {repo}/GZ/rules.json。
     *
     * @return null=成功；否则返回错误信息
     */
    public static String push(String rulesJson) {
        String repo = Setting.getRuleSyncRepo().trim();
        String token = Setting.getRuleSyncToken().trim();
        if (repo.isEmpty() || token.isEmpty()) return "未配置同步仓库或 Token";
        String path = Setting.getRuleSyncPath();
        if (TextUtils.isEmpty(path)) path = DEFAULT_PATH;
        try {
            String api = "https://api.github.com/repos/" + repo + "/contents/" + path;
            String existingSha = existingSha(api, token);
            JSONObject body = new JSONObject();
            body.put("message", "sync site rules from MoxiTVBox");
            body.put("content", Base64.getEncoder().encodeToString(rulesJson.getBytes(StandardCharsets.UTF_8)));
            if (existingSha != null) body.put("sha", existingSha);
            Request.Builder builder = new Request.Builder()
                    .url(api)
                    .header("Authorization", "token " + token)
                    .header("Accept", "application/vnd.github+json")
                    .put(RequestBody.create(body.toString(), JSON));
            try (Response response = OkHttp.client().newCall(builder.build()).execute()) {
                if (response.isSuccessful()) {
                    SpiderDebug.log(TAG, "pushed to %s", api);
                    return null;
                }
                String detail = response.body() == null ? "" : response.body().string();
                return "GitHub " + response.code() + " " + detail;
            }
        } catch (Throwable e) {
            SpiderDebug.log(TAG, "push error %s", e.toString());
            return String.valueOf(e.getMessage());
        }
    }

    private static String existingSha(String api, String token) throws IOException {
        Request request = new Request.Builder()
                .url(api)
                .header("Authorization", "token " + token)
                .header("Accept", "application/vnd.github+json")
                .get()
                .build();
        try (Response response = OkHttp.client().newCall(request).execute()) {
            if (!response.isSuccessful()) return null;
            ResponseBody body = response.body();
            if (body == null) return null;
            String text = body.string();
            try {
                JSONObject obj = new JSONObject(text);
                String sha = obj.optString("sha", null);
                return TextUtils.isEmpty(sha) ? null : sha;
            } catch (Throwable e) {
                return null;
            }
        }
    }
}
