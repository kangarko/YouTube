package com.matejpacan.voxelcraft;

import lombok.Getter;

@Getter
public enum Biome {

	PLAINS(
			0.5f, 0.5f,
			BlockType.GRASS, BlockType.DIRT,
			new float[] { 0.42f, 0.52f, 0.32f },
			new float[] { 0.40f, 0.50f, 0.30f },
			new float[] { 1.0f, 1.0f, 1.0f },
			new float[] { 1.0f, 1.0f, 1.0f },
			0.008f, new BlockType[] { BlockType.OAK_LOG },
			0.06f, new BlockType[] { BlockType.TALL_GRASS, BlockType.DANDELION, BlockType.ROSE },
			68, 12),

	FLOWER_PLAINS(
			0.55f, 0.6f,
			BlockType.GRASS, BlockType.DIRT,
			new float[] { 0.40f, 0.50f, 0.30f },
			new float[] { 0.38f, 0.48f, 0.28f },
			new float[] { 1.0f, 1.0f, 1.0f },
			new float[] { 1.0f, 1.0f, 1.0f },
			0.005f, new BlockType[] { BlockType.OAK_LOG, BlockType.BIRCH_LOG },
			0.15f, new BlockType[] { BlockType.DANDELION, BlockType.ROSE, BlockType.TALL_GRASS },
			65, 8),

	SUNFLOWER_PLAINS(
			0.58f, 0.55f,
			BlockType.GRASS, BlockType.DIRT,
			new float[] { 0.45f, 0.55f, 0.35f },
			new float[] { 0.43f, 0.53f, 0.33f },
			new float[] { 1.0f, 1.0f, 1.0f },
			new float[] { 1.0f, 1.0f, 1.0f },
			0.003f, new BlockType[] { BlockType.OAK_LOG },
			0.12f, new BlockType[] { BlockType.DANDELION, BlockType.TALL_GRASS },
			66, 6),

	FOREST(
			0.5f, 0.7f,
			BlockType.GRASS, BlockType.DIRT,
			new float[] { 0.32f, 0.45f, 0.25f },
			new float[] { 0.30f, 0.42f, 0.22f },
			new float[] { 1.0f, 1.0f, 1.0f },
			new float[] { 0.95f, 0.97f, 0.95f },
			0.07f, new BlockType[] { BlockType.OAK_LOG },
			0.06f, new BlockType[] { BlockType.TALL_GRASS, BlockType.FERN, BlockType.BROWN_MUSHROOM },
			70, 18),

	BIRCH_FOREST(
			0.45f, 0.65f,
			BlockType.GRASS, BlockType.DIRT,
			new float[] { 0.40f, 0.52f, 0.32f },
			new float[] { 0.45f, 0.55f, 0.38f },
			new float[] { 1.0f, 1.0f, 1.0f },
			new float[] { 1.0f, 1.0f, 1.0f },
			0.065f, new BlockType[] { BlockType.BIRCH_LOG },
			0.05f, new BlockType[] { BlockType.TALL_GRASS, BlockType.FERN, BlockType.DANDELION },
			68, 14),

	DARK_FOREST(
			0.5f, 0.8f,
			BlockType.GRASS, BlockType.DIRT,
			new float[] { 0.22f, 0.35f, 0.18f },
			new float[] { 0.20f, 0.32f, 0.15f },
			new float[] { 1.0f, 1.0f, 1.0f },
			new float[] { 0.90f, 0.92f, 0.90f },
			0.12f, new BlockType[] { BlockType.OAK_LOG },
			0.08f, new BlockType[] { BlockType.BROWN_MUSHROOM, BlockType.RED_MUSHROOM, BlockType.FERN },
			72, 20),

	OLD_GROWTH_FOREST(
			0.52f, 0.75f,
			BlockType.GRASS, BlockType.DIRT,
			new float[] { 0.28f, 0.42f, 0.22f },
			new float[] { 0.26f, 0.40f, 0.20f },
			new float[] { 1.0f, 1.0f, 1.0f },
			new float[] { 0.92f, 0.94f, 0.92f },
			0.09f, new BlockType[] { BlockType.OAK_LOG, BlockType.SPRUCE_LOG },
			0.07f, new BlockType[] { BlockType.FERN, BlockType.BROWN_MUSHROOM, BlockType.TALL_GRASS },
			75, 22),

	DESERT(
			1.0f, 0.0f,
			BlockType.SAND, BlockType.SANDSTONE,
			new float[] { 0.65f, 0.58f, 0.40f },
			new float[] { 0.65f, 0.58f, 0.40f },
			new float[] { 1.02f, 1.0f, 0.95f },
			new float[] { 1.0f, 0.98f, 0.95f },
			0.0f, new BlockType[] {},
			0.008f, new BlockType[] { BlockType.DEAD_BUSH, BlockType.CACTUS },
			64, 8),

	MESA(
			0.95f, 0.1f,
			BlockType.SAND, BlockType.SANDSTONE,
			new float[] { 0.68f, 0.50f, 0.35f },
			new float[] { 0.68f, 0.50f, 0.35f },
			new float[] { 1.05f, 0.95f, 0.90f },
			new float[] { 1.02f, 0.97f, 0.92f },
			0.0f, new BlockType[] {},
			0.004f, new BlockType[] { BlockType.DEAD_BUSH },
			85, 45),

	BADLANDS(
			0.92f, 0.05f,
			BlockType.SAND, BlockType.SANDSTONE,
			new float[] { 0.72f, 0.48f, 0.32f },
			new float[] { 0.72f, 0.48f, 0.32f },
			new float[] { 1.05f, 0.92f, 0.88f },
			new float[] { 1.03f, 0.95f, 0.90f },
			0.0f, new BlockType[] {},
			0.002f, new BlockType[] { BlockType.DEAD_BUSH },
			95, 55),

	TAIGA(
			0.18f, 0.55f,
			BlockType.GRASS, BlockType.DIRT,
			new float[] { 0.35f, 0.45f, 0.32f },
			new float[] { 0.32f, 0.42f, 0.30f },
			new float[] { 1.0f, 1.0f, 1.0f },
			new float[] { 0.95f, 0.98f, 0.95f },
			0.055f, new BlockType[] { BlockType.SPRUCE_LOG },
			0.04f, new BlockType[] { BlockType.FERN, BlockType.TALL_GRASS },
			72, 20),

	OLD_GROWTH_TAIGA(
			0.15f, 0.6f,
			BlockType.GRASS, BlockType.DIRT,
			new float[] { 0.30f, 0.42f, 0.28f },
			new float[] { 0.28f, 0.40f, 0.26f },
			new float[] { 1.0f, 1.0f, 1.0f },
			new float[] { 0.92f, 0.95f, 0.92f },
			0.08f, new BlockType[] { BlockType.SPRUCE_LOG },
			0.05f, new BlockType[] { BlockType.FERN, BlockType.BROWN_MUSHROOM },
			78, 25),

	SNOWY_TAIGA(
			0.0f, 0.45f,
			BlockType.SNOWY_GRASS, BlockType.DIRT,
			new float[] { 0.38f, 0.48f, 0.38f },
			new float[] { 0.35f, 0.45f, 0.35f },
			new float[] { 1.0f, 1.0f, 1.0f },
			new float[] { 0.98f, 0.98f, 1.0f },
			0.05f, new BlockType[] { BlockType.SPRUCE_LOG },
			0.025f, new BlockType[] { BlockType.FERN },
			75, 22),

	SNOWY_PLAINS(
			-0.1f, 0.35f,
			BlockType.SNOW_BLOCK, BlockType.DIRT,
			new float[] { 0.42f, 0.52f, 0.42f },
			new float[] { 0.40f, 0.50f, 0.40f },
			new float[] { 1.0f, 1.0f, 1.0f },
			new float[] { 0.98f, 0.98f, 1.0f },
			0.003f, new BlockType[] { BlockType.SPRUCE_LOG },
			0.005f, new BlockType[] {},
			65, 8),

	ICE_SPIKES(
			-0.2f, 0.25f,
			BlockType.SNOW_BLOCK, BlockType.SNOW_BLOCK,
			new float[] { 0.45f, 0.55f, 0.48f },
			new float[] { 0.42f, 0.52f, 0.45f },
			new float[] { 1.0f, 1.0f, 1.0f },
			new float[] { 0.95f, 0.98f, 1.0f },
			0.0f, new BlockType[] {},
			0.0f, new BlockType[] {},
			70, 15),

	FROZEN_PEAKS(
			-0.3f, 0.2f,
			BlockType.SNOW_BLOCK, BlockType.STONE,
			new float[] { 0.45f, 0.52f, 0.48f },
			new float[] { 0.42f, 0.50f, 0.45f },
			new float[] { 0.95f, 0.98f, 1.0f },
			new float[] { 0.92f, 0.95f, 1.0f },
			0.0f, new BlockType[] {},
			0.0f, new BlockType[] {},
			160, 80),

	MOUNTAINS(
			0.2f, 0.35f,
			BlockType.STONE, BlockType.STONE,
			new float[] { 0.38f, 0.45f, 0.35f },
			new float[] { 0.36f, 0.43f, 0.33f },
			new float[] { 0.95f, 0.95f, 0.98f },
			new float[] { 0.92f, 0.92f, 0.95f },
			0.01f, new BlockType[] { BlockType.SPRUCE_LOG },
			0.015f, new BlockType[] { BlockType.TALL_GRASS },
			120, 70),

	JAGGED_PEAKS(
			0.15f, 0.3f,
			BlockType.STONE, BlockType.STONE,
			new float[] { 0.36f, 0.43f, 0.34f },
			new float[] { 0.34f, 0.41f, 0.32f },
			new float[] { 0.92f, 0.92f, 0.95f },
			new float[] { 0.90f, 0.90f, 0.92f },
			0.0f, new BlockType[] {},
			0.005f, new BlockType[] {},
			180, 70),

	STONY_PEAKS(
			0.25f, 0.25f,
			BlockType.STONE, BlockType.STONE,
			new float[] { 0.40f, 0.46f, 0.38f },
			new float[] { 0.38f, 0.44f, 0.36f },
			new float[] { 0.90f, 0.90f, 0.92f },
			new float[] { 0.88f, 0.88f, 0.90f },
			0.005f, new BlockType[] { BlockType.SPRUCE_LOG },
			0.01f, new BlockType[] { BlockType.TALL_GRASS },
			150, 60),

	MEADOW(
			0.42f, 0.58f,
			BlockType.GRASS, BlockType.DIRT,
			new float[] { 0.44f, 0.56f, 0.38f },
			new float[] { 0.42f, 0.54f, 0.36f },
			new float[] { 1.0f, 1.0f, 1.0f },
			new float[] { 1.0f, 1.0f, 1.0f },
			0.006f, new BlockType[] { BlockType.OAK_LOG, BlockType.BIRCH_LOG },
			0.10f, new BlockType[] { BlockType.TALL_GRASS, BlockType.DANDELION, BlockType.ROSE },
			95, 30),

	CHERRY_GROVE(
			0.48f, 0.6f,
			BlockType.GRASS, BlockType.DIRT,
			new float[] { 0.42f, 0.52f, 0.40f },
			new float[] { 0.65f, 0.45f, 0.52f },
			new float[] { 1.0f, 1.0f, 1.0f },
			new float[] { 1.0f, 1.0f, 1.0f },
			0.04f, new BlockType[] { BlockType.BIRCH_LOG },
			0.08f, new BlockType[] { BlockType.TALL_GRASS, BlockType.DANDELION, BlockType.ROSE },
			90, 25),

	WINDSWEPT_HILLS(
			0.35f, 0.4f,
			BlockType.GRASS, BlockType.STONE,
			new float[] { 0.38f, 0.48f, 0.32f },
			new float[] { 0.36f, 0.46f, 0.30f },
			new float[] { 0.98f, 0.98f, 1.0f },
			new float[] { 0.95f, 0.95f, 0.98f },
			0.02f, new BlockType[] { BlockType.OAK_LOG },
			0.03f, new BlockType[] { BlockType.TALL_GRASS },
			100, 40),

	WINDSWEPT_FOREST(
			0.38f, 0.5f,
			BlockType.GRASS, BlockType.STONE,
			new float[] { 0.34f, 0.45f, 0.28f },
			new float[] { 0.32f, 0.43f, 0.26f },
			new float[] { 0.98f, 0.98f, 1.0f },
			new float[] { 0.95f, 0.95f, 0.98f },
			0.05f, new BlockType[] { BlockType.OAK_LOG, BlockType.SPRUCE_LOG },
			0.04f, new BlockType[] { BlockType.TALL_GRASS, BlockType.FERN },
			105, 45),

	SWAMP(
			0.62f, 0.92f,
			BlockType.GRASS, BlockType.DIRT,
			new float[] { 0.28f, 0.36f, 0.22f },
			new float[] { 0.26f, 0.34f, 0.18f },
			new float[] { 0.95f, 0.98f, 0.92f },
			new float[] { 0.92f, 0.95f, 0.90f },
			0.03f, new BlockType[] { BlockType.OAK_LOG },
			0.06f, new BlockType[] { BlockType.BROWN_MUSHROOM, BlockType.RED_MUSHROOM, BlockType.DEAD_BUSH },
			62, 4),

	MANGROVE_SWAMP(
			0.68f, 0.95f,
			BlockType.GRASS, BlockType.DIRT,
			new float[] { 0.30f, 0.40f, 0.24f },
			new float[] { 0.28f, 0.38f, 0.22f },
			new float[] { 0.95f, 0.98f, 0.92f },
			new float[] { 0.92f, 0.95f, 0.90f },
			0.05f, new BlockType[] { BlockType.OAK_LOG },
			0.05f, new BlockType[] { BlockType.FERN, BlockType.BROWN_MUSHROOM },
			61, 3),

	JUNGLE(
			0.92f, 1.0f,
			BlockType.GRASS, BlockType.DIRT,
			new float[] { 0.25f, 0.42f, 0.22f },
			new float[] { 0.23f, 0.40f, 0.20f },
			new float[] { 1.0f, 1.0f, 0.98f },
			new float[] { 0.98f, 1.0f, 0.96f },
			0.10f, new BlockType[] { BlockType.JUNGLE_LOG },
			0.14f, new BlockType[] { BlockType.FERN, BlockType.TALL_GRASS },
			72, 20),

	SPARSE_JUNGLE(
			0.88f, 0.88f,
			BlockType.GRASS, BlockType.DIRT,
			new float[] { 0.30f, 0.45f, 0.26f },
			new float[] { 0.28f, 0.43f, 0.24f },
			new float[] { 1.0f, 1.0f, 0.98f },
			new float[] { 0.98f, 1.0f, 0.96f },
			0.045f, new BlockType[] { BlockType.JUNGLE_LOG, BlockType.OAK_LOG },
			0.10f, new BlockType[] { BlockType.FERN, BlockType.TALL_GRASS },
			70, 15),

	BAMBOO_JUNGLE(
			0.9f, 0.95f,
			BlockType.GRASS, BlockType.DIRT,
			new float[] { 0.28f, 0.44f, 0.24f },
			new float[] { 0.26f, 0.42f, 0.22f },
			new float[] { 1.0f, 1.0f, 0.98f },
			new float[] { 0.98f, 1.0f, 0.96f },
			0.06f, new BlockType[] { BlockType.JUNGLE_LOG },
			0.12f, new BlockType[] { BlockType.FERN, BlockType.TALL_GRASS },
			70, 16),

	SAVANNA(
			0.88f, 0.18f,
			BlockType.GRASS, BlockType.DIRT,
			new float[] { 0.58f, 0.55f, 0.38f },
			new float[] { 0.56f, 0.53f, 0.36f },
			new float[] { 1.0f, 0.98f, 0.95f },
			new float[] { 0.98f, 0.96f, 0.92f },
			0.012f, new BlockType[] { BlockType.OAK_LOG },
			0.035f, new BlockType[] { BlockType.TALL_GRASS, BlockType.DEAD_BUSH },
			68, 10),

	SAVANNA_PLATEAU(
			0.85f, 0.2f,
			BlockType.GRASS, BlockType.DIRT,
			new float[] { 0.56f, 0.52f, 0.36f },
			new float[] { 0.54f, 0.50f, 0.34f },
			new float[] { 1.0f, 0.98f, 0.95f },
			new float[] { 0.98f, 0.96f, 0.92f },
			0.008f, new BlockType[] { BlockType.OAK_LOG },
			0.028f, new BlockType[] { BlockType.TALL_GRASS, BlockType.DEAD_BUSH },
			100, 20),

	WINDSWEPT_SAVANNA(
			0.82f, 0.15f,
			BlockType.GRASS, BlockType.STONE,
			new float[] { 0.70f, 0.65f, 0.40f },
			new float[] { 0.68f, 0.62f, 0.38f },
			new float[] { 1.02f, 1.0f, 0.95f },
			new float[] { 0.98f, 0.96f, 0.92f },
			0.006f, new BlockType[] { BlockType.OAK_LOG },
			0.02f, new BlockType[] { BlockType.TALL_GRASS, BlockType.DEAD_BUSH },
			110, 50),

	BEACH(
			0.5f, 0.8f,
			BlockType.SAND, BlockType.SAND,
			new float[] { 0.55f, 0.75f, 0.42f },
			new float[] { 0.52f, 0.72f, 0.40f },
			new float[] { 1.02f, 1.0f, 0.95f },
			new float[] { 1.0f, 0.98f, 0.92f },
			0.0f, new BlockType[] {},
			0.0f, new BlockType[] {},
			63, 1),

	STONY_SHORE(
			0.4f, 0.5f,
			BlockType.STONE, BlockType.STONE,
			new float[] { 0.50f, 0.62f, 0.48f },
			new float[] { 0.48f, 0.58f, 0.45f },
			new float[] { 0.95f, 0.95f, 0.98f },
			new float[] { 0.92f, 0.92f, 0.95f },
			0.0f, new BlockType[] {},
			0.0f, new BlockType[] {},
			64, 3),

	RIVER(
			0.5f, 1.0f,
			BlockType.SAND, BlockType.SAND,
			new float[] { 0.55f, 0.75f, 0.42f },
			new float[] { 0.52f, 0.72f, 0.40f },
			new float[] { 1.0f, 1.0f, 1.0f },
			new float[] { 1.0f, 1.0f, 1.0f },
			0.0f, new BlockType[] {},
			0.0f, new BlockType[] {},
			58, 4),

	OCEAN(
			0.5f, 1.0f,
			BlockType.SAND, BlockType.SAND,
			new float[] { 0.55f, 0.75f, 0.42f },
			new float[] { 0.52f, 0.72f, 0.40f },
			new float[] { 1.0f, 1.0f, 1.02f },
			new float[] { 1.0f, 1.0f, 1.0f },
			0.0f, new BlockType[] {},
			0.0f, new BlockType[] {},
			45, 15),

	DEEP_OCEAN(
			0.5f, 1.0f,
			BlockType.GRAVEL, BlockType.GRAVEL,
			new float[] { 0.55f, 0.75f, 0.42f },
			new float[] { 0.52f, 0.72f, 0.40f },
			new float[] { 1.0f, 1.0f, 1.05f },
			new float[] { 1.0f, 1.0f, 1.02f },
			0.0f, new BlockType[] {},
			0.0f, new BlockType[] {},
			30, 12),

	LUKEWARM_OCEAN(
			0.6f, 1.0f,
			BlockType.SAND, BlockType.SAND,
			new float[] { 0.55f, 0.75f, 0.42f },
			new float[] { 0.52f, 0.72f, 0.40f },
			new float[] { 1.0f, 1.02f, 1.0f },
			new float[] { 1.0f, 1.0f, 1.0f },
			0.0f, new BlockType[] {},
			0.0f, new BlockType[] {},
			48, 12),

	WARM_OCEAN(
			0.7f, 1.0f,
			BlockType.SAND, BlockType.SAND,
			new float[] { 0.55f, 0.75f, 0.42f },
			new float[] { 0.52f, 0.72f, 0.40f },
			new float[] { 1.02f, 1.02f, 0.98f },
			new float[] { 1.0f, 1.0f, 0.98f },
			0.0f, new BlockType[] {},
			0.0f, new BlockType[] {},
			50, 10);

	private final float temperature;
	private final float humidity;
	private final BlockType surfaceBlock;
	private final BlockType subsurfaceBlock;
	private final float[] grassColor;
	private final float[] foliageColor;
	private final float[] sandTint;
	private final float[] stoneTint;
	private final float treeDensity;
	private final BlockType[] treeTypes;
	private final float vegetationDensity;
	private final BlockType[] vegetationTypes;
	private final int baseHeight;
	private final int heightVariation;

	Biome(float temperature, float humidity,
			BlockType surfaceBlock, BlockType subsurfaceBlock,
			float[] grassColor, float[] foliageColor,
			float[] sandTint, float[] stoneTint,
			float treeDensity, BlockType[] treeTypes,
			float vegetationDensity, BlockType[] vegetationTypes,
			int baseHeight, int heightVariation) {
		this.temperature = temperature;
		this.humidity = humidity;
		this.surfaceBlock = surfaceBlock;
		this.subsurfaceBlock = subsurfaceBlock;
		this.grassColor = grassColor;
		this.foliageColor = foliageColor;
		this.sandTint = sandTint;
		this.stoneTint = stoneTint;
		this.treeDensity = treeDensity;
		this.treeTypes = treeTypes;
		this.vegetationDensity = vegetationDensity;
		this.vegetationTypes = vegetationTypes;
		this.baseHeight = baseHeight;
		this.heightVariation = heightVariation;
	}

	public static Biome fromClimate(float temperature, float humidity, float continentality, float erosion,
			float weirdness, int terrainHeight) {
		final int seaLevel = 62;

		if (terrainHeight < seaLevel - 30)
			return DEEP_OCEAN;

		if (terrainHeight < seaLevel - 8) {
			if (temperature > 0.7f)
				return WARM_OCEAN;
			if (temperature > 0.5f)
				return LUKEWARM_OCEAN;
			return OCEAN;
		}

		if (terrainHeight >= seaLevel - 3 && terrainHeight <= seaLevel + 1) {
			if (erosion > 0.75f)
				return STONY_SHORE;
			if (continentality < 0.2f)
				return BEACH;
		}

		if (terrainHeight > 185 || terrainHeight > 170 && erosion < 0.15f) {
			if (temperature < 0.2f)
				return FROZEN_PEAKS;
			if (weirdness > 0.6f)
				return JAGGED_PEAKS;
			return STONY_PEAKS;
		}

		if (terrainHeight > 150 || terrainHeight > 130 && erosion < 0.25f)
			return MOUNTAINS;

		if (terrainHeight > 115 && erosion < 0.35f) {
			if (humidity > 0.5f)
				return WINDSWEPT_FOREST;
			return WINDSWEPT_HILLS;
		}

		if (terrainHeight > 95 && erosion < 0.45f) {
			if (temperature > 0.8f && humidity < 0.3f)
				return WINDSWEPT_SAVANNA;
			if (weirdness > 0.3f)
				return CHERRY_GROVE;
			return MEADOW;
		}

		if (temperature < 0.1f) {
			if (humidity < 0.35f)
				return weirdness > 0.75f ? ICE_SPIKES : SNOWY_PLAINS;
			if (terrainHeight > 80)
				return FROZEN_PEAKS;
			return SNOWY_TAIGA;
		}

		if (temperature < 0.25f) {
			if (humidity > 0.6f)
				return terrainHeight > 75 ? OLD_GROWTH_TAIGA : TAIGA;
			if (humidity > 0.4f)
				return TAIGA;
			return SNOWY_PLAINS;
		}

		if (temperature > 0.9f) {
			if (humidity < 0.2f) {
				if (weirdness > 0.6f || terrainHeight > 85)
					return terrainHeight > 95 ? BADLANDS : MESA;
				return DESERT;
			}
			if (humidity < 0.4f) {
				if (terrainHeight > 90)
					return SAVANNA_PLATEAU;
				return SAVANNA;
			}
			if (humidity > 0.85f)
				return terrainHeight > 68 ? JUNGLE : BAMBOO_JUNGLE;
			return SPARSE_JUNGLE;
		}

		if (humidity > 0.88f && temperature > 0.55f && terrainHeight < seaLevel + 6)
			return weirdness > 0.3f ? MANGROVE_SWAMP : SWAMP;

		if (terrainHeight > 75) {
			if (humidity > 0.6f)
				return WINDSWEPT_FOREST;
			if (erosion > 0.5f)
				return WINDSWEPT_HILLS;
			return MEADOW;
		}

		if (humidity > 0.7f) {
			if (weirdness > 0.5f)
				return DARK_FOREST;
			if (weirdness > 0.2f)
				return OLD_GROWTH_FOREST;
			if (weirdness < -0.3f)
				return BIRCH_FOREST;
			return FOREST;
		}

		if (humidity > 0.5f) {
			if (weirdness > 0.4f)
				return BIRCH_FOREST;
			if (weirdness < -0.35f)
				return FOREST;
			return FLOWER_PLAINS;
		}

		if (humidity > 0.35f) {
			if (weirdness > 0.5f)
				return SUNFLOWER_PLAINS;
			return FLOWER_PLAINS;
		}

		return PLAINS;
	}

	public BlockType getTreeLog() {
		if (this.treeTypes.length == 0)
			return null;
		return this.treeTypes[0];
	}

	public BlockType getTreeLeaves() {
		if (this.treeTypes.length == 0)
			return null;

		final BlockType log = this.treeTypes[0];
		if (log == BlockType.SPRUCE_LOG)
			return BlockType.SPRUCE_LEAVES;
		if (log == BlockType.BIRCH_LOG)
			return BlockType.BIRCH_LEAVES;
		if (log == BlockType.JUNGLE_LOG)
			return BlockType.OAK_LEAVES;
		return BlockType.OAK_LEAVES;
	}

	public boolean hasSecondaryTree() {
		return this.treeTypes.length > 1;
	}

	public BlockType getSecondaryTreeLog() {
		if (this.treeTypes.length < 2)
			return null;
		return this.treeTypes[1];
	}

	public BlockType getSecondaryTreeLeaves() {
		if (this.treeTypes.length < 2)
			return null;

		final BlockType log = this.treeTypes[1];
		if (log == BlockType.SPRUCE_LOG)
			return BlockType.SPRUCE_LEAVES;
		if (log == BlockType.BIRCH_LOG)
			return BlockType.BIRCH_LEAVES;
		if (log == BlockType.JUNGLE_LOG)
			return BlockType.OAK_LEAVES;
		return BlockType.OAK_LEAVES;
	}
}
