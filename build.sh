#!/bin/bash

# AI Code Improver Plugin Build Script
# This script builds the plugin JAR file

echo "Building AI Code Improver plugin..."

# Check prerequisites
echo "Checking prerequisites..."

# Check if we're in the project root
if [ ! -f "build.gradle.kts" ]; then
    echo "Error: build.gradle.kts not found. Please run this script from the project root directory."
    exit 1
fi

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "Error: Java is not installed or not in PATH."
    echo "Please install Java 21 or later. On Ubuntu/Debian: sudo apt install openjdk-21-jdk"
    echo "On Arch Linux: sudo pacman -S jdk-openjdk"
    echo "On macOS: brew install openjdk@21"
    exit 1
fi

# Check Java version (should be 21+)
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo "Error: Java version 21 or later is required. Current version: $JAVA_VERSION"
    exit 1
fi

echo "Prerequisites check passed."

# Set JAVA_HOME to Java 21 if available
if [ -d "/usr/lib/jvm/java-21-openjdk" ]; then
    export JAVA_HOME="/usr/lib/jvm/java-21-openjdk"
    echo "Using Java 21 from $JAVA_HOME"
elif [ -d "/usr/lib/jvm/java-21" ]; then
    export JAVA_HOME="/usr/lib/jvm/java-21"
    echo "Using Java 21 from $JAVA_HOME"
else
    echo "Warning: Java 21 not found in expected locations. Using system default."
fi

# Run the Gradle build
echo "Running Gradle build..."
./gradlew build

# Check if build was successful
if [ $? -eq 0 ]; then
    echo "Build successful!"
    echo "Plugin JAR can be found in: build/libs/"
    if [ -d "build/libs" ]; then
        ls -la build/libs/*.jar 2>/dev/null || echo "No JAR files found in build/libs/"
    else
        echo "build/libs directory not found"
    fi
else
    echo "Build failed!"
    exit 1
fi