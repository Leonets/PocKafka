#!/usr/bin/env bash

function getProperty {
    PROP_KEY="$1"
    PROP_VALUE=$(grep "${PROP_KEY}" gradle.properties | cut -d'=' -f2)
    echo "${PROP_VALUE}"
}

SERVICE_VERSION=$(getProperty "version")
echo "${SERVICE_VERSION}"