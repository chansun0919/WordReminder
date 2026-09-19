#!/bin/bash
# 在本机（无需 Android Studio）构建 WordReminder 的 debug APK。
# 用法：bash build_apk.sh   构建产物在 app/build/outputs/apk/debug/app-debug.apk
set -e

ROOT=/Users/chenxin/.android_build
mkdir -p "$ROOT"
LOG="$ROOT/build.log"
exec > >(tee -a "$LOG") 2>&1
echo "===== build start $(date) ====="

# 1) JDK 17 (arm64)
if [ ! -d "$ROOT/jdk" ]; then
  echo "[1/6] 下载 JDK17 ..."
  curl -L -o "$ROOT/jdk.tar.gz" "https://api.adoptium.net/v3/binary/latest/17/ga/mac/aarch64/jdk/hotspot/normal/eclipse"
  mkdir -p "$ROOT/jdk"
  tar -xzf "$ROOT/jdk.tar.gz" -C "$ROOT/jdk" --strip-components=1
fi
export JAVA_HOME="$ROOT/jdk"
"$JAVA_HOME/bin/java" -version

# 2) Android 命令行工具
export ANDROID_HOME="$ROOT/sdk"
if [ ! -d "$ANDROID_HOME/cmdline-tools/latest" ]; then
  echo "[2/6] 下载 cmdline-tools ..."
  curl -L -o "$ROOT/cmd.zip" "https://dl.google.com/android/repository/commandlinetools-mac-11076708_latest.zip"
  mkdir -p "$ANDROID_HOME/cmdline-tools"
  unzip -q -o "$ROOT/cmd.zip" -d "$ANDROID_HOME/cmdline-tools"
  [ -d "$ANDROID_HOME/cmdline-tools/cmdline-tools" ] && mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
fi
SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
yes | "$SDKMANAGER" --licenses >/dev/null

# 3) 安卓平台/构建工具
echo "[3/6] 安装 SDK 组件 (platform-tools, android-34, build-tools 34.0.0) ..."
"$SDKMANAGER" "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# 4) Gradle 8.5
if [ ! -d "$ROOT/gradle" ]; then
  echo "[4/6] 下载 Gradle 8.9 ..."
  curl -L -o "$ROOT/gradle.zip" "https://services.gradle.org/distributions/gradle-8.9-bin.zip"
  mkdir -p "$ROOT/gradle"
  unzip -q -o "$ROOT/gradle.zip" -d "$ROOT/gradle"
fi
GRADLE="$ROOT/gradle/gradle-8.9/bin/gradle"

# 5) 生成 gradle wrapper（供本地与 GitHub Actions 通用）
echo "[5/6] 生成 gradle wrapper ..."
cd /Users/chenxin/WorkBuddy/2026-09-19-12-59-55/WordReminder
"$GRADLE" wrapper --no-daemon

# 6) 编译 APK
echo "[6/6] 编译 app-debug.apk ..."
"$GRADLE" assembleDebug --no-daemon --stacktrace

echo "===== build done $(date) ====="
find /Users/chenxin/WorkBuddy/2026-09-19-12-59-55/WordReminder/app/build/outputs -name "*.apk"
