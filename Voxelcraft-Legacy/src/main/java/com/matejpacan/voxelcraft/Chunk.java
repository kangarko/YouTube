package com.matejpacan.voxelcraft;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

import lombok.Getter;
import lombok.Setter;

public class Chunk {

	public static final int SIZE = 16;
	public static final int HEIGHT = 256;
	public static final int MAX_LIGHT = 15;

	public static final int SEA_LEVEL = 62;

	@Getter
	private final int chunkX, chunkZ;

	private final BlockType[][][] blocks;
	private final byte[][][] blockMeta;
	private final byte[][][] skyLight;
	private final byte[][][] blockLight;
	private final Biome[] biomeMap;
	private final TextureAtlas atlas;

	@Setter
	private World world;

	@Getter
	private int vao;
	private int vbo;
	private int ebo;
	@Getter
	private int indexCount;
	@Getter
	private int waterVao;
	private int waterVbo;
	private int waterEbo;
	@Getter
	private int waterIndexCount;
	@Getter
	@Setter
	private boolean meshDirty = true;
	@Getter
	@Setter
	private boolean lightDirty = true;
	private boolean hasMesh = false;
	private boolean hasWaterMesh = false;
	@Getter
	@Setter
	private int lodLevel = 0;
	private int maxBlockY = 0;
	private volatile ChunkMeshData pendingMeshData;
	private static final int VERTEX_STRIDE = 14;
	private static final ThreadLocal<float[]> SOLID_VERTEX_BUFFER = ThreadLocal
			.withInitial(() -> new float[250000]);
	private static final ThreadLocal<int[]> SOLID_INDEX_BUFFER = ThreadLocal
			.withInitial(() -> new int[400000]);
	private static final ThreadLocal<float[]> WATER_VERTEX_BUFFER = ThreadLocal
			.withInitial(() -> new float[80000]);
	private static final ThreadLocal<int[]> WATER_INDEX_BUFFER = ThreadLocal
			.withInitial(() -> new int[120000]);

	private static final class MeshArray {
		private float[] vertices;
		private int vertexFloats;
		private int[] indices;
		private int indexCount;
		private final ThreadLocal<float[]> vertexHolder;
		private final ThreadLocal<int[]> indexHolder;

		MeshArray(ThreadLocal<float[]> vertexHolder, ThreadLocal<int[]> indexHolder) {
			this.vertexHolder = vertexHolder;
			this.indexHolder = indexHolder;
			this.vertices = vertexHolder.get();
			this.indices = indexHolder.get();
			this.vertexFloats = 0;
			this.indexCount = 0;
		}

		int baseVertexIndex() {
			return this.vertexFloats / VERTEX_STRIDE;
		}

		void ensureVertexCapacity(int additional) {
			final int required = this.vertexFloats + additional;
			if (required <= this.vertices.length)
				return;
			final int newSize = Math.max(required, this.vertices.length * 2);
			this.vertices = Arrays.copyOf(this.vertices, newSize);
			this.vertexHolder.set(this.vertices);
		}

		void ensureIndexCapacity(int additional) {
			final int required = this.indexCount + additional;
			if (required <= this.indices.length)
				return;
			final int newSize = Math.max(required, this.indices.length * 2);
			this.indices = Arrays.copyOf(this.indices, newSize);
			this.indexHolder.set(this.indices);
		}

		void addVertex(float v1, float v2, float v3, float v4, float v5, float v6, float v7, float v8, float v9,
				float v10, float v11, float v12, float v13, float v14) {
			this.ensureVertexCapacity(14);
			this.vertices[this.vertexFloats++] = v1;
			this.vertices[this.vertexFloats++] = v2;
			this.vertices[this.vertexFloats++] = v3;
			this.vertices[this.vertexFloats++] = v4;
			this.vertices[this.vertexFloats++] = v5;
			this.vertices[this.vertexFloats++] = v6;
			this.vertices[this.vertexFloats++] = v7;
			this.vertices[this.vertexFloats++] = v8;
			this.vertices[this.vertexFloats++] = v9;
			this.vertices[this.vertexFloats++] = v10;
			this.vertices[this.vertexFloats++] = v11;
			this.vertices[this.vertexFloats++] = v12;
			this.vertices[this.vertexFloats++] = v13;
			this.vertices[this.vertexFloats++] = v14;
		}

		void addTriangle(int i1, int i2, int i3) {
			this.ensureIndexCapacity(3);
			this.indices[this.indexCount++] = i1;
			this.indices[this.indexCount++] = i2;
			this.indices[this.indexCount++] = i3;
		}

		float[] getVertices() {
			return this.vertices;
		}

		int getVertexFloats() {
			return this.vertexFloats;
		}

		int[] getIndices() {
			return this.indices;
		}

		int getIndexCount() {
			return this.indexCount;
		}
	}

	private static final float FADE_DURATION = 0.5f;
	private float fadeProgress = 0.0f;
	private boolean fadeComplete = false;

	public Chunk(int chunkX, int chunkZ, TextureAtlas atlas) {
		this.chunkX = chunkX;
		this.chunkZ = chunkZ;
		this.atlas = atlas;
		this.blocks = new BlockType[SIZE][HEIGHT][SIZE];
		this.blockMeta = new byte[SIZE][HEIGHT][SIZE];
		this.skyLight = new byte[SIZE][HEIGHT][SIZE];
		this.blockLight = new byte[SIZE][HEIGHT][SIZE];
		this.biomeMap = new Biome[SIZE * SIZE];

		for (int x = 0; x < SIZE; x++)
			for (int y = 0; y < HEIGHT; y++)
				for (int z = 0; z < SIZE; z++) {
					this.blocks[x][y][z] = BlockType.AIR;
					this.blockMeta[x][y][z] = 0;
					this.skyLight[x][y][z] = 0;
					this.blockLight[x][y][z] = 0;
				}
	}

	public void generateTerrain() {
		final int[] heightMap = new int[SIZE * SIZE];
		final long chunkSeed = this.chunkX * 341873128712L + this.chunkZ * 132897987541L;
		final java.util.Random chunkRand = new java.util.Random(chunkSeed);

		for (int x = 0; x < SIZE; x++)
			for (int z = 0; z < SIZE; z++) {
				final int worldX = this.chunkX * SIZE + x;
				final int worldZ = this.chunkZ * SIZE + z;

				final double distFromSpawn = Math.sqrt(worldX * worldX + worldZ * worldZ);
				double spawnLandBoost = Math.max(0, 1.0 - distFromSpawn / 600.0);
				spawnLandBoost = spawnLandBoost * spawnLandBoost;

				final float tempBase = (float) this.temperatureNoise(worldX, worldZ);
				final float humidityBase = (float) this.humidityNoise(worldX, worldZ);
				final float erosion = (float) this.erosionNoise(worldX, worldZ);
				final float weirdness = (float) this.weirdnessNoise(worldX, worldZ);

				final double continent1 = this.octaveNoise(worldX * 0.0008, worldZ * 0.0008, 111111L, 4, 0.5, 2.0);
				final double continent2 = this.octaveNoise(worldX * 0.0003, worldZ * 0.0003, 222222L, 3, 0.6, 2.2);
				double continentShape = continent1 * 0.6 + continent2 * 0.4 + spawnLandBoost * 0.8;
				continentShape = Math.max(-1, Math.min(1, continentShape));

				double landMask = (continentShape + 0.35) / 1.35;
				landMask = Math.max(0, Math.min(1, landMask));

				final double baseTerrainNoise = this.octaveNoise(worldX * 0.003, worldZ * 0.003, 333333L, 5, 0.5, 2.0);
				final double hillNoise = this.octaveNoise(worldX * 0.008, worldZ * 0.008, 444444L, 4, 0.45, 2.0);
				final double microDetail = this.octaveNoise(worldX * 0.02, worldZ * 0.02, 555555L, 3, 0.5, 2.0);

				double baseHeight = SEA_LEVEL + 8;
				baseHeight += landMask * 35;
				baseHeight += baseTerrainNoise * 25 * landMask;

				final double hillStrength = Math.max(0, hillNoise) * landMask;
				baseHeight += hillStrength * hillStrength * 40;

				final double ridgeNoise = this.ridgedNoise(worldX * 0.004, worldZ * 0.004, 666666L, 4);
				double mountainMask = Math.max(0, landMask - 0.5) * 2.0;
				mountainMask *= 1.0 - erosion * 0.6;
				baseHeight += ridgeNoise * mountainMask * 80;

				final double peakNoise = this.ridgedNoise(worldX * 0.012, worldZ * 0.012, 777777L, 3);
				if (mountainMask > 0.3)
					baseHeight += peakNoise * (mountainMask - 0.3) * 60;

				baseHeight += microDetail * 4;

				final double valleyNoise = this.noise(worldX * 0.002, worldZ * 0.002, 888888L);
				if (valleyNoise < -0.2 && landMask > 0.3) {
					final double valleyDepth = (-valleyNoise - 0.2) / 0.8;
					baseHeight -= valleyDepth * valleyDepth * 20 * erosion;
				}

				final double riverNoise = Math.abs(this.noise(worldX * 0.004, worldZ * 0.004, 999999L));
				final double riverBank = 0.12;
				final double riverCore = 0.04;
				final boolean nearRiver = riverNoise < riverBank && baseHeight > SEA_LEVEL - 5 && baseHeight < SEA_LEVEL + 40;

				if (nearRiver)
					if (riverNoise < riverCore) {
						double coreDepth = (riverCore - riverNoise) / riverCore;
						coreDepth = coreDepth * coreDepth;
						final int targetDepth = SEA_LEVEL - 1 - (int) (coreDepth * 3);
						baseHeight = Math.min(baseHeight, targetDepth);
					} else {
						double bankFactor = (riverBank - riverNoise) / (riverBank - riverCore);
						bankFactor = bankFactor * bankFactor * bankFactor;
						final double targetHeight = SEA_LEVEL + 1 + (1 - bankFactor) * (baseHeight - SEA_LEVEL - 1);
						baseHeight = Math.min(baseHeight, targetHeight);
					}

				if (landMask < 0.15) {
					final double oceanFloor = this.octaveNoise(worldX * 0.008, worldZ * 0.008, 101010L, 3, 0.5, 2.0);
					final double oceanDepth = (0.15 - landMask) / 0.15;
					baseHeight = SEA_LEVEL - 5 - oceanDepth * 35 + oceanFloor * 8;
				}

				final int prelimHeight = (int) Math.max(1, Math.min(HEIGHT - 5, baseHeight));

				final double altitudeCooling = Math.max(0, baseHeight - SEA_LEVEL) / 180.0;
				float biomeTemp = (float) (tempBase - altitudeCooling * 0.7 + landMask * 0.15);
				biomeTemp = Math.max(0, Math.min(1, biomeTemp));

				float biomeHumidity = (float) (humidityBase + (1 - landMask) * 0.2);
				biomeHumidity = Math.max(0, Math.min(1, biomeHumidity));

				Biome biome = Biome.fromClimate(biomeTemp, biomeHumidity, (float) continentShape, erosion, weirdness,
						prelimHeight);

				if (distFromSpawn < 200
						&& (biome == Biome.OCEAN || biome == Biome.DEEP_OCEAN || biome == Biome.BEACH)) {
					if (biomeHumidity > 0.6)
						biome = Biome.FOREST;
					else if (biomeTemp > 0.7)
						biome = Biome.SAVANNA;
					else
						biome = Biome.PLAINS;
					baseHeight = Math.max(baseHeight, SEA_LEVEL + 4 + hillNoise * 12);
				}

				this.biomeMap[x * SIZE + z] = biome;

				double height = baseHeight;

				if (biome == Biome.MOUNTAINS || biome == Biome.JAGGED_PEAKS || biome == Biome.STONY_PEAKS
						|| biome == Biome.FROZEN_PEAKS) {
					final double peakDetail = this.ridgedNoise(worldX * 0.015, worldZ * 0.015, 121212L, 4);
					height += peakDetail * biome.getHeightVariation() * 0.7;
					if (biome == Biome.JAGGED_PEAKS)
						height += Math.abs(this.noise(worldX * 0.04, worldZ * 0.04, 131313L)) * 30;
				}

				if (biome == Biome.MESA || biome == Biome.BADLANDS) {
					final double mesaPlateau = this.noise(worldX * 0.005, worldZ * 0.005, 141414L);
					if (mesaPlateau > 0)
						height += mesaPlateau * mesaPlateau * 50;
				}

				if (biome == Biome.WINDSWEPT_HILLS || biome == Biome.WINDSWEPT_FOREST)
					height += this.ridgedNoise(worldX * 0.02, worldZ * 0.02, 151515L, 3) * 20;

				final int h = (int) Math.max(1, Math.min(HEIGHT - 5, height));
				heightMap[x * SIZE + z] = h;

				BlockType surfaceBlock = biome.getSurfaceBlock();
				final BlockType subsurfaceBlock = biome.getSubsurfaceBlock();

				final boolean warmCoast = h <= SEA_LEVEL + 1 && h >= SEA_LEVEL - 2 && !this.isOceanBiome(biome)
						&& biome != Biome.BEACH && biome != Biome.STONY_SHORE && biome != Biome.RIVER;
				if (warmCoast && biomeTemp > 0.65f && biomeHumidity < 0.55f)
					surfaceBlock = BlockType.SAND;

				boolean exposeStone = false;
				if (this.isMountainBiome(biome) && h > SEA_LEVEL + 65)
					exposeStone = true;
				else if (biome == Biome.WINDSWEPT_HILLS && h > SEA_LEVEL + 50 && chunkRand.nextFloat() < 0.25f)
					exposeStone = true;
				else if (biome == Biome.STONY_SHORE && chunkRand.nextFloat() < 0.7f)
					exposeStone = true;

				final boolean isBeachArea = biome == Biome.BEACH
						|| h <= SEA_LEVEL + 3 && h >= SEA_LEVEL - 4 && !this.isOceanBiome(biome)
								&& biome != Biome.STONY_SHORE;
				int beachSandDepth = isBeachArea ? 4 + chunkRand.nextInt(3) : 0;

				final boolean isUnderwaterFloor = h < SEA_LEVEL && (this.isOceanBiome(biome) || biome == Biome.RIVER);
				if (isUnderwaterFloor)
					beachSandDepth = 3 + chunkRand.nextInt(2);

				final boolean isRiverBank = riverNoise < 0.12 && riverNoise >= 0.04 && h >= SEA_LEVEL && h <= SEA_LEVEL + 4;
				final boolean isRiverBed = riverNoise < 0.04 && h < SEA_LEVEL;
				if (isRiverBank || isRiverBed)
					beachSandDepth = 3 + chunkRand.nextInt(2);

				for (int y = 0; y < HEIGHT; y++)
					if (y == 0)
						this.blocks[x][y][z] = BlockType.BEDROCK;
					else if (y < 5)
						this.blocks[x][y][z] = chunkRand.nextFloat() < 0.5f ? BlockType.BEDROCK : BlockType.STONE;
					else if (y < h - 4)
						this.blocks[x][y][z] = BlockType.STONE;
					else if (y < h) {
						if (beachSandDepth > 0 && y >= h - beachSandDepth)
							this.blocks[x][y][z] = BlockType.SAND;
						else if (exposeStone && y > h - 2)
							this.blocks[x][y][z] = BlockType.STONE;
						else if (this.isMountainBiome(biome) && y > h - 2)
							this.blocks[x][y][z] = BlockType.GRAVEL;
						else if (biome == Biome.MESA || biome == Biome.BADLANDS)
							this.blocks[x][y][z] = BlockType.SANDSTONE;
						else
							this.blocks[x][y][z] = subsurfaceBlock;
					} else if (y == h) {
						if (beachSandDepth > 0) {
							if ((isRiverBank || isRiverBed) && chunkRand.nextFloat() < 0.3f)
								this.blocks[x][y][z] = BlockType.GRAVEL;
							else
								this.blocks[x][y][z] = BlockType.SAND;
						} else if (exposeStone)
							this.blocks[x][y][z] = BlockType.STONE;
						else if (this.isMountainBiome(biome) && h > SEA_LEVEL + 50)
							this.blocks[x][y][z] = BlockType.SNOW_BLOCK;
						else if (biome == Biome.ICE_SPIKES || biome == Biome.SNOWY_PLAINS
								|| biome == Biome.FROZEN_PEAKS)
							this.blocks[x][y][z] = BlockType.SNOW_BLOCK;
						else if (biome == Biome.SNOWY_TAIGA || biome == Biome.TAIGA && h > SEA_LEVEL + 15
								|| biome == Biome.OLD_GROWTH_TAIGA && h > SEA_LEVEL + 20)
							this.blocks[x][y][z] = BlockType.SNOWY_GRASS;
						else
							this.blocks[x][y][z] = surfaceBlock;
					} else if (y <= SEA_LEVEL && y > h)
						this.blocks[x][y][z] = BlockType.WATER;
					else
						this.blocks[x][y][z] = BlockType.AIR;

				if (h > SEA_LEVEL + 90 && biomeTemp < 0.45f && this.blocks[x][h][z] != BlockType.WATER)
					this.blocks[x][h][z] = BlockType.SNOW_BLOCK;

				if (this.isColdBiome(biome) && h > SEA_LEVEL)
					if (h + 1 < HEIGHT && this.blocks[x][h + 1][z] == BlockType.AIR && chunkRand.nextFloat() < 0.25f)
						this.blocks[x][h + 1][z] = BlockType.SNOW_BLOCK;

				if ((biome == Biome.DESERT || biome == Biome.MESA || biome == Biome.BADLANDS) && h > SEA_LEVEL) {
					final int sandDepth = 4 + chunkRand.nextInt(5);
					for (int y = h; y > Math.max(h - sandDepth, SEA_LEVEL); y--)
						if (this.blocks[x][y][z] == BlockType.STONE)
							this.blocks[x][y][z] = BlockType.SANDSTONE;
				}

				if (biome == Biome.ICE_SPIKES && h > SEA_LEVEL && chunkRand.nextFloat() < 0.025f) {
					final int spikeHeight = 6 + chunkRand.nextInt(18);
					for (int y = h + 1; y < Math.min(h + spikeHeight, HEIGHT - 1); y++)
						this.blocks[x][y][z] = BlockType.SNOW_BLOCK;
				}

				if (biome == Biome.FROZEN_PEAKS && h > SEA_LEVEL + 80 && chunkRand.nextFloat() < 0.015f) {
					final int spikeHeight = 3 + chunkRand.nextInt(8);
					for (int y = h + 1; y < Math.min(h + spikeHeight, HEIGHT - 1); y++)
						this.blocks[x][y][z] = BlockType.SNOW_BLOCK;
				}
			}

		this.generateOres(chunkRand);
		this.generateBiomeTrees(heightMap, chunkRand);
		this.generateFallenTrees(heightMap, chunkRand);
		this.generateBiomeVegetation(heightMap, chunkRand);
		this.generateWaterFeatures(heightMap, chunkRand);

		for (int x = 0; x < SIZE; x++)
			for (int z = 0; z < SIZE; z++)
				for (int y = HEIGHT - 1; y >= 0; y--)
					if (this.blocks[x][y][z] != BlockType.AIR) {
						if (y > this.maxBlockY)
							this.maxBlockY = y;
						break;
					}
		this.maxBlockY = Math.min(this.maxBlockY + 10, HEIGHT - 1);

		this.meshDirty = true;
		this.lightDirty = true;
	}

	private void generateWaterFeatures(int[] heightMap, java.util.Random rand) {
		final int worldBaseX = this.chunkX * SIZE;
		final int worldBaseZ = this.chunkZ * SIZE;

		for (int x = 0; x < SIZE; x++)
			for (int z = 0; z < SIZE; z++) {
				final int worldX = worldBaseX + x;
				final int worldZ = worldBaseZ + z;
				final int h = heightMap[x * SIZE + z];
				final Biome biome = this.biomeMap[x * SIZE + z];

				if (this.isOceanBiome(biome) || biome == Biome.DESERT || biome == Biome.MESA || biome == Biome.BADLANDS)
					continue;

				final double springNoise = this.noise(worldX * 0.02, worldZ * 0.02, 567890L);
				if (springNoise > 0.85 && h > SEA_LEVEL + 15 && h < SEA_LEVEL + 60)
					if (this.isMountainBiome(biome) || biome == Biome.WINDSWEPT_HILLS || biome == Biome.TAIGA
							|| biome == Biome.OLD_GROWTH_TAIGA) {
						final int springY = h - 1 - rand.nextInt(3);
						if (springY > SEA_LEVEL && this.blocks[x][springY][z] != BlockType.AIR) {
							boolean hasDropOff = false;
							for (final int[] dir : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
								final int nx = x + dir[0];
								final int nz = z + dir[1];
								if (nx >= 0 && nx < SIZE && nz >= 0 && nz < SIZE) {
									final int neighborH = heightMap[nx * SIZE + nz];
									if (neighborH < springY - 2) {
										hasDropOff = true;
										break;
									}
								}
							}
							if (hasDropOff) {
								this.blocks[x][springY][z] = BlockType.WATER;
								if (springY + 1 < HEIGHT && this.blocks[x][springY + 1][z] == BlockType.AIR)
									this.blocks[x][springY + 1][z] = BlockType.WATER;
							}
						}
					}

				final double lakeNoise = this.noise(worldX * 0.008, worldZ * 0.008, 678901L);
				final double lakeNoise2 = this.noise(worldX * 0.015, worldZ * 0.015, 789012L);
				if (lakeNoise > 0.6 && lakeNoise2 > 0.4 && h > SEA_LEVEL && h < SEA_LEVEL + 25)
					if (biome == Biome.FOREST || biome == Biome.PLAINS || biome == Biome.FLOWER_PLAINS
							|| biome == Biome.MEADOW || biome == Biome.TAIGA || biome == Biome.SWAMP) {
						final double distFromCenter = Math.sqrt(
								Math.pow(lakeNoise - 0.8, 2) + Math.pow(lakeNoise2 - 0.7, 2));
						if (distFromCenter < 0.15) {
							final int lakeDepth = 2 + (int) ((0.15 - distFromCenter) * 20);
							final int lakeBottom = h - lakeDepth;
							if (lakeBottom >= SEA_LEVEL - 5) {
								for (int y = lakeBottom; y <= h; y++)
									if (y == lakeBottom)
										this.blocks[x][y][z] = BlockType.SAND;
									else
										this.blocks[x][y][z] = BlockType.WATER;
								if (h + 1 < HEIGHT)
									this.blocks[x][h + 1][z] = BlockType.AIR;
							}
						}
					}

				final double pondNoise = this.noise(worldX * 0.05, worldZ * 0.05, 234567L);
				if (pondNoise > 0.9 && h > SEA_LEVEL && h < SEA_LEVEL + 15)
					if (biome == Biome.PLAINS || biome == Biome.FLOWER_PLAINS || biome == Biome.MEADOW
							|| biome == Biome.FOREST || biome == Biome.SWAMP) {
						this.blocks[x][h - 1][z] = BlockType.DIRT;
						this.blocks[x][h][z] = BlockType.WATER;
						if (h + 1 < HEIGHT)
							this.blocks[x][h + 1][z] = BlockType.AIR;
					}
			}
	}

	private void generateBiomeVegetation(int[] heightMap, java.util.Random rand) {
		for (int x = 0; x < SIZE; x++)
			for (int z = 0; z < SIZE; z++) {
				final int height = heightMap[x * SIZE + z];
				final Biome biome = this.biomeMap[x * SIZE + z];

				if ((height <= SEA_LEVEL) || (height + 1 >= HEIGHT) || (this.blocks[x][height + 1][z] != BlockType.AIR))
					continue;
				if (this.isOceanBiome(biome) || biome == Biome.BEACH || biome == Biome.STONY_SHORE
						|| biome == Biome.ICE_SPIKES || biome == Biome.FROZEN_PEAKS || biome == Biome.RIVER)
					continue;

				final BlockType[] vegTypes = biome.getVegetationTypes();
				if (vegTypes.length == 0)
					continue;

				final float vegChance = biome.getVegetationDensity();

				if (rand.nextFloat() < vegChance) {
					final BlockType veg = vegTypes[rand.nextInt(vegTypes.length)];

					if (veg == BlockType.CACTUS) {
						if (this.blocks[x][height][z] == BlockType.SAND)
							this.generateCactus(x, height + 1, z, rand);
					} else if (veg == BlockType.SUGAR_CANE) {
						if (this.isNearWater(x, height, z))
							this.generateSugarCane(x, height + 1, z, rand);
					} else {
						final BlockType ground = this.blocks[x][height][z];
						if (ground == BlockType.GRASS || ground == BlockType.SNOWY_GRASS ||
								ground == BlockType.DIRT || ground == BlockType.SNOW_BLOCK)
							this.blocks[x][height + 1][z] = veg;
						else if (ground == BlockType.SAND && veg == BlockType.DEAD_BUSH)
							this.blocks[x][height + 1][z] = BlockType.DEAD_BUSH;
					}
				}
			}

		for (int i = 0; i < 2; i++) {
			final int centerX = rand.nextInt(SIZE);
			final int centerZ = rand.nextInt(SIZE);
			final Biome biome = this.biomeMap[centerX * SIZE + centerZ];

			if (biome == Biome.PLAINS || biome == Biome.FLOWER_PLAINS || biome == Biome.SUNFLOWER_PLAINS
					|| biome == Biome.FOREST || biome == Biome.SAVANNA || biome == Biome.MEADOW
					|| biome == Biome.CHERRY_GROVE || biome == Biome.BIRCH_FOREST) {
				final int radius = 1 + rand.nextInt(3);
				for (int dx = -radius; dx <= radius; dx++)
					for (int dz = -radius; dz <= radius; dz++) {
						final int nx = centerX + dx;
						final int nz = centerZ + dz;

						if (nx < 0 || nx >= SIZE || nz < 0 || nz >= SIZE)
							continue;
						if (dx * dx + dz * dz > radius * radius)
							continue;

						final int height = heightMap[nx * SIZE + nz];
						if (height <= SEA_LEVEL || height + 1 >= HEIGHT || (this.blocks[nx][height][nz] != BlockType.GRASS) || (this.blocks[nx][height + 1][nz] != BlockType.AIR))
							continue;

						if (rand.nextFloat() < 0.4f)
							this.blocks[nx][height + 1][nz] = BlockType.TALL_GRASS;
					}
			}
		}

		if (rand.nextFloat() < 0.3f) {
			final int fx = rand.nextInt(SIZE);
			final int fz = rand.nextInt(SIZE);
			final Biome biome = this.biomeMap[fx * SIZE + fz];

			if (biome == Biome.PLAINS || biome == Biome.FLOWER_PLAINS || biome == Biome.SUNFLOWER_PLAINS
					|| biome == Biome.FOREST || biome == Biome.JUNGLE || biome == Biome.MEADOW
					|| biome == Biome.CHERRY_GROVE || biome == Biome.BIRCH_FOREST) {
				final int height = heightMap[fx * SIZE + fz];
				if (height > SEA_LEVEL && height + 1 < HEIGHT)
					if (this.blocks[fx][height][fz] == BlockType.GRASS && this.blocks[fx][height + 1][fz] == BlockType.AIR) {
						final BlockType flower = rand.nextBoolean() ? BlockType.DANDELION : BlockType.ROSE;
						this.blocks[fx][height + 1][fz] = flower;
					}
			}
		}
	}

	private void generateCactus(int x, int y, int z, java.util.Random rand) {
		final int cactusHeight = 1 + rand.nextInt(3);
		for (int h = 0; h < cactusHeight; h++)
			if (y + h < HEIGHT && this.blocks[x][y + h][z] == BlockType.AIR)
				this.blocks[x][y + h][z] = BlockType.CACTUS;
	}

	private void generateSugarCane(int x, int y, int z, java.util.Random rand) {
		final int caneHeight = 1 + rand.nextInt(3);
		for (int h = 0; h < caneHeight; h++)
			if (y + h < HEIGHT && this.blocks[x][y + h][z] == BlockType.AIR)
				this.blocks[x][y + h][z] = BlockType.SUGAR_CANE;
	}

	private boolean isNearWater(int x, int y, int z) {
		final int[][] neighbors = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
		for (final int[] offset : neighbors) {
			final int nx = x + offset[0];
			final int nz = z + offset[1];
			if (nx >= 0 && nx < SIZE && nz >= 0 && nz < SIZE)
				if (this.blocks[nx][y][nz] == BlockType.WATER)
					return true;
		}
		return false;
	}

	private boolean isOceanBiome(Biome biome) {
		return biome == Biome.OCEAN || biome == Biome.DEEP_OCEAN || biome == Biome.LUKEWARM_OCEAN
				|| biome == Biome.WARM_OCEAN;
	}

	private boolean isMountainBiome(Biome biome) {
		return biome == Biome.MOUNTAINS || biome == Biome.JAGGED_PEAKS || biome == Biome.STONY_PEAKS
				|| biome == Biome.FROZEN_PEAKS || biome == Biome.WINDSWEPT_HILLS || biome == Biome.WINDSWEPT_FOREST;
	}

	private boolean isColdBiome(Biome biome) {
		return biome == Biome.TAIGA || biome == Biome.OLD_GROWTH_TAIGA || biome == Biome.SNOWY_TAIGA
				|| biome == Biome.SNOWY_PLAINS || biome == Biome.ICE_SPIKES || biome == Biome.FROZEN_PEAKS;
	}

	private void generateOres(java.util.Random rand) {
		this.generateOreVein(BlockType.COAL_ORE, rand, 25, 5, 128, 10);
		this.generateOreVein(BlockType.IRON_ORE, rand, 20, 5, 72, 8);
		this.generateOreVein(BlockType.GOLD_ORE, rand, 6, 5, 32, 6);
		this.generateOreVein(BlockType.DIAMOND_ORE, rand, 3, 5, 20, 5);
		this.generateOreVein(BlockType.REDSTONE_ORE, rand, 8, 5, 20, 6);
		this.generateOreVein(BlockType.LAPIS_ORE, rand, 4, 5, 40, 5);
	}

	private void generateOreVein(BlockType ore, java.util.Random rand, int veinsPerChunk, int minY, int maxY,
			int veinSize) {
		for (int i = 0; i < veinsPerChunk; i++) {
			int x = rand.nextInt(SIZE);
			int y = minY + rand.nextInt(Math.max(1, maxY - minY));
			int z = rand.nextInt(SIZE);

			for (int j = 0; j < veinSize; j++) {
				final int ox = x + rand.nextInt(3) - 1;
				final int oy = y + rand.nextInt(3) - 1;
				final int oz = z + rand.nextInt(3) - 1;

				if (ox >= 0 && ox < SIZE && oy >= 1 && oy < HEIGHT && oz >= 0 && oz < SIZE)
					if (this.blocks[ox][oy][oz] == BlockType.STONE)
						this.blocks[ox][oy][oz] = ore;

				x = ox;
				y = oy;
				z = oz;
			}
		}
	}

	private double gradientDot(int ix, int iz, double dx, double dz, long seed) {
		long hash = ix * 374761393L + iz * 668265263L + seed;
		hash = (hash ^ hash >> 13) * 1274126177L;
		hash = (hash ^ hash >> 15) * 2048419325L;
		final int h = (int) (hash & 7);

		final double u = h < 4 ? dx : dz;
		final double v = h < 4 ? dz : dx;
		return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
	}

	private double smoothstep(double t) {
		return t * t * t * (t * (t * 6 - 15) + 10);
	}

	private double lerp(double a, double b, double t) {
		return a + t * (b - a);
	}

	private double noise(double x, double z, long seed) {
		final int ix = (int) Math.floor(x);
		final int iz = (int) Math.floor(z);
		final double fx = x - ix;
		final double fz = z - iz;

		final double v00 = this.gradientDot(ix, iz, fx, fz, seed);
		final double v10 = this.gradientDot(ix + 1, iz, fx - 1, fz, seed);
		final double v01 = this.gradientDot(ix, iz + 1, fx, fz - 1, seed);
		final double v11 = this.gradientDot(ix + 1, iz + 1, fx - 1, fz - 1, seed);

		final double sx = this.smoothstep(fx);
		final double sz = this.smoothstep(fz);

		final double nx0 = this.lerp(v00, v10, sx);
		final double nx1 = this.lerp(v01, v11, sx);
		return this.lerp(nx0, nx1, sz);
	}

	private double octaveNoise(double x, double z, long seed, int octaves, double persistence, double lacunarity) {
		double total = 0;
		double amplitude = 1;
		double frequency = 1;
		double maxValue = 0;

		for (int i = 0; i < octaves; i++) {
			total += this.noise(x * frequency, z * frequency, seed + i * 31337L) * amplitude;
			maxValue += amplitude;
			amplitude *= persistence;
			frequency *= lacunarity;
		}

		return total / maxValue;
	}

	private double ridgedNoise(double x, double z, long seed, int octaves) {
		double total = 0;
		double amplitude = 1;
		double frequency = 1;
		double weight = 1;

		for (int i = 0; i < octaves; i++) {
			double signal = this.noise(x * frequency, z * frequency, seed + i * 71234L);
			signal = 1.0 - Math.abs(signal);
			signal *= signal * weight;
			weight = Math.min(1.0, Math.max(0.0, signal * 2));
			total += signal * amplitude;
			amplitude *= 0.5;
			frequency *= 2.1;
		}

		return total * 0.5;
	}

	private double warpedNoise(double x, double z, long seed, double warpStrength) {
		final double warpX = this.octaveNoise(x * 0.5, z * 0.5, seed + 100000L, 3, 0.5, 2.0) * warpStrength;
		final double warpZ = this.octaveNoise(x * 0.5, z * 0.5, seed + 200000L, 3, 0.5, 2.0) * warpStrength;
		return this.octaveNoise(x + warpX, z + warpZ, seed, 4, 0.5, 2.0);
	}

	private double temperatureNoise(double x, double z) {
		final long seed = 98765432L;
		final double warpX = this.noise(x * 0.0006, z * 0.0006, seed + 500000L) * 120;
		final double warpZ = this.noise(x * 0.0006, z * 0.0006, seed + 600000L) * 120;
		double temp = 0.55;
		temp += this.octaveNoise((x + warpX) * 0.0009, (z + warpZ) * 0.0009, seed, 4, 0.5, 2.0) * 0.45;
		return Math.max(0, Math.min(1, temp));
	}

	private double humidityNoise(double x, double z) {
		final long seed = 54321098L;
		final double warpX = this.noise(x * 0.0008, z * 0.0008, seed + 700000L) * 100;
		final double warpZ = this.noise(x * 0.0008, z * 0.0008, seed + 800000L) * 100;
		double humidity = 0.55;
		humidity += this.octaveNoise((x + warpX) * 0.00075, (z + warpZ) * 0.00075, seed, 4, 0.5, 2.0) * 0.45;
		return Math.max(0, Math.min(1, humidity));
	}

	private double erosionNoise(double x, double z) {
		final long seed = 22222222L;
		double erosion = this.octaveNoise(x * 0.0008, z * 0.0008, seed, 4, 0.45, 2.3);
		erosion += this.noise(x * 0.003, z * 0.003, seed + 1000L) * 0.2;
		return Math.max(0, Math.min(1, erosion * 0.5 + 0.5));
	}

	private double weirdnessNoise(double x, double z) {
		final long seed = 33333333L;
		final double weird = this.warpedNoise(x * 0.001, z * 0.001, seed, 4.0);
		return Math.max(-1, Math.min(1, weird * 1.5));
	}

	public Biome getBiomeAt(int localX, int localZ) {
		if (localX < 0 || localX >= SIZE || localZ < 0 || localZ >= SIZE)
			return Biome.PLAINS;
		return this.biomeMap[localX * SIZE + localZ];
	}

	private void generateBiomeTrees(int[] heightMap, java.util.Random rand) {
		final double chunkTreeDensityModifier = 0.4 + rand.nextDouble() * 1.2;

		for (int x = 0; x < SIZE; x++)
			for (int z = 0; z < SIZE; z++) {
				final int height = heightMap[x * SIZE + z];
				final Biome biome = this.biomeMap[x * SIZE + z];

				if (height <= SEA_LEVEL)
					continue;
				if (x < 2 || x >= SIZE - 2 || z < 2 || z >= SIZE - 2)
					continue;
				if (biome == Biome.BEACH || biome == Biome.OCEAN)
					continue;

				final BlockType groundBlock = this.blocks[x][height][z];
				if (groundBlock == BlockType.STONE || groundBlock == BlockType.SAND || groundBlock == BlockType.WATER)
					continue;

				final int worldX = this.chunkX * SIZE + x;
				final int worldZ = this.chunkZ * SIZE + z;

				final double riverNoise = Math.abs(this.noise(worldX * 0.004, worldZ * 0.004, 999999L));
				if (riverNoise < 0.10)
					continue;
				final long seed = worldX * 341873128712L + worldZ * 132897987541L;
				final java.util.Random treeRand = new java.util.Random(seed);

				final double treeDensity = biome.getTreeDensity() * chunkTreeDensityModifier;
				if (treeRand.nextDouble() < treeDensity) {
					final int baseY = height + 1;
					if ((baseY >= HEIGHT) || this.hasNearbyLog(x, baseY, z, 1))
						continue;

					BlockType log = biome.getTreeLog();
					BlockType leaves = biome.getTreeLeaves();

					if (biome.hasSecondaryTree() && treeRand.nextBoolean()) {
						log = biome.getSecondaryTreeLog();
						leaves = biome.getSecondaryTreeLeaves();
					}

					if (log != null && leaves != null) {
						final int treeVariant = treeRand.nextInt(10);
						if (log == BlockType.SPRUCE_LOG)
							this.generateSpruceTree(x, baseY, z, log, leaves);
						else if (log == BlockType.JUNGLE_LOG)
							this.generateJungleTree(x, baseY, z, log, leaves, treeRand);
						else if (log == BlockType.BIRCH_LOG)
							this.generateBirchTree(x, baseY, z, log, leaves);
						else if (log == BlockType.OAK_LOG) {
							if (treeVariant < 2)
								this.generateLargeOakTree(x, baseY, z, log, leaves, treeRand);
							else if (treeVariant < 4)
								this.generateSmallOakTree(x, baseY, z, log, leaves);
							else
								this.generateOakTree(x, baseY, z, log, leaves);
						} else
							this.generateOakTree(x, baseY, z, log, leaves);
					}
				}
			}
	}

	private boolean isLog(BlockType type) {
		return type == BlockType.OAK_LOG || type == BlockType.SPRUCE_LOG
				|| type == BlockType.BIRCH_LOG || type == BlockType.JUNGLE_LOG;
	}

	private void generateFallenTrees(int[] heightMap, java.util.Random rand) {
		final int worldBaseX = this.chunkX * SIZE;
		final int worldBaseZ = this.chunkZ * SIZE;

		for (int attempt = 0; attempt < 3; attempt++) {
			final int startX = 2 + rand.nextInt(SIZE - 4);
			final int startZ = 2 + rand.nextInt(SIZE - 4);

			final Biome biome = this.biomeMap[startX * SIZE + startZ];
			if (!this.isFallenTreeBiome(biome))
				continue;

			final double fallenTreeNoise = this.noise((worldBaseX + startX) * 0.015, (worldBaseZ + startZ) * 0.015, 876543L);
			if (fallenTreeNoise < 0.55)
				continue;

			final int baseHeight = heightMap[startX * SIZE + startZ];
			if (baseHeight <= SEA_LEVEL)
				continue;

			final BlockType groundBlock = this.blocks[startX][baseHeight][startZ];
			if (groundBlock != BlockType.GRASS && groundBlock != BlockType.DIRT
					&& groundBlock != BlockType.SNOWY_GRASS && groundBlock != BlockType.SNOW_BLOCK)
				continue;

			if (this.blocks[startX][baseHeight + 1][startZ] != BlockType.AIR)
				continue;

			final int direction = rand.nextInt(4);
			int dx = 0, dz = 0;
			switch (direction) {
				case 0 -> dx = 1;
				case 1 -> dx = -1;
				case 2 -> dz = 1;
				case 3 -> dz = -1;
			}

			final int length = 4 + rand.nextInt(5);

			if (!this.canPlaceFallenTree(startX, startZ, dx, dz, length, heightMap, baseHeight))
				continue;

			final BlockType logType = this.getFallenTreeLog(biome, rand);
			this.placeFallenTree(startX, baseHeight + 1, startZ, dx, dz, length, logType, rand);
		}
	}

	private boolean isFallenTreeBiome(Biome biome) {
		return biome == Biome.FOREST || biome == Biome.DARK_FOREST || biome == Biome.OLD_GROWTH_FOREST
				|| biome == Biome.BIRCH_FOREST || biome == Biome.TAIGA || biome == Biome.OLD_GROWTH_TAIGA
				|| biome == Biome.SNOWY_TAIGA || biome == Biome.SWAMP || biome == Biome.MANGROVE_SWAMP
				|| biome == Biome.JUNGLE || biome == Biome.SPARSE_JUNGLE || biome == Biome.BAMBOO_JUNGLE
				|| biome == Biome.WINDSWEPT_FOREST;
	}

	private boolean canPlaceFallenTree(int startX, int startZ, int dx, int dz, int length, int[] heightMap,
			int baseHeight) {
		for (int i = 0; i < length; i++) {
			final int x = startX + dx * i;
			final int z = startZ + dz * i;

			if (x < 0 || x >= SIZE || z < 0 || z >= SIZE)
				return false;

			final int groundY = heightMap[x * SIZE + z];
			if ((Math.abs(groundY - baseHeight) > 1) || (groundY + 1 >= HEIGHT))
				return false;

			final BlockType aboveGround = this.blocks[x][groundY + 1][z];
			if (aboveGround != BlockType.AIR && !aboveGround.isPlant())
				return false;

			if (this.hasNearbyLog(x, groundY + 1, z, 1))
				return false;
		}
		return true;
	}

	private BlockType getFallenTreeLog(Biome biome, java.util.Random rand) {
		final BlockType primaryLog = biome.getTreeLog();
		if (primaryLog != null)
			return primaryLog;

		if (biome == Biome.TAIGA || biome == Biome.OLD_GROWTH_TAIGA || biome == Biome.SNOWY_TAIGA)
			return BlockType.SPRUCE_LOG;
		if (biome == Biome.BIRCH_FOREST)
			return BlockType.BIRCH_LOG;
		if (biome == Biome.JUNGLE || biome == Biome.SPARSE_JUNGLE || biome == Biome.BAMBOO_JUNGLE)
			return BlockType.JUNGLE_LOG;

		return BlockType.OAK_LOG;
	}

	private void placeFallenTree(int startX, int y, int startZ, int dx, int dz, int length, BlockType logType,
			java.util.Random rand) {
		int axisMeta;
		if (dx != 0)
			axisMeta = 1;
		else if (dz != 0)
			axisMeta = 2;
		else
			throw new IllegalStateException("Fallen tree direction missing");
		for (int i = 0; i < length; i++) {
			final int x = startX + dx * i;
			final int z = startZ + dz * i;

			if (x < 0 || x >= SIZE || z < 0 || z >= SIZE)
				continue;

			int groundY = -1;
			for (int checkY = y + 2; checkY >= y - 2; checkY--)
				if (checkY >= 0 && checkY < HEIGHT && this.blocks[x][checkY][z] != BlockType.AIR
						&& !this.blocks[x][checkY][z].isPlant()) {
					groundY = checkY;
					break;
				}

			if (groundY < 0)
				groundY = y - 1;

			final int logY = groundY + 1;
			if (logY < HEIGHT && (this.blocks[x][logY][z] == BlockType.AIR || this.blocks[x][logY][z].isPlant())) {
				this.blocks[x][logY][z] = logType;
				this.blockMeta[x][logY][z] = (byte) axisMeta;
			}

			if (rand.nextFloat() < 0.15f && logY + 1 < HEIGHT && this.blocks[x][logY + 1][z] == BlockType.AIR)
				this.blocks[x][logY + 1][z] = BlockType.BROWN_MUSHROOM;
			else if (rand.nextFloat() < 0.08f && logY + 1 < HEIGHT && this.blocks[x][logY + 1][z] == BlockType.AIR)
				this.blocks[x][logY + 1][z] = BlockType.RED_MUSHROOM;
		}

		if (rand.nextFloat() < 0.3f) {
			final int rootX = startX - dx;
			final int rootZ = startZ - dz;
			if (rootX >= 0 && rootX < SIZE && rootZ >= 0 && rootZ < SIZE) {
				final int rootY = y;
				if (this.blocks[rootX][rootY][rootZ] == BlockType.AIR || this.blocks[rootX][rootY][rootZ].isPlant()) {
					this.blocks[rootX][rootY][rootZ] = logType;
					this.blockMeta[rootX][rootY][rootZ] = (byte) axisMeta;
				}
				if (rootY + 1 < HEIGHT && (this.blocks[rootX][rootY + 1][rootZ] == BlockType.AIR
						|| this.blocks[rootX][rootY + 1][rootZ].isPlant())) {
					this.blocks[rootX][rootY + 1][rootZ] = logType;
					this.blockMeta[rootX][rootY + 1][rootZ] = (byte) axisMeta;
				}
			}
		}
	}

	private boolean hasNearbyLog(int x, int y, int z, int radius) {
		final int minX = Math.max(0, x - radius);
		final int maxX = Math.min(SIZE - 1, x + radius);
		final int minZ = Math.max(0, z - radius);
		final int maxZ = Math.min(SIZE - 1, z + radius);
		final int minY = Math.max(0, y - 1);
		final int maxY = Math.min(HEIGHT - 1, y + 12);

		for (int nx = minX; nx <= maxX; nx++)
			for (int nz = minZ; nz <= maxZ; nz++)
				for (int ny = minY; ny <= maxY; ny++) {
					final BlockType type = this.blocks[nx][ny][nz];
					if (this.isLog(type))
						return true;
				}

		return false;
	}

	private void generateOakTree(int x, int y, int z, BlockType log, BlockType leaves) {
		if (x < 0 || x >= SIZE || z < 0 || z >= SIZE)
			return;
		if (y + 6 >= HEIGHT)
			return;

		for (int ly = 2; ly <= 5; ly++) {
			final int leafY = y + ly;
			if (leafY >= HEIGHT)
				continue;

			int radius;
			if (ly <= 3)
				radius = 2;
			else if (ly == 4)
				radius = 1;
			else
				radius = 0;

			for (int lx = -radius; lx <= radius; lx++)
				for (int lz = -radius; lz <= radius; lz++) {
					final int nx = x + lx;
					final int nz = z + lz;

					if (nx >= 0 && nx < SIZE && nz >= 0 && nz < SIZE) {
						if (radius == 2 && Math.abs(lx) == 2 && Math.abs(lz) == 2)
							continue;
						if (this.blocks[nx][leafY][nz] == BlockType.AIR)
							this.blocks[nx][leafY][nz] = leaves;
					}
				}
		}

		for (int h = 0; h < 4; h++)
			if (y + h < HEIGHT)
				this.blocks[x][y + h][z] = log;
	}

	private void generateLargeOakTree(int x, int y, int z, BlockType log, BlockType leaves, java.util.Random rand) {
		if (x < 0 || x >= SIZE || z < 0 || z >= SIZE)
			return;
		if (y + 8 >= HEIGHT)
			return;

		final int trunkHeight = 5 + rand.nextInt(2);

		for (int ly = 2; ly <= trunkHeight + 3; ly++) {
			final int leafY = y + ly;
			if (leafY >= HEIGHT)
				continue;

			int radius;
			if (ly <= trunkHeight)
				radius = 3;
			else if (ly == trunkHeight + 1)
				radius = 2;
			else if (ly == trunkHeight + 2)
				radius = 1;
			else
				radius = 0;

			for (int lx = -radius; lx <= radius; lx++)
				for (int lz = -radius; lz <= radius; lz++) {
					final int nx = x + lx;
					final int nz = z + lz;

					if (nx >= 0 && nx < SIZE && nz >= 0 && nz < SIZE) {
						if (radius == 3 && Math.abs(lx) == 3 && Math.abs(lz) == 3)
							continue;
						if (radius == 2 && Math.abs(lx) == 2 && Math.abs(lz) == 2 && rand.nextFloat() < 0.3f)
							continue;
						if (this.blocks[nx][leafY][nz] == BlockType.AIR)
							this.blocks[nx][leafY][nz] = leaves;
					}
				}
		}

		for (int h = 0; h < trunkHeight; h++)
			if (y + h < HEIGHT)
				this.blocks[x][y + h][z] = log;
	}

	private void generateSmallOakTree(int x, int y, int z, BlockType log, BlockType leaves) {
		if (x < 0 || x >= SIZE || z < 0 || z >= SIZE)
			return;
		if (y + 4 >= HEIGHT)
			return;

		for (int ly = 1; ly <= 3; ly++) {
			final int leafY = y + ly;
			if (leafY >= HEIGHT)
				continue;

			int radius;
			if (ly <= 2)
				radius = 1;
			else
				radius = 0;

			for (int lx = -radius; lx <= radius; lx++)
				for (int lz = -radius; lz <= radius; lz++) {
					final int nx = x + lx;
					final int nz = z + lz;

					if (nx >= 0 && nx < SIZE && nz >= 0 && nz < SIZE)
						if (this.blocks[nx][leafY][nz] == BlockType.AIR)
							this.blocks[nx][leafY][nz] = leaves;
				}
		}

		for (int h = 0; h < 3; h++)
			if (y + h < HEIGHT)
				this.blocks[x][y + h][z] = log;
	}

	private void generateBirchTree(int x, int y, int z, BlockType log, BlockType leaves) {
		if (x < 0 || x >= SIZE || z < 0 || z >= SIZE)
			return;
		if (y + 7 >= HEIGHT)
			return;

		for (int ly = 3; ly <= 6; ly++) {
			final int leafY = y + ly;
			if (leafY >= HEIGHT)
				continue;

			int radius;
			if (ly <= 4)
				radius = 2;
			else if (ly == 5)
				radius = 1;
			else
				radius = 0;

			for (int lx = -radius; lx <= radius; lx++)
				for (int lz = -radius; lz <= radius; lz++) {
					final int nx = x + lx;
					final int nz = z + lz;

					if (nx >= 0 && nx < SIZE && nz >= 0 && nz < SIZE) {
						if (radius == 2 && Math.abs(lx) == 2 && Math.abs(lz) == 2)
							continue;
						if (this.blocks[nx][leafY][nz] == BlockType.AIR)
							this.blocks[nx][leafY][nz] = leaves;
					}
				}
		}

		for (int h = 0; h < 5; h++)
			if (y + h < HEIGHT)
				this.blocks[x][y + h][z] = log;
	}

	private void generateSpruceTree(int x, int y, int z, BlockType log, BlockType leaves) {
		if (x < 0 || x >= SIZE || z < 0 || z >= SIZE)
			return;
		if (y + 9 >= HEIGHT)
			return;

		final int trunkHeight = 6;

		for (int ly = 2; ly <= trunkHeight + 2; ly++) {
			final int leafY = y + ly;
			if (leafY >= HEIGHT)
				continue;

			int radius;
			if (ly == trunkHeight + 2)
				radius = 0;
			else if (ly == trunkHeight + 1)
				radius = 1;
			else if (ly % 2 == 0)
				radius = 2;
			else
				radius = 1;

			for (int lx = -radius; lx <= radius; lx++)
				for (int lz = -radius; lz <= radius; lz++) {
					final int nx = x + lx;
					final int nz = z + lz;

					if (nx >= 0 && nx < SIZE && nz >= 0 && nz < SIZE) {
						if (radius == 2 && Math.abs(lx) == 2 && Math.abs(lz) == 2)
							continue;
						if (this.blocks[nx][leafY][nz] == BlockType.AIR)
							this.blocks[nx][leafY][nz] = leaves;
					}
				}
		}

		for (int h = 0; h < trunkHeight; h++)
			if (y + h < HEIGHT)
				this.blocks[x][y + h][z] = log;
	}

	private void generateJungleTree(int x, int y, int z, BlockType log, BlockType leaves, java.util.Random rand) {
		if (x < 0 || x >= SIZE || z < 0 || z >= SIZE)
			return;
		if (y + 12 >= HEIGHT)
			return;

		final int trunkHeight = 7 + rand.nextInt(4);

		for (int ly = trunkHeight - 3; ly <= trunkHeight + 2; ly++) {
			final int leafY = y + ly;
			if (leafY >= HEIGHT)
				continue;

			int radius;
			if (ly >= trunkHeight)
				radius = 2;
			else
				radius = 3;

			for (int lx = -radius; lx <= radius; lx++)
				for (int lz = -radius; lz <= radius; lz++) {
					final int nx = x + lx;
					final int nz = z + lz;

					if (nx >= 0 && nx < SIZE && nz >= 0 && nz < SIZE) {
						final double dist = Math.sqrt(lx * lx + lz * lz);
						if (dist > radius)
							continue;
						if (this.blocks[nx][leafY][nz] == BlockType.AIR)
							this.blocks[nx][leafY][nz] = leaves;
					}
				}
		}

		for (int h = 0; h < trunkHeight; h++)
			if (y + h < HEIGHT)
				this.blocks[x][y + h][z] = log;
	}

	public BlockType getBlock(int x, int y, int z) {
		if (x < 0 || x >= SIZE || y < 0 || y >= HEIGHT || z < 0 || z >= SIZE)
			return BlockType.AIR;
		final BlockType block = this.blocks[x][y][z];
		return block != null ? block : BlockType.AIR;
	}

	public int getBlockMeta(int x, int y, int z) {
		if (x < 0 || x >= SIZE || y < 0 || y >= HEIGHT || z < 0 || z >= SIZE)
			return 0;
		return this.blockMeta[x][y][z];
	}

	public void setBlock(int x, int y, int z, BlockType type) {
		this.setBlock(x, y, z, type, 0);
	}

	public void setBlock(int x, int y, int z, BlockType type, int metadata) {
		if (x < 0 || x >= SIZE || y < 0 || y >= HEIGHT || z < 0 || z >= SIZE)
			return;
		if (metadata < 0 || metadata > Byte.MAX_VALUE)
			throw new IllegalArgumentException("Metadata out of range: " + metadata);
		this.blocks[x][y][z] = type;
		this.blockMeta[x][y][z] = type == BlockType.AIR ? 0 : (byte) metadata;
		if (type != BlockType.AIR && y > this.maxBlockY)
			this.maxBlockY = y;
		this.meshDirty = true;
		this.lightDirty = true;
	}

	public int getSkyLight(int x, int y, int z) {
		if (x < 0 || x >= SIZE || y < 0 || y >= HEIGHT || z < 0 || z >= SIZE)
			return y >= HEIGHT ? MAX_LIGHT : 0;
		return this.skyLight[x][y][z];
	}

	public void setSkyLight(int x, int y, int z, int value) {
		if (x < 0 || x >= SIZE || y < 0 || y >= HEIGHT || z < 0 || z >= SIZE)
			return;
		this.skyLight[x][y][z] = (byte) value;
	}

	public int getBlockLight(int x, int y, int z) {
		if (x < 0 || x >= SIZE || y < 0 || y >= HEIGHT || z < 0 || z >= SIZE)
			return 0;
		return this.blockLight[x][y][z];
	}

	public void setBlockLight(int x, int y, int z, int value) {
		if (x < 0 || x >= SIZE || y < 0 || y >= HEIGHT || z < 0 || z >= SIZE)
			return;
		this.blockLight[x][y][z] = (byte) value;
	}

	private static final ThreadLocal<int[]> LIGHT_QUEUE_BUFFER = ThreadLocal
			.withInitial(() -> new int[SIZE * SIZE * HEIGHT]);

	private static int encodeLightPos(int x, int y, int z) {
		return x + z * SIZE + y * SIZE * SIZE;
	}

	private static int decodeLightX(int encoded) {
		return encoded % SIZE;
	}

	private static int decodeLightZ(int encoded) {
		return encoded / SIZE % SIZE;
	}

	private static int decodeLightY(int encoded) {
		return encoded / (SIZE * SIZE);
	}

	public void calculateSkyLight() {
		for (int x = 0; x < SIZE; x++)
			for (int y = 0; y < HEIGHT; y++)
				for (int z = 0; z < SIZE; z++)
					this.skyLight[x][y][z] = 0;

		final int[] queue = LIGHT_QUEUE_BUFFER.get();
		final int queueHead = 0;
		int queueTail = 0;

		for (int x = 0; x < SIZE; x++)
			for (int z = 0; z < SIZE; z++)
				for (int y = HEIGHT - 1; y >= 0; y--) {
					final BlockType block = this.blocks[x][y][z];
					if (block.blocksLight())
						break;
					this.skyLight[x][y][z] = MAX_LIGHT;
					queue[queueTail++] = encodeLightPos(x, y, z);
				}

		this.propagateLightOptimized(queue, queueHead, queueTail, true);
	}

	public void calculateBlockLight() {
		for (int x = 0; x < SIZE; x++)
			for (int y = 0; y < HEIGHT; y++)
				for (int z = 0; z < SIZE; z++)
					this.blockLight[x][y][z] = 0;

		final int[] queue = LIGHT_QUEUE_BUFFER.get();
		final int queueHead = 0;
		int queueTail = 0;

		for (int x = 0; x < SIZE; x++)
			for (int y = 0; y < HEIGHT; y++)
				for (int z = 0; z < SIZE; z++) {
					final BlockType block = this.blocks[x][y][z];
					if (block.getLightEmission() > 0) {
						this.blockLight[x][y][z] = (byte) block.getLightEmission();
						queue[queueTail++] = encodeLightPos(x, y, z);
					}
				}

		this.propagateLightOptimized(queue, queueHead, queueTail, false);
	}

	private void propagateLightOptimized(int[] queue, int head, int tail, boolean isSkyLight) {
		while (head < tail) {
			final int encoded = queue[head++];
			final int x = decodeLightX(encoded);
			final int z = decodeLightZ(encoded);
			final int y = decodeLightY(encoded);

			final int currentLight = isSkyLight ? this.skyLight[x][y][z] : this.blockLight[x][y][z];

			for (int dir = 0; dir < 6; dir++) {
				int nx = x, ny = y, nz = z;
				switch (dir) {
					case 0 -> nx++;
					case 1 -> nx--;
					case 2 -> ny++;
					case 3 -> ny--;
					case 4 -> nz++;
					case 5 -> nz--;
				}

				if (ny < 0 || ny >= HEIGHT)
					continue;
				if (nx < 0 || nx >= SIZE || nz < 0 || nz >= SIZE)
					continue;

				int newLight;
				if (isSkyLight && dir == 3 && currentLight == MAX_LIGHT)
					newLight = MAX_LIGHT;
				else
					newLight = currentLight - 1;

				if (newLight <= 0)
					continue;

				final BlockType neighborBlock = this.blocks[nx][ny][nz];
				if (neighborBlock.blocksLight())
					continue;

				final byte[] lightArray = isSkyLight ? this.skyLight[nx][ny] : this.blockLight[nx][ny];
				if (newLight > lightArray[nz]) {
					lightArray[nz] = (byte) newLight;
					if (tail < queue.length)
						queue[tail++] = encodeLightPos(nx, ny, nz);
				}
			}
		}
	}

	public void buildMesh(World world) {
		this.buildMeshDataThreaded(world);
		this.uploadMeshToGPU();
	}

	public void buildMeshDataThreaded(World world) {
		if (!this.meshDirty)
			return;
		this.world = world;
		if (this.lightDirty) {
			this.calculateSkyLight();
			this.calculateBlockLight();
			this.lightDirty = false;
		}
		this.syncBoundaryLightFromNeighbors(world);
		final MeshArray solid = new MeshArray(SOLID_VERTEX_BUFFER, SOLID_INDEX_BUFFER);
		final MeshArray water = new MeshArray(WATER_VERTEX_BUFFER, WATER_INDEX_BUFFER);
		this.rebuildGeometryThreaded(world, solid, water);
		final ChunkMeshData data = new ChunkMeshData();
		data.vertices = Arrays.copyOf(solid.getVertices(), solid.getVertexFloats());
		data.indices = Arrays.copyOf(solid.getIndices(), solid.getIndexCount());
		data.vertexCount = solid.getVertexFloats();
		data.indexCount = solid.getIndexCount();
		data.waterVertices = Arrays.copyOf(water.getVertices(), water.getVertexFloats());
		data.waterIndices = Arrays.copyOf(water.getIndices(), water.getIndexCount());
		data.waterVertexCount = water.getVertexFloats();
		data.waterIndexCount = water.getIndexCount();
		data.ready = true;
		this.pendingMeshData = data;
		this.meshDirty = false;
	}

	private void syncBoundaryLightFromNeighbors(World world) {
		final int[] skyQueue = LIGHT_QUEUE_BUFFER.get();
		final int[] blockQueue = new int[SIZE * SIZE * HEIGHT];
		int skyTail = 0;
		int blockTail = 0;

		for (int edge = 0; edge < 4; edge++) {
			final int fixedX = edge == 0 ? 0 : edge == 1 ? SIZE - 1 : -1;
			final int fixedZ = edge == 2 ? 0 : edge == 3 ? SIZE - 1 : -1;
			final boolean isXEdge = fixedX >= 0;

			for (int i = 0; i < SIZE; i++) {
				final int x = isXEdge ? fixedX : i;
				final int z = isXEdge ? i : fixedZ;
				for (int y = 0; y < HEIGHT; y++) {
					if (this.blocks[x][y][z].blocksLight())
						continue;
					final int worldX = this.chunkX * SIZE + x;
					final int worldZ = this.chunkZ * SIZE + z;
					final int currentSky = this.skyLight[x][y][z];
					final int currentBlock = this.blockLight[x][y][z];
					final int neighborX = isXEdge ? fixedX == 0 ? worldX - 1 : worldX + 1 : worldX;
					final int neighborZ = isXEdge ? worldZ : fixedZ == 0 ? worldZ - 1 : worldZ + 1;
					final int neighborSky = world.getSkyLightAt(neighborX, y, neighborZ);
					final int neighborBlock = world.getBlockLightAt(neighborX, y, neighborZ);
					final int newSky = Math.max(0, neighborSky - 1);
					final int newBlock = Math.max(0, neighborBlock - 1);
					if (newSky > currentSky) {
						this.skyLight[x][y][z] = (byte) newSky;
						skyQueue[skyTail++] = encodeLightPos(x, y, z);
					}
					if (newBlock > currentBlock) {
						this.blockLight[x][y][z] = (byte) newBlock;
						blockQueue[blockTail++] = encodeLightPos(x, y, z);
					}
				}
			}
		}

		if (skyTail > 0)
			this.propagateLightOptimized(skyQueue, 0, skyTail, true);
		if (blockTail > 0)
			this.propagateLightOptimized(blockQueue, 0, blockTail, false);
	}

	private void rebuildGeometryThreaded(World world, MeshArray solid, MeshArray water) {
		final int worldOffsetX = this.chunkX * SIZE;
		final int worldOffsetZ = this.chunkZ * SIZE;
		final int yLimit = Math.max(this.maxBlockY + 1, SEA_LEVEL + 5);
		for (int x = 0; x < SIZE; x++)
			for (int y = 0; y < yLimit; y++)
				for (int z = 0; z < SIZE; z++) {
					final BlockType block = this.blocks[x][y][z];
					if (block == BlockType.AIR)
						continue;
					final int metadata = this.blockMeta[x][y][z] & 0xFF;
					final float wx = worldOffsetX + x;
					final float wz = worldOffsetZ + z;
					if (block.isPlant()) {
						this.addCrossGeometry(solid, x, y, z, wx, wz, block, world);
						continue;
					}
					if (!block.isSolid())
						continue;
					final MeshArray target = block.isWater() ? water : solid;
					for (final BlockType.Face face : BlockType.Face.values()) {
						final int nx = x + face.getNx();
						final int ny = y + face.getNy();
						final int nz = z + face.getNz();
						BlockType neighbor;
						if (nx >= 0 && nx < SIZE && nz >= 0 && nz < SIZE)
							neighbor = this.getBlock(nx, ny, nz);
						else
							neighbor = world.getBlockAt(worldOffsetX + nx, ny, worldOffsetZ + nz);
						if (neighbor.isSolid() && !neighbor.isTransparent())
							continue;
						if (this.isLeafBlock(block) && this.isLeafBlock(neighbor)) {
							final boolean renderFace = face.getNx() > 0 || face.getNy() > 0 || face.getNz() > 0;
							if (!renderFace)
								continue;
						}
						if ((block == BlockType.GLASS && neighbor == BlockType.GLASS) || (block.isWater() && neighbor.isWater()))
							continue;
						this.addFaceWithAO(target, x, y, z, wx, wz, face, block, world, metadata);
					}
				}
	}

	public void uploadMeshToGPU() {
		final ChunkMeshData data = this.pendingMeshData;
		if (data == null || !data.ready)
			return;
		if (data.vertexCount == 0) {
			this.indexCount = 0;
			this.waterIndexCount = 0;
			this.pendingMeshData = null;
			return;
		}
		if (!this.hasMesh) {
			this.vao = glGenVertexArrays();
			this.vbo = glGenBuffers();
			this.ebo = glGenBuffers();
			this.hasMesh = true;
		}
		glBindVertexArray(this.vao);
		final FloatBuffer vertexBuffer = BufferPool.acquireFloat(data.vertexCount);
		vertexBuffer.put(data.vertices, 0, data.vertexCount).flip();
		glBindBuffer(GL_ARRAY_BUFFER, this.vbo);
		glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_DYNAMIC_DRAW);
		final IntBuffer indexBuffer = BufferPool.acquireInt(data.indexCount);
		indexBuffer.put(data.indices, 0, data.indexCount).flip();
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, this.ebo);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_DYNAMIC_DRAW);
		this.indexCount = data.indexCount;
		final int stride = VERTEX_STRIDE * Float.BYTES;
		glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
		glEnableVertexAttribArray(0);
		glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3L * Float.BYTES);
		glEnableVertexAttribArray(1);
		glVertexAttribPointer(2, 3, GL_FLOAT, false, stride, 5L * Float.BYTES);
		glEnableVertexAttribArray(2);
		glVertexAttribPointer(3, 3, GL_FLOAT, false, stride, 8L * Float.BYTES);
		glEnableVertexAttribArray(3);
		glVertexAttribPointer(4, 1, GL_FLOAT, false, stride, 11L * Float.BYTES);
		glEnableVertexAttribArray(4);
		glVertexAttribPointer(5, 1, GL_FLOAT, false, stride, 12L * Float.BYTES);
		glEnableVertexAttribArray(5);
		glVertexAttribPointer(6, 1, GL_FLOAT, false, stride, 13L * Float.BYTES);
		glEnableVertexAttribArray(6);
		BufferPool.releaseFloat(vertexBuffer);
		BufferPool.releaseInt(indexBuffer);
		if (data.waterVertexCount > 0 && data.waterIndexCount > 0) {
			if (!this.hasWaterMesh) {
				this.waterVao = glGenVertexArrays();
				this.waterVbo = glGenBuffers();
				this.waterEbo = glGenBuffers();
				this.hasWaterMesh = true;
			}
			glBindVertexArray(this.waterVao);
			final FloatBuffer waterVertexBuffer = BufferPool.acquireFloat(data.waterVertexCount);
			waterVertexBuffer.put(data.waterVertices, 0, data.waterVertexCount).flip();
			glBindBuffer(GL_ARRAY_BUFFER, this.waterVbo);
			glBufferData(GL_ARRAY_BUFFER, waterVertexBuffer, GL_DYNAMIC_DRAW);
			final IntBuffer waterIndexBuffer = BufferPool.acquireInt(data.waterIndexCount);
			waterIndexBuffer.put(data.waterIndices, 0, data.waterIndexCount).flip();
			glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, this.waterEbo);
			glBufferData(GL_ELEMENT_ARRAY_BUFFER, waterIndexBuffer, GL_DYNAMIC_DRAW);
			this.waterIndexCount = data.waterIndexCount;
			glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
			glEnableVertexAttribArray(0);
			glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3L * Float.BYTES);
			glEnableVertexAttribArray(1);
			glVertexAttribPointer(2, 3, GL_FLOAT, false, stride, 5L * Float.BYTES);
			glEnableVertexAttribArray(2);
			glVertexAttribPointer(3, 3, GL_FLOAT, false, stride, 8L * Float.BYTES);
			glEnableVertexAttribArray(3);
			glVertexAttribPointer(4, 1, GL_FLOAT, false, stride, 11L * Float.BYTES);
			glEnableVertexAttribArray(4);
			glVertexAttribPointer(5, 1, GL_FLOAT, false, stride, 12L * Float.BYTES);
			glEnableVertexAttribArray(5);
			glVertexAttribPointer(6, 1, GL_FLOAT, false, stride, 13L * Float.BYTES);
			glEnableVertexAttribArray(6);
			BufferPool.releaseFloat(waterVertexBuffer);
			BufferPool.releaseInt(waterIndexBuffer);
		} else
			this.waterIndexCount = 0;
		glBindVertexArray(0);
		this.pendingMeshData = null;
	}

	private void addCrossGeometry(MeshArray buffers,
			int localX, int localY, int localZ,
			float worldX, float worldZ, BlockType block, World world) {

		final int[] tile = block.getTileForFace(BlockType.Face.NORTH, 0);
		if (tile == null)
			return;

		final float[] uv = this.atlas.getTileUV(tile[0], tile[1]);
		final float u1 = uv[0], v1 = uv[1], u2 = uv[2], v2 = uv[3];

		final Biome biome = this.getBiomeAt(localX, localZ);

		float[] color;
		if (block.needsGrassTint())
			color = BlockType.getGrassColor(biome);
		else if (block.needsFoliageTint())
			color = BlockType.getFoliageColor(biome);
		else if (block.needsTint(BlockType.Face.TOP))
			color = BlockType.getGrassColor(biome);
		else if (this.isSandBlock(block) && biome != null)
			color = biome.getSandTint();
		else
			color = new float[] { 1.0f, 1.0f, 1.0f };

		final float noise = this.plantNoise(worldX, worldZ);
		float cr = color[0] * (0.85f + noise * 0.25f);
		float cg = color[1] * (0.85f + noise * 0.25f);
		float cb = color[2] * (0.85f + noise * 0.25f);

		if (block == BlockType.ROSE) {
			cr = 0.65f + noise * 0.15f;
			cg = 0.15f + noise * 0.1f;
			cb = 0.18f + noise * 0.1f;
		} else if (block == BlockType.DANDELION) {
			cr = 0.75f + noise * 0.15f;
			cg = 0.65f + noise * 0.15f;
			cb = 0.15f + noise * 0.1f;
		}

		final float skyLight = this.getSkyLight(localX, localY, localZ) / (float) MAX_LIGHT;
		final float blockLight = this.getBlockLight(localX, localY, localZ) / (float) MAX_LIGHT;

		final float x = worldX;
		final float y = localY;
		final float z = worldZ;

		final float[][] quad1 = {
				{ x, y, z },
				{ x, y + 1, z },
				{ x + 1, y + 1, z + 1 },
				{ x + 1, y, z + 1 }
		};

		final float[][] quad2 = {
				{ x + 1, y, z },
				{ x + 1, y + 1, z },
				{ x, y + 1, z + 1 },
				{ x, y, z + 1 }
		};

		final float[][] uvCoords = {
				{ u1, v1 }, { u1, v2 }, { u2, v2 }, { u2, v1 }
		};

		this.addCrossQuad(buffers, quad1, uvCoords, cr, cg, cb, skyLight, blockLight);
		this.addCrossQuad(buffers, quad2, uvCoords, cr, cg, cb, skyLight, blockLight);
	}

	private void addCrossQuad(MeshArray buffers,
			float[][] quadVerts, float[][] uvCoords,
			float cr, float cg, float cb, float skyLight, float blockLight) {

		final int baseIndex = buffers.baseVertexIndex();
		for (int i = 0; i < 4; i++)
			buffers.addVertex(quadVerts[i][0], quadVerts[i][1], quadVerts[i][2],
					uvCoords[i][0], uvCoords[i][1],
					0.0f, 1.0f, 0.0f,
					cr, cg, cb,
					1.0f, skyLight, blockLight);
		buffers.addTriangle(baseIndex, baseIndex + 1, baseIndex + 2);
		buffers.addTriangle(baseIndex, baseIndex + 2, baseIndex + 3);
	}

	private void addFaceWithAO(MeshArray buffers,
			int localX, int localY, int localZ,
			float worldX, float worldZ,
			BlockType.Face face, BlockType block, World world, int metadata) {

		final int[] tile = block.getTileForFace(face, metadata);
		if (tile == null)
			return;

		final float[] uv = this.atlas.getTileUV(tile[0], tile[1]);
		final float u1 = uv[0], v1 = uv[1], u2 = uv[2], v2 = uv[3];

		final float fnx = face.getNx();
		final float fny = face.getNy();
		final float fnz = face.getNz();

		final Biome biome = this.getBiomeAt(localX, localZ);

		float[] color;
		if (block.isWater())
			color = new float[] { 0.1f, 0.1f, 1.0f };
		else if (block.needsFoliageTint())
			color = BlockType.getFoliageColor(biome);
		else if (block.needsTint(face))
			color = BlockType.getGrassColor(biome);
		else if (this.isGrassBlock(block) && this.isSideFace(face))
			color = new float[] { 0.62f, 0.62f, 0.62f };
		else if (this.isLogBlock(block))
			color = new float[] { 0.98f, 0.98f, 1.0f };
		else if (this.isSandBlock(block) && biome != null)
			color = biome.getSandTint();
		else if (this.isStoneBlock(block) && biome != null)
			color = biome.getStoneTint();
		else
			color = new float[] { 1.0f, 1.0f, 1.0f };
		float cr = color[0], cg = color[1], cb = color[2];

		float leafOcclusion = 0.0f;
		if (this.isLeafBlock(block)) {
			leafOcclusion = this.calculateLeafOcclusion(localX, localY, localZ, world);
			final float occlusionDarkening = 1.0f - leafOcclusion * 0.55f;
			cr *= occlusionDarkening;
			cg *= occlusionDarkening;
			cb *= occlusionDarkening;
		}

		final float[][] faceVertices = this.getFaceVertices(worldX, localY, worldZ, face);
		final float[][] faceUVs = this.getFaceUVs(face, u1, v1, u2, v2);

		final float[] aoValues = this.calculateFaceAO(localX, localY, localZ, face, world);
		final float[][] lightValues = this.calculateFaceLightSeparate(localX, localY, localZ, face, world);

		if (this.isLeafBlock(block))
			for (int i = 0; i < 4; i++)
				aoValues[i] = Math.max(0.0f, aoValues[i] - leafOcclusion * 0.3f);

		final int baseIndex = buffers.baseVertexIndex();

		for (int i = 0; i < 4; i++)
			buffers.addVertex(faceVertices[i][0], faceVertices[i][1], faceVertices[i][2],
					faceUVs[i][0], faceUVs[i][1],
					fnx, fny, fnz,
					cr, cg, cb,
					aoValues[i], lightValues[i][0], lightValues[i][1]);

		final float ao02 = aoValues[0] + aoValues[2];
		final float ao13 = aoValues[1] + aoValues[3];

		if (ao02 > ao13) {
			buffers.addTriangle(baseIndex, baseIndex + 1, baseIndex + 2);
			buffers.addTriangle(baseIndex, baseIndex + 2, baseIndex + 3);
		} else {
			buffers.addTriangle(baseIndex + 1, baseIndex + 2, baseIndex + 3);
			buffers.addTriangle(baseIndex + 1, baseIndex + 3, baseIndex);
		}
	}

	private float[] calculateFaceAO(int x, int y, int z, BlockType.Face face, World world) {
		final float[] ao = new float[4];
		final int worldX = this.chunkX * SIZE + x;
		final int worldZ = this.chunkZ * SIZE + z;
		final float[][] faceVerts = this.getFaceVertices(worldX, y, worldZ, face);

		for (int i = 0; i < 4; i++) {
			final int vx = (int) Math.floor(faceVerts[i][0]);
			final int vy = (int) Math.floor(faceVerts[i][1]);
			final int vz = (int) Math.floor(faceVerts[i][2]);
			ao[i] = this.calculateVertexAOByPosition(vx, vy, vz, face, world);
		}

		return ao;
	}

	private float calculateVertexAOByPosition(int vx, int vy, int vz, BlockType.Face face, World world) {
		final int ny = face.getNy();
		final int nx = face.getNx();
		final int nz = face.getNz();

		boolean b00, b01, b10, b11;

		if (ny != 0) {
			b00 = this.isBlockOpaque(vx - 1, vy + (ny > 0 ? 0 : -1), vz - 1, world);
			b01 = this.isBlockOpaque(vx, vy + (ny > 0 ? 0 : -1), vz - 1, world);
			b10 = this.isBlockOpaque(vx - 1, vy + (ny > 0 ? 0 : -1), vz, world);
			b11 = this.isBlockOpaque(vx, vy + (ny > 0 ? 0 : -1), vz, world);
		} else if (nx != 0) {
			b00 = this.isBlockOpaque(vx + (nx > 0 ? 0 : -1), vy - 1, vz - 1, world);
			b01 = this.isBlockOpaque(vx + (nx > 0 ? 0 : -1), vy, vz - 1, world);
			b10 = this.isBlockOpaque(vx + (nx > 0 ? 0 : -1), vy - 1, vz, world);
			b11 = this.isBlockOpaque(vx + (nx > 0 ? 0 : -1), vy, vz, world);
		} else {
			b00 = this.isBlockOpaque(vx - 1, vy - 1, vz + (nz > 0 ? 0 : -1), world);
			b01 = this.isBlockOpaque(vx, vy - 1, vz + (nz > 0 ? 0 : -1), world);
			b10 = this.isBlockOpaque(vx - 1, vy, vz + (nz > 0 ? 0 : -1), world);
			b11 = this.isBlockOpaque(vx, vy, vz + (nz > 0 ? 0 : -1), world);
		}

		final int count = (b00 ? 1 : 0) + (b01 ? 1 : 0) + (b10 ? 1 : 0) + (b11 ? 1 : 0);

		if (b00 && b11 || b01 && b10)
			return 0.0f;

		return 1.0f - count * 0.2f;
	}

	private boolean isBlockOpaque(int wx, int wy, int wz, World world) {
		final BlockType block = world.getBlockAt(wx, wy, wz);
		return block.blocksLight();
	}

	private boolean isLeafBlock(BlockType block) {
		return block == BlockType.OAK_LEAVES || block == BlockType.BIRCH_LEAVES
				|| block == BlockType.SPRUCE_LEAVES;
	}

	private boolean isLogBlock(BlockType block) {
		return block == BlockType.OAK_LOG || block == BlockType.BIRCH_LOG
				|| block == BlockType.JUNGLE_LOG || block == BlockType.SPRUCE_LOG;
	}

	private boolean isSandBlock(BlockType block) {
		return block == BlockType.SAND || block == BlockType.SANDSTONE;
	}

	private float plantNoise(float x, float z) {
		long seed = (long) (x * 12345.6789 + z * 98765.4321);
		seed = seed * 1103515245 + 12345;
		return (seed >> 16 & 0x7FFF) / 32768.0f;
	}

	private boolean isStoneBlock(BlockType block) {
		return block == BlockType.STONE || block == BlockType.COBBLESTONE || block == BlockType.MOSSY_COBBLESTONE
				|| block == BlockType.STONE_BRICKS
				|| block == BlockType.GRAVEL;
	}

	private boolean isGrassBlock(BlockType block) {
		return block == BlockType.GRASS || block == BlockType.SNOWY_GRASS;
	}

	private boolean isSideFace(BlockType.Face face) {
		return face == BlockType.Face.NORTH || face == BlockType.Face.SOUTH
				|| face == BlockType.Face.EAST || face == BlockType.Face.WEST;
	}

	private float calculateLeafOcclusion(int localX, int localY, int localZ, World world) {
		final int worldX = this.chunkX * SIZE + localX;
		final int worldZ = this.chunkZ * SIZE + localZ;

		int leafCount = 0;
		int totalChecked = 0;

		for (int dx = -1; dx <= 1; dx++)
			for (int dy = -1; dy <= 1; dy++)
				for (int dz = -1; dz <= 1; dz++) {
					if (dx == 0 && dy == 0 && dz == 0)
						continue;

					totalChecked++;
					final BlockType neighbor = world.getBlockAt(worldX + dx, localY + dy, worldZ + dz);
					if (this.isLeafBlock(neighbor) || neighbor.blocksLight())
						leafCount++;
				}

		return leafCount / (float) totalChecked;
	}

	private float[][] calculateFaceLightSeparate(int x, int y, int z, BlockType.Face face, World world) {
		final int worldX = this.chunkX * SIZE + x;
		final int worldZ = this.chunkZ * SIZE + z;

		final int faceNX = worldX + face.getNx();
		final int faceNY = y + face.getNy();
		final int faceNZ = worldZ + face.getNz();

		final int fallbackSky = world.getSkyLightAt(faceNX, faceNY, faceNZ);
		final int fallbackBlock = world.getBlockLightAt(faceNX, faceNY, faceNZ);

		final float[][] lightValues = new float[4][2];
		final float[][] faceVerts = this.getFaceVertices(worldX, y, worldZ, face);

		for (int i = 0; i < 4; i++) {
			final int vx = (int) Math.floor(faceVerts[i][0]);
			final int vy = (int) Math.floor(faceVerts[i][1]);
			final int vz = (int) Math.floor(faceVerts[i][2]);

			final float[] separateLight = this.calculateVertexLightSeparate(vx, vy, vz, face, world,
					fallbackSky / (float) MAX_LIGHT, fallbackBlock / (float) MAX_LIGHT);
			lightValues[i][0] = separateLight[0];
			lightValues[i][1] = separateLight[1];
		}

		return lightValues;
	}

	private float[] calculateVertexLightSeparate(int vx, int vy, int vz, BlockType.Face face, World world,
			float fallbackSky, float fallbackBlock) {
		final int[][] samples = this.getLightSamplesForVertex(vx, vy, vz, face);
		float totalSky = 0;
		float totalBlock = 0;
		int count = 0;

		for (final int[] sample : samples) {
			final BlockType sampleBlock = world.getBlockAt(sample[0], sample[1], sample[2]);
			if (!sampleBlock.blocksLight()) {
				final int sky = world.getSkyLightAt(sample[0], sample[1], sample[2]);
				final int block = world.getBlockLightAt(sample[0], sample[1], sample[2]);
				totalSky += sky;
				totalBlock += block;
				count++;
			}
		}

		if (count > 0)
			return new float[] { totalSky / count / MAX_LIGHT, totalBlock / count / MAX_LIGHT };
		return new float[] { fallbackSky, fallbackBlock };
	}

	private int[][] getLightSamplesForVertex(int vx, int vy, int vz, BlockType.Face face) {
		return switch (face) {
			case TOP -> new int[][] {
					{ vx - 1, vy, vz - 1 },
					{ vx, vy, vz - 1 },
					{ vx - 1, vy, vz },
					{ vx, vy, vz }
			};
			case BOTTOM -> new int[][] {
					{ vx - 1, vy - 1, vz - 1 },
					{ vx, vy - 1, vz - 1 },
					{ vx - 1, vy - 1, vz },
					{ vx, vy - 1, vz }
			};
			case NORTH -> new int[][] {
					{ vx - 1, vy - 1, vz - 1 },
					{ vx, vy - 1, vz - 1 },
					{ vx - 1, vy, vz - 1 },
					{ vx, vy, vz - 1 }
			};
			case SOUTH -> new int[][] {
					{ vx - 1, vy - 1, vz },
					{ vx, vy - 1, vz },
					{ vx - 1, vy, vz },
					{ vx, vy, vz }
			};
			case EAST -> new int[][] {
					{ vx, vy - 1, vz - 1 },
					{ vx, vy - 1, vz },
					{ vx, vy, vz - 1 },
					{ vx, vy, vz }
			};
			case WEST -> new int[][] {
					{ vx - 1, vy - 1, vz - 1 },
					{ vx - 1, vy - 1, vz },
					{ vx - 1, vy, vz - 1 },
					{ vx - 1, vy, vz }
			};
		};
	}

	private float[][] getFaceUVs(BlockType.Face face, float u1, float v1, float u2, float v2) {
		return switch (face) {
			case TOP, BOTTOM -> new float[][] {
					{ u1, v1 }, { u2, v1 }, { u2, v2 }, { u1, v2 }
			};
			case NORTH, SOUTH, EAST, WEST -> new float[][] {
					{ u1, v1 }, { u1, v2 }, { u2, v2 }, { u2, v1 }
			};
		};
	}

	private float[][] getFaceVertices(float x, float y, float z, BlockType.Face face) {
		return switch (face) {
			case TOP -> new float[][] {
					{ x, y + 1, z },
					{ x + 1, y + 1, z },
					{ x + 1, y + 1, z + 1 },
					{ x, y + 1, z + 1 }
			};
			case BOTTOM -> new float[][] {
					{ x, y, z + 1 },
					{ x + 1, y, z + 1 },
					{ x + 1, y, z },
					{ x, y, z }
			};
			case NORTH -> new float[][] {
					{ x, y, z },
					{ x, y + 1, z },
					{ x + 1, y + 1, z },
					{ x + 1, y, z }
			};
			case SOUTH -> new float[][] {
					{ x + 1, y, z + 1 },
					{ x + 1, y + 1, z + 1 },
					{ x, y + 1, z + 1 },
					{ x, y, z + 1 }
			};
			case EAST -> new float[][] {
					{ x + 1, y, z },
					{ x + 1, y + 1, z },
					{ x + 1, y + 1, z + 1 },
					{ x + 1, y, z + 1 }
			};
			case WEST -> new float[][] {
					{ x, y, z + 1 },
					{ x, y + 1, z + 1 },
					{ x, y + 1, z },
					{ x, y, z }
			};
		};
	}

	public void updateFade(float deltaTime) {
		if (this.fadeComplete)
			return;

		this.fadeProgress += deltaTime / FADE_DURATION;
		if (this.fadeProgress >= 1.0f) {
			this.fadeProgress = 1.0f;
			this.fadeComplete = true;
		}
	}

	public float getFadeProgress() {
		final float t = this.fadeProgress;
		return t * t * (3.0f - 2.0f * t);
	}

	public int getOpaqueVao() {
		return this.vao;
	}

	public int getOpaqueIndexCount() {
		return this.indexCount;
	}

	public boolean isReadyToRender() {
		return this.hasMesh && this.indexCount > 0;
	}

	public boolean hasWaterToRender() {
		return this.hasWaterMesh && this.waterIndexCount > 0;
	}

	public void renderOpaque() {
		if (!this.hasMesh || this.indexCount == 0)
			return;

		glBindVertexArray(this.vao);
		glDrawElements(GL_TRIANGLES, this.indexCount, GL_UNSIGNED_INT, 0);
		glBindVertexArray(0);
	}

	public void renderWater() {
		if (!this.hasWaterMesh || this.waterIndexCount == 0)
			return;

		glBindVertexArray(this.waterVao);
		glDrawElements(GL_TRIANGLES, this.waterIndexCount, GL_UNSIGNED_INT, 0);
		glBindVertexArray(0);
	}

	public void cleanup() {
		if (this.hasMesh) {
			glDeleteVertexArrays(this.vao);
			glDeleteBuffers(this.vbo);
			glDeleteBuffers(this.ebo);
			this.hasMesh = false;
		}
		if (this.hasWaterMesh) {
			glDeleteVertexArrays(this.waterVao);
			glDeleteBuffers(this.waterVbo);
			glDeleteBuffers(this.waterEbo);
			this.hasWaterMesh = false;
		}
	}
}
