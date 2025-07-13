#!/usr/bin/env bash

set -e

PARENT_PATH=$( cd "$(dirname "${BASH_SOURCE[0]}")" ; pwd -P)

TARGET_PATH=$PARENT_PATH/java/cli/target/
JAR_NAME=ips2plant-cli.jar

if [[ ! -f $TARGET_PATH$JAR_NAME ]]; then
  echo "$JAR_NAME not found, executing mvn package"
  mvn clean package -f $PARENT_PATH/java/pom.xml
fi

echo; echo "Running ips2plant"

java -jar $TARGET_PATH$JAR_NAME "$@"

echo "Finished with exit code $?"