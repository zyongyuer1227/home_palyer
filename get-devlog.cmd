@echo off
rem ============================================================
rem  从手机/模拟器拉取 dev.log 到本项目根目录
rem  用法：连接设备后双击本脚本，或命令行执行 get-devlog.cmd
rem  生成：%CD%\dev.log
rem ============================================================
setlocal enabledelayedexpansion

where adb >nul 2>nul
if errorlevel 1 (
    echo [错误] 找不到 adb，请确认 Android SDK platform-tools 已加入 PATH，
    echo        或把下面的路径加到 PATH 后重试：
    echo        %LOCALAPPDATA%\Android\Sdk\platform-tools
    exit /b 1
)

echo 正在检查连接的设备...
set "HAS_DEVICE=0"
for /f "tokens=1" %%i in ('adb devices 2^>nul ^| findstr /r "device$"') do set "HAS_DEVICE=1"

if "!HAS_DEVICE!"=="0" (
    echo [错误] 没有检测到已连接的设备。请先通过 USB 连接手机（开启 USB 调试）
    echo        或启动模拟器，然后重试。
    exit /b 1
)

echo 从内部存储导出（debug 包，run-as）...
adb exec-out run-as com.iptv.player cat files/dev.log > dev.log 2>nul
if errorlevel 1 (
    echo 内部导出失败，改从共享存储拉取...
    adb pull /sdcard/Android/data/com.iptv.player/files/dev.log dev.log >nul 2>nul
)

if exist dev.log (
    echo.
    echo 已生成：%CD%\dev.log
    for %%f in (dev.log) do echo 大小：%%~zf 字节
) else (
    echo [错误] 拉取失败。请确认：
    echo   - App 已安装并运行过（至少启动过一次）
    echo   - 若用的是 release 包，请改为从共享存储手动复制：
    echo     /sdcard/Android/data/com.iptv.player/files/dev.log
    exit /b 1
)
endlocal
