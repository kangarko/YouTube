package com.matejpacan.voxelcraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PendingTreeBlocks {

	private final Map<Long, List<TreeGenerator.TreeBlock>> pendingBlocks = new ConcurrentHashMap<>();

	public void addPendingBlock(final int chunkX, final int chunkZ, final TreeGenerator.TreeBlock block) {
		final long key = this.chunkKey(chunkX, chunkZ);

		this.pendingBlocks.computeIfAbsent(key, _ -> Collections.synchronizedList(new ArrayList<>())).add(block);
	}

	public List<TreeGenerator.TreeBlock> getPendingBlocks(final int chunkX, final int chunkZ) {
		final long key = this.chunkKey(chunkX, chunkZ);
		return this.pendingBlocks.remove(key);
	}

	private long chunkKey(final int x, final int z) {
		return (long) x << 32 | z & 0xFFFFFFFFL;
	}
}
