#!/bin/bash

# Start Spring Boot application with Azurite configuration
# Make sure Azurite is running first (./start-azurite.sh)

export USE_AZURITE=true
export AZURE_STORAGE_CONTAINER_NAME=test-container

echo "Starting Spring Boot application with Azurite..."
echo "USE_AZURITE=$USE_AZURITE"
echo "AZURE_STORAGE_CONTAINER_NAME=$AZURE_STORAGE_CONTAINER_NAME"
echo ""

java -jar target/azure-storage-test-1.0.0.jar
