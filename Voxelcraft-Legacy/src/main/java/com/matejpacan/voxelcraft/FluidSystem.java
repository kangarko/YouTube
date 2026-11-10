package com.matejpacan.voxelcraft;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class FluidSystem {

	private final World world;
	private final Queue<FluidUpdate> updateQueue = new ArrayDeque<>();
	private final Set<Long> scheduledUpdates = new HashSet<>();

	private static final int MAX_UPDATES_PER_TICK = 32;
	public static final int WATER_MAX_LEVEL = 7;
	public static final int LAVA_MAX_LEVEL = 3;

	public FluidSystem(World world) {
		this.world = world;
	}

	public void scheduleUpdate(int x, int y, int z) {
		if (y < 0 || y >= Chunk.HEIGHT)
			return;

		final long key = this.posKey(x, y, z);
		if (!this.scheduledUpdates.contains(key)) {
			this.scheduledUpdates.add(key);
			this.updateQueue.add(new FluidUpdate(x, y, z));
		}
	}

	public void tick() {
		int updates = 0;

		while (!this.updateQueue.isEmpty() && updates < MAX_UPDATES_PER_TICK) {
			final FluidUpdate update = this.updateQueue.poll();
			final long key = this.posKey(update.x, update.y, update.z);
			this.scheduledUpdates.remove(key);

			this.processFluidAt(update.x, update.y, update.z);
			updates++;
		}
	}

	private void processFluidAt(int x, int y, int z) {
		final BlockType block = this.world.getBlockAt(x, y, z);
		if (block == null)
			return;

		if (block == BlockType.WATER)
			this.updateWater(x, y, z, 0);
		else if (block == BlockType.WATER_FLOWING) {
			final int level = this.world.getFluidLevel(x, y, z);
			this.updateWater(x, y, z, level);
		} else if (block == BlockType.LAVA)
			this.updateLava(x, y, z, 0);
		else if (block == BlockType.LAVA_FLOWING) {
			final int level = this.world.getFluidLevel(x, y, z);
			this.updateLava(x, y, z, level);
		}
	}

	private void updateWater(int x, int y, int z, int level) {
		final BlockType current = this.world.getBlockAt(x, y, z);
		final boolean isSource = current == BlockType.WATER;

		if (!isSource && !this.hasWaterSource(x, y, z, level)) {
			this.world.setBlockAt(x, y, z, BlockType.AIR);
			return;
		}

		final BlockType below = this.world.getBlockAt(x, y - 1, z);
		if (below == BlockType.AIR) {
			this.world.setBlockAt(x, y - 1, z, BlockType.WATER_FLOWING, 0, 1);
			this.scheduleUpdate(x, y - 1, z);
			return;
		}

		if (below == BlockType.LAVA || below == BlockType.LAVA_FLOWING) {
			this.world.setBlockAt(x, y - 1, z, BlockType.OBSIDIAN);
			return;
		}

		if (isSource || level < WATER_MAX_LEVEL) {
			final int nextLevel = isSource ? 1 : level + 1;
			if (nextLevel <= WATER_MAX_LEVEL)
				this.spreadHorizontally(x, y, z, nextLevel, true);
		}
	}

	private void updateLava(int x, int y, int z, int level) {
		final BlockType current = this.world.getBlockAt(x, y, z);
		final boolean isSource = current == BlockType.LAVA;

		if (!isSource && !this.hasLavaSource(x, y, z, level)) {
			this.world.setBlockAt(x, y, z, BlockType.AIR);
			return;
		}

		final BlockType below = this.world.getBlockAt(x, y - 1, z);
		if (below == BlockType.AIR) {
			this.world.setBlockAt(x, y - 1, z, BlockType.LAVA_FLOWING, 0, 1);
			this.scheduleUpdate(x, y - 1, z);
			return;
		}

		if (below == BlockType.WATER || below == BlockType.WATER_FLOWING) {
			this.world.setBlockAt(x, y - 1, z, BlockType.STONE);
			return;
		}

		if (isSource || level < LAVA_MAX_LEVEL) {
			final int nextLevel = isSource ? 1 : level + 1;
			if (nextLevel <= LAVA_MAX_LEVEL)
				this.spreadHorizontally(x, y, z, nextLevel, false);
		}
	}

	private void spreadHorizontally(int x, int y, int z, int nextLevel, boolean isWater) {
		final BlockType flowingType = isWater ? BlockType.WATER_FLOWING : BlockType.LAVA_FLOWING;

		final int[][] neighbors = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
		for (final int[] offset : neighbors) {
			final int nx = x + offset[0];
			final int nz = z + offset[1];

			final BlockType neighbor = this.world.getBlockAt(nx, y, nz);
			if (neighbor == null)
				continue;

			if (isWater && neighbor.isLava()) {
				this.world.setBlockAt(nx, y, nz, BlockType.COBBLESTONE);
				continue;
			}
			if (!isWater && neighbor.isWater()) {
				this.world.setBlockAt(nx, y, nz, BlockType.STONE);
				continue;
			}

			if (neighbor == BlockType.AIR) {
				this.world.setBlockAt(nx, y, nz, flowingType, 0, nextLevel);
				this.scheduleUpdate(nx, y, nz);
			} else if (neighbor == flowingType) {
				final int neighborLevel = this.world.getFluidLevel(nx, y, nz);
				if (neighborLevel > nextLevel) {
					this.world.setFluidLevel(nx, y, nz, nextLevel);
					this.scheduleUpdate(nx, y, nz);
				}
			}
		}
	}

	private boolean hasWaterSource(int x, int y, int z, int currentLevel) {
		final BlockType above = this.world.getBlockAt(x, y + 1, z);
		if (above == BlockType.WATER || above == BlockType.WATER_FLOWING)
			return true;

		final int[][] neighbors = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
		for (final int[] offset : neighbors) {
			final BlockType neighbor = this.world.getBlockAt(x + offset[0], y, z + offset[1]);
			if (neighbor == BlockType.WATER)
				return true;
			if (neighbor == BlockType.WATER_FLOWING) {
				final int neighborLevel = this.world.getFluidLevel(x + offset[0], y, z + offset[1]);
				if (neighborLevel < currentLevel)
					return true;
			}
		}
		return false;
	}

	private boolean hasLavaSource(int x, int y, int z, int currentLevel) {
		final BlockType above = this.world.getBlockAt(x, y + 1, z);
		if (above == BlockType.LAVA || above == BlockType.LAVA_FLOWING)
			return true;

		final int[][] neighbors = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
		for (final int[] offset : neighbors) {
			final BlockType neighbor = this.world.getBlockAt(x + offset[0], y, z + offset[1]);
			if (neighbor == BlockType.LAVA)
				return true;
			if (neighbor == BlockType.LAVA_FLOWING) {
				final int neighborLevel = this.world.getFluidLevel(x + offset[0], y, z + offset[1]);
				if (neighborLevel < currentLevel)
					return true;
			}
		}
		return false;
	}

	public void onBlockChanged(int x, int y, int z, BlockType oldBlock, BlockType newBlock) {
		if (oldBlock.isWater() || oldBlock.isLava()) {
			final int[][] neighbors = { { -1, 0, 0 }, { 1, 0, 0 }, { 0, -1, 0 }, { 0, 1, 0 }, { 0, 0, -1 }, { 0, 0, 1 } };
			for (final int[] offset : neighbors) {
				final BlockType neighbor = this.world.getBlockAt(x + offset[0], y + offset[1], z + offset[2]);
				if (neighbor != null && (neighbor.isWater() || neighbor.isLava()))
					this.scheduleUpdate(x + offset[0], y + offset[1], z + offset[2]);
			}
		}

		if (newBlock == BlockType.WATER || newBlock == BlockType.LAVA)
			this.scheduleUpdate(x, y, z);

		if (newBlock == BlockType.AIR) {
			final BlockType above = this.world.getBlockAt(x, y + 1, z);
			if (above != null && (above.isWater() || above.isLava()))
				this.scheduleUpdate(x, y + 1, z);

			final int[][] neighbors = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
			for (final int[] offset : neighbors) {
				final BlockType neighbor = this.world.getBlockAt(x + offset[0], y, z + offset[1]);
				if (neighbor != null && (neighbor.isWater() || neighbor.isLava()))
					this.scheduleUpdate(x + offset[0], y, z + offset[1]);
			}
		}
	}

	private long posKey(int x, int y, int z) {
		return (long) (x + 30000000) & 0x3FFFFFF | ((long) y & 0xFF) << 26
				| ((long) (z + 30000000) & 0x3FFFFFF) << 34;
	}

	private record FluidUpdate(int x, int y, int z) {
	}
}
