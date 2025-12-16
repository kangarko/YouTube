package com.matejpacan.voxelcraft;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ChunkMeshBuilder {
	private final ExecutorService executor;
	private final ConcurrentLinkedQueue<Chunk> uploadQueue;
	private final Set<Chunk> chunksInFlight;
	private final AtomicInteger pendingCount;
	private static final int MAX_PENDING = 64;

	public ChunkMeshBuilder() {
		final int threads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
		this.executor = Executors.newFixedThreadPool(threads);
		this.uploadQueue = new ConcurrentLinkedQueue<>();
		this.chunksInFlight = ConcurrentHashMap.newKeySet();
		this.pendingCount = new AtomicInteger(0);
	}

	public boolean submitForBuilding(Chunk chunk, World world) {
		if ((this.pendingCount.get() >= MAX_PENDING) || !this.chunksInFlight.add(chunk))
			return false;
		this.pendingCount.incrementAndGet();
		this.executor.submit(() -> {
			try {
				chunk.buildMeshDataThreaded(world);
				this.uploadQueue.add(chunk);
			} catch (final Exception e) {
				this.chunksInFlight.remove(chunk);
				this.pendingCount.decrementAndGet();
			}
		});
		return true;
	}

	public void processUploads(int maxPerFrame) {
		int uploaded = 0;
		while (uploaded < maxPerFrame) {
			final Chunk chunk = this.uploadQueue.poll();
			if (chunk == null)
				return;
			chunk.uploadMeshToGPU();
			this.chunksInFlight.remove(chunk);
			this.pendingCount.decrementAndGet();
			uploaded++;
		}
	}

	public void cancelChunk(Chunk chunk) {
		this.uploadQueue.remove(chunk);
		if (this.chunksInFlight.remove(chunk))
			this.pendingCount.decrementAndGet();
	}

	public boolean isChunkInFlight(Chunk chunk) {
		return this.chunksInFlight.contains(chunk);
	}

	public int getPendingCount() {
		return this.pendingCount.get();
	}

	public void shutdown() {
		this.executor.shutdownNow();
		this.uploadQueue.clear();
		this.chunksInFlight.clear();
		this.pendingCount.set(0);
	}
}
