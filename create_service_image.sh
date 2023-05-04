#!/usr/bin/env bash

function getProperty {
    PROP_KEY="$1"
    PROP_VALUE=$(grep "${PROP_KEY}" gradle.properties | cut -d'=' -f2)
    echo "${PROP_VALUE}"
}

SERVICE_VERSION=$(getProperty "version")

docker build --build-arg VERSION="${SERVICE_VERSION}" --build-arg BUILD_ON="$(date)" --build-arg USER_ID=$(id -u) --build-arg GROUP_ID=$(id -g) -f docker/service/Dockerfile . -t "$1:$SERVICE_VERSION"

