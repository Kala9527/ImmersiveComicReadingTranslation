# Android Studio 调试环境配置记录

## 已完成配置

- SDK 路径：`D:\Package_tools\AndroidStudio\SDK`
- Android Studio：`D:\Package_tools\AndroidStudio\IDE\android-studio`
- JDK：`D:\Package_tools\AndroidStudio\IDE\android-studio\jbr`
- Gradle 缓存：`D:\Package_tools\AndroidStudio\Gradle`
- AVD 路径：`D:\Package_tools\AndroidStudio\Config\AndroidUser\avd`
- 已修正 `D:\Package_tools\AndroidStudio\AndroidEnv.ps1` 中的 `ANDROID_AVD_HOME`
- 已把用户环境变量写入 `HKCU\Environment`
- 已创建兼容联接：`D:\Package_tools\AndroidStudio\Config\AVD -> D:\Package_tools\AndroidStudio\Config\AndroidUser\avd`
- 已创建 `D:\Package_tools\AndroidStudio\Config\Emulator`，修复 `emu-last-feature-flags.protobuf.lock (error: 3)`。该错误是 emulator home 目录不存在导致的。
- 已安装/确认：
  - `platform-tools`
  - `emulator`
  - `cmdline-tools;latest` 22.0
  - `platforms;android-36`
  - `build-tools;36.1.0`
  - `system-images;android-36;google_apis;x86_64`
  - `system-images;android-36;default;x86_64`
- 已创建 AVD：
  - `Comic_Debug_Basic_API36`
  - `Comic_Debug_API36`
  - 保留原 `Small_Phone`
- 已把 Android Studio 项目部署目标从不存在的 `Medium_Phone.avd` 改为 `Comic_Debug_Basic_API36.avd`
- `.\gradlew.bat assembleDebug` 已验证构建成功

## 推荐启动方式

从项目目录运行：

```powershell
.\open-android-studio.ps1
```

或手动先加载环境：

```powershell
. D:\Package_tools\AndroidStudio\AndroidEnv.ps1
D:\Package_tools\AndroidStudio\IDE\android-studio\bin\studio64.exe D:\工作区文件夹\test_explore\ImmersiveComicReadingTranslation
```

## AVD 状态

命令行可识别 AVD：

```powershell
D:\Package_tools\AndroidStudio\SDK\emulator\emulator.exe -list-avds
```

当前已验证 `Comic_Debug_Basic_API36` 可以启动到 `adb devices`，并成功安装/启动 `app-debug.apk`。

建议在 Android Studio 的 Device Manager 中启动 `Comic_Debug_Basic_API36`。如果仍长时间黑屏或不出现在 Running Devices：

1. 关闭所有 `emulator.exe` / `qemu-system-x86_64.exe` 进程。
2. 打开 Device Manager，选择 `Comic_Debug_Basic_API36`。
3. 使用 Cold Boot Now。
4. 如果仍失败，在设备设置里把 Graphics 改为 `Software`。
5. 确认 Windows Defender/安全软件没有拦截 `qemu-system-x86_64.exe` 本地端口。

## APK

Debug APK：

```text
app/build/outputs/apk/debug/app-debug.apk
```
