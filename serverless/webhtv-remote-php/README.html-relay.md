# html-relay.php — WebHTV 站点解析中转（单文件 PHP 版）

与 [html-relay.js](https://github.com/ssmhdssmhd/MXYS/blob/MXTV/serverless/webhtv-remote-deno/README.html-relay.md)（Deno/Vercel/Cloudflare）功能一致，但**零依赖、单文件**，可直接部署到任意 PHP 虚拟主机，无需 Node / 无服务器平台。

## 背景

部分资源站（如 `svip.xgplay17.com`）对**播放页**做 Cloudflare UA 校验：WebHTV 用默认 UA 抓播放页被 302 拦截 → “播放地址解析失败”。本 PHP 用**浏览器 UA** 抓播放页，提取真实 `index.m3u8?sign=...` 直链返回 JSON。m3u8 所在 CDN 不校验 UA，可正常播放。

## 部署（两步）

1. 把 `html-relay.php` 上传到任意 PHP 主机的 web 目录（建议 PHP 7.0+，需要 cURL 扩展，虚拟主机普遍自带）。
2. 若希望用 `域名/?url=` 访问，把文件改名为 `index.php`；不改名则用 `/html-relay.php?url=`。

## 调用

```
GET /?url=<播放页URL>         # url 已做 URL 编码
GET /?url=<播放页URL>&cookie=<Cookie>  # 可选携带 Cookie
POST {"url":"...","cookie":"..."}
OPTIONS                        # CORS 预检
```

返回（成功）：

```json
{ "ok": true, "url": "https://.../index.m3u8?sign=...", "referer": "https://.../play/xxx" }
```

失败返回 `ok=false` 与 `error` 原因，HTTP 502。

## 在 WebHTV 中使用（JSON 解析，type=1）

配置一条 JSON 解析接口，解析地址指向本 PHP 并做 URL 拼接：

```
https://<你的域名>/index.php?url=
```

播放「播放地址」填播放页 URL（如 `https://svip.xgplay17.com/play/36542_...`），WebHTV 请求中转后取返回的 `url` 字段播放。

## 验证

直接浏览器打开（会返回 JSON）：

```
https://<你的域名>/index.php?url=https%3A%2F%2Fsvip.xgplay17.com%2Fplay%2F36542_Y_27TY7-ho7LH6p7Xm
```

## 注意

- `sign` 直链有时效，过期后重走一次中转（每次实时抓取）即可。
- 该站点 Cloudflare 仅做 UA 校验（无 JS challenge），无需无头浏览器，PHP 直接请求即可。
- 本文件不依赖服务器允许 `allow_url_fopen`，统一走 cURL。