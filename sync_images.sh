#!/usr/bin/env bash

function getProperty {
    PROP_KEY="$1"
    PROP_VALUE=$(grep "${PROP_KEY}" gradle.properties | cut -d'=' -f2)
    echo "${PROP_VALUE}"
}

SERVICE_VERSION=$(getProperty "version")

echo "Pushing service image $1:$SERVICE_VERSION ..."
docker push "$1:$SERVICE_VERSION"
if [ $? -eq 0 ]
then
	echo "Service image $1:$SERVICE_VERSION pushed successfully"
else
	echo "Problems occurred while pushing service image $1:$SERVICE_VERSION - Exiting..."
	exit 1
fi

echo "Tagging service image $1:$SERVICE_VERSION to $1:$3"
docker tag "$1:$SERVICE_VERSION" "$1:$3"
echo "Pushing service image $1:$3 ..."
docker push "$1:$3"
if [ $? -eq 0 ]
then
	echo "Service image tag $1:$3 pushed successfully"
else
	echo "Problems occurred while pushing service image tag $1:$3 - Exiting..."
	exit 1
fi

echo "Pushing builder image $2:$3 ..."
docker push "$2:$3"
if [ $? -eq 0 ]
then
	echo "Builder $2:$3 image pushed"
else
	echo "Problems occurred while pushing builder image $2:$3 - Exiting..."
	exit 1
fi

echo "Cleaning up local docker agent..."
docker rmi "$1:$SERVICE_VERSION"
docker rmi "$1:$3"
docker rmi "$2:$3"
docker image prune -f --filter="dangling=true"

echo "Local docker agent cleaned up"
