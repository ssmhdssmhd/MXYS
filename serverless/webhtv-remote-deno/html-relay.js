// html-relay.js — WebHTV 站点 JSON 中转函数
//
// 用途：
//   部分资源站（如 svip.xgplay17.com）对"播放页"做 Cloudflare UA 校验，
//   直接抓页会 302 拦截，导致 WebHTV 解析不到 m3u8 直链。
//   本中转用浏览器 UA 抓播放页，从页面里提取真实 m3u8 直链并返回 JSON，
//   供 WebHTV 以 type=1 JSON 解析接口使用。m3u8 所在 CDN 不校验 UA，可正常播放。
//
// 跨平台：
//   本文件只依赖标准 fetch / URL / Response，可在 Deno Deploy、Vercel、
//   Cloudflare Workers 等任何支持 fetch 的 Edge 运行时直接运行。
//   入口见文末 `export const handleHtmlRelayRequest`；各平台接入见 README.html-relay.md。

// 抓页时使用的浏览器 UA，用于通过播放页的 Cloudflare UA 校验。
const BROWSER_UA =
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36';

// 从播放页 HTML 中提取 m3u8 直链的优先级列表。
// xgplay 系列页内形如 `const url = "/2026-08-22/xxx/index.m3u8?sign=..."`。
// 逐条尝试，解出的是相对路径（不含域名），后续用页面真实域名补全。
function extractRelativeM3u8Path(html) {
  const patterns = [
    /const\s+url\s*=\s*["']([^"']*m3u8[^"']*)["']/g,
    /["']url["']\s*:\s*["']([^"']*m3u8[^"']*)["']/g,
    /(["'])((?:https?:)?\/\/[^"']*?m3u8[^"']*)\1/g,
    /(["'])(\/[^"']*?m3u8[^"']*)\1/g,
  ];
  for (const pattern of patterns) {
    const match = pattern.exec(html);
    if (match) {
      const value = match[1].endsWith('m3u8') ? match[1] : match[2] || match[1];
      if (value) return value.trim();
    }
  }
  return '';
}

// 把页面里提取的 m3u8 相对路径，基于页面/脚本所在目录补成绝对直链。
function toAbsoluteUrl(baseUrl, path) {
  // 已经是完整 URL（含协议）则原样返回
  if (/^https?:\/\//i.test(path)) return path;
  try {
    return new URL(path, baseUrl).toString();
  } catch {
    return '';
  }
}

// 去掉 m3u8 直链上的 hash 干扰项之外的参数修正：除了 sign 保留，其余 query 部分原样带出。
// 说明：sign/hash 是站点防盗链签名，必须原样保留，播放器才能取到分片。

/**
 * 核心处理逻辑：抓播放页 -> 提取 m3u8 -> 返回标准 JSON。
 * @param {string} pageUrl 播放页 URL（如 https://svip.xgplay17.com/play/xxx）
 * @param {{timeoutMs?:number, cookie?:string}} [options]
 * @returns {Promise<{ok:boolean, url:string, referer:string, error?:string, debug?:object}>}
 */
export async function resolveM3u8FromPage(pageUrl, options = {}) {
  const timeoutMs = options.timeoutMs || 15000;
  const headers = {
    'User-Agent': BROWSER_UA,
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
    'Accept-Language': 'zh-CN,zh;q=0.9,en;q=0.8',
    'Upgrade-Insecure-Requests': '1',
    'Cache-Control': 'no-cache',
  };
  if (options.cookie) headers['Cookie'] = options.cookie;

  let response;
  try {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeoutMs);
    try {
      response = await fetch(pageUrl, {
        headers,
        redirect: 'follow',
        signal: controller.signal,
      });
    } finally {
      clearTimeout(timer);
    }
  } catch (e) {
    return {
      ok: false,
      url: '',
      referer: '',
      error: `抓取播放页失败: ${e && e.message ? e.message : String(e)}`,
    };
  }

  if (!response.ok) {
    return {
      ok: false,
      url: '',
      referer: '',
      error: `播放页返回 HTTP ${response.status}${response.url ? ' (' + response.url + ')' : ''}`,
    };
  }

  let html = '';
  try {
    html = await response.text();
  } catch (e) {
    return { ok: false, url: '', referer: '', error: '读取播放页内容失败' };
  }

  // 用最终实际域名（可能经过重定向）作为 base，补全相对 m3u8 路径
  const finalUrl = response.url || pageUrl;
  const relative = extractRelativeM3u8Path(html);
  if (!relative) {
    return {
      ok: false,
      url: '',
      referer: finalUrl,
      error: '播放页中未找到 m3u8 直链',
      debug: { htmlLength: html.length },
    };
  }

  const directUrl = toAbsoluteUrl(finalUrl, relative);
  if (!directUrl) {
    return { ok: false, url: '', referer: finalUrl, error: 'm3u8 直链补全失败', debug: { relative } };
  }

  return {
    ok: true,
    url: directUrl,
    referer: finalUrl,
    debug: { relative },
  };
}

// 标准 WebHTV 解析返回体：直接返回直链即可，referer/ua 供需要校验的源使用。
const RESPONSE_HEADERS = {
  'content-type': 'application/json; charset=utf-8',
  'cache-control': 'no-store',
  'access-control-allow-origin': '*',
  'access-control-allow-methods': 'GET,POST,OPTIONS',
  'access-control-allow-headers': 'content-type,authorization',
};

function json(data, status = 200) {
  return new Response(JSON.stringify(data), { status, headers: RESPONSE_HEADERS });
}

function percentDecodeSource(value) {
  if (!value) return '';
  try {
    return decodeURIComponent(value);
  } catch {
    return value;
  }
}

/**
 * HTTP 入口。兼容两种传参方式：
 *  - GET/POST `?url=<播放页URL>`（所给 URL 已做百分号编码）
 *  - POST JSON body `{"url": "<播放页URL>"}`
 * 返回体示例：{"ok":true,"url":"https://.../index.m3u8?sign=...","referer":"https://..."}
 */
export async function handleHtmlRelayRequest(request) {
  if (request.method === 'OPTIONS') {
    return new Response(null, { status: 204, headers: RESPONSE_HEADERS });
  }

  let pageUrl = '';
  let cookie = '';
  try {
    const url = new URL(request.url);
    pageUrl = percentDecodeSource(url.searchParams.get('url') || '');
  } catch {
    /* ignore */
  }

  if (!pageUrl) {
    // 尝试从 POST body 读取
    try {
      const body = await request.json();
      pageUrl = String(body.url || body.pageUrl || '');
      cookie = String(body.cookie || '');
    } catch {
      /* ignore */
    }
  }

  if (!pageUrl) {
    return json({ ok: false, url: '', referer: '', error: '缺少 url 参数，例如 http://<host>/?url=<播放页URL>' }, 400);
  }

  const result = await resolveM3u8FromPage(pageUrl, { cookie });
  return json(result, result.ok ? 200 : 502);
}