# html-relay — WebHTV 站点解析中转

部分资源站（如 `svip.xgplay17.com`）对**播放页**做 Cloudflare UA 校验：WebHTV 用自身默认 UA 抓播放页时被 302 拦截，导致"播放地址解析失败"。但这类站的 m3u8 所在 CDN **不校验 UA**。

本中转用**浏览器 UA** 抓播放页 -> 提取真实 `index.m3u8?sign=...` 直链 -> 返回标准 JSON，供 WebHTV 以「JSON 解析接口」使用，从而绕过播放页的 Cloudflare 拦截。

## 返回体

```
GET/POST /m3u8?url=<播放页URL(百分号编码)>

{ "ok": true,
  "url": "https://.../index.m3u8?sign=...",
  "referer": "https://.../play/xxx" }
```

失败时 `ok=false`，`error` 给出原因，HTTP 502。

## 依赖

无。仅使用标准 `fetch / URL / Response`，可在 **Deno Deploy / Vercel / Cloudflare Workers** 任意 Edge 运行时运行。

## 部署

### 方式一：Deno Deploy（推荐）
`main.js` 已接入 `/m3u8` 与 `/api/html-relay` 两个路径。直接部署整个 `webhtv-remote-deno` 目录即可，无需额外配置。

### 方式二：Vercel
新建 `api/m3u8.js`：

```js
import { handleHtmlRelayRequest } from '../html-relay.js';
export default handleHtmlRelayRequest;
```

### 方式三：Cloudflare Workers
在 `wrangler.toml` 中把入口指向一个调用 `handleHtmlRelayRequest(request)` 的 handler 即可。

## 在 WebHTV 中使用（JSON 解析，type=1）

配置一条 JSON 解析接口，解析地址指向中转并进行 URL 拼接：

```
https://<你的中转域名>/m3u8?url=
```

它的已拼接请求形如 `https://<中转>/m3u8?url=<播放页URL>`，返回 `{ "url": "..." }`，WebHTV 会自动取 `url` 字段播放。

用法示例：播放该视频时，把「播放地址」填为本来的播放页 URL
`https://svip.xgplay17.com/play/36542_Y_27TY7-ho7LH6p7Xm`，经中转解析后即可直接播放。

## 注意

- 返回的直链带 `?sign=`，会按站点规则**过期**。过期后重走一次中转即可（每次中转都是实时抓取）。
- 该站点 Cloudflare 仅做 UA 校验（无 JS challenge），因此本中转无需无头浏览器即可工作。