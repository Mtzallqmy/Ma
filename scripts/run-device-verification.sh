#!/usr/bin/env bash
# Preserve failed test exit status while collecting any screenshots already captured.
set +e
./gradlew :app:connectedDebugAndroidTest --stacktrace
verification_status=$?
adb pull /sdcard/Download/dawa-hayah-verification ui-verification
exit "$verification_status"
