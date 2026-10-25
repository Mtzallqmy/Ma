#!/usr/bin/env bash
# Preserve failed test exit status while collecting any screenshots already captured.
set +e
./gradlew :app:connectedDebugAndroidTest --stacktrace
verification_status=$?
adb pull /sdcard/Android/data/com.dawahayah.app.debug/files/verification ui-verification
exit "$verification_status"
