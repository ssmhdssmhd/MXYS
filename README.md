# 影視TV · KFTV 二开分支

**KFTV 分支**为基于 [FongMi/TV](https://github.com/FongMi/TV) 的二次开发分支，适用于 Android TV（电视）与手机，整合媒体浏览与播放体验，支持外部配置与 [CatVod](https://github.com/CatVodTVOfficial/CatVodTVJarLoader) Spider 接口扩展。

**App 本身不内置或提供任何内容来源。** 外部内容需自行配置，也可开启本地媒体文件或推送媒体网址。

## 📌 二开仓库说明

- 本分支（`KFTV`）承载二次开发代码，**所有改造请提交到此分支**。
- 构建出的 APK 由 GitHub Actions **云端自动编译 + 正式签名**，发布到仓库 **Releases（发行版）** 页，方便直接下载安装。
- 编译变体：
  - **`leanback`** — 电视版
  - **`mobile`** — 手机版

## 📦 云端构建与发行（GitHub Actions）

仓库已配置工作流 [.github/workflows/build-release.yml](.github/workflows/build-release.yml)：

1. 打开仓库 **Actions → “构建发行版 (MXGTV)” → Run workflow**
2. 填写 **版本号**（例如 `0.0.1`、`5.14.4`，按规则百位进一）
3. 构建完成后自动在 **Releases** 生成发行版，APK 已正式签名可直接安装

APK 文件命名遵循发版规则：`MXGTV.{版本号} {构建时间}-{设备}.apk`

> 例如电视版：`MXGTV.0.0.1 202609170401-leanback.apk`（时间格式 `YYYYMMDDHHMM`，UTC）

## 🔐 签名说明（重要）

- Release 签名 keystore **已生成**并安全存放于仓库 **Settings → Secrets**（`KFTV_KEYSTORE`、`KFTV_STORE_PASS`、`KFTV_KEY_PASS`、`KFTV_KEY_ALIAS`），云端编译时自动调用。
- **签名文件与密码不会提交到代码仓库**，避免泄露。若要自行管理签名：
  - 生成 keystore：
    ```bash
    keytool -genkeypair -v -keystore sign/kftv-release.keystore -alias kftv -keyalg RSA -keysize 2048 -validity 8000 -storetype PKCS12
    ```
  - 在仓库 Settings → Secrets 配置上述 4 个密钥；或在本地 `app/../local.properties` 中配置
    ```properties
    storeFile=你的 keystore 绝对路径
    keyAlias=别名
    storePassword=密码
    ```
- 构建逻辑见 [app/build.gradle](app/build.gradle)：优先读取环境变量（云编译），其次 `local.properties`（本地）。

## ⚙️ 本地构建

前置环境：**JDK 21、Android SDK、Python 3.10**，配套 AAR 已随仓库提供（`app/libs/`）。

```bash
# 电视版
./gradlew :app:assembleLeanbackRelease
# 手机版
./gradlew :app:assembleMobileRelease
```

APK 输出目录：`Release/apk/`（含 `leanback-*.apk` 与 `mobile-*.apk`）。

若本地无签名配置，工作流会自动以可构建方式处理；正式发行请始终使用云端含签名的构建。

## 📝 更新日志

### v.0.0.1 (2026-09-17)
- 初始化 KFTV 二开分支（基于 FongMi/TV）
- 生成 Release 签名 keystore 并安全存入仓库 Secrets
- 添加 GitHub Actions 云端构建工作流，同时产出 `leanback`（电视）+ `mobile`（手机）版本
- APK 命名遵循 `MXGTV.{版本号} {时间}.apk` 规则，发布到 Releases 发行版