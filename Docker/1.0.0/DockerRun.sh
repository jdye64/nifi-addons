#!/bin/bash
SLEEP_SEC="20"
NIFI_PORT="8080"
NIFI_IMAGE_NAME="jdye64/nifi-addons:1.0.0"

# Checks for an instance of $NIFI_IMAGE_NAME already running
CONTAINER_ID=$(docker ps | grep $NIFI_IMAGE_NAME | awk '{ print $1 }')
if [ -n "$CONTAINER_ID" ]; then
	echo "There is already an instance of $NIFI_IMAGE_NAME running as container $CONTAINER_ID"
	while true; do
    	read -p "Would you like to kill the already running $NIFI_IMAGE_NAME container?" yn
    	case $yn in
        	[Yy]* ) echo "killing Docker container $CONTAINER_ID"; docker kill $CONTAINER_ID; break;;
        	[Nn]* ) exit;;
        	* ) echo "Please answer yes or no.";;
    	esac
	done
fi

echo "Launching latest NiFi instance"
CONTAINER_ID=$(docker run -t -d -p $NIFI_PORT:$NIFI_PORT $NIFI_IMAGE_NAME)

IP_ADDR="127.0.0.1"
echo "IPAddress: $IP_ADDR"
NIFI_URL="http://$IP_ADDR:$NIFI_PORT/nifi"
echo "Opening NiFi WebUI at $NIFI_URL"
echo "Sleeping for $SLEEP_SEC seconds before opening browser to give NiFi time to launch WebUI"
sleep $SLEEP_SEC
open $NIFI_URL