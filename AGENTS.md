# AGENTS.md — IPTVPlayer 项目交接文档

> 本文件用于跨会话续接开发。新会话先读本文件即可掌握项目全貌与续接点。

## 1. 项目是什么

面向 Web IPTV 后端(`/opt/web_iptv/server`, Linux, 默认端口 8081)的**原生 Android 播放端**。
技术栈:**Kotlin + Jetpack Compose + Media3(ExoPlayer)**。纯客户端,不含源管理。
完整需求见上级目录 `../ANDROID_APP_SPEC.md`。

- 项目路径:`C:\Users\zyongyuer\Desktop\web_iptv\IPTVPlayer`
- 包名:`com.iptv.player`,minSdk 26 / targetSdk 34 / compileSdk 36
- 三块核心能力:直播(浏览/搜索/收藏/分组/EPG/播放)、网盘点播(目录→画质→播放)、连接管理(多服务器)

## 2. 构建环境(Windows 上的关键前提,别搞错)

- **JDK**:系统 `JAVA_HOME` 是 JDK 8,不能用。必须用 Android Studio 自带的 JBR(JDK 21):
  `D:\Program Files\Android\Android Studio\jbr`
- **Android Studio** 装在 `D:\Program Files\Android\Android Studio`
- **Android SDK**:`C:\Users\zyongyuer\AppData\Local\Android\Sdk`(local.properties 已指向)
- **Gradle**:wrapper 8.14.5,`gradle-wrapper.properties` 的 distributionUrl 指向**腾讯镜像**
  `https://mirrors.cloud.tencent.com/gradle/gradle-8.14.5-bin.zip`

### 构建命令
```powershell
$env:JAVA_HOME="D:\Program Files\Android\Android Studio\jbr"
cd C:\Users\zyongyuer\Desktop\web_iptv\IPTVPlayer
.\gradlew.bat :app:assembleDebug
```
- APK 产物:`app\build\outputs\apk\debug\app-debug.apk`
- 增量构建约 25~40s;首次/换 Gradle 版本会重新下载依赖(走阿里云镜像,可用)。

### 国内镜像(已配置,勿删)
- 全局 Gradle init 脚本:`C:\Users\zyongyuer\.gradle\init.d\mirror.init.gradle`(阿里云 public/google/gradle-plugin/central,作用于 Android Studio 打开的所有项目)
- 项目内 `settings.gradle.kts`:阿里云 google + central
- `gradle.properties` 含 `android.builder.sdkDownload=false`(禁止 AGP 自动去 dl.google.com 下 SDK,否则会卡死)
- SDK 平台/组件如需安装,用 `sdkmanager --repository_url=https://mirrors.cloud.tencent.com/AndroidSDK/` 或直接下载腾讯镜像 zip 手动解压

### 常见坑
1. **Android Studio 开着并正在 sync 时,命令行构建会卡死**(抢 Gradle 锁/文件锁)。
   解决:杀掉 AS 的 gradle 守护进程
   `Get-CimInstance Win32_Process | ? {$_.Name -eq "java.exe"} | % { Stop-Process -Id $_.ProcessId -Force}`
   或把构建放后台(Start-Process + 日志重定向)等 AS 空闲。
2. **`Could not read workspace metadata from ...\transforms\...`** = 上次构建被强杀导致缓存损坏。
   解决:停守护进程后删除 `~/.gradle\caches\<版本>\transforms` 和 `fileHashes`,重跑。
3. 用 `.\gradlew.bat` 直接跑(工具函数里 `gradlew.bat` 相对路径偶尔解析失败,用绝对路径)。

## 3. 代码结构

```
app/src/main/java/com/iptv/player/
  IptvApplication.kt        App 入口:FileLogger.init, PlaybackController.init, ServerRepo
  MainActivity.kt           enableEdgeToEdge + IptvTheme { AppRoot() }
  data/
    api/
      Dtos.kt               @Serializable DTO:Source/Channel/ChannelPage/StreamInfo/NasBrowse/NasStreamInfo/Epg/Group...
      ApiService.kt         Retrofit 接口,相对路径("api/health","api/channels","api/nas/{id}/browse"...)
      ApiClient.kt          核心:动态 baseUrl(拦截器按需重写 scheme/host/port/路径前缀);
                            checkHealth 在 Dispatchers.IO;absolute()/resolve() 构建播放 URL;joinPath 支持反向代理子路径
      HttpExt.kt            public bodyOrThrow()
      ServerConfig.kt
    repo/
      ServerRepo.kt         多服务器状态 + DataStore 持久化 + 切换时 applyBase
      ChannelRepo.kt        sources/channels/streamInfo/epg(withContext IO, Result)
      FavRepo.kt            favorites/favoriteIds StateFlow, refresh/toggle
      NasRepo.kt            browse/streamInfo + fileUrl/playlistUrl/vodUrl 构造
    local/
      ServerStore.kt        Preferences DataStore(servers JSON + activeId)
      FileLogger.kt         dev.log:内部存储 + 共享存储(Android/data/.../files/dev.log)双写 + Logcat
  player/
    Quality.kt             NasQuality(原画/1080P/720P/480P)
    PlaybackController.kt  ExoPlayer 单例;直播候选链;NAS 候选;TS 断流重连(3次/1.5s);
                           status: StateFlow<String>;errors: SharedFlow<String>;FileLogger 埋点
  ui/
    theme/Theme.kt         深蓝黑 + 青色主题,统一圆角/字体
    App.kt                 NavHost(server/main/player/group);MainScaffold 底部栏(直播/点播/我的)+服务器切换下拉;GroupScreen
    server/ServerScreen.kt 添加服务器(health 校验)/列表/切换/删除;显示 dev.log 路径
    home/HomeController.kt 分页(limit=200, offset);筛选;防抖搜索;grid/list
    home/HomeScreen.kt     搜索框/chips/下拉刷新/无限滚动/EPG 弹窗/分组弹窗
    home/ChannelViews.kt   TV 风格 ChannelCard + ChannelRow
    nas/NasScreen.kt       源选择 → 目录浏览(面包屑) → 画质弹窗
    fav/FavoritesScreen.kt 收藏/分组两个 Tab
    epg/EpgDialog.kt
    player/PlayerScreen.kt 横屏全屏;自绘控制条(3s 自动隐藏);缓冲动画;画质切换
    common/Components.kt   LoadingBox/ErrorBox/EmptyBox/OfflineBox;时间/大小格式化
    common/PlayPayload.kt  sealed Live(Channel)/Nas(sourceId,path,name,quality,tryDirect),导航用 JSON 传参
```

## 4. 播放决策逻辑(spec §4,已在 PlaybackController 实现)

- **直播**:先 `/api/channels/:id/streaminfo` 拿 type/forceProxy,按 §4.1 Android 简化链构造候选:
  - hls→manifest 直连 → 代理HLS;forceProxy 时 hls→relay
  - mp4→源站直连 → 代理HLS
  - mpegts→`/api/ts/:id/ts`(可重连)→ 代理HLS
  - flv/unknown→代理HLS
  - rtmp/rtsp/udp→relay → 代理HLS
  - 末尾追加 fmp4;全失败 toast"播放失败:所有方式均不可用"
  - TS/fMP4 断流重连 3 次(间隔 1.5s),再失败顺延下一候选
- **NAS**:非原画→`vod.m3u8?path=&h=`;原画且扩展名 mp4/m4v/mov + audioCodec∈{aac,mp3,opus,vorbis,flac} 或开启"尝试直连"→ `file?path=` 直连,失败回退 `playlist.m3u8?path=`
- 状态文案通过 `PlaybackController.status` 展示(直连/服务器代理/转码中/重连 n/3)

## 5. 已实现 / 未实现(对照 spec M0-M7)

已实现:
- M0 工程 ✓  M1 连接/健康 ✓  M2 数据层 ✓  M3 直播列表 ✓
- M4 播放器候选链+重连 ✓(PiP 未做)  M5 网盘点播 ✓  M6 收藏/分组 ✓
- M7 错误/空态 UI ✓,下拉刷新 ✓,dev.log ✓;真机联调进行中;release 签名未做

未实现/待做(续接点):
- [ ] **release 正式打包**(生成 keystore + signingConfig + assembleRelease,可直接分享安装)
- [ ] **画中画/后台播放**(M4 可选增强)
- [ ] **自签名 https 证书开关**(当前标准 TLS 校验,自签名会失败)
- [ ] 真机联调反馈修复
- [ ] 按用户反馈的界面/体验打磨

## 6. 后端 API 约定(播放端用到的)

- Base = `http://host:port`,支持 https/公网 IP/反向代理/子路径(如 `https://x.com/iptv`)
- 除 `/api/admin/*` 外全部公开;管理接口要 Bearer token(本 App 不用)
- 关键端点:`/api/health`、`/api/sources`、`/api/channels`(grp/q/favorite/sourceId/limit/offset)、
  `/api/channels/:id/streaminfo`、`/api/favorites`、`/api/groups`、`/api/epg/:id`、
  `/api/stream/:id/playlist.m3u8`、`/api/stream/:id/manifest.m3u8`、`/api/ts/:id/ts`、`/api/ts/:id/fmp4`、
  `/api/relay/:id/playlist.m3u8`、`/api/nas/:sourceId/browse|streaminfo|file|playlist.m3u8|vod.m3u8|hls/*`
- ts/fmp4 为无限时长流,断线需自动重连(已实现)

## 7. dev.log

- 运行日志双写到:内部 `/data/data/com.iptv.player/files/dev.log`(完整,debug 包可用 run-as 导出)
  和共享 `/sdcard/Android/data/com.iptv.player/files/dev.log`(无需 root)
- 项目根目录 `get-devlog.cmd`:连上设备双击 → 生成到项目根目录 `dev.log`
- 日志内容:启动、服务器切换、播放候选链(每条 URL+方式)、断流重连、候选顺延、播放失败等
- 排查问题先看这个文件。

## 8. 版本清单(gradle/libs.versions.toml)

AGP 8.10.1 / Kotlin 2.1.10 / Compose BOM 2025.01.01 / core-ktx 1.15.0 / lifecycle 2.8.7 /
activity-compose 1.9.3 / navigation-compose 2.8.5 / retrofit 2.11.0 / okhttp 4.12.0 /
kotlinx-serialization-json 1.7.3 / coroutines 1.9.0 / coil 2.7.0 / media3 1.5.1 / datastore 1.1.1
buildToolsVersion=36.0.0(本机已装 35 与 36,勿让 AGP 自动下载)
