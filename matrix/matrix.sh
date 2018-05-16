
jvm_test() {
    name=$1
    path=$2

    echo $1
    ../gradlew -b jvm.gradle -Pjdk_path=$path -Presult_path=results/$name -q clean check
}

rm -rf results

jvm_test jdk8 /usr/java/jdk1.8.0_161
jvm_test jdk9 /usr/java/jdk-9.0.1
jvm_test jdk10 /usr/java/jdk-10.0.1

echo "Gathering results"
../gradlew -q -b matrix-results.gradle summarize
echo "Done"
