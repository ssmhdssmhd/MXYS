<?php
/**
 * mxmx.php
 * 沫兮 - 链接转发/解析接口（完整版，独立于 mx.php）
 *
 * 与 mx.php 的区别：
 * - mx.php 仅走「官替系统」两步转发，遇到官替解析不出的站点会返回 500
 * - mxmx.php 在官替解析失败时，会直接抓取源播放页提取 m3u8 兜底（支持 JS 相对路径，自动补全站点域名）
 *
 * 流程：
 * 1. 用户调用本文件 mxmx.php?url={url}
 * 2. 先请求 http://114.134.184.91:9005/?url={url} 获取到新的 url
 * 3. 再把 url 传给 http://114.134.184.91:9005/mx.php?url={url}
 * 4. 若官替解析为空，直接抓取源播放页提取 m3u8（相对路径自动补全域名）
 * 5. 按固定格式输出 json
 */

// 记录脚本开始时间（用于计算耗时 time）
$start_time = microtime(true);

// 允许跨域
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

$request_method = isset($_SERVER['REQUEST_METHOD']) ? $_SERVER['REQUEST_METHOD'] : 'GET';
if ($request_method === 'OPTIONS') {
    http_response_code(200);
    exit;
}

// 第一步接口地址
$first_url = 'http://114.134.184.91:9005/?url=';

// 第二步接口地址
$second_url = 'http://114.134.184.91:9005/mx.php?url=';

// 用户传入的源 url
$input_url = isset($_GET['url']) ? trim($_GET['url']) : '';

/**
 * 发起 HTTP 请求获取内容
 */
function http_get($url) {
    $ch = curl_init($url);
    curl_setopt_array($ch, array(
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_HEADER => false,
        CURLOPT_FOLLOWLOCATION => true,
        CURLOPT_TIMEOUT => 30,
        CURLOPT_CONNECTTIMEOUT => 10,
        CURLOPT_SSL_VERIFYPEER => false,
        CURLOPT_SSL_VERIFYHOST => false,
        CURLOPT_USERAGENT => 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    ));
    $result = curl_exec($ch);
    if ($result === false) {
        $error = curl_error($ch);
        curl_close($ch);
        return array(
            'success' => false,
            'error' => $error
        );
    }
    curl_close($ch);
    return array(
        'success' => true,
        'content' => $result
    );
}

// 缺少 url 参数，直接返回错误
if (empty($input_url)) {
    echo json_encode(array(
        'code' => 400,
        'msg' => '缺少 url 参数',
        'url' => '',
        'time' => 0,
    ), JSON_UNESCAPED_SLASHES);
    exit;
}

/**
 * 直接抓取源播放页提取可播放的 m3u8 地址
 * 支持绝对地址，也支持 JS 里的相对路径（相对路径自动补全站点域名）
 */
function scrape_m3u8($url) {
    $resp = http_get($url);
    if (!$resp['success']) {
        return '';
    }
    $html = $resp['content'];
    // 1) 绝对地址 m3u8
    if (preg_match('#https?://[^"\'<>\s]+\.m3u8[^"\'<>\s]*#i', $html, $m)) {
        return trim($m[0]);
    }
    // 2) JS/页面中的相对路径（如 const url = "/2025-07-28/xxx/index.m3u8?sign=..."）
    if (preg_match('#(["\'])(/[^"\']*\.m3u8[^"\']*)\1#i', $html, $m)) {
        $parts = parse_url($url);
        $scheme = isset($parts['scheme']) ? $parts['scheme'] : 'https';
        $host = isset($parts['host']) ? $parts['host'] : '';
        if ($host !== '') {
            return trim($scheme . '://' . $host . $m[2]);
        }
    }
    return '';
}

// 第一步：先调用第一个接口，把用户 url 传进去
$step1 = http_get($first_url . urlencode($input_url));

if (!$step1['success']) {
    // 第一步失败
    echo json_encode(array(
        'code' => 500,
        'msg' => 'step1 request failed: ' . $step1['error'],
        'url' => '',
        'time' => 0,
    ), JSON_UNESCAPED_SLASHES);
    exit;
}

$step1_content = json_decode($step1['content'], true);

// 从第一步返回中提取 url 和来源 source
$parsed_url = '';
$source = $input_url;
if (is_array($step1_content)) {
    if (isset($step1_content['url'])) {
        $parsed_url = $step1_content['url'];
    } elseif (isset($step1_content['msg'])) {
        $parsed_url = $step1_content['msg'];
    }
    if (isset($step1_content['source']) && !empty($step1_content['source'])) {
        $source = $step1_content['source'];
    }
} else {
    // 若返回的不是 JSON，尝试当作纯文本 url 处理
    $parsed_url = trim($step1_content['content'] ?? '');
}

// 官替系统没解析出来时，直接抓取源播放页提取 m3u8 兜底
if (empty($parsed_url)) {
    $parsed_url = scrape_m3u8($input_url);
}

if (empty($parsed_url)) {
    echo json_encode(array(
        'code' => 500,
        'msg' => 'no url found in step1 response',
        'url' => '',
        'time' => 0,
    ), JSON_UNESCAPED_SLASHES);
    exit;
}

// 已拿到可直接播放的 m3u8 时，跳过第二步直接返回
if (stripos($parsed_url, '.m3u8') !== false) {
    $elapsed_ms = round((microtime(true) - $start_time) * 1000, 1);
    echo json_encode(array(
        'code' => 200,
        'msg' => $parsed_url,
        'url' => $parsed_url,
        'time' => $elapsed_ms,
        'KFZ' => '沫兮官替系统',
        'source' => $source,
    ), JSON_UNESCAPED_SLASHES);
    exit;
}

// 第二步：把拿到的官替 url 传给第二个接口（跳转获取可播放地址）
$step2 = http_get($second_url . urlencode($parsed_url));

if (!$step2['success']) {
    echo json_encode(array(
        'code' => 500,
        'msg' => 'step2 request failed: ' . $step2['error'],
        'url' => '',
        'time' => 0,
    ), JSON_UNESCAPED_SLASHES);
    exit;
}

// 从第二步返回中提取可播放的 m3u8 流地址
$stream_url = $parsed_url; // 兜底：拿不到时用官替地址
$step2_content = json_decode($step2['content'], true);
if (is_array($step2_content) && isset($step2_content['url']) && !empty($step2_content['url'])) {
    $stream_url = $step2_content['url'];
}
// 第二步仍拿不到可播放地址时，最后直接抓取源播放页兜底
if (stripos($stream_url, '.m3u8') === false) {
    $direct = scrape_m3u8($input_url);
    if ($direct !== '') {
        $stream_url = $direct;
    }
}

// 计算整体耗时（毫秒）
$elapsed_ms = round((microtime(true) - $start_time) * 1000, 1);

// 最终输出：m3u8 流地址填入 msg 和 url 字段（示例格式）
echo json_encode(array(
    'code' => 200,
    'msg' => $stream_url,
    'url' => $stream_url,
    'time' => $elapsed_ms,
    'KFZ' => '沫兮官替系统',
    'source' => $source,
), JSON_UNESCAPED_SLASHES);
