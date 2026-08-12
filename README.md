# Immersive Comic Reading Translation

Immersive Comic Reading Translation 是一个 Android 漫画沉浸式悬浮翻译 MVP。它通过悬浮窗、屏幕捕获、OCR 和 OpenAI 兼容翻译接口，在阅读漫画时提供侧边翻译结果面板。

## 功能亮点

- Android 原生 Java 实现，可用 Android Studio 打开。
- OCR 与翻译模型分开配置，支持 OpenAI 兼容 `/chat/completions` 风格接口。
- API Key 使用 Android Keystore 加密保存。
- 悬浮窗权限、屏幕捕获授权与前台服务流程。
- 点击悬浮球后截取当前屏幕，执行 OCR -> 纠错翻译两阶段处理。
- 侧边结果面板支持展开 / 收起、复制、重试和快速设置入口。
- 错误阶段提示：权限、截图、OCR 网络 / 解析、翻译网络 / 解析。

## 项目结构

```text
.
├─ app/
│  ├─ build.gradle
│  └─ src/
│     ├─ main/              # 正式应用源码与资源
│     └─ debug/             # 调试入口与示例页面
├─ gradle/wrapper/
├─ build.gradle
├─ settings.gradle
└─ gradlew.bat
```

## 本地开发

要求：

- Android Studio
- JDK 17
- Android SDK，compileSdk 36

用 Android Studio 打开项目根目录，等待 Gradle 同步完成后运行 `app` 模块。

## 构建 APK

Windows：

```powershell
.\gradlew.bat assembleDebug
```

macOS / Linux：

```bash
./gradlew assembleDebug
```

APK 输出位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 使用流程

1. 安装 APK。
2. 打开应用，配置 OCR 模型与翻译模型的 Base URL、模型 ID 和 API Key。
3. 分别测试 OCR 与翻译模型。
4. 点击启动悬浮翻译。
5. 授予悬浮窗权限和屏幕捕获权限。
6. 切换到漫画 App，点击悬浮球进行翻译。

## MVP 边界

- 不内置任何云服务 API Key，必须由用户自行配置。
- 截图只在用户点击悬浮球后产生。
- 当前版本提供侧边译文面板，不做气泡级原图覆盖。
- 遇到 `FLAG_SECURE` 页面时，Android 系统会阻止截图。

## 注意事项

- `.gradle/`、`build/`、`.idea/`、APK 等本地和构建产物不提交。
- `local.properties` 是本机 Android SDK 路径，不提交到仓库。
- 使用屏幕捕获、悬浮窗和网络模型时，请遵守目标平台、模型服务和内容版权规则。

## 感谢与支持

感谢你关注这个 MVP。阅读漫画时被语言挡住真的很可惜，我希望这个项目能把“看懂下一格”这件事变得更自然一点。如果你喜欢这个方向，欢迎 Star、Fork、提 Issue 或给我建议，你的每一次支持都会让我更有劲继续完善它。
