package com.matejpacan.voxelcraft;

public class Chunk {

	public static final int WIDTH = 16;
	public static final int HEIGHT = 256;
	public static final int DEPTH = 16;

	private static final int SEA_LEVEL = 64;
	private static final int BASE_TERRAIN_HEIGHT = 62;
	private static final int TERRAIN_AMPLITUDE = 32;
	private static final int STONE_DEPTH = 4;

	private final int chunkX;
	private final int chunkZ;
	private final BlockType[][][] blocks;
	private boolean dirty;
	private boolean generated;

	public Chunk(final int chunkX, final int chunkZ) {
		this.chunkX = chunkX;
		this.chunkZ = chunkZ;
		this.blocks = new BlockType[Chunk.WIDTH][Chunk.HEIGHT][Chunk.DEPTH];
		this.dirty = true;
		this.generated = false;

		for (int x = 0; x < Chunk.WIDTH; x++)
			for (int y = 0; y < Chunk.HEIGHT; y++)
				for (int z = 0; z < Chunk.DEPTH; z++)
					this.blocks[x][y][z] = BlockType.AIR;
	}

	public void generateTerrain(final NoiseGenerator noise) {
		if (this.generated)
			return;

		final int worldX = this.chunkX * Chunk.WIDTH;
		final int worldZ = this.chunkZ * Chunk.DEPTH;

		for (int x = 0; x < Chunk.WIDTH; x++)
			for (int z = 0; z < Chunk.DEPTH; z++) {
				final int globalX = worldX + x;
				final int globalZ = worldZ + z;

				final int surfaceHeight = this.calculateSurfaceHeight(noise, globalX, globalZ);

				this.blocks[x][0][z] = BlockType.BEDROCK;

				for (int y = 1; y < 5; y++)
					if (y <= surfaceHeight) {
						final double bedrockNoise = noise.noise2D(globalX * 0.5 + y, globalZ * 0.5);
						this.blocks[x][y][z] = bedrockNoise > 0 ? BlockType.BEDROCK : BlockType.STONE;
					}

				for (int y = 5; y <= surfaceHeight; y++)
					if (y < surfaceHeight - Chunk.STONE_DEPTH)
						this.blocks[x][y][z] = this.getStoneType(y, noise, globalX, globalZ);
					else if (y < surfaceHeight)
						this.blocks[x][y][z] = BlockType.DIRT;
					else
						this.blocks[x][y][z] = BlockType.GRASS;

				if (surfaceHeight < Chunk.SEA_LEVEL)
					for (int y = surfaceHeight + 1; y <= Chunk.SEA_LEVEL; y++)
						this.blocks[x][y][z] = BlockType.WATER;
			}

		this.generated = true;
		this.dirty = true;
	}

	private int calculateSurfaceHeight(final NoiseGenerator noise, final int x, final int z) {
		final double scale = 0.01;
		final double detailScale = 0.05;

		double heightNoise = noise.octaveNoise2D(x * scale, z * scale, 4, 0.5, 2.0);
		double detailNoise = noise.octaveNoise2D(x * detailScale, z * detailScale, 2, 0.5, 2.0);

		heightNoise = (heightNoise + 1.0) / 2.0;
		detailNoise = (detailNoise + 1.0) / 2.0;

		final double combinedNoise = heightNoise * 0.8 + detailNoise * 0.2;

		return (int) (Chunk.BASE_TERRAIN_HEIGHT + combinedNoise * Chunk.TERRAIN_AMPLITUDE);
	}

	private BlockType getStoneType(final int y, final NoiseGenerator noise, final int x, final int z) {
		final double oreNoise = noise.noise2D(x * 0.1 + y * 0.1, z * 0.1);

		if (y < 16 && oreNoise > 0.7)
			return BlockType.DIAMOND_ORE;
		if (y < 32 && oreNoise > 0.65)
			return BlockType.GOLD_ORE;
		if (y < 64 && oreNoise > 0.6)
			return BlockType.IRON_ORE;
		if (oreNoise > 0.55)
			return BlockType.COAL_ORE;

		return BlockType.STONE;
	}

	public BlockType getBlock(final int x, final int y, final int z) {
		if (x < 0 || x >= Chunk.WIDTH || y < 0 || y >= Chunk.HEIGHT || z < 0 || z >= Chunk.DEPTH)
			return BlockType.AIR;
		return this.blocks[x][y][z];
	}

	public void setBlock(final int x, final int y, final int z, final BlockType type) {
		if (x < 0 || x >= Chunk.WIDTH || y < 0 || y >= Chunk.HEIGHT || z < 0 || z >= Chunk.DEPTH)
			return;
		this.blocks[x][y][z] = type;
		this.dirty = true;
	}

	public int getChunkX() {
		return this.chunkX;
	}

	public int getChunkZ() {
		return this.chunkZ;
	}

	public int getWorldX() {
		return this.chunkX * Chunk.WIDTH;
	}

	public int getWorldZ() {
		return this.chunkZ * Chunk.DEPTH;
	}

	public boolean isDirty() {
		return this.dirty;
	}

	public void setDirty(final boolean dirty) {
		this.dirty = dirty;
	}

	public boolean hasLeaves() {
		for (int x = 0; x < Chunk.WIDTH; x++)
			for (int y = 0; y < Chunk.HEIGHT; y++)
				for (int z = 0; z < Chunk.DEPTH; z++)
					if (this.blocks[x][y][z].isLeaves())
						return true;
		return false;
	}
}
