#!/bin/bash
# repair_gradle.sh
# Removes any corrupt gradle-wrapper.jar and regenerates a clean, stable wrapper configuration.

set -e

WRAPPER_JAR="gradle/wrapper/gradle-wrapper.jar"
GRADLE_VERSION="8.5"

echo "=== Initializing Gradle Wrapper Repair ==="

# Delete existing corrupt wrapper JAR
if [ -f "$WRAPPER_JAR" ]; then
    echo "Deleting potentially corrupt wrapper jar: $WRAPPER_JAR..."
    rm -f "$WRAPPER_JAR"
fi

# Regenerate using local gradle CLI
if command -v gradle &> /dev/null; then
    echo "Generating fresh Gradle wrapper v${GRADLE_VERSION}..."
    gradle wrapper --gradle-version "$GRADLE_VERSION"
    echo "Successfully regenerated wrapper."
else
    echo "Warning: Local 'gradle' command not found. Unable to regenerate wrapper automatically."
    echo "Downloading stable wrapper binary directly..."
    mkdir -p gradle/wrapper
    curl -L -o "$WRAPPER_JAR" "https://raw.githubusercontent.com/gradle/gradle/v${GRADLE_VERSION}.0/gradle/wrapper/gradle-wrapper.jar"
    echo "Successfully downloaded wrapper jar via curl."
fi

# Ensure executable permissions are correct
if [ -f "gradlew" ]; then
    chmod +x gradlew
    echo "Set executable permission on gradlew."
fi

echo "=== Repair Completed Successfully! ==="
