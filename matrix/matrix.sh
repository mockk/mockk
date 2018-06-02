
jvm_test() {
    name=$1
    path=$2

    echo $1
    ../gradlew -b jvm.gradle -Pjdk_path=$path -Presult_path=results/$name -q clean check
}

android_unit() {
    echo "Android unit tests"
    ../gradlew -b android.gradle -Presult_path=results/android -q clean testReleaseUnit
    cp -r build/test-results/testReleaseUnitTest results/android
    rm -rf results/android/binary
}

android_ait() {
    sdk_path=$1
    emulator=$2

    echo $emulator

    #$sdk_path/emulator/emulator -avd $emulator -use-system-libs test -no-skin -no-audio -no-window &
    #android-wait-for-emulator
    #adb shell input keyevent 82 &

    ../gradlew -b android-ait.gradle -q clean connectedCheck

    cp -r build/outputs/androidTest-results/connected results/android-ait
}

rm -rf results

jvm_test jdk6 /usr/java/jdk1.6.0_45
jvm_test jdk7 /usr/java/jdk1.7.0_80
jvm_test jdk8 /usr/java/jdk1.8.0_161
jvm_test jdk9 /usr/java/jdk-9.0.1
jvm_test jdk10 /usr/java/jdk-10.0.1
android_unit
# done manually
#android_ait $HOME/Android/Sdk Nexus_S_API_P

echo "Gathering results"
../gradlew -q -b matrix-results.gradle summarize
echo "Done"
