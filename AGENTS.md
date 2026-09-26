# 小骏TV 项目说明

本文档是项目目录结构和当前功能需求的开发基准。所有后续改动都应优先保证 Android TV 大屏和遥控器 D-pad 交互，不要把移动端交互直接套用到电视端。

## 项目目录

```text
mytv-android/
├── core/
│   ├── data/                 数据模型、直播源、节目单和持久化
│   ├── util/                 通用工具
│   └── designsystem/         Compose 主题和基础 UI
├── tv/                       Android TV 主版本（API 23+）
│   ├── src/main/java/.../ui/ 页面、设置、频道和播放器界面
│   ├── src/main/java/.../ui/screensold/
│   │                           播放页、选台页和遥控器操作逻辑
│   ├── src/main/jniLibs/     MPV/FFmpeg 原生库（armeabi-v7a、arm64-v8a）
│   ├── src/main/res/         图标、启动图、台标和主题资源
│   └── libs/                 Media3 FFmpeg 解码器 AAR
├── tv-legacy/                Android 4.3 兼容版本（API 18+）
├── ijkplayer-java/           IJK 播放器 Java 封装
├── gradle/                   Gradle Wrapper 和版本目录
├── docs/screenshots/         README 使用的项目截图
├── .github/workflows/        GitHub Actions 编译流程
├── build.gradle.kts          公共 Android 配置、签名和 APK 命名
├── settings.gradle.kts       模块注册和依赖仓库
├── gradle/libs.versions.toml 依赖、AGP、Kotlin、Media3 版本
└── README.md                中英文项目说明
```

## 当前产品定位

- 产品名称：小骏TV。
- 产品形态：电视端 IPTV 直播播放器。
- 首屏目标：打开应用后进入直播，不展示更新、推送或无关服务页面。
- 主要使用场景：Android TV、电视盒子、遥控器和 Android TV 模拟器。
- 直播源：支持内置 M3U8 源，也支持用户管理自定义直播源。
- 当前内置直播源：`http://192.168.2.1:88/iptv.m3u8`。
- 节目单：从直播源的 `x-tvg-url` 或配置的 EPG 地址加载；当前内置 EPG 为 `http://192.168.2.1:88/e.xml`。

内置地址属于局域网测试配置，发布版本不得擅自替换为第三方远程服务，也不得添加隐藏的联网、更新、推送或统计请求。

## 支持平台和构建

| 版本 | 模块 | 最低系统 | ABI | 构建命令 |
| --- | --- | --- | --- | --- |
| Android TV 主版本 | `tv` | Android 5.0 / API 21 以上 | armeabi-v7a、arm64-v8a | `./gradlew :tv:assembleOriginalRelease` |
| Android 4.3 兼容版本 | `tv-legacy` | Android 4.3 / API 18 以上 | 由设备和依赖决定 | `./gradlew :tv-legacy:assembleRelease` |

主版本的 Gradle 配置当前使用 compileSdk 36、targetSdk 36、Media3 1.11.1。GitHub Actions 使用 JDK 17 编译两个模块的 Debug APK，并上传构建产物供验证。

## 播放器功能

播放器切换必须保留当前频道和播放状态，不得在切换内核后自动跳回第一个节目。

- MPV：支持软件解码和硬件解码模式；初始化失败或超时时要回到可操作的错误状态。
- VLC：支持直播和 4K，保持正确的画面比例，不得因分辨率把画面异常放大。
- IJK：为旧设备提供兼容播放路径。
- Media3/ExoPlayer：支持 HLS、RTSP、DASH，并集成 FFmpeg 音频软解。
- 播放失败、加载超时、无网络和网络过慢时，必须显示明确的可恢复提示，不能让页面永久卡在加载状态。
- 播放器异常不得导致主界面或菜单闪退；切换内核失败时应保留频道选择并允许再次尝试。

## 电视交互要求

- 上下键切换频道或移动选台焦点。
- 左右键切换线路、打开节目单或移动菜单焦点。
- OK 键确认频道、确认设置和打开播放控制菜单。
- 播放页面第一次按返回显示退出确认窗口，默认焦点在“确定退出”。
- 退出窗口包含“确定退出”和“进入设置”；再次按返回关闭窗口。
- 频道菜单、设置列表和对话框必须有清晰的焦点状态，支持遥控器连续操作。
- 默认使用经典选台界面，并启用台标显示。
- 手机触摸操作可以作为辅助方式，但不能牺牲电视遥控器体验。

## 网络和错误处理

- 无网、网络慢、直播源失效和 EPG 加载失败都必须有精美、清晰、可恢复的提示。
- 加载超时后必须允许返回、重试、切换频道或进入设置。
- 不得通过阻塞主线程等待网络请求。
- 不得加入自动更新、推送、远程升级、隐藏 HTTP 服务或未声明的第三方请求。
- 用户添加的直播源可以新增、编辑、删除，不能产生重复记录。

## 资源和发布

- 应用名称保持为“小骏TV”。
- APK 应包含当前应用图标和启动图，图标在手机和电视启动器中都不能出现异常黑边或多余色圈。
- 发布 APK 使用 GitHub Releases；源码中的 `.env.github`、签名文件和令牌绝不能提交。
- 发布前至少验证：构建通过、模拟器启动、遥控器返回流程、频道切换、播放器切换、无网超时和 APK 安装。

## 变更约束

1. 新增功能必须服务于直播观看、电视交互、稳定性或设备兼容性。
2. 修改播放器时，同时检查 MPV、VLC、IJK、Media3 四条路径的状态恢复和错误处理。
3. 修改频道或设置界面时，必须检查 D-pad 焦点、默认焦点和返回键行为。
4. 修改构建配置时，必须同时考虑 Android TV 主版本和 Android 4.3 兼容版本。
5. 不要提交测试截图、设备日志、局域网凭据、令牌、签名密钥或临时 APK；文档专用截图只放在 `docs/screenshots/`。
