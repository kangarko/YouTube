package com.matejpacan.voxelcraft;

import java.util.Arrays;

public class GreedyMesher {

	private static final int SIZE = Chunk.SIZE;
	private static final int HEIGHT = Chunk.HEIGHT;

	private static final ThreadLocal<int[]> MASK_BUFFER = ThreadLocal.withInitial(() -> new int[SIZE * HEIGHT]);
	private static final ThreadLocal<FaceData[]> FACE_DATA_BUFFER = ThreadLocal.withInitial(() -> {
		final FaceData[] data = new FaceData[SIZE * HEIGHT];
		for (int i = 0; i < data.length; i++)
			data[i] = new FaceData();
		return data;
	});

	public static class FaceData {
		BlockType blockType;
		int tileX, tileY;
		float ao0, ao1, ao2, ao3;
		float skyLight, blockLight;
		float colorR, colorG, colorB;
		boolean valid;

		void reset() {
			this.blockType = null;
			this.valid = false;
		}

		boolean canMergeWith(FaceData other) {
			if (!this.valid || !other.valid || (this.blockType != other.blockType))
				return false;
			if (this.tileX != other.tileX || this.tileY != other.tileY)
				return false;
			if (Math.abs(this.colorR - other.colorR) > 0.01f ||
					Math.abs(this.colorG - other.colorG) > 0.01f ||
					Math.abs(this.colorB - other.colorB) > 0.01f)
				return false;
			final float aoTolerance = 0.15f;
			if (Math.abs(this.ao0 - other.ao0) > aoTolerance ||
					Math.abs(this.ao1 - other.ao1) > aoTolerance ||
					Math.abs(this.ao2 - other.ao2) > aoTolerance ||
					Math.abs(this.ao3 - other.ao3) > aoTolerance)
				return false;
			final float lightTolerance = 0.1f;
			if (Math.abs(this.skyLight - other.skyLight) > lightTolerance ||
					Math.abs(this.blockLight - other.blockLight) > lightTolerance)
				return false;
			return true;
		}
	}

	public interface QuadEmitter {
		void emitQuad(
				float x1, float y1, float z1,
				float x2, float y2, float z2,
				float x3, float y3, float z3,
				float x4, float y4, float z4,
				float u1, float v1, float u2, float v2,
				float nx, float ny, float nz,
				float cr, float cg, float cb,
				float ao0, float ao1, float ao2, float ao3,
				float skyLight, float blockLight,
				int widthInBlocks, int heightInBlocks);
	}

	public static void meshFace(
			BlockType.Face face,
			BlockType[][][] blocks,
			Chunk chunk,
			World world,
			TextureAtlas atlas,
			int maxBlockY,
			QuadEmitter emitter) {
		final int[] mask = MASK_BUFFER.get();
		final FaceData[] faceData = FACE_DATA_BUFFER.get();

		final int chunkX = chunk.getChunkX();
		final int chunkZ = chunk.getChunkZ();
		final int worldOffsetX = chunkX * SIZE;
		final int worldOffsetZ = chunkZ * SIZE;

		final int yLimit = Math.max(maxBlockY + 1, Chunk.SEA_LEVEL + 5);

		switch (face) {
			case TOP, BOTTOM -> meshHorizontalFace(face, blocks, chunk, world, atlas, worldOffsetX, worldOffsetZ,
					yLimit, mask, faceData, emitter, 1);
			case NORTH, SOUTH -> meshZFace(face, blocks, chunk, world, atlas, worldOffsetX, worldOffsetZ, yLimit, mask,
					faceData, emitter, 1);
			case EAST, WEST -> meshXFace(face, blocks, chunk, world, atlas, worldOffsetX, worldOffsetZ, yLimit, mask,
					faceData, emitter, 1);
		}
	}

	private static void meshHorizontalFace(
			BlockType.Face face,
			BlockType[][][] blocks,
			Chunk chunk,
			World world,
			TextureAtlas atlas,
			int worldOffsetX, int worldOffsetZ,
			int yLimit,
			int[] mask,
			FaceData[] faceData,
			QuadEmitter emitter,
			int step) {
		final int ny = face.getNy();
		final boolean isTop = face == BlockType.Face.TOP;

		for (int y = 0; y < yLimit; y++) {
			Arrays.fill(mask, 0, SIZE * SIZE, 0);
			for (int i = 0; i < SIZE * SIZE; i++)
				faceData[i].reset();

			for (int x = 0; x < SIZE; x++)
				for (int z = 0; z < SIZE; z++) {
					final BlockType block = blocks[x][y][z];
					if (block == BlockType.AIR || block.isPlant() || !block.isSolid() || block.isWater())
						continue;

					final int neighborY = y + ny;
					BlockType neighbor;
					if (neighborY >= 0 && neighborY < HEIGHT)
						neighbor = blocks[x][neighborY][z];
					else
						neighbor = BlockType.AIR;

					if (neighbor.isSolid() && !neighbor.isTransparent())
						continue;
					if (isLeafBlock(block) && isLeafBlock(neighbor) && !isTop)
						continue;
					if (block == BlockType.GLASS && neighbor == BlockType.GLASS)
						continue;

					final int idx = x * SIZE + z;
					mask[idx] = 1;

					final FaceData fd = faceData[idx];
					fd.valid = true;
					fd.blockType = block;

					final int[] tile = block.getTileForFace(face, 0);
					if (tile != null) {
						fd.tileX = tile[0];
						fd.tileY = tile[1];
					}

					final Biome biome = chunk.getBiomeAt(x, z);
					final float[] color = getColorForBlock(block, face, biome);
					fd.colorR = color[0];
					fd.colorG = color[1];
					fd.colorB = color[2];

					final float[] ao = calculateSimpleAO(x, y, z, face, blocks, world, worldOffsetX, worldOffsetZ);
					fd.ao0 = ao[0];
					fd.ao1 = ao[1];
					fd.ao2 = ao[2];
					fd.ao3 = ao[3];

					int lightY = y + ny;
					if (lightY < 0)
						lightY = 0;
					if (lightY >= HEIGHT)
						lightY = HEIGHT - 1;
					fd.skyLight = chunk.getSkyLight(x, lightY, z) / (float) Chunk.MAX_LIGHT;
					fd.blockLight = chunk.getBlockLight(x, lightY, z) / (float) Chunk.MAX_LIGHT;
				}

			for (int x = 0; x < SIZE; x++)
				for (int z = 0; z < SIZE;) {
					final int idx = x * SIZE + z;
					if (mask[idx] == 0) {
						z++;
						continue;
					}

					final FaceData startFd = faceData[idx];

					int width = 1;
					while (z + width < SIZE) {
						final int nextIdx = x * SIZE + z + width;
						if (mask[nextIdx] == 0 || !startFd.canMergeWith(faceData[nextIdx]))
							break;
						width++;
					}

					int height = 1;
					outer:
					while (x + height < SIZE) {
						for (int dz = 0; dz < width; dz++) {
							final int checkIdx = (x + height) * SIZE + z + dz;
							if (mask[checkIdx] == 0 || !startFd.canMergeWith(faceData[checkIdx]))
								break outer;
						}
						height++;
					}

					for (int dx = 0; dx < height; dx++)
						for (int dz = 0; dz < width; dz++)
							mask[(x + dx) * SIZE + z + dz] = 0;

					final float[] uv = atlas.getTileUV(startFd.tileX, startFd.tileY);
					final float u1 = uv[0], v1 = uv[1], u2 = uv[2], v2 = uv[3];

					final float wx = worldOffsetX + x;
					final float wz = worldOffsetZ + z;
					final float fy = isTop ? y + 1 : y;

					if (isTop)
						emitter.emitQuad(
								wx, fy, wz,
								wx + height, fy, wz,
								wx + height, fy, wz + width,
								wx, fy, wz + width,
								u1, v1, u2, v2,
								0, 1, 0,
								startFd.colorR, startFd.colorG, startFd.colorB,
								startFd.ao0, startFd.ao1, startFd.ao2, startFd.ao3,
								startFd.skyLight, startFd.blockLight,
								height, width);
					else
						emitter.emitQuad(
								wx, fy, wz + width,
								wx + height, fy, wz + width,
								wx + height, fy, wz,
								wx, fy, wz,
								u1, v1, u2, v2,
								0, -1, 0,
								startFd.colorR, startFd.colorG, startFd.colorB,
								startFd.ao0, startFd.ao1, startFd.ao2, startFd.ao3,
								startFd.skyLight, startFd.blockLight,
								height, width);

					z += width;
				}
		}
	}

	private static void meshZFace(
			BlockType.Face face,
			BlockType[][][] blocks,
			Chunk chunk,
			World world,
			TextureAtlas atlas,
			int worldOffsetX, int worldOffsetZ,
			int yLimit,
			int[] mask,
			FaceData[] faceData,
			QuadEmitter emitter,
			int step) {
		final int nz = face.getNz();
		final boolean isNorth = face == BlockType.Face.NORTH;

		for (int z = 0; z < SIZE; z++) {
			Arrays.fill(mask, 0, SIZE * yLimit, 0);
			for (int i = 0; i < SIZE * yLimit; i++)
				faceData[i].reset();

			for (int x = 0; x < SIZE; x++)
				for (int y = 0; y < yLimit; y++) {
					final BlockType block = blocks[x][y][z];
					if (block == BlockType.AIR || block.isPlant() || !block.isSolid() || block.isWater())
						continue;

					final int neighborZ = z + nz;
					BlockType neighbor;
					if (neighborZ >= 0 && neighborZ < SIZE)
						neighbor = blocks[x][y][neighborZ];
					else
						neighbor = world.getBlockAt(worldOffsetX + x, y, worldOffsetZ + neighborZ);

					if (neighbor.isSolid() && !neighbor.isTransparent())
						continue;
					if (isLeafBlock(block) && isLeafBlock(neighbor))
						continue;
					if (block == BlockType.GLASS && neighbor == BlockType.GLASS)
						continue;

					final int idx = x * yLimit + y;
					mask[idx] = 1;

					final FaceData fd = faceData[idx];
					fd.valid = true;
					fd.blockType = block;

					final int[] tile = block.getTileForFace(face, 0);
					if (tile != null) {
						fd.tileX = tile[0];
						fd.tileY = tile[1];
					}

					final Biome biome = chunk.getBiomeAt(x, z);
					final float[] color = getColorForBlock(block, face, biome);
					fd.colorR = color[0];
					fd.colorG = color[1];
					fd.colorB = color[2];

					final float[] ao = calculateSimpleAO(x, y, z, face, blocks, world, worldOffsetX, worldOffsetZ);
					fd.ao0 = ao[0];
					fd.ao1 = ao[1];
					fd.ao2 = ao[2];
					fd.ao3 = ao[3];

					final int lightZ = z + nz;
					if (lightZ < 0 || lightZ >= SIZE) {
						fd.skyLight = world.getSkyLightAt(worldOffsetX + x, y, worldOffsetZ + lightZ)
								/ (float) Chunk.MAX_LIGHT;
						fd.blockLight = world.getBlockLightAt(worldOffsetX + x, y, worldOffsetZ + lightZ)
								/ (float) Chunk.MAX_LIGHT;
					} else {
						fd.skyLight = chunk.getSkyLight(x, y, lightZ) / (float) Chunk.MAX_LIGHT;
						fd.blockLight = chunk.getBlockLight(x, y, lightZ) / (float) Chunk.MAX_LIGHT;
					}
				}

			for (int x = 0; x < SIZE; x++)
				for (int y = 0; y < yLimit;) {
					final int idx = x * yLimit + y;
					if (mask[idx] == 0) {
						y++;
						continue;
					}

					final FaceData startFd = faceData[idx];

					int height = 1;
					while (y + height < yLimit) {
						final int nextIdx = x * yLimit + y + height;
						if (mask[nextIdx] == 0 || !startFd.canMergeWith(faceData[nextIdx]))
							break;
						height++;
					}

					int width = 1;
					outer:
					while (x + width < SIZE) {
						for (int dy = 0; dy < height; dy++) {
							final int checkIdx = (x + width) * yLimit + y + dy;
							if (mask[checkIdx] == 0 || !startFd.canMergeWith(faceData[checkIdx]))
								break outer;
						}
						width++;
					}

					for (int dx = 0; dx < width; dx++)
						for (int dy = 0; dy < height; dy++)
							mask[(x + dx) * yLimit + y + dy] = 0;

					final float[] uv = atlas.getTileUV(startFd.tileX, startFd.tileY);
					final float u1 = uv[0], v1 = uv[1], u2 = uv[2], v2 = uv[3];

					final float wx = worldOffsetX + x;
					final float wz = worldOffsetZ + z;
					final float fz = isNorth ? wz : wz + 1;

					if (isNorth)
						emitter.emitQuad(
								wx, y, fz,
								wx, y + height, fz,
								wx + width, y + height, fz,
								wx + width, y, fz,
								u1, v1, u2, v2,
								0, 0, -1,
								startFd.colorR, startFd.colorG, startFd.colorB,
								startFd.ao0, startFd.ao1, startFd.ao2, startFd.ao3,
								startFd.skyLight, startFd.blockLight,
								width, height);
					else
						emitter.emitQuad(
								wx + width, y, fz,
								wx + width, y + height, fz,
								wx, y + height, fz,
								wx, y, fz,
								u1, v1, u2, v2,
								0, 0, 1,
								startFd.colorR, startFd.colorG, startFd.colorB,
								startFd.ao0, startFd.ao1, startFd.ao2, startFd.ao3,
								startFd.skyLight, startFd.blockLight,
								width, height);

					y += height;
				}
		}
	}

	private static void meshXFace(
			BlockType.Face face,
			BlockType[][][] blocks,
			Chunk chunk,
			World world,
			TextureAtlas atlas,
			int worldOffsetX, int worldOffsetZ,
			int yLimit,
			int[] mask,
			FaceData[] faceData,
			QuadEmitter emitter,
			int step) {
		final int nx = face.getNx();
		final boolean isEast = face == BlockType.Face.EAST;

		for (int x = 0; x < SIZE; x++) {
			Arrays.fill(mask, 0, SIZE * yLimit, 0);
			for (int i = 0; i < SIZE * yLimit; i++)
				faceData[i].reset();

			for (int z = 0; z < SIZE; z++)
				for (int y = 0; y < yLimit; y++) {
					final BlockType block = blocks[x][y][z];
					if (block == BlockType.AIR || block.isPlant() || !block.isSolid() || block.isWater())
						continue;

					final int neighborX = x + nx;
					BlockType neighbor;
					if (neighborX >= 0 && neighborX < SIZE)
						neighbor = blocks[neighborX][y][z];
					else
						neighbor = world.getBlockAt(worldOffsetX + neighborX, y, worldOffsetZ + z);

					if (neighbor.isSolid() && !neighbor.isTransparent())
						continue;
					if (isLeafBlock(block) && isLeafBlock(neighbor))
						continue;
					if (block == BlockType.GLASS && neighbor == BlockType.GLASS)
						continue;

					final int idx = z * yLimit + y;
					mask[idx] = 1;

					final FaceData fd = faceData[idx];
					fd.valid = true;
					fd.blockType = block;

					final int[] tile = block.getTileForFace(face, 0);
					if (tile != null) {
						fd.tileX = tile[0];
						fd.tileY = tile[1];
					}

					final Biome biome = chunk.getBiomeAt(x, z);
					final float[] color = getColorForBlock(block, face, biome);
					fd.colorR = color[0];
					fd.colorG = color[1];
					fd.colorB = color[2];

					final float[] ao = calculateSimpleAO(x, y, z, face, blocks, world, worldOffsetX, worldOffsetZ);
					fd.ao0 = ao[0];
					fd.ao1 = ao[1];
					fd.ao2 = ao[2];
					fd.ao3 = ao[3];

					final int lightX = x + nx;
					if (lightX < 0 || lightX >= SIZE) {
						fd.skyLight = world.getSkyLightAt(worldOffsetX + lightX, y, worldOffsetZ + z)
								/ (float) Chunk.MAX_LIGHT;
						fd.blockLight = world.getBlockLightAt(worldOffsetX + lightX, y, worldOffsetZ + z)
								/ (float) Chunk.MAX_LIGHT;
					} else {
						fd.skyLight = chunk.getSkyLight(lightX, y, z) / (float) Chunk.MAX_LIGHT;
						fd.blockLight = chunk.getBlockLight(lightX, y, z) / (float) Chunk.MAX_LIGHT;
					}
				}

			for (int z = 0; z < SIZE; z++)
				for (int y = 0; y < yLimit;) {
					final int idx = z * yLimit + y;
					if (mask[idx] == 0) {
						y++;
						continue;
					}

					final FaceData startFd = faceData[idx];

					int height = 1;
					while (y + height < yLimit) {
						final int nextIdx = z * yLimit + y + height;
						if (mask[nextIdx] == 0 || !startFd.canMergeWith(faceData[nextIdx]))
							break;
						height++;
					}

					int width = 1;
					outer:
					while (z + width < SIZE) {
						for (int dy = 0; dy < height; dy++) {
							final int checkIdx = (z + width) * yLimit + y + dy;
							if (mask[checkIdx] == 0 || !startFd.canMergeWith(faceData[checkIdx]))
								break outer;
						}
						width++;
					}

					for (int dz = 0; dz < width; dz++)
						for (int dy = 0; dy < height; dy++)
							mask[(z + dz) * yLimit + y + dy] = 0;

					final float[] uv = atlas.getTileUV(startFd.tileX, startFd.tileY);
					final float u1 = uv[0], v1 = uv[1], u2 = uv[2], v2 = uv[3];

					final float wx = worldOffsetX + x;
					final float wz = worldOffsetZ + z;
					final float fx = isEast ? wx + 1 : wx;

					if (isEast)
						emitter.emitQuad(
								fx, y, wz,
								fx, y + height, wz,
								fx, y + height, wz + width,
								fx, y, wz + width,
								u1, v1, u2, v2,
								1, 0, 0,
								startFd.colorR, startFd.colorG, startFd.colorB,
								startFd.ao0, startFd.ao1, startFd.ao2, startFd.ao3,
								startFd.skyLight, startFd.blockLight,
								width, height);
					else
						emitter.emitQuad(
								fx, y, wz + width,
								fx, y + height, wz + width,
								fx, y + height, wz,
								fx, y, wz,
								u1, v1, u2, v2,
								-1, 0, 0,
								startFd.colorR, startFd.colorG, startFd.colorB,
								startFd.ao0, startFd.ao1, startFd.ao2, startFd.ao3,
								startFd.skyLight, startFd.blockLight,
								width, height);

					y += height;
				}
		}
	}

	private static float[] getColorForBlock(BlockType block, BlockType.Face face, Biome biome) {
		if (block.isWater())
			return new float[] { 0.2f, 0.5f, 0.9f };
		if (block.needsFoliageTint())
			return BlockType.getFoliageColor(biome);
		if (block.needsTint(face))
			return BlockType.getGrassColor(biome);
		if (isGrassBlock(block) && isSideFace(face))
			return new float[] { 0.62f, 0.62f, 0.62f };
		if (isLogBlock(block))
			return new float[] { 0.98f, 0.98f, 1.0f };
		if (isSandBlock(block) && biome != null)
			return biome.getSandTint();
		if (isStoneBlock(block) && biome != null)
			return biome.getStoneTint();
		return new float[] { 1.0f, 1.0f, 1.0f };
	}

	private static float[] calculateSimpleAO(int x, int y, int z, BlockType.Face face, BlockType[][][] blocks,
			World world, int worldOffsetX, int worldOffsetZ) {
		final float[] ao = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };

		final int[][] corners = getCornerOffsets(face);
		for (int i = 0; i < 4; i++) {
			final int[] corner = corners[i];
			final int cx = x + corner[0];
			final int cy = y + corner[1];
			final int cz = z + corner[2];

			int occluderCount = 0;
			final int[][] occluders = getOccluderOffsets(face, i);
			for (final int[] off : occluders) {
				final int ox = cx + off[0];
				final int oy = cy + off[1];
				final int oz = cz + off[2];

				BlockType occluder;
				if (ox >= 0 && ox < SIZE && oz >= 0 && oz < SIZE && oy >= 0 && oy < HEIGHT)
					occluder = blocks[ox][oy][oz];
				else
					occluder = world.getBlockAt(worldOffsetX + ox, oy, worldOffsetZ + oz);

				if (occluder.blocksLight())
					occluderCount++;
			}

			ao[i] = 1.0f - occluderCount * 0.2f;
		}

		return ao;
	}

	private static int[][] getCornerOffsets(BlockType.Face face) {
		return switch (face) {
			case TOP -> new int[][] { { 0, 1, 0 }, { 1, 1, 0 }, { 1, 1, 1 }, { 0, 1, 1 } };
			case BOTTOM -> new int[][] { { 0, 0, 1 }, { 1, 0, 1 }, { 1, 0, 0 }, { 0, 0, 0 } };
			case NORTH -> new int[][] { { 0, 0, 0 }, { 0, 1, 0 }, { 1, 1, 0 }, { 1, 0, 0 } };
			case SOUTH -> new int[][] { { 1, 0, 1 }, { 1, 1, 1 }, { 0, 1, 1 }, { 0, 0, 1 } };
			case EAST -> new int[][] { { 1, 0, 0 }, { 1, 1, 0 }, { 1, 1, 1 }, { 1, 0, 1 } };
			case WEST -> new int[][] { { 0, 0, 1 }, { 0, 1, 1 }, { 0, 1, 0 }, { 0, 0, 0 } };
		};
	}

	private static int[][] getOccluderOffsets(BlockType.Face face, int cornerIndex) {
		final int ny = face.getNy();
		final int nx = face.getNx();
		final int nz = face.getNz();

		if (ny != 0) {
			final int yOff = ny > 0 ? 0 : -1;
			return switch (cornerIndex) {
				case 0 -> new int[][] { { -1, yOff, -1 }, { 0, yOff, -1 }, { -1, yOff, 0 } };
				case 1 -> new int[][] { { 0, yOff, -1 }, { 1, yOff, -1 }, { 1, yOff, 0 } };
				case 2 -> new int[][] { { 1, yOff, 0 }, { 1, yOff, 1 }, { 0, yOff, 1 } };
				case 3 -> new int[][] { { -1, yOff, 0 }, { -1, yOff, 1 }, { 0, yOff, 1 } };
				default -> new int[][] {};
			};
		} else if (nz != 0) {
			final int zOff = nz > 0 ? 0 : -1;
			return switch (cornerIndex) {
				case 0 -> new int[][] { { -1, -1, zOff }, { 0, -1, zOff }, { -1, 0, zOff } };
				case 1 -> new int[][] { { -1, 0, zOff }, { -1, 1, zOff }, { 0, 1, zOff } };
				case 2 -> new int[][] { { 0, 1, zOff }, { 1, 1, zOff }, { 1, 0, zOff } };
				case 3 -> new int[][] { { 1, 0, zOff }, { 1, -1, zOff }, { 0, -1, zOff } };
				default -> new int[][] {};
			};
		} else {
			final int xOff = nx > 0 ? 0 : -1;
			return switch (cornerIndex) {
				case 0 -> new int[][] { { xOff, -1, -1 }, { xOff, -1, 0 }, { xOff, 0, -1 } };
				case 1 -> new int[][] { { xOff, 0, -1 }, { xOff, 1, -1 }, { xOff, 1, 0 } };
				case 2 -> new int[][] { { xOff, 1, 0 }, { xOff, 1, 1 }, { xOff, 0, 1 } };
				case 3 -> new int[][] { { xOff, 0, 1 }, { xOff, -1, 1 }, { xOff, -1, 0 } };
				default -> new int[][] {};
			};
		}
	}

	private static boolean isLeafBlock(BlockType block) {
		return block == BlockType.OAK_LEAVES || block == BlockType.BIRCH_LEAVES || block == BlockType.SPRUCE_LEAVES;
	}

	private static boolean isLogBlock(BlockType block) {
		return block == BlockType.OAK_LOG || block == BlockType.BIRCH_LOG || block == BlockType.JUNGLE_LOG
				|| block == BlockType.SPRUCE_LOG;
	}

	private static boolean isSandBlock(BlockType block) {
		return block == BlockType.SAND || block == BlockType.SANDSTONE;
	}

	private static boolean isStoneBlock(BlockType block) {
		return block == BlockType.STONE || block == BlockType.COBBLESTONE || block == BlockType.MOSSY_COBBLESTONE
				|| block == BlockType.STONE_BRICKS || block == BlockType.GRAVEL;
	}

	private static boolean isGrassBlock(BlockType block) {
		return block == BlockType.GRASS || block == BlockType.SNOWY_GRASS;
	}

	private static boolean isSideFace(BlockType.Face face) {
		return face == BlockType.Face.NORTH || face == BlockType.Face.SOUTH || face == BlockType.Face.EAST
				|| face == BlockType.Face.WEST;
	}
}
