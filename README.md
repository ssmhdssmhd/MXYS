# 影視TV · KFTV 二开分支（TVBoxOS 底座）

**KFTV 分支**为基于 **TVBoxOS**（[q215613905/TVBoxOS](https://github.com/q215613905/TVBoxOS)）的开源影视二次开发工程，同时适配 **Android TV** 与**手机**，支持各类爬虫源 / XML / JSON / jar / remote 接口、聚合模式、直播源等。

> 说明：因 FongMi/TV 依赖上游**私有的 Media3 播放器 AAR（lib-*.aar，不入库）**，导致云端无法自编。故本二开底座改用 **TVBoxOS** —— 它使用标准 Google ExoPlayer，**全部依赖来自 Maven，无私有 AAR，可纯开源云端构建**。

## 📌 二开仓库说明

- 本分支（`KFTV`）承载二次开发源码，**所有改造请提交到此分支**。
- 云编译由 GitHub Actions 完成，APK **正式签名**后发布到仓库 **Releases（发行版）** 页，方便直接下载安装。
- 支持 Flavor（按脚本引擎 / ABI）：
  - `java`（arm64 + armv7，Min SDK 19）
  - `java32`（仅 armeabi-v7a）
  - `java64`（仅 arm64-v8a，Min SDK 21）
  - `python*`（需 Python 环境，默认不构建）

## 📦 云端构建与发行（GitHub Actions）

工作流：`.github/workflows/build-release-tvbox.yml`（已放入默认分支 main，用于触发）

1. 仓库 **Actions → “构建发行版 MXGTV (TVBoxOS 底座)” → Run workflow**
2. 填写 **版本号**（例如 `0.0.1`，按规则百位进一）
3. 按需选择 **Flavor**（默认 `java64,java,java32`）
4. 构建完成自动在 **Releases** 生成发行版

APK 命名：`MXGTV.{版本号}.{构建时间}-{flavor}.apk`
> 例如：`MXGTV.0.0.1.202609180101-java64.apk`（时间 `YYYYMMDDHHMM`，UTC）

## 🔐 签名说明

- Release keystore 已生成并存放于仓库 **Secrets**（`KFTV_KEYSTORE` / `KFTV_STORE_PASS` / `KFTV_KEY_PASS` / `KFTV_KEY_ALIAS`），云编译自动调用。
- **签名文件与密码不入库**。本地构建可配置环境变量 `KFTV_KEYSTORE_FILE` 等，或在 `gradle.properties` 配置 `RELEASE_STORE_FILE` / `RELEASE_KEY_ALIAS` / `RELEASE_STORE_PASSWORD` / `RELEASE_KEY_PASSWORD`。

## ⚙️ 本地构建

前置：**JDK 11/17、Android SDK (platform 33)**（Gradle 7.5 / AGP 7.2.2）

```bash
# 构建 java64（arm64）
./gradlew :app:assembleJava64Release
# 构建全部 java flavor
./gradlew :app:assembleJavaRelease :app:assembleJava32Release :app:assembleJava64Release
```

APK 输出：`app/build/outputs/apk/<flavor>/release/TVBox_release-<flavor>.apk`

## 📝 更新日志

### v.0.0.1 (2026-09-18)
- 二开底座由 FongMi/TV 调整为 **TVBoxOS**（解决私有 AAR 无法云编译问题）
- 接入现有签名（Secrets）与云编译 Workflow，输出 `MXGTV.{版本}.{时间}-{flavor}.apk`
- 说明：`java` 为同时适配电视+手机的通用版，`java32`/`java64` 按位数区分