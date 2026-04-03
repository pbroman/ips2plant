#!/usr/bin/env bash

set -e

PARENT_PATH=$( cd "$(dirname "${BASH_SOURCE[0]}")" ; pwd -P)

JAR_PATH=$PARENT_PATH/quarkus/target/ips2plant-cli-runner.jar

if [ ! -f "$JAR_PATH" ]; then
    echo "Jar not found at $JAR_PATH — run build-quarkus.sh first"
    exit 1
fi

echo; echo "Running ips2plant (quarkus)"

java -jar "$JAR_PATH" "$@"

echo "Finished with exit code $?"