#!/bin/bash

# RapidPhotoUpload Backend - Run Script
# =====================================
# This script sets up Java 21 and runs the backend

echo "🚀 RapidPhotoUpload Backend"
echo "============================"
echo ""

# Set JAVA_HOME
export JAVA_HOME="/c/Program Files/Microsoft/jdk-21.0.8.9-hotspot"
export PATH="$JAVA_HOME/bin:$PATH"

# Verify Java
echo "🔍 Checking Java 21..."
if ! "$JAVA_HOME/bin/java.exe" -version 2>&1 | grep -q "21.0"; then
    echo "❌ Java 21 not found!"
    echo ""
    echo "Expected: C:/Program Files/Microsoft/jdk-21.0.8.9-hotspot/bin/java.exe"
    echo ""
    exit 1
fi

echo "✅ Java 21 found!"
"$JAVA_HOME/bin/java.exe" -version
echo ""

# Build if needed
if [ "$1" == "--build" ] || [ "$1" == "-b" ]; then
    echo "🔨 Building project..."
    bash gradlew build
    if [ $? -ne 0 ]; then
        echo ""
        echo "❌ Build failed!"
        exit 1
    fi
    echo ""
    echo "✅ Build successful!"
    echo ""
fi

# Run the backend
echo "🚀 Starting backend..."
echo ""
bash gradlew bootRun

