@echo off
REM 构建脚本。需要先设好 JAVA_HOME 和 ANDROID_HOME。
REM 没设的话，下面两个 set 是给本机默认路径兜底，换成你自己的即可。

if "%JAVA_HOME%"=="" set "JAVA_HOME=%USERPROFILE%\jdk17"
if "%ANDROID_HOME%"=="" set "ANDROID_HOME=%USERPROFILE%\android-sdk"
set "ANDROID_SDK_ROOT=%ANDROID_HOME%"
set "PATH=%JAVA_HOME%\bin;%PATH%"

REM 优先用项目自带的 wrapper，没有就找 PATH 里的 gradle
if exist "%~dp0gradlew.bat" (
    call "%~dp0gradlew.bat" %*
) else (
    call gradle %*
)