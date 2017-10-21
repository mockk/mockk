#!/bin/bash

JDKS=`find /usr/java -maxdepth 1 -mindepth 1 ! -type l`

export JAVA_OPTS="--add-opens=java.base/java.util=ALL-UNNAMED"

for JDK in $JDKS; do
    ./gradlew clean test -Dorg.gradle.java.home=$JDK 2>/dev/null | egrep "requires Java 7 or later to run|Starting MockK implementation|BUILD"
done
