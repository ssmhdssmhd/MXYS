<?php
/**
 * mx.php
 * 沫兮 - 链接转发/解析接口
 *
 * 流程：
 * 1. 用户调用本文件
 * 2. 本文件先请求 http://114.134.184.91:9005/?url={url} 获取到新的 url
 * 3. 再请求 http://114.134.184.91:9005/mx.php?url={url} 获取最终返回内容
 * 4. 将最终返回内容原样输出
 */

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

// 从第一步返回中提取 url
$parsed_url = '';
if (is_array($step1_content)) {
    if (isset($step1_content['url'])) {
        $parsed_url = $step1_content['url'];
    } elseif (isset($step1_content['msg'])) {
        $parsed_url = $step1_content['msg'];
    }
} else {
    // 若返回的不是 JSON，尝试当作纯文本 url 处理
    $parsed_url = trim($step1_content['content'] ?? '');
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

// 第二步：把拿到的 url 传给第二个接口
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

// 将最终返回内容原样输出
echo $step2['content'];