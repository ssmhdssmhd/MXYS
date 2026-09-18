# MXYS

> 基于 FongMi 开源项目二次开发的仓库（二开仓库）

## 📌 二开仓库说明

本仓库将 **FongMi** 在 GitHub 上的全部项目按分支镜像存放，用于在此基础之上进行二次开发（二开）。

- **`main` 分支**：二开主分支。本仓库的自有功能与代码均在此分支开发维护，**二开改动只允许提交到 main**。
- **其余分支**：FongMi 各上游项目的最新镜像分支，仅作功能参考与代码合并来源，**请勿直接修改这些分支**。
- 所有镜像分支通过 GitHub Actions **每日自动同步上游最新代码**（也可手动触发）。

## 📂 分支一览（FongMi 全部项目，共 21 个）

| 分支 | 上游项目 | 上游默认分支 | 说明 |
|---|---|---|---|
| AutoClick | FongMi/AutoClick | main | AccessibilityService 自动点击示例 |
| BeiDou-ijl15 | FongMi/BeiDou-ijl15 | BeiDou | 北斗定制分辨率客户端（v83 修改，fork） |
| Box | FongMi/Box | main | Firebase 云存储使用示例 |
| Brita | FongMi/Brita | master | Brita 使用历史 |
| Calculator | FongMi/Calculator | master | ksoap2 使用示例 |
| CatVodSpider | FongMi/CatVodSpider | main | 影视爬虫 Spider |
| Cosmic | FongMi/Cosmic | master | MapleStory Global v83 服务端模拟器（fork） |
| FFmpeg | FongMi/FFmpeg | master | FFmpeg 镜像（fork） |
| FongMi | FongMi/FongMi | main | 作者主页仓库 |
| Launcher | FongMi/Launcher | main | Android TV 桌面启动器示例 |
| libplacebo | FongMi/libplacebo | master | libplacebo 官方镜像（fork） |
| media | FongMi/media | release | 多媒体解码库（fork） |
| mpv | FongMi/mpv | master | mpv 命令行媒体播放器（fork） |
| mpv-android | FongMi/mpv-android | master | 基于 libmpv 的 Android 播放器（fork） |
| nodejs-mobile | FongMi/nodejs-mobile | main | Android/iOS 嵌入式 Node.js（fork） |
| Painter | FongMi/Painter | master | 绘画 |
| PainterObject | FongMi/PainterObject | master | 绘画对象 |
| Release | FongMi/Release | fongmi | 发布版本（APK 等产物） |
| Scan | FongMi/Scan | main | 簡訊實聯制掃描 |
| TV | FongMi/TV | fongmi | 影视TV（盒端播放器） |
| Xiaomi-Tools | FongMi/Xiaomi-Tools | main | 小米工具 |

## 🔄 自动同步更新

- GitHub Actions 工作流：[.github/workflows/sync-upstream.yml](.github/workflows/sync-upstream.yml)
- 每天 04:00 (UTC) 自动执行，也可在 Actions 页面手动 `Run workflow` 触发。
- 同步策略：仅当分支落后于上游时进行快进（fast-forward）更新；若分支被本地改动导致分叉，会跳过并提示，**绝不覆盖本地修改**。

## 🛠 二开流程建议

1. 影视二开请使用 **`KFTV`** 分支（基于 TVBoxOS，支持云端构建发行版）。
2. 在 KFTV 上进行修改开发
3. 按版本规则更新版本号与更新日志，提交并推送
4. 在 Actions 手动触发云编译，产物发布到 Releases 供下载

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

当前版本：**v.0.0.3**

## 📝 更新日志

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
