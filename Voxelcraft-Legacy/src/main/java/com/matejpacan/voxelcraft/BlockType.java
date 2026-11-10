package com.matejpacan.voxelcraft;

import lombok.Getter;

@Getter
public enum BlockType {

	AIR(false, false, 0, null, null, null),

	GRASS(true, true, 0,
			new int[] { 0, 0 },
			new int[] { 0, 2 },
			new int[] { 0, 3 }),

	DIRT(true, true, 0,
			new int[] { 0, 2 },
			new int[] { 0, 2 },
			new int[] { 0, 2 }),

	STONE(true, true, 0,
			new int[] { 0, 1 },
			new int[] { 0, 1 },
			new int[] { 0, 1 }),

	COBBLESTONE(true, true, 0,
			new int[] { 1, 0 },
			new int[] { 1, 0 },
			new int[] { 1, 0 }),

	OBSIDIAN(true, true, 0,
			new int[] { 5, 2 },
			new int[] { 5, 2 },
			new int[] { 5, 2 }),

	BEDROCK(true, true, 0,
			new int[] { 1, 1 },
			new int[] { 1, 1 },
			new int[] { 1, 1 }),

	SAND(true, true, 0,
			new int[] { 1, 2 },
			new int[] { 1, 2 },
			new int[] { 1, 2 }),

	GRAVEL(true, true, 0,
			new int[] { 1, 3 },
			new int[] { 1, 3 },
			new int[] { 1, 3 }),

	OAK_LOG(true, true, 0,
			new int[] { 1, 5 },
			new int[] { 1, 5 },
			new int[] { 1, 4 }),

	OAK_PLANKS(true, true, 0,
			new int[] { 0, 4 },
			new int[] { 0, 4 },
			new int[] { 0, 4 }),

	OAK_LEAVES(true, false, 0,
			new int[] { 3, 4 },
			new int[] { 3, 4 },
			new int[] { 3, 4 }),

	BRICKS(true, true, 0,
			new int[] { 0, 7 },
			new int[] { 0, 7 },
			new int[] { 0, 7 }),

	GLASS(true, false, 0,
			new int[] { 3, 1 },
			new int[] { 3, 1 },
			new int[] { 3, 1 }),

	WATER(true, false, 0,
			new int[] { 15, 13 },
			new int[] { 15, 13 },
			new int[] { 15, 13 }) {
		@Override
		public boolean isWater() {
			return true;
		}

		@Override
		public boolean isSourceWater() {
			return true;
		}
	},

	WATER_FLOWING(true, false, 0,
			new int[] { 15, 13 },
			new int[] { 15, 13 },
			new int[] { 15, 13 }) {
		@Override
		public boolean isWater() {
			return true;
		}
	},

	GLOWSTONE(true, true, 15,
			new int[] { 6, 9 },
			new int[] { 6, 9 },
			new int[] { 6, 9 }),

	TORCH(false, false, 14,
			new int[] { 5, 0 },
			new int[] { 5, 0 },
			new int[] { 5, 0 }),

	LAVA(true, false, 15,
			new int[] { 14, 13 },
			new int[] { 14, 13 },
			new int[] { 14, 13 }) {
		@Override
		public boolean isLava() {
			return true;
		}

		@Override
		public boolean isSourceLava() {
			return true;
		}
	},

	LAVA_FLOWING(true, false, 12,
			new int[] { 14, 13 },
			new int[] { 14, 13 },
			new int[] { 14, 13 }) {
		@Override
		public boolean isLava() {
			return true;
		}
	},

	GOLD_ORE(true, true, 0,
			new int[] { 2, 0 },
			new int[] { 2, 0 },
			new int[] { 2, 0 }),

	IRON_ORE(true, true, 0,
			new int[] { 2, 1 },
			new int[] { 2, 1 },
			new int[] { 2, 1 }),

	COAL_ORE(true, true, 0,
			new int[] { 2, 2 },
			new int[] { 2, 2 },
			new int[] { 2, 2 }),

	DIAMOND_ORE(true, true, 0,
			new int[] { 3, 2 },
			new int[] { 3, 2 },
			new int[] { 3, 2 }),

	REDSTONE_ORE(true, true, 0,
			new int[] { 3, 3 },
			new int[] { 3, 3 },
			new int[] { 3, 3 }),

	LAPIS_ORE(true, true, 0,
			new int[] { 0, 10 },
			new int[] { 0, 10 },
			new int[] { 0, 10 }),

	GOLD_BLOCK(true, true, 0,
			new int[] { 1, 6 },
			new int[] { 1, 6 },
			new int[] { 1, 6 }),

	IRON_BLOCK(true, true, 0,
			new int[] { 1, 7 },
			new int[] { 1, 7 },
			new int[] { 1, 7 }),

	DIAMOND_BLOCK(true, true, 0,
			new int[] { 1, 8 },
			new int[] { 1, 8 },
			new int[] { 1, 8 }),

	LAPIS_BLOCK(true, true, 0,
			new int[] { 0, 9 },
			new int[] { 0, 9 },
			new int[] { 0, 9 }),

	BOOKSHELF(true, true, 0,
			new int[] { 0, 4 },
			new int[] { 0, 4 },
			new int[] { 2, 3 }),

	MOSSY_COBBLESTONE(true, true, 0,
			new int[] { 2, 4 },
			new int[] { 2, 4 },
			new int[] { 2, 4 }),

	SPONGE(true, true, 0,
			new int[] { 3, 0 },
			new int[] { 3, 0 },
			new int[] { 3, 0 }),

	TNT(true, true, 0,
			new int[] { 0, 9 },
			new int[] { 0, 10 },
			new int[] { 0, 8 }),

	CRAFTING_TABLE(true, true, 0,
			new int[] { 2, 11 },
			new int[] { 0, 4 },
			new int[] { 3, 11 }),

	FURNACE(true, true, 0,
			new int[] { 3, 14 },
			new int[] { 3, 14 },
			new int[] { 2, 12 }),

	FURNACE_LIT(true, true, 13,
			new int[] { 3, 14 },
			new int[] { 3, 14 },
			new int[] { 2, 13 }),

	DISPENSER(true, true, 0,
			new int[] { 3, 14 },
			new int[] { 3, 14 },
			new int[] { 2, 14 }),

	STONE_BRICKS(true, true, 0,
			new int[] { 3, 6 },
			new int[] { 3, 6 },
			new int[] { 3, 6 }),

	SANDSTONE(true, true, 0,
			new int[] { 11, 0 },
			new int[] { 13, 0 },
			new int[] { 12, 0 }),

	SNOW_BLOCK(true, true, 0,
			new int[] { 4, 2 },
			new int[] { 4, 2 },
			new int[] { 4, 2 }),

	SNOWY_GRASS(true, true, 0,
			new int[] { 4, 2 },
			new int[] { 0, 2 },
			new int[] { 4, 4 }),

	ICE(true, false, 0,
			new int[] { 4, 3 },
			new int[] { 4, 3 },
			new int[] { 4, 3 }),

	CLAY(true, true, 0,
			new int[] { 8, 4 },
			new int[] { 8, 4 },
			new int[] { 8, 4 }),

	CACTUS(true, false, 0,
			new int[] { 4, 5 },
			new int[] { 4, 7 },
			new int[] { 4, 6 }),

	SUGAR_CANE(false, false, 0,
			new int[] { 4, 9 },
			new int[] { 4, 9 },
			new int[] { 4, 9 }),

	PUMPKIN(true, true, 0,
			new int[] { 6, 6 },
			new int[] { 6, 6 },
			new int[] { 7, 6 }),

	JACK_O_LANTERN(true, true, 15,
			new int[] { 6, 6 },
			new int[] { 6, 6 },
			new int[] { 6, 8 }),

	MELON(true, true, 0,
			new int[] { 9, 8 },
			new int[] { 9, 8 },
			new int[] { 8, 8 }),

	NETHERRACK(true, true, 0,
			new int[] { 7, 0 },
			new int[] { 7, 0 },
			new int[] { 7, 0 }),

	SOUL_SAND(true, true, 0,
			new int[] { 8, 6 },
			new int[] { 8, 6 },
			new int[] { 8, 6 }),

	WHITE_WOOL(true, true, 0,
			new int[] { 4, 0 },
			new int[] { 4, 0 },
			new int[] { 4, 0 }),

	ORANGE_WOOL(true, true, 0,
			new int[] { 13, 1 },
			new int[] { 13, 1 },
			new int[] { 13, 1 }),

	MAGENTA_WOOL(true, true, 0,
			new int[] { 13, 2 },
			new int[] { 13, 2 },
			new int[] { 13, 2 }),

	LIGHT_BLUE_WOOL(true, true, 0,
			new int[] { 13, 3 },
			new int[] { 13, 3 },
			new int[] { 13, 3 }),

	YELLOW_WOOL(true, true, 0,
			new int[] { 13, 4 },
			new int[] { 13, 4 },
			new int[] { 13, 4 }),

	LIME_WOOL(true, true, 0,
			new int[] { 13, 5 },
			new int[] { 13, 5 },
			new int[] { 13, 5 }),

	PINK_WOOL(true, true, 0,
			new int[] { 13, 6 },
			new int[] { 13, 6 },
			new int[] { 13, 6 }),

	GRAY_WOOL(true, true, 0,
			new int[] { 13, 7 },
			new int[] { 13, 7 },
			new int[] { 13, 7 }),

	LIGHT_GRAY_WOOL(true, true, 0,
			new int[] { 14, 0 },
			new int[] { 14, 0 },
			new int[] { 14, 0 }),

	CYAN_WOOL(true, true, 0,
			new int[] { 14, 1 },
			new int[] { 14, 1 },
			new int[] { 14, 1 }),

	PURPLE_WOOL(true, true, 0,
			new int[] { 14, 2 },
			new int[] { 14, 2 },
			new int[] { 14, 2 }),

	BLUE_WOOL(true, true, 0,
			new int[] { 14, 3 },
			new int[] { 14, 3 },
			new int[] { 14, 3 }),

	BROWN_WOOL(true, true, 0,
			new int[] { 14, 4 },
			new int[] { 14, 4 },
			new int[] { 14, 4 }),

	GREEN_WOOL(true, true, 0,
			new int[] { 14, 5 },
			new int[] { 14, 5 },
			new int[] { 14, 5 }),

	RED_WOOL(true, true, 0,
			new int[] { 14, 6 },
			new int[] { 14, 6 },
			new int[] { 14, 6 }),

	BLACK_WOOL(true, true, 0,
			new int[] { 14, 7 },
			new int[] { 14, 7 },
			new int[] { 14, 7 }),

	SPRUCE_LOG(true, true, 0,
			new int[] { 1, 5 },
			new int[] { 1, 5 },
			new int[] { 7, 4 }),

	BIRCH_LOG(true, true, 0,
			new int[] { 1, 5 },
			new int[] { 1, 5 },
			new int[] { 7, 5 }),

	JUNGLE_LOG(true, true, 0,
			new int[] { 1, 5 },
			new int[] { 1, 5 },
			new int[] { 9, 9 }),

	SPRUCE_LEAVES(true, false, 0,
			new int[] { 3, 4 },
			new int[] { 3, 4 },
			new int[] { 3, 4 }),

	BIRCH_LEAVES(true, false, 0,
			new int[] { 3, 4 },
			new int[] { 3, 4 },
			new int[] { 3, 4 }),

	SPRUCE_PLANKS(true, true, 0,
			new int[] { 6, 12 },
			new int[] { 6, 12 },
			new int[] { 6, 12 }),

	BIRCH_PLANKS(true, true, 0,
			new int[] { 6, 13 },
			new int[] { 6, 13 },
			new int[] { 6, 13 }),

	COBWEB(false, false, 0,
			new int[] { 0, 11 },
			new int[] { 0, 11 },
			new int[] { 0, 11 }),

	TALL_GRASS(false, false, 0,
			new int[] { 2, 7 },
			new int[] { 2, 7 },
			new int[] { 2, 7 }),

	FERN(false, false, 0,
			new int[] { 2, 8 },
			new int[] { 2, 8 },
			new int[] { 2, 8 }),

	DEAD_BUSH(false, false, 0,
			new int[] { 3, 7 },
			new int[] { 3, 7 },
			new int[] { 3, 7 }),

	DANDELION(false, false, 0,
			new int[] { 0, 13 },
			new int[] { 0, 13 },
			new int[] { 0, 13 }),

	ROSE(false, false, 0,
			new int[] { 0, 12 },
			new int[] { 0, 12 },
			new int[] { 0, 12 }),

	BROWN_MUSHROOM(false, false, 1,
			new int[] { 1, 13 },
			new int[] { 1, 13 },
			new int[] { 1, 13 }),

	RED_MUSHROOM(false, false, 1,
			new int[] { 1, 12 },
			new int[] { 1, 12 },
			new int[] { 1, 12 });

	private final boolean solid;
	private final boolean opaque;
	private final int lightEmission;
	private final int[] topTile;
	private final int[] bottomTile;
	private final int[] sideTile;

	BlockType(boolean solid, boolean opaque, int lightEmission, int[] topTile, int[] bottomTile, int[] sideTile) {
		this.solid = solid;
		this.opaque = opaque;
		this.lightEmission = lightEmission;
		this.topTile = topTile;
		this.bottomTile = bottomTile;
		this.sideTile = sideTile;
	}

	public boolean needsTint(Face face) {
		if ((this == GRASS && face == Face.TOP) || this == OAK_LEAVES || this == BIRCH_LEAVES)
			return true;
		if (this == TALL_GRASS || this == FERN)
			return true;
		if (this == SUGAR_CANE)
			return true;
		return false;
	}

	public static float[] getGrassColor() {
		return new float[] { 0.55f, 0.72f, 0.42f };
	}

	public static float[] getGrassColor(Biome biome) {
		if (biome == null)
			return getGrassColor();
		return biome.getGrassColor();
	}

	public static float[] getFoliageColor(Biome biome) {
		if (biome == null)
			return getGrassColor();
		return biome.getFoliageColor();
	}

	public static float[] getSpruceColor() {
		return new float[] { 0.38f, 0.55f, 0.38f };
	}

	public static float[] getBirchColor() {
		return new float[] { 0.52f, 0.68f, 0.45f };
	}

	public static float[] getJungleColor() {
		return new float[] { 0.35f, 0.65f, 0.35f };
	}

	public boolean needsFoliageTint() {
		return this == OAK_LEAVES || this == BIRCH_LEAVES || this == SPRUCE_LEAVES;
	}

	public boolean needsGrassTint() {
		return this == TALL_GRASS || this == FERN || this == SUGAR_CANE;
	}

	public int[] getTileForFace(Face face, int metadata) {
		if (!this.solid && this != TORCH && this != COBWEB && this != TALL_GRASS && this != FERN
				&& this != DEAD_BUSH && this != DANDELION && this != ROSE
				&& this != BROWN_MUSHROOM && this != RED_MUSHROOM && this != SUGAR_CANE)
			return null;

		if (this.isAxisOrientedLog()) {
			final int axis = this.decodeAxis(metadata);
			final boolean isEndFace = axis == 0 && (face == Face.TOP || face == Face.BOTTOM)
					|| axis == 1 && (face == Face.EAST || face == Face.WEST)
					|| axis == 2 && (face == Face.NORTH || face == Face.SOUTH);
			return isEndFace ? this.topTile : this.sideTile;
		}

		if (this.isHorizontallyFacing()) {
			final Face front = this.decodeFacing(metadata);
			if (face == Face.TOP)
				return this.topTile;
			if (face == Face.BOTTOM)
				return this.bottomTile;
			if (face == front)
				return this.sideTile;
			return this.topTile;
		}

		return switch (face) {
			case TOP -> this.topTile;
			case BOTTOM -> this.bottomTile;
			case NORTH, SOUTH, EAST, WEST -> this.sideTile;
		};
	}

	private boolean isAxisOrientedLog() {
		return this == OAK_LOG || this == SPRUCE_LOG || this == BIRCH_LOG || this == JUNGLE_LOG;
	}

	private boolean isHorizontallyFacing() {
		return this == FURNACE || this == FURNACE_LIT || this == DISPENSER;
	}

	private int decodeAxis(int metadata) {
		return switch (metadata) {
			case 0 -> 0;
			case 1 -> 1;
			case 2 -> 2;
			default -> throw new IllegalArgumentException("Invalid axis metadata " + metadata + " for " + this.name());
		};
	}

	private Face decodeFacing(int metadata) {
		return switch (metadata) {
			case 0 -> Face.NORTH;
			case 1 -> Face.SOUTH;
			case 2 -> Face.EAST;
			case 3 -> Face.WEST;
			default -> throw new IllegalArgumentException("Invalid facing metadata " + metadata + " for " + this.name());
		};
	}

	public boolean blocksLight() {
		return this.opaque;
	}

	public boolean isTransparent() {
		return !this.opaque && this.solid;
	}

	public boolean isPlant() {
		return this == TALL_GRASS || this == FERN || this == DEAD_BUSH
				|| this == DANDELION || this == ROSE || this == BROWN_MUSHROOM
				|| this == RED_MUSHROOM || this == SUGAR_CANE || this == COBWEB;
	}

	public boolean isWater() {
		return false;
	}

	public boolean isSourceWater() {
		return false;
	}

	public boolean isLava() {
		return false;
	}

	public boolean isSourceLava() {
		return false;
	}

	public enum Face {
		TOP(0, 1, 0),
		BOTTOM(0, -1, 0),
		NORTH(0, 0, -1),
		SOUTH(0, 0, 1),
		EAST(1, 0, 0),
		WEST(-1, 0, 0);

		@Getter
		private final int nx, ny, nz;

		Face(int nx, int ny, int nz) {
			this.nx = nx;
			this.ny = ny;
			this.nz = nz;
		}
	}
}
