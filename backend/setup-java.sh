#!/bin/bash

# Set JAVA_HOME for Microsoft JDK 21
export JAVA_HOME="/c/Program Files/Microsoft/jdk-21.0.8.9-hotspot"
export PATH="$JAVA_HOME/bin:$PATH"

# Verify Java installation
echo "🔍 Checking Java installation..."
"$JAVA_HOME/bin/java.exe" -version

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Java 21 is configured correctly!"
    echo "   JAVA_HOME: $JAVA_HOME"
    echo ""
else
    echo ""
    echo "❌ Java not found. Please check the installation path."
    echo "   Expected: C:/Program Files/Microsoft/jdk-21.0.8.9-hotspot/bin/java.exe"
    echo ""
    exit 1
fi

