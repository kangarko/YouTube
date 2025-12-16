package com.matejpacan.voxelcraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class World {

	private static final int PRIORITY_GENERATION_DISTANCE = 12;

	private final Map<Long, Chunk> chunks = new ConcurrentHashMap<>();
	private final Map<Long, Mesh> opaqueChunkMeshes = new ConcurrentHashMap<>();
	private final Map<Long, Mesh> transparentChunkMeshes = new ConcurrentHashMap<>();
	private final Set<Long> pendingChunkGeneration = ConcurrentHashMap.newKeySet();
	private final Set<Long> pendingMeshBuild = ConcurrentHashMap.newKeySet();
	private final ConcurrentLinkedQueue<Chunk> completedChunks = new ConcurrentLinkedQueue<>();
	private final ConcurrentLinkedQueue<ChunkMeshData> completedMeshData = new ConcurrentLinkedQueue<>();

	private final ExecutorService chunkGenerationExecutor;
	private final ExecutorService meshBuildExecutor;
	private final ChunkMeshBuilder meshBuilder = new ChunkMeshBuilder();
	private final NoiseGenerator noiseGenerator;
	private final PendingTreeBlocks pendingTreeBlocks = new PendingTreeBlocks();
	private final int renderDistance;
	private final long worldSeed;
	private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);

	private int lastCenterChunkX = Integer.MIN_VALUE;
	private int lastCenterChunkZ = Integer.MIN_VALUE;

	public World(final int renderDistance) {
		this(renderDistance, System.currentTimeMillis());
	}

	public World(final int renderDistance, final long seed) {
		this.renderDistance = renderDistance;
		this.worldSeed = seed;
		this.noiseGenerator = new NoiseGenerator(seed);

		final int processorCount = Runtime.getRuntime().availableProcessors();
		this.chunkGenerationExecutor = Executors.newFixedThreadPool(Math.max(2, processorCount - 2));
		this.meshBuildExecutor = Executors.newFixedThreadPool(Math.max(2, processorCount - 2));
	}

	public void update(final float playerX, final float playerZ) {
		final int centerChunkX = (int) Math.floor(playerX / Chunk.WIDTH);
		final int centerChunkZ = (int) Math.floor(playerZ / Chunk.DEPTH);

		this.processCompletedChunks();
		this.processCompletedMeshData();

		final boolean positionChanged = centerChunkX != this.lastCenterChunkX || centerChunkZ != this.lastCenterChunkZ;

		if (positionChanged) {
			this.lastCenterChunkX = centerChunkX;
			this.lastCenterChunkZ = centerChunkZ;
			this.queueChunksForGeneration(centerChunkX, centerChunkZ);
			this.unloadDistantChunks(centerChunkX, centerChunkZ);
		}

		this.queueDirtyChunksForMeshBuild(centerChunkX, centerChunkZ);
	}

	private void processCompletedChunks() {
		Chunk chunk;
		while ((chunk = this.completedChunks.poll()) != null) {
			final long key = this.chunkKey(chunk.getChunkX(), chunk.getChunkZ());
			this.chunks.put(key, chunk);
			this.pendingChunkGeneration.remove(key);
			this.generateTreesForChunk(chunk);
			this.applyPendingTreeBlocks(chunk);
			this.generateTreesFromNeighboringChunks(chunk.getChunkX(), chunk.getChunkZ());
			chunk.setDirty(true);
		}
	}

	private void processCompletedMeshData() {
		ChunkMeshData meshData;
		while ((meshData = this.completedMeshData.poll()) != null) {
			final long key = this.chunkKey(meshData.getChunkX(), meshData.getChunkZ());
			this.pendingMeshBuild.remove(key);

			final Mesh oldOpaqueMesh = this.opaqueChunkMeshes.get(key);
			if (oldOpaqueMesh != null)
				oldOpaqueMesh.cleanup();

			final Mesh oldTransparentMesh = this.transparentChunkMeshes.get(key);
			if (oldTransparentMesh != null)
				oldTransparentMesh.cleanup();

			if (!meshData.isOpaqueEmpty()) {
				final Mesh newOpaqueMesh = meshData.createOpaqueMesh();
				this.opaqueChunkMeshes.put(key, newOpaqueMesh);
			} else
				this.opaqueChunkMeshes.remove(key);

			if (!meshData.isTransparentEmpty()) {
				final Mesh newTransparentMesh = meshData.createTransparentMesh();
				this.transparentChunkMeshes.put(key, newTransparentMesh);
			} else
				this.transparentChunkMeshes.remove(key);

			final Chunk chunk = this.chunks.get(key);
			if (chunk != null)
				chunk.setDirty(false);
		}
	}

	private void queueChunksForGeneration(final int centerX, final int centerZ) {
		final List<int[]> chunksToLoad = new ArrayList<>();

		for (int dx = -this.renderDistance; dx <= this.renderDistance; dx++)
			for (int dz = -this.renderDistance; dz <= this.renderDistance; dz++) {
				final int distSq = dx * dx + dz * dz;
				if (distSq <= this.renderDistance * this.renderDistance) {
					final int cx = centerX + dx;
					final int cz = centerZ + dz;
					final long key = this.chunkKey(cx, cz);

					if (!this.chunks.containsKey(key) && !this.pendingChunkGeneration.contains(key))
						chunksToLoad.add(new int[] { cx, cz, distSq });
				}
			}

		chunksToLoad.sort((a, b) -> {
			final boolean aPriority = a[2] <= World.PRIORITY_GENERATION_DISTANCE * World.PRIORITY_GENERATION_DISTANCE;
			final boolean bPriority = b[2] <= World.PRIORITY_GENERATION_DISTANCE * World.PRIORITY_GENERATION_DISTANCE;
			if (aPriority != bPriority)
				return aPriority ? -1 : 1;
			return Integer.compare(a[2], b[2]);
		});

		for (final int[] coord : chunksToLoad) {
			final int cx = coord[0];
			final int cz = coord[1];
			final long key = this.chunkKey(cx, cz);

			if (this.pendingChunkGeneration.add(key))
				this.chunkGenerationExecutor.submit(() -> this.generateChunkAsync(cx, cz));
		}
	}

	private void generateChunkAsync(final int cx, final int cz) {
		if (this.isShuttingDown.get())
			return;

		final Chunk chunk = new Chunk(cx, cz);
		chunk.generateTerrain(this.noiseGenerator);
		this.completedChunks.offer(chunk);
	}

	private void queueDirtyChunksForMeshBuild(final int centerX, final int centerZ) {
		final List<Chunk> dirtyChunks = new ArrayList<>();

		for (final Map.Entry<Long, Chunk> entry : this.chunks.entrySet()) {
			final Chunk chunk = entry.getValue();
			if (chunk.isDirty() && !this.pendingMeshBuild.contains(entry.getKey()))
				dirtyChunks.add(chunk);
		}

		dirtyChunks.sort((a, b) -> {
			final int dxA = a.getChunkX() - centerX;
			final int dzA = a.getChunkZ() - centerZ;
			final int dxB = b.getChunkX() - centerX;
			final int dzB = b.getChunkZ() - centerZ;
			return Integer.compare(dxA * dxA + dzA * dzA, dxB * dxB + dzB * dzB);
		});

		for (final Chunk chunk : dirtyChunks) {
			final long key = this.chunkKey(chunk.getChunkX(), chunk.getChunkZ());
			if (this.pendingMeshBuild.add(key))
				this.meshBuildExecutor.submit(() -> this.buildMeshAsync(chunk));
		}
	}

	private void buildMeshAsync(final Chunk chunk) {
		if (this.isShuttingDown.get())
			return;

		final ChunkMeshData meshData = this.meshBuilder.buildMeshData(chunk, this);
		this.completedMeshData.offer(meshData);
	}

	private void unloadDistantChunks(final int centerX, final int centerZ) {
		final List<Long> toRemove = this.collectDistantChunkKeys(centerX, centerZ);

		for (final Long key : toRemove) {
			this.chunks.remove(key);
			this.pendingChunkGeneration.remove(key);
			this.pendingMeshBuild.remove(key);

			final Mesh opaqueMesh = this.opaqueChunkMeshes.remove(key);
			if (opaqueMesh != null)
				opaqueMesh.cleanup();

			final Mesh transparentMesh = this.transparentChunkMeshes.remove(key);
			if (transparentMesh != null)
				transparentMesh.cleanup();
		}
	}

	private List<Long> collectDistantChunkKeys(final int centerX, final int centerZ) {
		final int unloadDistance = this.renderDistance + 2;
		final List<Long> toRemove = new ArrayList<>();

		for (final Map.Entry<Long, Chunk> entry : this.chunks.entrySet()) {
			final Chunk chunk = entry.getValue();
			final int dx = chunk.getChunkX() - centerX;
			final int dz = chunk.getChunkZ() - centerZ;
			final int distSq = dx * dx + dz * dz;

			if (distSq > unloadDistance * unloadDistance)
				toRemove.add(entry.getKey());
		}

		return toRemove;
	}

	public BlockType getBlock(final int x, final int y, final int z) {
		if (y < 0 || y >= Chunk.HEIGHT)
			return BlockType.AIR;

		final int chunkX = Math.floorDiv(x, Chunk.WIDTH);
		final int chunkZ = Math.floorDiv(z, Chunk.DEPTH);

		final Chunk chunk = this.chunks.get(this.chunkKey(chunkX, chunkZ));
		if (chunk == null)
			return BlockType.AIR;

		final int localX = Math.floorMod(x, Chunk.WIDTH);
		final int localZ = Math.floorMod(z, Chunk.DEPTH);

		return chunk.getBlock(localX, y, localZ);
	}

	public Map<Long, Mesh> getOpaqueChunkMeshes() {
		return this.opaqueChunkMeshes;
	}

	public Map<Long, Mesh> getTransparentChunkMeshes() {
		return this.transparentChunkMeshes;
	}

	public Map<Long, Chunk> getChunks() {
		return this.chunks;
	}

	public long chunkKey(final int x, final int z) {
		return (long) x << 32 | z & 0xFFFFFFFFL;
	}

	public int getSurfaceHeight(final int x, final int z) {
		final int chunkX = Math.floorDiv(x, Chunk.WIDTH);
		final int chunkZ = Math.floorDiv(z, Chunk.DEPTH);
		this.ensureChunkGenerated(chunkX, chunkZ);

		for (int y = Chunk.HEIGHT - 1; y >= 0; y--) {
			final BlockType block = this.getBlock(x, y, z);
			if (block != BlockType.AIR && block != BlockType.WATER && block != BlockType.OAK_LEAVES)
				return y + 1;
		}
		return 64;
	}

	public void ensureChunkGenerated(final int chunkX, final int chunkZ) {
		final long key = this.chunkKey(chunkX, chunkZ);
		if (this.chunks.containsKey(key))
			return;

		final Chunk chunk = new Chunk(chunkX, chunkZ);
		chunk.generateTerrain(this.noiseGenerator);
		this.chunks.put(key, chunk);
		this.generateTreesForChunk(chunk);
		this.applyPendingTreeBlocks(chunk);
		chunk.setDirty(true);
	}

	private void generateTreesForChunk(final Chunk chunk) {
		final List<int[]> treePositions = TreeGenerator.getTreePositionsForChunk(
				chunk.getChunkX(), chunk.getChunkZ(), this.worldSeed, this.noiseGenerator);

		for (final int[] pos : treePositions) {
			final int baseX = pos[0];
			final int baseY = pos[1];
			final int baseZ = pos[2];

			if (!this.canPlaceTree(baseX, baseY, baseZ))
				continue;

			final List<TreeGenerator.TreeBlock> treeBlocks = TreeGenerator.generateTree(baseX, baseY, baseZ,
					this.worldSeed);

			for (final TreeGenerator.TreeBlock block : treeBlocks) {
				final int blockChunkX = Math.floorDiv(block.worldX(), Chunk.WIDTH);
				final int blockChunkZ = Math.floorDiv(block.worldZ(), Chunk.DEPTH);

				if (blockChunkX == chunk.getChunkX() && blockChunkZ == chunk.getChunkZ())
					this.placeTreeBlock(chunk, block);
				else {
					final Chunk targetChunk = this.chunks.get(this.chunkKey(blockChunkX, blockChunkZ));
					if (targetChunk != null)
						this.placeTreeBlock(targetChunk, block);
					else
						this.pendingTreeBlocks.addPendingBlock(blockChunkX, blockChunkZ, block);
				}
			}
		}
	}

	private void generateTreesFromNeighboringChunks(final int chunkX, final int chunkZ) {
		final int treeReach = TreeGenerator.MAX_TREE_RADIUS / Chunk.WIDTH + 1;

		for (int dx = -treeReach; dx <= treeReach; dx++)
			for (int dz = -treeReach; dz <= treeReach; dz++) {
				if (dx == 0 && dz == 0)
					continue;

				final int neighborX = chunkX + dx;
				final int neighborZ = chunkZ + dz;
				final Chunk neighborChunk = this.chunks.get(this.chunkKey(neighborX, neighborZ));

				if (neighborChunk == null) {
					final List<int[]> treePositions = TreeGenerator.getTreePositionsForChunk(
							neighborX, neighborZ, this.worldSeed, this.noiseGenerator);

					for (final int[] pos : treePositions) {
						final List<TreeGenerator.TreeBlock> treeBlocks = TreeGenerator.generateTree(pos[0], pos[1],
								pos[2], this.worldSeed);

						for (final TreeGenerator.TreeBlock block : treeBlocks) {
							final int blockChunkX = Math.floorDiv(block.worldX(), Chunk.WIDTH);
							final int blockChunkZ = Math.floorDiv(block.worldZ(), Chunk.DEPTH);

							if (blockChunkX == chunkX && blockChunkZ == chunkZ) {
								final Chunk targetChunk = this.chunks.get(this.chunkKey(chunkX, chunkZ));
								if (targetChunk != null)
									this.placeTreeBlock(targetChunk, block);
							}
						}
					}
				}
			}
	}

	private void applyPendingTreeBlocks(final Chunk chunk) {
		final List<TreeGenerator.TreeBlock> pending = this.pendingTreeBlocks.getPendingBlocks(chunk.getChunkX(),
				chunk.getChunkZ());
		if (pending == null)
			return;

		for (final TreeGenerator.TreeBlock block : pending)
			this.placeTreeBlock(chunk, block);
	}

	private boolean canPlaceTree(final int x, final int y, final int z) {
		final BlockType ground = this.getBlock(x, y - 1, z);
		if (ground != BlockType.GRASS && ground != BlockType.DIRT)
			return false;

		for (int dy = 0; dy < TreeGenerator.getMaxTreeHeight(); dy++) {
			final BlockType above = this.getBlock(x, y + dy, z);
			if (above != BlockType.AIR && above != BlockType.OAK_LEAVES)
				return false;
		}

		return true;
	}

	private void placeTreeBlock(final Chunk chunk, final TreeGenerator.TreeBlock block) {
		final int localX = Math.floorMod(block.worldX(), Chunk.WIDTH);
		final int localZ = Math.floorMod(block.worldZ(), Chunk.DEPTH);

		if (block.worldY() < 0 || block.worldY() >= Chunk.HEIGHT)
			return;

		final BlockType existing = chunk.getBlock(localX, block.worldY(), localZ);
		if (block.blockType() == BlockType.OAK_LEAVES) {
			if (existing != BlockType.AIR)
				return;
		} else if (block.blockType() == BlockType.OAK_LOG)
			if (existing != BlockType.AIR && existing != BlockType.OAK_LEAVES)
				return;

		chunk.setBlock(localX, block.worldY(), localZ, block.blockType());
	}

	public void cleanup() {
		this.isShuttingDown.set(true);
		this.chunkGenerationExecutor.shutdownNow();
		this.meshBuildExecutor.shutdownNow();

		for (final Mesh mesh : this.opaqueChunkMeshes.values())
			mesh.cleanup();
		this.opaqueChunkMeshes.clear();

		for (final Mesh mesh : this.transparentChunkMeshes.values())
			mesh.cleanup();
		this.transparentChunkMeshes.clear();

		this.chunks.clear();
	}
}
