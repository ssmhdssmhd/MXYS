<?php
/**
 * html-relay.php — WebHTV 站点解析中转（单文件 PHP 版）
 *
 * 用途：
 *   部分资源站（如 svip.xgplay17.com）对"播放页"做 Cloudflare UA 校验，
 *   WebHTV 用默认 UA 直接抓播放页会被 302 拦截，导致"播放地址解析失败"。
 *   本文件用浏览器 UA 抓播放页 -> 提取真实 index.m3u8?sign=... 直链 -> 返回 JSON。
 *   m3u8 所在 CDN 不校验 UA，可正常播放。
 *
 * 部署：
 *   本文件零依赖、单文件，上传到任意支持 PHP 的主机/虚拟主机即可。
 *   目录名改为 index.php 则直接访问 <你的域名>/?url=<播放页URL>；
 *   不改名则可访问 <你的域名>/html-relay.php?url=<播放页URL>。
 *
 * 调用：
 *   GET  /?url=<播放页URL>(已URL编码)          -> 返回 JSON
 *   GET  /?url=<播放页URL>&cookie=<Cookie>      -> 可选携带 Cookie 抓页
 *   POST body: {"url":"<播放页URL>","cookie":"..."}
 *   OPTIONS                                     -> CORS 预检
 *
 * 返回体示例：
 *   {"ok":true,"url":"https://.../index.m3u8?sign=...","referer":"https://..."}
 *   失败时：{"ok":false,"url":"","referer":"","error":"原因"}
 */

error_reporting(E_ERROR | E_PARSE);
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET,POST,OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');
header('Cache-Control: no-store');

if (($_SERVER['REQUEST_METHOD'] ?? 'GET') === 'OPTIONS') {
    http_response_code(204);
    exit;
}

// 浏览器 UA（用于通过播放页的 Cloudflare UA 校验）
const BROWSER_UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36';

// 解析输入 URL：优先 GET ?url=，其次 POST JSON body
function readInput(): array {
    $url = $_GET['url'] ?? '';
    $cookie = $_GET['cookie'] ?? '';
    if ($url === '') {
        $raw = file_get_contents('php://input');
        if ($raw !== false) {
            $body = json_decode($raw, true);
            if (is_array($body)) {
                $url = $body['url'] ?? '';
                $cookie = $body['cookie'] ?? '';
            }
        }
    }
    // 兼容已编码与未编码
    $url = trim((string)$url);
    $decoded = urldecode($url);
    // 若传入的是明文 URL（含 ://），保持原样；否则用解码结果
    if (stripos($url, '://') !== false) {
        return ['url' => $url, 'cookie' => $cookie];
    }
    return ['url' => $decoded, 'cookie' => $cookie];
}

// 从播放页 HTML 提取 m3u8 相对路径
function extractRelativeM3u8Path(string $html): string {
    $patterns = array(
        '/const\s+url\s*=\s*["\']([^"\']*m3u8[^"\']*)["\']/',
        '/["\']url["\']\s*:\s*["\']([^"\']*m3u8[^"\']*)["\']/',
        '/(["\'])((?:https?:)?\/\/[^"\']*?m3u8[^"\']*)\1/',
        '/(["\'])(\/[^"\']*?m3u8[^"\']*)\1/',
    );
    foreach ($patterns as $pattern) {
        if (preg_match($pattern, $html, $m)) {
            $value = substr($m[0], -4, 4) === 'm3u8' ? $m[1] : ($m[2] ?? $m[1]);
            $value = trim((string)$value);
            if ($value !== '') return $value;
        }
    }
    return '';
}

// 相对路径 -> 绝对直链（基于页面最终域名）
function toAbsoluteUrl(string $baseUrl, string $path): string {
    if (preg_match('/^https?:\/\//i', $path)) return $path;
    $parsed = parse_url($baseUrl);
    if (!isset($parsed['scheme']) || !isset($parsed['host'])) return '';
    $base = $parsed['scheme'] . '://' . $parsed['host'];
    if (isset($parsed['port'])) $base .= ':' . $parsed['port'];
    if ($path !== '' && $path[0] === '/') return $base . $path;
    $dir = isset($parsed['path']) ? dirname((string)$parsed['path']) : '/';
    $dir = ($dir === '/' || $dir === '.') ? '/' : $dir . '/';
    return $base . $dir . $path;
}

// 用 cURL 抓播放页（follow 重定向）
function fetchPage(string $pageUrl, string $cookie = ''): array {
    $timeout = 15;
    $ch = curl_init();
    curl_setopt_array($ch, array(
        CURLOPT_URL => $pageUrl,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_FOLLOWLOCATION => true,
        CURLOPT_MAXREDIRS => 5,
        CURLOPT_TIMEOUT => $timeout,
        CURLOPT_CONNECTTIMEOUT => $timeout,
        CURLOPT_SSL_VERIFYPEER => false,
        CURLOPT_SSL_VERIFYHOST => false,
        CURLOPT_USERAGENT => BROWSER_UA,
        CURLOPT_HTTPHEADER => array(
            'Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
            'Accept-Language: zh-CN,zh;q=0.9,en;q=0.8',
            'Cache-Control: no-cache',
            'Upgrade-Insecure-Requests: 1',
        ),
    ));
    if ($cookie !== '') {
        curl_setopt($ch, CURLOPT_COOKIE, $cookie);
    }
    $html = curl_exec($ch);
    $status = (int)curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $effective = (string)curl_getinfo($ch, CURLINFO_EFFECTIVE_URL);
    $error = curl_error($ch);
    curl_close($ch);

    if ($html === false) {
        return array('error' => '抓取播放页失败: ' . ($error ?: 'cURL error'), 'status' => 0, 'effective' => '');
    }
    if ($status !== 200) {
        return array('error' => '播放页返回 HTTP ' . $status, 'status' => $status, 'effective' => $effective);
    }
    return array('html' => $html, 'status' => $status, 'effective' => $effective);
}

// 主流程
$input = readInput();
$pageUrl = $input['url'];
$cookie = $input['cookie'];

if ($pageUrl === '') {
    http_response_code(400);
    echo json_encode(array('ok' => false, 'url' => '', 'referer' => '', 'error' => '缺少 url 参数，例如 http://<域名>/?url=<播放页URL>'), JSON_UNESCAPED_UNICODE);
    exit;
}

if (!preg_match('#^https?://#i', $pageUrl)) {
    http_response_code(400);
    echo json_encode(array('ok' => false, 'url' => '', 'referer' => '', 'error' => 'url 参数必须是 http(s) 完整地址'), JSON_UNESCAPED_UNICODE);
    exit;
}

$fetched = fetchPage($pageUrl, $cookie);
if (isset($fetched['error'])) {
    http_response_code(502);
    echo json_encode(array('ok' => false, 'url' => '', 'referer' => '', 'error' => $fetched['error']), JSON_UNESCAPED_UNICODE);
    exit;
}

$html = $fetched['html'];
$finalUrl = $fetched['effective'] !== '' ? $fetched['effective'] : $pageUrl;

$relative = extractRelativeM3u8Path($html);
if ($relative === '') {
    http_response_code(502);
    echo json_encode(array(
        'ok' => false,
        'url' => '',
        'referer' => $finalUrl,
        'error' => '播放页中未找到 m3u8 直链',
    ), JSON_UNESCAPED_UNICODE);
    exit;
}

$directUrl = toAbsoluteUrl($finalUrl, $relative);
if ($directUrl === '') {
    http_response_code(502);
    echo json_encode(array('ok' => false, 'url' => '', 'referer' => $finalUrl, 'error' => 'm3u8 直链补全失败'), JSON_UNESCAPED_UNICODE);
    exit;
}

echo json_encode(array(
    'ok' => true,
    'url' => $directUrl,
    'referer' => $finalUrl,
), JSON_UNESCAPED_UNICODE);
exit;