# DEVELOPMENT_LOG.md — IPTVPlayer 开发日志

记录开发过程中的进度、决策与踩坑,供跨会话/交接使用。技术细节与续接点见 `AGENTS.md`。

## 2026-08-20 第一轮:从零搭建到可运行

### 完成内容
- 按 `ANDROID_APP_SPEC.md` 从零搭建 Android 项目(Kotlin + Compose + Media3)。
- M0–M6 全部实现:工程/连接/数据层/直播列表/播放器/网盘点播/收藏分组。
- M7 部分:错误/空态 UI、下拉刷新、dev.log。

### 关键决策
- **动态服务器地址**:Retrofit 用占位 baseUrl,OkHttp 拦截器把请求重写到当前服务器(scheme/host/port + 路径前缀)。支持多服务器运行时切换,不用重建 Retrofit。
- **播放候选链**:`PlaybackController` 按 spec §4.1 构造候选(直连→代理→TS→fMP4),TS/fMP4 断流重连 3 次,全失败 toast。
- **UI 导航**:字符串路由 + JSON 传参(`PlayPayload` sealed class),避免导航序列化兼容问题。

### 环境踩坑(重要,详见 AGENTS.md §2)
- 官方源在国内不可用 → 全面切阿里云/腾讯镜像(init.d 全局脚本 + settings.gradle.kts + gradle-wrapper.properties)。
- Android SDK 只有 android-36.1,AGP 要 android-36 → 从腾讯镜像下载 `platform-36_r02.zip` 手动安装,并手工补 `source.properties`。
- `gradle.properties` 关闭 `android.builder.sdkDownload`,否则 AGP 卡在 dl.google.com。
- 构建被超时强杀 → Gradle transforms 缓存损坏(`Could not read workspace metadata`)→ 删缓存重跑。
- Android Studio 开着 sync 时命令行构建会抢锁卡死 → 杀 AS 的 java 守护进程或后台构建。

## 2026-08-20 第二轮:修复连接问题 + 支持反向代理

- **修复 `/api/health` 永远失败**:`checkHealth` 在主线程做阻塞网络请求抛 `NetworkOnMainThreadException` 被吞掉 → 改为 `withContext(Dispatchers.IO)`。
- **支持反向代理/公网/子路径**:URL 构建加入 `joinPath`,保留用户输入的基础路径前缀(如 `https://x.com/iptv/api/...`),拦截器、`absolute()`、`checkHealth` 三处统一。

## 2026-08-20 第三轮:界面与体验美化

- 主题:深蓝黑 + 青色主色,统一圆角/字体/间距。
- 首页:圆角搜索框(一键清空)、筛选 chips、频道计数、下拉刷新、TV 风格频道卡片(16:9 logo + 渐变遮罩 + 悬浮收藏/节目单按钮)、列表行美化。
- 播放器:顶部/底部渐变遮罩、控制条 3s 自动隐藏、缓冲动画、暂停态居中大播放键、进度条样式。
- 通用组件:Loading/Error/Empty/Offline 带图标。

## 2026-08-20 第四轮:dev.log 日志体系

- `FileLogger`:双写内部存储 + 共享存储 + Logcat;记录启动、服务器切换、播放候选链、重连、失败。
- 项目根目录新增 `get-devlog.cmd`:连设备双击即拉取 dev.log 到项目根目录。

## 待办(续接顺序)

1. release 正式打包(keystore + signingConfig + assembleRelease)
2. 画中画 / 后台播放
3. 自签名 https 证书开关(可选)
4. 真机联调反馈修复
5. 界面/体验继续打磨(按用户反馈)
