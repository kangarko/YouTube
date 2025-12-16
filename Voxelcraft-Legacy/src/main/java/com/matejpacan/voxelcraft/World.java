package com.matejpacan.voxelcraft;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class World {

	public static final int RENDER_DISTANCE = 24;
	private static final int UNLOAD_DISTANCE = RENDER_DISTANCE + 2;
	private static final int MAX_TERRAIN_READY_PER_FRAME = 16;
	private static final int MAX_MESH_BUILDS_PER_FRAME = 16;
	private static final int MAX_CHUNKS_TO_SCAN_PER_FRAME = 400;

	private static final int LOD_0_DISTANCE = 6;
	private static final int LOD_1_DISTANCE = 12;
	private static final int LOD_2_DISTANCE = 18;

	private final Map<Long, Chunk> chunks = new ConcurrentHashMap<>();
	private final Set<Long> chunksBeingGenerated = ConcurrentHashMap.newKeySet();
	private final Queue<Chunk> terrainReadyQueue = new ConcurrentLinkedQueue<>();
	private final Queue<Chunk> meshBuildQueue = new ArrayDeque<>();
	private final TextureAtlas atlas;
	private final FluidSystem fluidSystem;
	private final Map<Long, Byte> fluidLevels = new ConcurrentHashMap<>();
	private final ChunkMeshBuilder meshBuilder = new ChunkMeshBuilder();
	private final ExecutorService terrainExecutor;

	private int lastCameraCX = Integer.MIN_VALUE;
	private int lastCameraCZ = Integer.MIN_VALUE;
	private int fluidTickCounter = 0;
	private int loadScanRadius = 0;
	private int fadeUpdateIndex = 0;

	public World(TextureAtlas atlas) {
		this.atlas = atlas;
		this.fluidSystem = new FluidSystem(this);
		final int terrainThreads = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
		this.terrainExecutor = Executors.newFixedThreadPool(terrainThreads);
	}

	public void loadChunkSync(int cx, int cz) {
		final long key = this.chunkKey(cx, cz);
		if (this.chunks.containsKey(key))
			return;
		final Chunk chunk = new Chunk(cx, cz, this.atlas);
		chunk.generateTerrain();
		chunk.calculateSkyLight();
		chunk.calculateBlockLight();
		chunk.setLightDirty(false);
		chunk.setMeshDirty(true);
		this.chunks.put(key, chunk);
		this.queueMeshBuild(chunk);
	}

	public void update(float cameraX, float cameraZ, float deltaTime) {
		final int cameraCX = Math.floorDiv((int) cameraX, Chunk.SIZE);
		final int cameraCZ = Math.floorDiv((int) cameraZ, Chunk.SIZE);

		final boolean cameraMovedChunk = cameraCX != this.lastCameraCX || cameraCZ != this.lastCameraCZ;

		if (cameraMovedChunk) {
			this.lastCameraCX = cameraCX;
			this.lastCameraCZ = cameraCZ;
			this.loadScanRadius = 0;
			this.unloadDistantChunks(cameraCX, cameraCZ);
			this.updateChunkLODLevels(cameraCX, cameraCZ);
		}

		this.scanAndQueueChunks(cameraCX, cameraCZ);
		this.processTerrainReadyQueue();
		this.processMeshBuildQueue();
		this.meshBuilder.processUploads(20);

		this.fluidTickCounter++;
		if (this.fluidTickCounter >= 8) {
			this.fluidTickCounter = 0;
			this.fluidSystem.tick();
		}

		this.updateFadesBatched(deltaTime);
	}

	private void updateChunkLODLevels(int cameraCX, int cameraCZ) {
		for (final Chunk chunk : this.chunks.values()) {
			final int dx = chunk.getChunkX() - cameraCX;
			final int dz = chunk.getChunkZ() - cameraCZ;
			final double dist = Math.sqrt(dx * dx + dz * dz);

			int newLod;
			if (dist <= LOD_0_DISTANCE)
				newLod = 0;
			else if (dist <= LOD_1_DISTANCE)
				newLod = 1;
			else if (dist <= LOD_2_DISTANCE)
				newLod = 2;
			else
				newLod = 3;

			if (newLod != chunk.getLodLevel()) {
				chunk.setLodLevel(newLod);
				chunk.setMeshDirty(true);
				this.queueMeshBuild(chunk);
			}
		}
	}

	public void update(float cameraX, float cameraZ) {
		this.update(cameraX, cameraZ, 0.016f);
	}

	private void scanAndQueueChunks(int cameraCX, int cameraCZ) {
		if ((this.loadScanRadius > RENDER_DISTANCE) || (this.chunksBeingGenerated.size() > 100))
			return;

		int chunksScanned = 0;
		int r = this.loadScanRadius;

		outer:
		while (r <= RENDER_DISTANCE && chunksScanned < MAX_CHUNKS_TO_SCAN_PER_FRAME) {
			for (int dx = -r; dx <= r; dx++)
				for (int dz = -r; dz <= r; dz++) {
					if (chunksScanned >= MAX_CHUNKS_TO_SCAN_PER_FRAME)
						break outer;

					if (Math.abs(dx) != r && Math.abs(dz) != r)
						continue;

					final double distSq = dx * dx + dz * dz;
					if (distSq > RENDER_DISTANCE * RENDER_DISTANCE)
						continue;

					final int cx = cameraCX + dx;
					final int cz = cameraCZ + dz;
					final long key = this.chunkKey(cx, cz);

					if (!this.chunks.containsKey(key) && this.chunksBeingGenerated.add(key)) {
						final int fcx = cx, fcz = cz;
						this.terrainExecutor.submit(() -> {
							final Chunk chunk = new Chunk(fcx, fcz, this.atlas);
							chunk.generateTerrain();
							chunk.calculateSkyLight();
							chunk.calculateBlockLight();
							chunk.setLightDirty(false);
							chunk.setMeshDirty(true);
							this.terrainReadyQueue.add(chunk);
						});
					}
					chunksScanned++;
				}
			r++;
		}
		this.loadScanRadius = r;
	}

	private void processTerrainReadyQueue() {
		int processed = 0;
		while (processed < MAX_TERRAIN_READY_PER_FRAME) {
			final Chunk chunk = this.terrainReadyQueue.poll();
			if (chunk == null)
				break;

			final long key = this.chunkKey(chunk.getChunkX(), chunk.getChunkZ());
			this.chunksBeingGenerated.remove(key);

			final int dx = chunk.getChunkX() - this.lastCameraCX;
			final int dz = chunk.getChunkZ() - this.lastCameraCZ;
			if (dx * dx + dz * dz > UNLOAD_DISTANCE * UNLOAD_DISTANCE)
				continue;

			this.chunks.put(key, chunk);
			this.queueMeshBuild(chunk);
			this.queueNeighborMeshRebuilds(chunk.getChunkX(), chunk.getChunkZ());
			processed++;
		}
	}

	private void updateFadesBatched(float deltaTime) {
		if (this.chunks.isEmpty())
			return;

		final List<Chunk> chunkList = new ArrayList<>(this.chunks.values());
		final int batchSize = Math.max(50, chunkList.size() / 10);
		final int end = Math.min(this.fadeUpdateIndex + batchSize, chunkList.size());

		for (int i = this.fadeUpdateIndex; i < end; i++)
			chunkList.get(i).updateFade(deltaTime);

		this.fadeUpdateIndex = end;
		if (this.fadeUpdateIndex >= chunkList.size())
			this.fadeUpdateIndex = 0;
	}

	private final Set<Chunk> meshBuildQueueSet = ConcurrentHashMap.newKeySet();

	private void queueMeshBuild(Chunk chunk) {
		if (this.meshBuilder.isChunkInFlight(chunk))
			return;
		if (this.meshBuildQueueSet.add(chunk))
			this.meshBuildQueue.add(chunk);
	}

	private void queueNeighborMeshRebuilds(int cx, int cz) {
		final int[][] neighbors = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
		for (final int[] offset : neighbors) {
			final Chunk neighbor = this.getChunk(cx + offset[0], cz + offset[1]);
			if (neighbor != null) {
				neighbor.setMeshDirty(true);
				this.queueMeshBuild(neighbor);
			}
		}
	}

	private boolean hasAllNeighbors(Chunk chunk) {
		final int cx = chunk.getChunkX();
		final int cz = chunk.getChunkZ();
		return this.getChunk(cx - 1, cz) != null && this.getChunk(cx + 1, cz) != null &&
				this.getChunk(cx, cz - 1) != null && this.getChunk(cx, cz + 1) != null;
	}

	private void processMeshBuildQueue() {
		int meshesBuilt = 0;
		final List<Chunk> deferred = new ArrayList<>();

		while (!this.meshBuildQueue.isEmpty() && meshesBuilt < MAX_MESH_BUILDS_PER_FRAME) {
			final Chunk chunk = this.meshBuildQueue.poll();
			if (chunk == null)
				continue;

			this.meshBuildQueueSet.remove(chunk);

			final long key = this.chunkKey(chunk.getChunkX(), chunk.getChunkZ());
			if (!this.chunks.containsKey(key) || this.meshBuilder.isChunkInFlight(chunk))
				continue;

			if (!this.hasAllNeighbors(chunk)) {
				deferred.add(chunk);
				continue;
			}

			if (!this.meshBuilder.submitForBuilding(chunk, this)) {
				deferred.add(chunk);
				break;
			}
			meshesBuilt++;
		}

		for (final Chunk chunk : deferred)
			this.queueMeshBuild(chunk);
	}

	private void unloadDistantChunks(int cameraCX, int cameraCZ) {
		final List<Long> toRemove = new ArrayList<>();

		for (final Map.Entry<Long, Chunk> entry : this.chunks.entrySet()) {
			final Chunk chunk = entry.getValue();
			final int dx = chunk.getChunkX() - cameraCX;
			final int dz = chunk.getChunkZ() - cameraCZ;
			final double distSq = dx * dx + dz * dz;

			if (distSq > UNLOAD_DISTANCE * UNLOAD_DISTANCE)
				toRemove.add(entry.getKey());
		}

		for (final Long key : toRemove) {
			this.chunksBeingGenerated.remove(key);
			final Chunk chunk = this.chunks.remove(key);
			if (chunk != null) {
				this.meshBuilder.cancelChunk(chunk);
				chunk.cleanup();
			}
		}
	}

	/**
	 * Get a unique key for chunk coordinates.
	 */
	private long chunkKey(int cx, int cz) {
		return (long) cx << 32 | cz & 0xFFFFFFFFL;
	}

	private boolean supportsMetadata(BlockType type) {
		return type == BlockType.OAK_LOG || type == BlockType.SPRUCE_LOG
				|| type == BlockType.BIRCH_LOG || type == BlockType.JUNGLE_LOG
				|| type == BlockType.FURNACE || type == BlockType.FURNACE_LIT
				|| type == BlockType.DISPENSER;
	}

	private boolean isAxisMetadata(BlockType type) {
		return type == BlockType.OAK_LOG || type == BlockType.SPRUCE_LOG
				|| type == BlockType.BIRCH_LOG || type == BlockType.JUNGLE_LOG;
	}

	private boolean isFacingMetadata(BlockType type) {
		return type == BlockType.FURNACE || type == BlockType.FURNACE_LIT || type == BlockType.DISPENSER;
	}

	private void clearUnsupportedPlantsAbove(int x, int y, int z) {
		final int ny = y + 1;
		if (ny >= Chunk.HEIGHT)
			return;
		final BlockType above = this.getBlockAt(x, ny, z);
		if (!above.isPlant())
			return;
		this.setBlockAt(x, ny, z, BlockType.AIR, 0, 0);
	}

	/**
	 * Get chunk at chunk coordinates.
	 */
	public Chunk getChunk(int cx, int cz) {
		return this.chunks.get(this.chunkKey(cx, cz));
	}

	/**
	 * Check if a chunk at the given world coordinates is loaded.
	 */
	public boolean isChunkLoadedAt(int worldX, int worldZ) {
		final int cx = Math.floorDiv(worldX, Chunk.SIZE);
		final int cz = Math.floorDiv(worldZ, Chunk.SIZE);
		return this.chunks.containsKey(this.chunkKey(cx, cz));
	}

	/**
	 * Get block at world coordinates.
	 */
	public BlockType getBlockAt(int x, int y, int z) {
		if (y < 0 || y >= Chunk.HEIGHT)
			return BlockType.AIR;

		final int cx = Math.floorDiv(x, Chunk.SIZE);
		final int cz = Math.floorDiv(z, Chunk.SIZE);

		final Chunk chunk = this.getChunk(cx, cz);
		if (chunk == null)
			return BlockType.AIR;

		final int lx = Math.floorMod(x, Chunk.SIZE);
		final int lz = Math.floorMod(z, Chunk.SIZE);

		return chunk.getBlock(lx, y, lz);
	}

	/**
	 * Get sky light at world coordinates.
	 */
	public int getSkyLightAt(int x, int y, int z) {
		if (y < 0)
			return 0;
		if (y >= Chunk.HEIGHT)
			return Chunk.MAX_LIGHT;

		final int cx = Math.floorDiv(x, Chunk.SIZE);
		final int cz = Math.floorDiv(z, Chunk.SIZE);

		final Chunk chunk = this.getChunk(cx, cz);
		if (chunk == null)
			return Chunk.MAX_LIGHT;

		final int lx = Math.floorMod(x, Chunk.SIZE);
		final int lz = Math.floorMod(z, Chunk.SIZE);

		return chunk.getSkyLight(lx, y, lz);
	}

	/**
	 * Get block light at world coordinates.
	 */
	public int getBlockLightAt(int x, int y, int z) {
		if (y < 0 || y >= Chunk.HEIGHT)
			return 0;

		final int cx = Math.floorDiv(x, Chunk.SIZE);
		final int cz = Math.floorDiv(z, Chunk.SIZE);

		final Chunk chunk = this.getChunk(cx, cz);
		if (chunk == null)
			return 0;

		final int lx = Math.floorMod(x, Chunk.SIZE);
		final int lz = Math.floorMod(z, Chunk.SIZE);

		return chunk.getBlockLight(lx, y, lz);
	}

	/**
	 * Set block at world coordinates.
	 */
	public void setBlockAt(int x, int y, int z, BlockType type) {
		this.setBlockAt(x, y, z, type, 0, 0);
	}

	/**
	 * Set block at world coordinates with metadata.
	 */
	public void setBlockAt(int x, int y, int z, BlockType type, int metadata) {
		this.setBlockAt(x, y, z, type, metadata, 0);
	}

	/**
	 * Set block at world coordinates with metadata and fluid level.
	 */
	public void setBlockAt(int x, int y, int z, BlockType type, int metadata, int fluidLevel) {
		if (y < 0 || y >= Chunk.HEIGHT)
			return;
		if (metadata < 0)
			throw new IllegalArgumentException("Negative metadata at " + x + "," + y + "," + z);
		if (metadata > Byte.MAX_VALUE)
			throw new IllegalArgumentException("Metadata too large at " + x + "," + y + "," + z);
		if (metadata != 0 && !this.supportsMetadata(type))
			throw new IllegalArgumentException("Metadata not supported for " + type);
		if (this.isAxisMetadata(type) && metadata > 2)
			throw new IllegalArgumentException("Invalid axis metadata " + metadata + " for " + type);
		if (this.isFacingMetadata(type) && metadata > 3)
			throw new IllegalArgumentException("Invalid facing metadata " + metadata + " for " + type);

		final int cx = Math.floorDiv(x, Chunk.SIZE);
		final int cz = Math.floorDiv(z, Chunk.SIZE);

		final Chunk chunk = this.getChunk(cx, cz);
		if (chunk == null)
			return;

		final int lx = Math.floorMod(x, Chunk.SIZE);
		final int lz = Math.floorMod(z, Chunk.SIZE);

		final BlockType previous = chunk.getBlock(lx, y, lz);
		chunk.setBlock(lx, y, lz, type, metadata);

		final long posKey = this.fluidPosKey(x, y, z);
		if (type == BlockType.WATER_FLOWING)
			this.fluidLevels.put(posKey, (byte) fluidLevel);
		else
			this.fluidLevels.remove(posKey);

		final boolean supportRemoved = type == BlockType.AIR || !type.isSolid();

		chunk.buildMesh(this);

		this.fluidSystem.onBlockChanged(x, y, z, previous, type);

		if (supportRemoved)
			this.clearUnsupportedPlantsAbove(x, y, z);

		final boolean affectsNeighbors = lx == 0 || lx == Chunk.SIZE - 1 || lz == 0 || lz == Chunk.SIZE - 1
				|| previous.getLightEmission() > 0 || type.getLightEmission() > 0
				|| previous.blocksLight() != type.blocksLight();

		if (affectsNeighbors) {
			this.rebuildChunkMeshWithLighting(cx - 1, cz);
			this.rebuildChunkMeshWithLighting(cx + 1, cz);
			this.rebuildChunkMeshWithLighting(cx, cz - 1);
			this.rebuildChunkMeshWithLighting(cx, cz + 1);
		}
	}

	public int getFluidLevel(int x, int y, int z) {
		final BlockType block = this.getBlockAt(x, y, z);
		if (block == BlockType.WATER)
			return 0;
		if (block != BlockType.WATER_FLOWING)
			return -1;
		final Byte level = this.fluidLevels.get(this.fluidPosKey(x, y, z));
		return level != null ? level : FluidSystem.WATER_MAX_LEVEL;
	}

	public void setFluidLevel(int x, int y, int z, int level) {
		this.fluidLevels.put(this.fluidPosKey(x, y, z), (byte) level);

		final int cx = Math.floorDiv(x, Chunk.SIZE);
		final int cz = Math.floorDiv(z, Chunk.SIZE);
		final Chunk chunk = this.getChunk(cx, cz);
		if (chunk != null) {
			chunk.setMeshDirty(true);
			chunk.buildMesh(this);
		}
	}

	private long fluidPosKey(int x, int y, int z) {
		return (long) (x + 30000000) & 0x3FFFFFF | ((long) y & 0xFF) << 26
				| ((long) (z + 30000000) & 0x3FFFFFF) << 34;
	}

	public FluidSystem getFluidSystem() {
		return this.fluidSystem;
	}

	private void rebuildChunkMeshWithLighting(int cx, int cz) {
		final Chunk chunk = this.getChunk(cx, cz);
		if (chunk != null) {
			chunk.setLightDirty(true);
			chunk.setMeshDirty(true);
			chunk.buildMesh(this);
		}
	}

	public void render() {
		for (final Chunk chunk : this.chunks.values())
			chunk.renderOpaque();
	}

	public Iterable<Chunk> getChunks() {
		return this.chunks.values();
	}

	public int getLoadedChunkCount() {
		return this.chunks.size();
	}

	public int getPendingChunkCount() {
		return this.chunksBeingGenerated.size() + this.meshBuildQueue.size() + this.terrainReadyQueue.size();
	}

	/**
	 * Check if a bounding box collides with any solid block in the world.
	 *
	 * @param minX minimum X of bounding box
	 * @param minY minimum Y of bounding box
	 * @param minZ minimum Z of bounding box
	 * @param maxX maximum X of bounding box
	 * @param maxY maximum Y of bounding box
	 * @param maxZ maximum Z of bounding box
	 * @return true if collision detected
	 */
	public boolean checkCollision(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
		final int startX = (int) Math.floor(minX);
		final int startY = (int) Math.floor(minY);
		final int startZ = (int) Math.floor(minZ);
		final int endX = (int) Math.floor(maxX);
		final int endY = (int) Math.floor(maxY);
		final int endZ = (int) Math.floor(maxZ);

		for (int x = startX; x <= endX; x++)
			for (int y = startY; y <= endY; y++)
				for (int z = startZ; z <= endZ; z++) {
					final BlockType block = this.getBlockAt(x, y, z);
					if (block.isSolid() && block != BlockType.WATER && !block.isPlant())
						return true;
				}
		return false;
	}

	/**
	 * Check if a point is inside a solid block.
	 */
	public boolean isInsideSolidBlock(float x, float y, float z) {
		final BlockType block = this.getBlockAt((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
		return block.isSolid() && block != BlockType.WATER && !block.isPlant();
	}

	/**
	 * Find the highest solid block at the given X/Z coordinates.
	 * Returns the Y coordinate of the top of the block, or -1 if no solid block
	 * found.
	 */
	public int getTopBlockY(int x, int z) {
		for (int y = Chunk.HEIGHT - 1; y >= 0; y--) {
			final BlockType block = this.getBlockAt(x, y, z);
			if (block != null && block.isSolid() && !block.isWater() && !block.isPlant())
				return y;
		}
		return -1;
	}

	/**
	 * Cleanup all chunks.
	 */
	public void cleanup() {
		this.terrainExecutor.shutdownNow();
		this.meshBuilder.shutdown();
		for (final Chunk chunk : this.chunks.values())
			chunk.cleanup();
		this.chunks.clear();
		this.meshBuildQueue.clear();
		this.meshBuildQueueSet.clear();
		this.chunksBeingGenerated.clear();
		this.terrainReadyQueue.clear();
	}
}
