# Voxelcraft

Legacy branch:

![Day Demo](https://i.imgur.com/1Xj9NtK.jpeg)
![Night Demo](https://i.imgur.com/pleQdCy.jpeg)

This branch:
![Modern Demo](https://i.imgur.com/nu85QH0.jpeg)

**READ SETUP INSTRUCTIONS FIRST TO GET THIS TO WORK**

NOTICE: Check out the legacy branch which I demonstrated in the beginning of my YouTube video as it contains more features, albeit a completely different codebase because I started from scratch when I recorded the video. This codebase corresponds to what we built in it.

A Minecraft-style voxel engine written in Java using LWJGL and OpenGL 4.1.

## Requirements

- Java 21
- Maven

## Setup

### 1. Download Texture Atlas

The game requires `terrain.png` from Minecraft. Download it from the Minecraft Wiki:

**[Download terrain.png](https://minecraft.wiki/index.php?title=Terrain.png#/media/File:201203210917_terrain.png)**

Save the file to `src/main/resources/terrain.png`.

### 2. Compile and Run

I tested it on macOS with Apple Silicon but should work everywhere with a few tweaks. Here is the launching script for macOS:

```bash
./run.sh
```

Or manually:

```bash
mvn compile
java -XstartOnFirstThread \
     --enable-native-access=ALL-UNNAMED \
     -Xmx2G \
     -cp "target/classes:$(mvn dependency:build-classpath -q -DincludeScope=runtime -Dmdep.outputFile=/dev/stdout)" \
     com.matejpacan.voxelcraft.Voxelcraft
```

## Controls

| Key          | Action                |
|--------------|-----------------------|
| W/A/S/D      | Move                  |
| Space        | Jump                  |
| Shift        | Sprint                |
| Mouse        | Look around           |
| Left Click   | Break block           |
| Right Click  | Place block           |
| Scroll Wheel | Change selected block |
| 1-9          | Select hotbar slot    |
| E            | Open inventory        |
| T            | Open chat/commands    |
| ESC          | Pause/Release mouse   |

## Commands

Press T to open chat, tab to tab complete. Available commands:

- `/tp <x> <y> <z>` - Teleport to coordinates (use `~` for relative, e.g. `/tp ~ ~10 ~`)
- `/time set|add|query <value>` - Manage time (values: day, noon, night, sunrise, sunset, midnight, or 0-23999)
- `/cycle on|off` - Toggle day/night cycle
- `/clear` - Clear chat history
- `/help` - Show available commands

## Implemented Features

### Rendering
- Chunk-based voxel rendering with greedy meshing optimization
- Frustum culling and occlusion culling
- Dynamic shadow mapping
- Volumetric clouds
- God rays (crepuscular rays)
- Post-processing effects
- Day/night cycle with sun and moon
- Dynamic sky rendering

### Gameplay
- First-person camera with smooth movement
- Block placement and destruction
- Hotbar and inventory system
- Item drops with physics
- Multiple block types (stone, dirt, grass, wood, leaves, sand, water, etc.)
- Flight mode
- Commands with tab-completion

### World Generation
- Procedural terrain generation with Simplex noise
- Multiple biomes (plains, forest, desert, mountains, ocean, swamp, taiga, jungle, savanna, badlands)
- Trees and vegetation
- Water bodies and beaches
- Caves

### Technical
- Chunk loading/unloading based on player position
- Efficient mesh batching
- Buffer pooling for memory optimization
- Fluid simulation system
