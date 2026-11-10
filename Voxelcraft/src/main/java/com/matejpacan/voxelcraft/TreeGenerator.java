package com.matejpacan.voxelcraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TreeGenerator {

	private static final int MIN_TREE_HEIGHT = 5;
	private static final int MAX_TREE_HEIGHT = 7;
	private static final double TREE_DENSITY = 0.008;

	public enum TreeType {
		SMALL_OAK,
		MEDIUM_OAK,
		TALL_OAK
	}

	public record TreeBlock(int worldX, int worldY, int worldZ, BlockType blockType) {
	}

	public static List<int[]> getTreePositionsForChunk(final int chunkX, final int chunkZ, final long worldSeed,
			final NoiseGenerator noise) {
		final List<int[]> positions = new ArrayList<>();
		final Random random = new Random(worldSeed ^ chunkX * 341873128712L + chunkZ * 132897987541L);

		final int worldX = chunkX * Chunk.WIDTH;
		final int worldZ = chunkZ * Chunk.DEPTH;

		for (int x = 0; x < Chunk.WIDTH; x++)
			for (int z = 0; z < Chunk.DEPTH; z++)
				if (random.nextDouble() < TreeGenerator.TREE_DENSITY) {
					final int globalX = worldX + x;
					final int globalZ = worldZ + z;
					final int surfaceY = TreeGenerator.calculateSurfaceHeight(noise, globalX, globalZ);

					if (surfaceY > 64 && surfaceY < 100)
						positions.add(new int[] { globalX, surfaceY + 1, globalZ });
				}

		return positions;
	}

	private static int calculateSurfaceHeight(final NoiseGenerator noise, final int x, final int z) {
		final double scale = 0.01;
		final double detailScale = 0.05;

		double heightNoise = noise.octaveNoise2D(x * scale, z * scale, 4, 0.5, 2.0);
		double detailNoise = noise.octaveNoise2D(x * detailScale, z * detailScale, 2, 0.5, 2.0);

		heightNoise = (heightNoise + 1.0) / 2.0;
		detailNoise = (detailNoise + 1.0) / 2.0;

		final double combinedNoise = heightNoise * 0.8 + detailNoise * 0.2;
		return (int) (62 + combinedNoise * 32);
	}

	public static List<TreeBlock> generateTree(final int baseX, final int baseY, final int baseZ,
			final long worldSeed) {
		final Random random = new Random(
				worldSeed ^ baseX * 73856093L ^ baseY * 19349663L ^ baseZ * 83492791L);
		final TreeType type = TreeType.values()[random.nextInt(TreeType.values().length)];

		return switch (type) {
			case SMALL_OAK -> TreeGenerator.generateSmallOak(baseX, baseY, baseZ, random);
			case MEDIUM_OAK -> TreeGenerator.generateMediumOak(baseX, baseY, baseZ, random);
			case TALL_OAK -> TreeGenerator.generateTallOak(baseX, baseY, baseZ, random);
		};
	}

	private static List<TreeBlock> generateSmallOak(final int baseX, final int baseY, final int baseZ,
			final Random random) {
		final List<TreeBlock> blocks = new ArrayList<>();
		final int trunkHeight = TreeGenerator.MIN_TREE_HEIGHT + random.nextInt(2);

		for (int y = 0; y < trunkHeight; y++)
			blocks.add(new TreeBlock(baseX, baseY + y, baseZ, BlockType.OAK_LOG));

		final int leafStartY = baseY + trunkHeight - 2;
		for (int y = 0; y <= 2; y++) {
			final int radius = y == 0 ? 2 : y == 2 ? 1 : 2;
			for (int dx = -radius; dx <= radius; dx++)
				for (int dz = -radius; dz <= radius; dz++) {
					if (dx == 0 && dz == 0 && y < 2)
						continue;
					if (Math.abs(dx) == radius && Math.abs(dz) == radius && random.nextBoolean())
						continue;
					blocks.add(new TreeBlock(baseX + dx, leafStartY + y, baseZ + dz, BlockType.OAK_LEAVES));
				}
		}

		blocks.add(new TreeBlock(baseX, leafStartY + 3, baseZ, BlockType.OAK_LEAVES));

		return blocks;
	}

	private static List<TreeBlock> generateMediumOak(final int baseX, final int baseY, final int baseZ,
			final Random random) {
		final List<TreeBlock> blocks = new ArrayList<>();
		final int trunkHeight = TreeGenerator.MIN_TREE_HEIGHT + 1 + random.nextInt(2);

		for (int y = 0; y < trunkHeight; y++)
			blocks.add(new TreeBlock(baseX, baseY + y, baseZ, BlockType.OAK_LOG));

		final int leafStartY = baseY + trunkHeight - 3;
		for (int y = 0; y <= 3; y++) {
			int radius;
			if (y == 0)
				radius = 1;
			else if (y == 3)
				radius = 1;
			else
				radius = 2;

			for (int dx = -radius; dx <= radius; dx++)
				for (int dz = -radius; dz <= radius; dz++) {
					if (dx == 0 && dz == 0 && y < 3)
						continue;
					if (Math.abs(dx) == radius && Math.abs(dz) == radius)
						if (y != 1 && y != 2 || random.nextInt(3) == 0)
							continue;
					blocks.add(new TreeBlock(baseX + dx, leafStartY + y, baseZ + dz, BlockType.OAK_LEAVES));
				}
		}

		blocks.add(new TreeBlock(baseX, leafStartY + 4, baseZ, BlockType.OAK_LEAVES));

		return blocks;
	}

	private static List<TreeBlock> generateTallOak(final int baseX, final int baseY, final int baseZ,
			final Random random) {
		final List<TreeBlock> blocks = new ArrayList<>();
		final int trunkHeight = TreeGenerator.MAX_TREE_HEIGHT + random.nextInt(2);

		for (int y = 0; y < trunkHeight; y++)
			blocks.add(new TreeBlock(baseX, baseY + y, baseZ, BlockType.OAK_LOG));

		final int branchY = baseY + trunkHeight - 4;
		final int[] branchDirs = { random.nextInt(4) };
		for (final int dir : branchDirs) {
			final int dx = dir == 0 ? 1 : dir == 1 ? -1 : 0;
			final int dz = dir == 2 ? 1 : dir == 3 ? -1 : 0;
			blocks.add(new TreeBlock(baseX + dx, branchY, baseZ + dz, BlockType.OAK_LOG));
			blocks.add(new TreeBlock(baseX + dx * 2, branchY + 1, baseZ + dz * 2, BlockType.OAK_LOG));

			for (int lx = -1; lx <= 1; lx++)
				for (int lz = -1; lz <= 1; lz++)
					blocks.add(
							new TreeBlock(baseX + dx * 2 + lx, branchY + 2, baseZ + dz * 2 + lz, BlockType.OAK_LEAVES));
		}

		final int leafStartY = baseY + trunkHeight - 3;
		for (int y = 0; y <= 3; y++) {
			int radius;
			if (y == 0 || y == 3)
				radius = 1;
			else
				radius = 2;

			for (int dx = -radius; dx <= radius; dx++)
				for (int dz = -radius; dz <= radius; dz++) {
					if (dx == 0 && dz == 0 && y < 3)
						continue;
					if (Math.abs(dx) == radius && Math.abs(dz) == radius && random.nextInt(3) == 0)
						continue;
					blocks.add(new TreeBlock(baseX + dx, leafStartY + y, baseZ + dz, BlockType.OAK_LEAVES));
				}
		}

		blocks.add(new TreeBlock(baseX, leafStartY + 4, baseZ, BlockType.OAK_LEAVES));

		return blocks;
	}

	public static final int MAX_TREE_RADIUS = 3;

	public static int getMaxTreeHeight() {
		return TreeGenerator.MAX_TREE_HEIGHT + 5;
	}
}
