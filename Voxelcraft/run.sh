#!/bin/bash
cd "$(dirname "$0")"

echo "Building VoxelCraft..."
mvn -q compile dependency:copy-dependencies -DoutputDirectory=target/libs

if [ $? -ne 0 ]; then
    echo "Build failed!"
    exit 1
fi

echo "Running VoxelCraft..."
java -XstartOnFirstThread \
     --enable-preview \
     --enable-native-access=ALL-UNNAMED \
     -cp "target/classes:target/libs/*" \
     com.matejpacan.voxelcraft.Voxelcraft
