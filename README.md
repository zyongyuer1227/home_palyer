# Home Player for Android 📺

![License](https://img.shields.io/badge/license-MIT-green.svg)
![Android](https://img.shields.io/badge/platform-Android-3DDC84.svg)
![Min SDK](https://img.shields.io/badge/min%20SDK-26-blue.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF.svg)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2025.01.01-4285F4.svg)
![Media3](https://img.shields.io/badge/Media3-1.5.1-orange.svg)
![Release](https://img.shields.io/badge/release-v1.0.0-informational.svg)

**Home Player 是 Web IPTV 服务的原生 Android 客户端**,专注手机端的直播观看、频道收藏与分组、NAS 网盘点播,以及清爽的播放器体验。

> **⚠️ 非独立应用**:Home Player 只是**前端客户端**。没有部署在你自己设备/NAS 上的 [Web IPTV](https://github.com/zyongyuer1227/web_iptv) 后端,它**无法独立工作**——它自身不抓取、不管理任何直播源。单独下载安装毫无意义,请**先部署并启动 Web IPTV 服务,再安装本 App**。

> **配套关系**:Home Player 与本仓库的开源服务端 [Web IPTV](https://github.com/zyongyuer1227/web_iptv) 配套使用,两者由**同一作者**开发,但**不一定同步发布**;发布信息请以作者在 [Web IPTV Releases](https://github.com/zyongyuer1227/web_iptv/releases) 的说明为准。

## 功能特性

- **多服务器配置** — 可配置多个后端服务器,并带健康检查。
- **直播观看** — 频道浏览、搜索、源过滤、收藏、自定义分组,以及 EPG 节目单查询。
- **NAS 网盘点播** — 目录浏览、画质选择、剧集列表与下一集连播。
- **Media3/ExoPlayer 播放** — 支持 HLS、直连、代理、中继、TS/fMP4 兜底与断流重连。
- **播放体验** — 竖屏播放、横屏全屏、状态栏安全区布局、静音、拖动进度条、亮度与音量手势。
- **本地排障日志** — 自动脱敏 URL/token/路径等敏感信息。

## 环境要求

- Android 8.0 及以上。
- 一个可用的 Web IPTV 后端服务。
- 从源码构建需要 Android Studio 或 JDK 17+。
- 需要安装 compile SDK 36 的 Android SDK。

## 构建

克隆仓库后用 Android Studio 打开,或命令行构建:

```bash
./gradlew :app:assembleDebug
```

Debug APK 输出到:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 发布构建(签名)

发布签名**刻意**只通过本地属性配置,不进入版本库。将 `local.properties.example` 复制为 `local.properties`,按你的机器和 keystore 修改配置后运行:

```bash
./gradlew :app:assembleRelease
```

Release APK 输出到:

```text
app/build/outputs/apk/release/app-release.apk
```

**不要**提交 `local.properties`、keystore。Release APK 由 CI 发版时自动附带(仓库根目录的 `app-release.apk` 为发布用签名包,由发布流程维护)。

## 后端接口

App 依赖的后端接口大致如下:

- `GET /api/health`
- `GET /api/sources`
- `GET /api/channels`
- `GET /api/channels/{id}/streaminfo`
- `GET /api/favorites`
- `POST /api/favorites`
- `DELETE /api/favorites/{id}`
- `GET /api/groups`
- `PUT /api/groups/{id}/items`
- `GET /api/nas/{sourceId}/browse`
- `GET /api/nas/{sourceId}/streaminfo`

服务器地址可在 App 内配置,例如 `http://192.168.1.100:8081` 或反向代理路径。

## 许可

本项目基于 **MIT License** 开源,详见 [LICENSE](LICENSE)。
