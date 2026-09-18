# 沫兮TVBox · KFTV 二开分支（TVBoxOS 底座）

**KFTV 分支**为基于 **TVBoxOS**（[q215613905/TVBoxOS](https://github.com/q215613905/TVBoxOS)）的开源影视二次开发工程，应用名为 **沫兮TVBox**，产物分 **电视版（leanback）+ 手机版（mobile）** 两个安装包，支持各类爬虫源 / XML / JSON / jar / remote 接口、聚合模式、直播源等。

> 说明：因 FongMi/TV 依赖上游**私有的 Media3 播放器 AAR（lib-*.aar，不入库）**，导致云端无法自编。故本二开底座改用 **TVBoxOS** —— 它使用标准 Google ExoPlayer，**全部依赖来自 Maven，无私有 AAR，可纯开源云端构建**。

## 📌 二开仓库说明

- 本分支（`KFTV`）承载二次开发源码，**所有改造请提交到此分支**。
- 云编译由 GitHub Actions 完成，APK **正式签名**后发布到仓库 **Releases（发行版）** 页，方便直接下载安装。
- **两个构建变体（UI 维度）**：
  - `leanback` → **电视版**：横屏盒子 UI，应用于机顶盒/TV，应用名 **沫兮TVBox-TV**，包名 `com.moxi.tvbox.tv`
  - `mobile` → **手机版**：自适应竖屏/横屏，应用于手机/平板，应用名 **沫兮TVBox**，包名 `com.moxi.tvbox.mobile`
  - 两版均含 `armeabi-v7a + arm64-v8a` 全 ABI，默认不构建 Python 脚本引擎。
- **覆盖更新**：同一签名 + 固定包名，升级时下载同名新版本即可覆盖安装（各有自己的版本链）。

## 📦 云端构建与发行（GitHub Actions）

工作流：`.github/workflows/build-release-tvbox.yml`（已放入默认分支 main，用于触发）

1. 仓库 **Actions → "构建发行版 沫兮TVBox (TVBoxOS 底座)" → Run workflow**
2. 填写 **版本号**（例如 `0.0.2`，按规则百位进一）
3. Flavor 默认 `leanback,mobile`（可只构建其一）
4. 构建完成自动在 **Releases** 生成发行版

APK 命名：`MoxiTVBox.{版本号}.{构建时间}-{flavor}.apk`
> 例如：`MoxiTVBox.0.0.2.202609181030-leanback.apk`（时间 `YYYYMMDDHHMM`，UTC）

## 🔐 签名说明

- Release keystore 已生成并存放于仓库 **Secrets**（`KFTV_KEYSTORE` / `KFTV_STORE_PASS` / `KFTV_KEY_PASS` / `KFTV_KEY_ALIAS`），云编译自动调用。
- **签名文件与密码不入库**。本地构建可配置环境变量 `KFTV_KEYSTORE_FILE` 等，或在 `gradle.properties` 配置 `RELEASE_STORE_FILE` / `RELEASE_KEY_ALIAS` / `RELEASE_STORE_PASSWORD` / `RELEASE_KEY_PASSWORD`。
- 同包名同签名即可实现**覆盖更新**。

## ⚙️ 本地构建

前置：**JDK 11/17、Android SDK (platform 33)**（Gradle 7.5 / AGP 7.2.2）

```bash
# 构建电视版
./gradlew :app:assembleLeanbackRelease
# 构建手机版
./gradlew :app:assembleMobileRelease
# 或一次构建全部
./gradlew :app:assembleLeanbackRelease :app:assembleMobileRelease
```

APK 输出：`app/build/outputs/apk/<flavor>/release/TVBox_release-<flavor>.apk`

## 📝 更新日志

### v.0.0.2 (2026-09-18)
- 应用正式命名 **沫兮TVBox**
- 拆分为 **leanback（电视版）/ mobile（手机版）** 两个变体（原 java/java32/java64 按 ABI 维度调整为用户 UI 维度）
- 电视版锁定横屏、手机版自适应；应用名与包名分别区分（`com.moxi.tvbox.tv` / `com.moxi.tvbox.mobile`）
- 同一签名 + 固定包名，支持**覆盖更新**
- 移除 Python(pyramid) 脚本引擎依赖，缩小云编译不确定面
- 云编译 workflow 更新，输出 `MoxiTVBox.{版本}.{时间}-{flavor}.apk`

### v.0.0.1 (2026-09-18)
- 二开底座由 FongMi/TV 调整为 **TVBoxOS**（解决私有 AAR 无法云编译问题）
- 接入现有签名（Secrets）与云编译 Workflow，输出 `MXGTV.{版本}.{时间}-{flavor}.apk`
- 说明：`java` 为同时适配电视+手机的通用版，`java32`/`java64` 按位数区分