#!/usr/bin/env bash

set -e

PARENT_PATH=$( cd "$(dirname "${BASH_SOURCE[0]}")" ; pwd -P)

JAR_PATH=$PARENT_PATH/ips2plant-cli-runner.jar

echo; echo "Running ips2plant"

java -jar $JAR_PATH "$@"

echo "Finished with exit code $?"