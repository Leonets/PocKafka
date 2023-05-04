#!/usr/bin/env bash
mkdir -p "$(pwd)/build"

GRADLE_PARAMS="-PGucciOmsNexusUsername=${NEXUS_CREDENTIALS_USR} -PGucciOmsNexusPassword=${NEXUS_CREDENTIALS_PSW} --dependency-verification off --info build shadowJar downloadAgent"

docker run --rm -i --user $(id -u):$(id -g) -v $(pwd)/src:/opt/builder/src -v $(pwd)/build:/opt/builder/build "$1:$2" ${GRADLE_PARAMS}