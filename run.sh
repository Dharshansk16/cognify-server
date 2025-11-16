#!/bin/bash

echo "🚀 Starting Cognify Backend..."
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

echo "📦 Starting database containers..."
docker-compose up -d

echo ""
echo "⏳ Waiting for databases to be ready..."
sleep 5

echo ""
echo "🔨 Building the application..."
mvn clean package -DskipTests

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Build successful!"
    echo ""
    echo "🎯 Starting Spring Boot application..."
    java -jar target/cognify-backend-0.0.1-SNAPSHOT.jar
else
    echo ""
    echo "❌ Build failed. Please check the errors above."
    exit 1
fi
