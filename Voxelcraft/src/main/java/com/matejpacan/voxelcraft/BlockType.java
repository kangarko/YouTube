package com.matejpacan.voxelcraft;

public enum BlockType {
	AIR(-1, -1, false, 1.0f, 1.0f, 1.0f),
	GRASS(0, 0, 3, 0, 2, 0, true, 0.486f, 0.741f, 0.286f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f),
	DIRT(2, 0, true, 1.0f, 1.0f, 1.0f),
	STONE(1, 0, true, 1.0f, 1.0f, 1.0f),
	BEDROCK(1, 1, true, 1.0f, 1.0f, 1.0f),
	OAK_LOG(5, 1, 4, 1, 5, 1, true, 0.76f, 0.6f, 0.42f, 1.0f, 1.0f, 1.0f, 0.76f, 0.6f, 0.42f),
	COAL_ORE(2, 2, true, 1.0f, 1.0f, 1.0f),
	IRON_ORE(1, 2, true, 1.0f, 1.0f, 1.0f),
	GOLD_ORE(0, 2, true, 1.0f, 1.0f, 1.0f),
	DIAMOND_ORE(2, 3, true, 1.0f, 1.0f, 1.0f),
	GLASS(1, 3, false, 1.0f, 1.0f, 1.0f),
	WATER(13, 12, false, 0.247f, 0.463f, 0.894f),
	OAK_LEAVES(4, 3, false, 0.486f, 0.741f, 0.286f);

	private static final int ATLAS_SIZE = 16;
	private static final float TILE_SIZE = 1.0f / BlockType.ATLAS_SIZE;

	private final int topTexX, topTexY;
	private final int sideTexX, sideTexY;
	private final int bottomTexX, bottomTexY;
	private final boolean solid;

	private final float topTintR, topTintG, topTintB;
	private final float sideTintR, sideTintG, sideTintB;
	private final float bottomTintR, bottomTintG, bottomTintB;

	BlockType(final int texX, final int texY, final boolean solid, final float r, final float g, final float b) {
		this(texX, texY, texX, texY, texX, texY, solid, r, g, b, r, g, b, r, g, b);
	}

	BlockType(final int topX, final int topY, final int sideX, final int sideY, final int bottomX, final int bottomY,
			final boolean solid,
			final float topR, final float topG, final float topB,
			final float sideR, final float sideG, final float sideB,
			final float bottomR, final float bottomG, final float bottomB) {
		this.topTexX = topX;
		this.topTexY = topY;
		this.sideTexX = sideX;
		this.sideTexY = sideY;
		this.bottomTexX = bottomX;
		this.bottomTexY = bottomY;
		this.solid = solid;
		this.topTintR = topR;
		this.topTintG = topG;
		this.topTintB = topB;
		this.sideTintR = sideR;
		this.sideTintG = sideG;
		this.sideTintB = sideB;
		this.bottomTintR = bottomR;
		this.bottomTintG = bottomG;
		this.bottomTintB = bottomB;
	}

	public boolean isTransparent() {
		return !this.solid || this == GLASS;
	}

	public boolean blocksMovement() {
		return this.solid || this == OAK_LEAVES;
	}

	public boolean isLeaves() {
		return this == OAK_LEAVES;
	}

	public float[] getTopUV() {
		return this.getUV(this.topTexX, this.topTexY);
	}

	public float[] getSideUV() {
		return this.getUV(this.sideTexX, this.sideTexY);
	}

	public float[] getBottomUV() {
		return this.getUV(this.bottomTexX, this.bottomTexY);
	}

	public float[] getTopTint() {
		return new float[] { this.topTintR, this.topTintG, this.topTintB };
	}

	public float[] getSideTint() {
		return new float[] { this.sideTintR, this.sideTintG, this.sideTintB };
	}

	public float[] getBottomTint() {
		return new float[] { this.bottomTintR, this.bottomTintG, this.bottomTintB };
	}

	private float[] getUV(final int texX, final int texY) {
		final float u = texX * BlockType.TILE_SIZE;
		final float v = texY * BlockType.TILE_SIZE;
		return new float[] {
				u, v,
				u, v + BlockType.TILE_SIZE,
				u + BlockType.TILE_SIZE, v + BlockType.TILE_SIZE,
				u + BlockType.TILE_SIZE, v
		};
	}
}
