#!/usr/bin/env bash

set -e

PARENT_PATH=$( cd "$(dirname "${BASH_SOURCE[0]}")" ; pwd -P)

echo; echo "Building ips2plant quarkus jar..."

cd "$PARENT_PATH/quarkus"
./mvnw clean package -DskipTests -Dquarkus.package.jar.type=uber-jar

echo; echo "Build complete: quarkus/target/ips2plant-cli-runner.jar"