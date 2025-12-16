#!/bin/bash
# Run script for Voxel Demo on macOS Apple Silicon
# Requires: Java 21, Maven

cd "$(dirname "$0")"

# Always recompile to ensure latest changes
echo "Compiling..."
mvn compile -q

if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

# Run with required macOS flags
echo "Starting Voxel Demo..."
java -XstartOnFirstThread \
     --enable-native-access=ALL-UNNAMED \
     -Xmx2G \
     -cp "target/classes:$(mvn dependency:build-classpath -q -DincludeScope=runtime -Dmdep.outputFile=/dev/stdout)" \
     com.matejpacan.voxelcraft.Voxelcraft
