# Azure Storage Container Test Service

A Spring Boot service for testing Azure Blob Storage connectivity. Designed for PCF deployment with support for local development using Azurite emulator.

## Features

- Read/write blobs to Azure Storage containers
- Account key authentication
- VCAP_SERVICES integration for PCF
- Local testing with Azurite emulator
- Dynamic endpoint with credentials passed via HTTP headers
- Web UI for interactive testing

## Prerequisites

- Java 1.8
- Maven 3.x
- Node.js (for Azurite local emulator)

## REST API Endpoints

### Static Endpoints (use configured credentials)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/blobs/test` | Test connection to Azure Storage |
| GET | `/api/blobs` | List all blobs in container |
| POST | `/api/blobs/{blobName}` | Upload blob (body = content) |
| GET | `/api/blobs/{blobName}` | Download blob content |
| DELETE | `/api/blobs/{blobName}` | Delete a blob |
| GET | `/api/blobs/{blobName}/exists` | Check if blob exists |
| GET | `/actuator/health` | Health check endpoint |
| GET | `/` | Web UI for interactive testing |

### Dynamic Endpoints (pass credentials via headers)

These endpoints allow you to pass Azure Storage credentials via HTTP headers, useful for testing different storage accounts without restarting the app.

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/dynamic/test` | Test connection |
| GET | `/api/dynamic/blobs` | List all blobs |
| POST | `/api/dynamic/blobs/{blobName}` | Upload blob |
| GET | `/api/dynamic/blobs/{blobName}` | Download blob |
| DELETE | `/api/dynamic/blobs/{blobName}` | Delete blob |
| GET | `/api/dynamic/blobs/{blobName}/exists` | Check if blob exists |

#### Required Headers

| Header | Description |
|--------|-------------|
| `X-Azure-Account-Name` | Storage account name |
| `X-Azure-Account-Key` | Storage account key |
| `X-Azure-Container-Name` | Container name |

#### Optional Headers

| Header | Description |
|--------|-------------|
| `X-Azure-Blob-Endpoint` | Custom blob endpoint (for Azurite or sovereign clouds) |

#### Example: Dynamic endpoint with Azurite

```bash
# Test connection
curl http://localhost:8080/api/dynamic/test \
  -H "X-Azure-Account-Name: devstoreaccount1" \
  -H "X-Azure-Account-Key: Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==" \
  -H "X-Azure-Container-Name: my-container" \
  -H "X-Azure-Blob-Endpoint: http://127.0.0.1:10000/devstoreaccount1"

# Upload a blob
curl -X POST http://localhost:8080/api/dynamic/blobs/test-file.txt \
  -H "X-Azure-Account-Name: devstoreaccount1" \
  -H "X-Azure-Account-Key: Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==" \
  -H "X-Azure-Container-Name: my-container" \
  -H "X-Azure-Blob-Endpoint: http://127.0.0.1:10000/devstoreaccount1" \
  -H "Content-Type: text/plain" \
  -d "Hello from dynamic endpoint!"

# Download the blob
curl http://localhost:8080/api/dynamic/blobs/test-file.txt \
  -H "X-Azure-Account-Name: devstoreaccount1" \
  -H "X-Azure-Account-Key: Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==" \
  -H "X-Azure-Container-Name: my-container" \
  -H "X-Azure-Blob-Endpoint: http://127.0.0.1:10000/devstoreaccount1"

# List blobs
curl http://localhost:8080/api/dynamic/blobs \
  -H "X-Azure-Account-Name: devstoreaccount1" \
  -H "X-Azure-Account-Key: Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==" \
  -H "X-Azure-Container-Name: my-container" \
  -H "X-Azure-Blob-Endpoint: http://127.0.0.1:10000/devstoreaccount1"
```

#### Example: Dynamic endpoint with real Azure Storage

```bash
curl http://localhost:8080/api/dynamic/test \
  -H "X-Azure-Account-Name: yourstorageaccount" \
  -H "X-Azure-Account-Key: your-base64-encoded-key" \
  -H "X-Azure-Container-Name: your-container"
```

## Local Development with Azurite

Azurite is an Azure Storage emulator included in this project for local testing.

### 1. Build the Project

```bash
mvn clean package -DskipTests
```

### 2. Install Azurite Dependencies (first time only)

```bash
cd Azurite-3.35.0
npm ci
cd ..
```

### 3. Start Azurite

Option A - Use the script:
```bash
./start-azurite.sh
```

Option B - Manual:
```bash
node Azurite-3.35.0/dist/src/blob/main.js --blobHost 127.0.0.1 --blobPort 10000 -l ./azurite-data
```

Azurite will start with:
- Blob endpoint: `http://127.0.0.1:10000`
- Account name: `devstoreaccount1`
- Account key: `Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==`

### 4. Start the Application

In a separate terminal:

Option A - Use the script:
```bash
./start-app.sh
```

Option B - Manual:
```bash
export USE_AZURITE=true
export AZURE_STORAGE_CONTAINER_NAME=test-container
java -jar target/azure-storage-test-1.0.0.jar
```

### 5. Test with curl

```bash
# Test connection
curl http://localhost:8080/api/blobs/test

# Upload a blob
curl -X POST http://localhost:8080/api/blobs/test-file.txt \
  -H "Content-Type: text/plain" \
  -d "Hello from Azure Storage!"

# Download the blob
curl http://localhost:8080/api/blobs/test-file.txt

# List all blobs
curl http://localhost:8080/api/blobs

# Check if blob exists
curl http://localhost:8080/api/blobs/test-file.txt/exists

# Delete the blob
curl -X DELETE http://localhost:8080/api/blobs/test-file.txt
```

## PCF Deployment

### 1. Create User-Provided Service

```bash
cf cups azure-storage -p '{"account-name":"YOUR_ACCOUNT","account-key":"YOUR_KEY","container-name":"YOUR_CONTAINER"}'
```

### 2. Build and Push

```bash
mvn clean package -DskipTests
cf push
```

The app will automatically bind to the `azure-storage` service and read credentials from VCAP_SERVICES.

### manifest.yml Configuration

```yaml
applications:
  - name: azure-storage-test
    memory: 1G
    instances: 1
    path: target/azure-storage-test-1.0.0.jar
    buildpacks:
      - java_buildpack_offline
    env:
      JBP_CONFIG_OPEN_JDK_JRE: '{ jre: { version: 1.8.+ } }'
    services:
      - azure-storage
    health-check-type: http
    health-check-http-endpoint: /actuator/health
```

## Configuration

### Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| `USE_AZURITE` | Set to `true` to use Azurite emulator | For local dev |
| `AZURE_STORAGE_ACCOUNT_NAME` | Azure Storage account name | If not using VCAP |
| `AZURE_STORAGE_ACCOUNT_KEY` | Azure Storage account key | If not using VCAP |
| `AZURE_STORAGE_CONTAINER_NAME` | Blob container name | Yes |
| `AZURE_STORAGE_BLOB_ENDPOINT` | Custom blob endpoint URL | Optional |
| `AZURITE_BLOB_ENDPOINT` | Azurite endpoint override | Optional |
| `APP_SECURITY_USERNAME` | Web UI username (default: admin) | Optional |
| `APP_SECURITY_PASSWORD` | Web UI password (default: admin) | Optional |

## Authentication

The web UI and all API endpoints (except `/actuator/health`) are protected with HTTP Basic Authentication.

- **Default username:** `admin`
- **Default password:** `admin`

Override via environment variables:
```bash
export APP_SECURITY_USERNAME=myuser
export APP_SECURITY_PASSWORD=mysecretpassword
```

For PCF deployment, set in manifest.yml:
```yaml
env:
  APP_SECURITY_USERNAME: myuser
  APP_SECURITY_PASSWORD: mysecretpassword
```

Access with curl:
```bash
curl -u admin:admin http://localhost:8080/api/blobs/test
```

### VCAP_SERVICES Format

```json
{
  "user-provided": [{
    "name": "azure-storage",
    "credentials": {
      "account-name": "your-storage-account",
      "account-key": "your-storage-key",
      "container-name": "your-container"
    }
  }]
}
```

## Project Structure

```
├── Azurite-3.35.0/              # Azure Storage emulator
├── src/main/java/com/example/azurestoragetest/
│   ├── AzureStorageTestApplication.java
│   ├── config/
│   │   └── AzureStorageConfig.java
│   ├── controller/
│   │   ├── BlobStorageController.java
│   │   └── DynamicBlobStorageController.java
│   └── service/
│       ├── BlobStorageService.java
│       └── DynamicBlobStorageService.java
├── src/main/resources/
│   └── application.yml
├── manifest.yml                 # PCF deployment manifest
├── start-azurite.sh            # Script to start Azurite
├── start-app.sh                # Script to start app with Azurite
└── pom.xml
```
