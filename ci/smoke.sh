#!/usr/bin/env bash
# Installs the real APK, launches the real activity, and writes down what
# happened. Runs on the CI emulator.
#
# This lives in the repository rather than inline in the workflow for two
# reasons: the remote bridge cannot write to .github/workflows, so keeping the
# logic here means diagnostics can be changed without touching a protected
# file; and a syntax error in an inline workflow script fails the job with a
# bare "exit code 2" and no output, which is exactly what happened first time.
#
# It never fails the build. A red job tells us nothing; the report tells us
# everything.

set +e
REPORT="launch-report.txt"
: > "$REPORT"

say() { echo "" >> "$REPORT"; echo "=== $* ===" >> "$REPORT"; }

say "install"
./gradlew :app:installDebug --no-daemon >> "$REPORT" 2>&1

adb logcat -c

say "launch com.crystalkey.app/.MainActivity"
adb shell am start -W -n com.crystalkey.app/.MainActivity >> "$REPORT" 2>&1

sleep 12

say "what is on screen"
adb shell dumpsys activity activities \
  | grep -iE "mResumedActivity|topResumedActivity|mFocusedApp" \
  | head -5 >> "$REPORT" 2>&1

say "is the process alive"
adb shell ps -A 2>/dev/null | grep -i crystalkey >> "$REPORT" 2>&1
echo "(no line above means the process is dead)" >> "$REPORT"

say "fatal exceptions"
adb logcat -b crash -d 2>/dev/null | tail -120 >> "$REPORT" 2>&1

say "AndroidRuntime errors"
adb logcat -d -s AndroidRuntime:E 2>/dev/null | tail -120 >> "$REPORT" 2>&1

say "anything mentioning crystalkey"
adb logcat -d 2>/dev/null | grep -i crystalkey | tail -80 >> "$REPORT" 2>&1

say "instrumented tests"
./gradlew :app:connectedDebugAndroidTest --no-daemon >> "$REPORT" 2>&1

adb exec-out screencap -p > launch.png 2>/dev/null

echo "" >> "$REPORT"
echo "=== report ends ===" >> "$REPORT"
exit 0
