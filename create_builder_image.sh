#!/usr/bin/env bash
echo "Pulling image $1:$2"
docker pull "$1:$2"

echo "Copying updated gradle properties"
\cp gradle.properties docker/builder/gradle.properties
sed -i '/^version/d' docker/builder/gradle.properties

echo "Creating builder image using cache..."
docker build --build-arg USER_ID=$(id -u) --build-arg GROUP_ID=$(id -g) --cache-from "$1:$2" -f docker/builder/Dockerfile . -t "$1:$2"

echo "Removing gradle properties copy"
rm -f docker/builder/gradle.properties
