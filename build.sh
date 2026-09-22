#!/bin/sh
# Build helper: GTNH's gradle plugin requires a Java 25 JVM.
export JAVA_HOME="${FGM_JDK25:-/c/jdk/jdk-25.0.4.1+1}"
exec ./gradlew "$@"
