# MXYS

> 基于 FongMi 开源项目二次开发的仓库（二开仓库）

## 📌 二开仓库说明

本仓库将 **FongMi** 在 GitHub 上的全部项目按分支镜像存放，用于在此基础之上进行二次开发（二开）。

- **`main` 分支**：二开主分支。本仓库的自有功能与代码均在此分支开发维护，**二开改动只允许提交到 main**。
- **其余分支**：FongMi 各上游项目的最新镜像分支，仅作功能参考与代码合并来源，**请勿直接修改这些分支**。
- 所有镜像分支通过 GitHub Actions **每日自动同步上游最新代码**（也可手动触发）。

## 📂 分支一览与用处（点击分支名可跳转）

### 🛠 自有开发 / 二开分支

| 分支 | 用处 | 说明 |
|---|---|---|
| [main](https://github.com/ssmhdssmhd/MXYS/tree/main) | 二开主分支 | 仓库说明、云编译 Workflow、发行登记；二开改动请提交到此分支 |
| [MXTV](https://github.com/ssmhdssmhd/MXYS/tree/MXTV) | **沫兮TVBox 当前云编基础** | 基于 FongMi/tangtv 全家桶（私有 Media3+mpv 已入库、可云编），电视(leanback)/手机(mobile) 双版本，云端构建发行版 |
| [KFTV](https://github.com/ssmhdssmhd/MXYS/tree/KFTV) | **沫兮TVBox 备用基础** | 基于 TVBoxOS（纯开源、可云编、体积小），电视/手机双版本，作为轻量可回退版留存 |

### 📦 FongMi 上游镜像分支（每日 04:00 UTC 自动同步，勿直接修改）

| 分支 | 上游项目 | 默认分支 | 用处说明 |
|---|---|---|---|
| [AutoClick](https://github.com/ssmhdssmhd/MXYS/tree/AutoClick) | FongMi/AutoClick | main | AccessibilityService 自动点击示例 |
| [BeiDou-ijl15](https://github.com/ssmhdssmhd/MXYS/tree/BeiDou-ijl15) | FongMi/BeiDou-ijl15 | BeiDou | 北斗定制分辨率客户端（v83 修改，fork） |
| [Box](https://github.com/ssmhdssmhd/MXYS/tree/Box) | FongMi/Box | main | Firebase 云存储使用示例 |
| [Brita](https://github.com/ssmhdssmhd/MXYS/tree/Brita) | FongMi/Brita | master | Brita 使用历史 |
| [Calculator](https://github.com/ssmhdssmhd/MXYS/tree/Calculator) | FongMi/Calculator | master | ksoap2 使用示例 |
| [CatVodSpider](https://github.com/ssmhdssmhd/MXYS/tree/CatVodSpider) | FongMi/CatVodSpider | main | 影视爬虫 Spider（接口/爬虫开发参考） |
| [Cosmic](https://github.com/ssmhdssmhd/MXYS/tree/Cosmic) | FongMi/Cosmic | master | MapleStory Global v83 服务端模拟器（fork） |
| [FFmpeg](https://github.com/ssmhdssmhd/MXYS/tree/FFmpeg) | FongMi/FFmpeg | master | FFmpeg 镜像（fork） |
| [FongMi](https://github.com/ssmhdssmhd/MXYS/tree/FongMi) | FongMi/FongMi | main | 作者主页仓库 |
| [Launcher](https://github.com/ssmhdssmhd/MXYS/tree/Launcher) | FongMi/Launcher | main | Android TV 桌面启动器示例 |
| [libplacebo](https://github.com/ssmhdssmhd/MXYS/tree/libplacebo) | FongMi/libplacebo | master | libplacebo 官方镜像（fork） |
| [media](https://github.com/ssmhdssmhd/MXYS/tree/media) | FongMi/media | release | 多媒体解码库（AndroidX Media3 / ExoPlayer fork） |
| [mpv](https://github.com/ssmhdssmhd/MXYS/tree/mpv) | FongMi/mpv | master | mpv 命令行媒体播放器（fork） |
| [mpv-android](https://github.com/ssmhdssmhd/MXYS/tree/mpv-android) | FongMi/mpv-android | master | 基于 libmpv 的 Android 播放器（fork） |
| [nodejs-mobile](https://github.com/ssmhdssmhd/MXYS/tree/nodejs-mobile) | FongMi/nodejs-mobile | main | Android/iOS 嵌入式 Node.js（fork） |
| [Painter](https://github.com/ssmhdssmhd/MXYS/tree/Painter) | FongMi/Painter | master | 绘画 |
| [PainterObject](https://github.com/ssmhdssmhd/MXYS/tree/PainterObject) | FongMi/PainterObject | master | 绘画对象 |
| [Release](https://github.com/ssmhdssmhd/MXYS/tree/Release) | FongMi/Release | fongmi | 上游发布版本（APK 等产物） |
| [Scan](https://github.com/ssmhdssmhd/MXYS/tree/Scan) | FongMi/Scan | main | 簡訊實聯制掃描 |
| [TV](https://github.com/ssmhdssmhd/MXYS/tree/TV) | FongMi/TV | fongmi | 影视TV 上游原版（盒端播放器，含私有 media3，云端不可自编，作参考） |
| [Xiaomi-Tools](https://github.com/ssmhdssmhd/MXYS/tree/Xiaomi-Tools) | FongMi/Xiaomi-Tools | main | 小米工具 |

## 🔄 自动同步更新

- GitHub Actions 工作流：[.github/workflows/sync-upstream.yml](.github/workflows/sync-upstream.yml)
- 每天 04:00 (UTC) 自动执行，也可在 Actions 页面手动 `Run workflow` 触发。
- 同步策略：仅当分支落后于上游时进行快进（fast-forward）更新；若分支被本地改动导致分叉，会跳过并提示，**绝不覆盖本地修改**。

## 🛠 二开流程建议

1. 影视二开请使用 **`KFTV`** 分支（基于 TVBoxOS，支持云端构建发行版）。
2. 在 KFTV 上进行修改开发
3. 按版本规则更新版本号与更新日志，提交并推送
4. 在 Actions 手动触发云编译，产物发布到 Releases 供下载

## 📦 发行版编译来源（根据哪个分支编译）

本仓库的发行版由 GitHub Actions 云端编译，**发行版对应的源码分支是固定的**，认准文件名即可知道来源：

| 发行版 | APK 命名 | 编译源码分支 | Workflow | 底座 |
|---|---|---|---|---|
| **沫兮TVBox（当前正式版）** | `MoxiTVBox.{版本}.{时间}-{mode}-arm64_v8a.apk` | **[MXTV](https://github.com/ssmhdssmhd/MXYS/tree/MXTV)** | [build-release-fongmi.yml](.github/workflows/build-release-fongmi.yml) | FongMi/tangtv（可云编全家桶，含 mpv/ffmpeg） |
| 沫兮TVBox（备用回退版） | `MoxiTVBox.{版本}.{时间}-{flavor}.apk` | **[KFTV](https://github.com/ssmhdssmhd/MXYS/tree/KFTV)** | [build-release-tvbox.yml](.github/workflows/build-release-tvbox.yml) | TVBoxOS（纯开源、体积小） |

> **当前正式发行版（v.0.0.3 起）编译自 `MXTV` 分支**；KFTV（TVBoxOS）作为轻量可回退版保留。下载/引用前请留意 Release 的 APK 命名确定来源分支。

## 🎬 KFTV 开发分支（影视二开）

仓库包含 **`KFTV`** 分支，用于影视二次开发，底座为 **TVBoxOS**（[q215613905/TVBoxOS](https://github.com/q215613905/TVBoxOS)），可纯开源云端构建：

- **为什么换 TVBoxOS**：原 FongMi/TV 依赖上游**私有的 Media3 播放器 AAR（lib-*.aar，不入库）**，云端无法自编。TVBoxOS 使用标准 Google ExoPlayer，全依赖来自 Maven，**无私有 AAR，可云端构建**。
- **构建变体（flavor）**：`leanback`（电视版）/ `mobile`（手机版），各含 armv7+arm64
- **应用名**：**沫兮TVBox**（电视版加载 TV 后缀）
- **支持覆盖更新**：同一签名 + 固定包名，升级可覆盖安装
- **云端发行**：GitHub Actions 手动触发构建，APK 命名 `MoxiTVBox.{版本号}.{时间}-{flavor}.apk`，发布到 **Releases** 供下载
- **正式签名**：keystore 已安全存放于仓库 Secrets，签名文件不入库
- 详见 KFTV 分支的 [README](https://github.com/ssmhdssmhd/MXYS/tree/KFTV/README.md)

> 注：`TV` / `CatVodSpider` / `Release` 等仍作为 FongMi 上游**镜像分支**保留（由 sync 自动同步），可作功能参考与合并来源。

## 版本

版本号规则：`v.0.0.1`，百位进一（如 `v.0.0.99` 之后为 `v.0.1.0`）

当前版本：**v.0.0.9**

## 📝 更新日志

### v.0.0.9 (2026-09-19)
- ✅ **修复开启直走接口后视频卡顿**：接口请求改为独立**短超时（5s）**，不再用播放器默认长超时；并加**失败熔断**——接口连续失败 3 次自动暂停 60s，期间直接走本地清洗。避免接口响应慢/不可用时每次起播都被拖住（此前最多等 30s 才回退）
- ✅ 版本 v0.0.9（versionCode 569），签名 v1+v2+v3 齐全
- 发布包：`MoxiTVBox.0.0.9.{时间}-leanback/mobile-arm64_v8a.apk`（[Releases](https://github.com/ssmhdssmhd/MXYS/releases) 下载）

### v.0.0.8 (2026-09-19)
- ✅ **直走接口支持 JSON 封装响应**：接口可直接返回 `#EXTM3U` 文本，也可返回 JSON（如 `{"code":200,"url":...}` 含 `url`/`link`/`playUrl`/`m3u8` 等字段）——App 会解析出清单地址并逐级拉取（最多 3 层）到真实 `#EXTM3U`；任一层失败自动回退本地 HLS 清洗。已用 `mxqcb.ssmhd.com/api/clean/` 对 `fengbao12` 链接实测通过（解析成功，933 片段可播放）
- ✅ 版本 v0.0.8（versionCode 568），签名 v1+v2+v3 齐全
- 发布包：`MoxiTVBox.0.0.8.{时间}-leanback/mobile-arm64_v8a.apk`（[Releases](https://github.com/ssmhdssmhd/MXYS/releases) 下载）

### v.0.0.7 (2026-09-19)
- ✅ **修复手机版新功能未生效**：v0.0.6 时「直走接口 / 资源站规则 / 在线测试 / 规则同步」只加了电视版(leanback)设置页，手机版(mobile)漏加 UI。本次在手机版「设置 → 去广告」补齐同一组功能，与电视版完全一致
- ✅ 版本 v0.0.7（versionCode 567），签名 v1+v2+v3 齐全
- 发布包：`MoxiTVBox.0.0.7.{时间}-leanback/mobile-arm64_v8a.apk`（[Releases](https://github.com/ssmhdssmhd/MXYS/releases) 下载）

### v.0.0.6 (2026-09-19)
- ✅ **版本统一**：消除 v0.0.4/v0.0.5 同日重复记录，版本链为 v0.0.3 → v0.0.5 → **v0.0.6**，与 Releases 发行版严格一一对应
- ✅ **新增「直走接口」**（设置 → 去广告 → 直走接口，默认**关闭**）：开启后所有 `.m3u8` 播放都交给配置的接口去广告，接口不可用时自动回退本地 HLS 清洗
- ✅ **新增「资源站规则」**（直走接口下方）：每站点可配置域名 / 检测解析类型（both·duration·hash·direct）/ 检测方式（auto·sequence·blockcut·slicebatch·shortblock·fingerprint·both·hash）/ 索引方式（auto·single·fixed）/ 码率选择（first·highest·lowest）/ 固定子路径 / 站点级代理 / URL 匹配前缀+正则 / 缓存子目录 / 排序码 / 广告时长特征码（多组）/ 广告过滤正则
- ✅ **新增「在线测试」**：输入视频链接自动下载 m3u8 与采样片段分析，生成候选资源站规则，可直接编辑/删除（分析需真实下载）
- ✅ **新增「规则同步」**：把资源站规则上传到公开仓库 `GZ/rules.json`（GitHub Contents API，仓库/Token 在设置内配置，不写死）
- 版本 v0.0.6（versionCode 566），签名 v1+v2+v3 齐全
- 发布包：`MoxiTVBox.0.0.6.{时间}-leanback/mobile-arm64_v8a.apk`（[Releases](https://github.com/ssmhdssmhd/MXYS/releases) 下载）

### v.0.0.5 (2026-09-19)
- ✅ 修复**安装包无法安装**问题：强制启用 **v1+v2+v3 签名**（`app/build.gradle`），兼容所有 Android 版本
- ✅ 修复中文环境下应用名显示为「湯影视」的问题：`app_name` 统一为 **沫兮TVBox / 沫兮TVBox-TV**
- ✅ 发行版整理：清理历史杂散 Release，只保留当前一个正式发行版
- ✅ README 新增「发行版编译来源」说明

### v.0.0.3 (2026-09-18)
- ✅ **切换底座为 FongMi/TV（可云编全家桶）**：采用 `alantang1977/tangtv`（FongMi 私有 Media3 `1.11.0-alpha01-fongmi` AAR + mpv 原生库 + 坑模块 AAR 均已入库），原生 leanback/mobile，**可直接云端编译**
- ✅ 应用名 **沫兮TVBox**（电视版 `沫兮TVBox-TV`）/ 包名 `com.moxi.tvbox.{tv,mobile}`，版本 v.0.0.3
- ✅ **FongMi/tangtv 0.0.3 云端编译成功并发布**：`MoxiTVBox.0.0.3.{时间}-{mode}-arm64_v8a.apk`（[Releases](https://github.com/ssmhdssmhd/MXYS/releases) 可下载，约158MB含完整mpv/ffmpeg）
- ✅ 云编环境：JDK21 / SDK37 / NDK28&29 / cmake3.22 / Python3.10(Chaquopy)；同名同签名可**覆盖更新**
- ✳️ 说明：旧 MXTV（FongMi/TV 原版）因私有 media3 扩展未入库无法云编，已被此可云编基础替换；TVBoxOS 底座仍保留于 KFTV 分支可回退

### v.0.0.2 (2026-09-18)
- ✅ **沫兮TVBox 0.0.2 云端编译成功并发布**：`MoxiTVBox.0.0.2.{时间}-leanback/mobile.apk`（[Releases](https://github.com/ssmhdssmhd/MXYS/releases) 可下载）
- ✅ 应用正式命名 **沫兮TVBox**，拆分为 **leanback（电视版）/ mobile（手机版）** 双版本
- ✅ 电视版锁定横屏、手机版自适应；应用名/包名区分（`com.moxi.tvbox.tv` / `com.moxi.tvbox.mobile`）
- ✅ 同一签名 + 固定包名，支持**覆盖更新**
- ✅ 移除 Python(pyramid) 引擎，缩小云编译不确定面
- 云编译 workflow 更新为默认构建 `leanback,mobile`，输出 `MoxiTVBox.{版本}.{时间}-{flavor}.apk`

### v.0.0.1 (2026-09-18)
- ✅ MXGTV 0.0.1 云端编译**成功**并发布：`MXGTV.0.0.1.{时间}-java/java32/java64.apk`（[Releases](https://github.com/ssmhdssmhd/MXYS/releases) 可下载）
- KFTV 二开底座由 FongMi/TV 切换为 **TVBoxOS**（解决私有 AAR 无法云编译问题）
- 修复签名变量命名冲突 & CI 缺 `local.properties` 导致的编译失败
- 云编译 workflow：构建 java/java32/java64，正式签名，发布 Releases

### v.0.0.1 (2026-09-17)
- 初始化二开仓库
- 拉取 FongMi 全部 21 个项目至独立镜像分支
- 添加 GitHub Actions 自动同步上游 workflow
- main 分支添加二开仓库说明
- 新增 KFTV 二开分支
