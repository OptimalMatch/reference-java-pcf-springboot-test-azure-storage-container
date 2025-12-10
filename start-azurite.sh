#!/bin/bash

# Start Azurite Azure Storage Emulator
# This script builds and starts Azurite for local development/testing

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
AZURITE_DIR="$SCRIPT_DIR/Azurite-3.35.0"

cd "$AZURITE_DIR"

# Check if node_modules exists, if not install dependencies
if [ ! -d "node_modules" ]; then
    echo "Installing Azurite dependencies..."
    npm ci
fi

# Check if dist folder exists, if not build
if [ ! -d "dist" ]; then
    echo "Building Azurite..."
    npm run build
fi

echo "Starting Azurite..."
echo "Blob service: http://127.0.0.1:10000"
echo "Queue service: http://127.0.0.1:10001"
echo "Table service: http://127.0.0.1:10002"
echo ""
echo "Default account name: devstoreaccount1"
echo "Default account key: Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw=="
echo ""

# Start Azurite with blob service only (for this project)
node dist/src/blob/main.js --blobHost 127.0.0.1 --blobPort 10000 -l "$SCRIPT_DIR/azurite-data"
