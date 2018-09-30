#!/usr/bin/env bash

android-wait-for-emulator() {
    set +e

    bootanim=""
    failcounter=0
    until [[ "$bootanim" =~ "stopped" ]]; do
       bootanim=`$ANDROID_HOME/platform-tools/adb -e shell getprop init.svc.bootanim 2>&1`
       echo "Boot animation: $bootanim"
       if [[ "$bootanim" =~ "not found" ]]; then
          let "failcounter += 1"
          if [[ $failcounter -gt 15 ]]; then
            echo "Failed to start emulator"
            exit 1
          fi
       fi
       sleep 1
    done
    echo "Done"
}

if [ -z "$ANDROID_HOME" ]; then
    export ANDROID_HOME=$HOME/Android/Sdk
fi

export AVD=$1
shift

$ANDROID_HOME/emulator/emulator -avd "$AVD" -no-audio &
EMULATOR_PID=$!
android-wait-for-emulator
$ANDROID_HOME/platform-tools/adb shell input keyevent 82 &

$*
RC=$?

kill $EMULATOR_PID

exit $RC
