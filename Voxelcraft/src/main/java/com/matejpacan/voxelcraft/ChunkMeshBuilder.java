package com.matejpacan.voxelcraft;

public class ChunkMeshBuilder {

	private static final int INITIAL_CAPACITY = 65536;

	private float[] opaqueVertices;
	private float[] opaqueTexCoords;
	private float[] opaqueNormals;
	private float[] opaqueColors;
	private int[] opaqueIndices;
	private int opaqueVertexCount;
	private int opaqueTexCoordCount;
	private int opaqueNormalCount;
	private int opaqueColorCount;
	private int opaqueIndexCount;
	private int opaqueVertexIndex;

	private float[] transparentVertices;
	private float[] transparentTexCoords;
	private float[] transparentNormals;
	private float[] transparentColors;
	private int[] transparentIndices;
	private int transparentVertexCount;
	private int transparentTexCoordCount;
	private int transparentNormalCount;
	private int transparentColorCount;
	private int transparentIndexCount;
	private int transparentVertexIndex;

	public ChunkMeshBuilder() {
		this.opaqueVertices = new float[ChunkMeshBuilder.INITIAL_CAPACITY * 3];
		this.opaqueTexCoords = new float[ChunkMeshBuilder.INITIAL_CAPACITY * 2];
		this.opaqueNormals = new float[ChunkMeshBuilder.INITIAL_CAPACITY * 3];
		this.opaqueColors = new float[ChunkMeshBuilder.INITIAL_CAPACITY * 4];
		this.opaqueIndices = new int[ChunkMeshBuilder.INITIAL_CAPACITY * 6];

		this.transparentVertices = new float[ChunkMeshBuilder.INITIAL_CAPACITY * 3];
		this.transparentTexCoords = new float[ChunkMeshBuilder.INITIAL_CAPACITY * 2];
		this.transparentNormals = new float[ChunkMeshBuilder.INITIAL_CAPACITY * 3];
		this.transparentColors = new float[ChunkMeshBuilder.INITIAL_CAPACITY * 4];
		this.transparentIndices = new int[ChunkMeshBuilder.INITIAL_CAPACITY * 6];
	}

	public synchronized ChunkMeshData buildMeshData(final Chunk chunk, final World world) {
		this.opaqueVertexCount = 0;
		this.opaqueTexCoordCount = 0;
		this.opaqueNormalCount = 0;
		this.opaqueColorCount = 0;
		this.opaqueIndexCount = 0;
		this.opaqueVertexIndex = 0;

		this.transparentVertexCount = 0;
		this.transparentTexCoordCount = 0;
		this.transparentNormalCount = 0;
		this.transparentColorCount = 0;
		this.transparentIndexCount = 0;
		this.transparentVertexIndex = 0;

		final int worldX = chunk.getWorldX();
		final int worldZ = chunk.getWorldZ();

		for (int x = 0; x < Chunk.WIDTH; x++)
			for (int y = 0; y < Chunk.HEIGHT; y++)
				for (int z = 0; z < Chunk.DEPTH; z++) {
					final BlockType block = chunk.getBlock(x, y, z);
					if (block == BlockType.AIR)
						continue;

					final float wx = worldX + x;
					final float wz = worldZ + z;
					final boolean isTransparent = block.isTransparent();

					final int topResult = this.shouldRenderFace(world, worldX + x, y, worldZ + z, 0, 1, 0, block);
					if (topResult > 0)
						this.addTopFace(wx, y, wz, block, topResult == 2, isTransparent);

					final int bottomResult = this.shouldRenderFace(world, worldX + x, y, worldZ + z, 0, -1, 0, block);
					if (bottomResult > 0)
						this.addBottomFace(wx, y, wz, block, bottomResult == 2, isTransparent);

					final int frontResult = this.shouldRenderFace(world, worldX + x, y, worldZ + z, 0, 0, 1, block);
					if (frontResult > 0)
						this.addFrontFace(wx, y, wz, block, frontResult == 2, isTransparent);

					final int backResult = this.shouldRenderFace(world, worldX + x, y, worldZ + z, 0, 0, -1, block);
					if (backResult > 0)
						this.addBackFace(wx, y, wz, block, backResult == 2, isTransparent);

					final int rightResult = this.shouldRenderFace(world, worldX + x, y, worldZ + z, 1, 0, 0, block);
					if (rightResult > 0)
						this.addRightFace(wx, y, wz, block, rightResult == 2, isTransparent);

					final int leftResult = this.shouldRenderFace(world, worldX + x, y, worldZ + z, -1, 0, 0, block);
					if (leftResult > 0)
						this.addLeftFace(wx, y, wz, block, leftResult == 2, isTransparent);
				}

		float[] finalOpaqueVertices = null;
		float[] finalOpaqueTexCoords = null;
		float[] finalOpaqueNormals = null;
		float[] finalOpaqueColors = null;
		int[] finalOpaqueIndices = null;

		if (this.opaqueVertexCount > 0) {
			finalOpaqueVertices = new float[this.opaqueVertexCount];
			finalOpaqueTexCoords = new float[this.opaqueTexCoordCount];
			finalOpaqueNormals = new float[this.opaqueNormalCount];
			finalOpaqueColors = new float[this.opaqueColorCount];
			finalOpaqueIndices = new int[this.opaqueIndexCount];

			System.arraycopy(this.opaqueVertices, 0, finalOpaqueVertices, 0, this.opaqueVertexCount);
			System.arraycopy(this.opaqueTexCoords, 0, finalOpaqueTexCoords, 0, this.opaqueTexCoordCount);
			System.arraycopy(this.opaqueNormals, 0, finalOpaqueNormals, 0, this.opaqueNormalCount);
			System.arraycopy(this.opaqueColors, 0, finalOpaqueColors, 0, this.opaqueColorCount);
			System.arraycopy(this.opaqueIndices, 0, finalOpaqueIndices, 0, this.opaqueIndexCount);
		}

		float[] finalTransparentVertices = null;
		float[] finalTransparentTexCoords = null;
		float[] finalTransparentNormals = null;
		float[] finalTransparentColors = null;
		int[] finalTransparentIndices = null;

		if (this.transparentVertexCount > 0) {
			finalTransparentVertices = new float[this.transparentVertexCount];
			finalTransparentTexCoords = new float[this.transparentTexCoordCount];
			finalTransparentNormals = new float[this.transparentNormalCount];
			finalTransparentColors = new float[this.transparentColorCount];
			finalTransparentIndices = new int[this.transparentIndexCount];

			System.arraycopy(this.transparentVertices, 0, finalTransparentVertices, 0, this.transparentVertexCount);
			System.arraycopy(this.transparentTexCoords, 0, finalTransparentTexCoords, 0, this.transparentTexCoordCount);
			System.arraycopy(this.transparentNormals, 0, finalTransparentNormals, 0, this.transparentNormalCount);
			System.arraycopy(this.transparentColors, 0, finalTransparentColors, 0, this.transparentColorCount);
			System.arraycopy(this.transparentIndices, 0, finalTransparentIndices, 0, this.transparentIndexCount);
		}

		return new ChunkMeshData(chunk.getChunkX(), chunk.getChunkZ(),
				finalOpaqueVertices, finalOpaqueTexCoords, finalOpaqueNormals, finalOpaqueColors, finalOpaqueIndices,
				finalTransparentVertices, finalTransparentTexCoords, finalTransparentNormals, finalTransparentColors,
				finalTransparentIndices);
	}

	private int shouldRenderFace(final World world, final int x, final int y, final int z, final int dx, final int dy,
			final int dz, final BlockType currentBlock) {
		final BlockType neighbor = world.getBlock(x + dx, y + dy, z + dz);

		if (currentBlock == BlockType.WATER) {
			if (neighbor == BlockType.WATER)
				return 0;
			if (neighbor == BlockType.AIR)
				return 1;
			return 0;
		}

		if (neighbor == BlockType.AIR)
			return 1;

		if (currentBlock.isLeaves() && neighbor.isLeaves())
			return 2;

		if (currentBlock.isTransparent() && neighbor != currentBlock
				|| !currentBlock.isTransparent() && neighbor.isTransparent())
			return 1;

		return 0;
	}

	private void ensureOpaqueCapacity() {
		final int additionalVertices = 4;
		final int requiredVertexCapacity = this.opaqueVertexCount + additionalVertices * 3;
		if (requiredVertexCapacity > this.opaqueVertices.length) {
			final int newCapacity = Math.max(requiredVertexCapacity, this.opaqueVertices.length * 2);
			this.opaqueVertices = this.grow(this.opaqueVertices, newCapacity);
			this.opaqueNormals = this.grow(this.opaqueNormals, newCapacity);
		}

		final int requiredColorCapacity = this.opaqueColorCount + additionalVertices * 4;
		if (requiredColorCapacity > this.opaqueColors.length)
			this.opaqueColors = this.grow(this.opaqueColors,
					Math.max(requiredColorCapacity, this.opaqueColors.length * 2));

		final int requiredTexCoordCapacity = this.opaqueTexCoordCount + additionalVertices * 2;
		if (requiredTexCoordCapacity > this.opaqueTexCoords.length)
			this.opaqueTexCoords = this.grow(this.opaqueTexCoords,
					Math.max(requiredTexCoordCapacity, this.opaqueTexCoords.length * 2));

		final int requiredIndexCapacity = this.opaqueIndexCount + additionalVertices / 4 * 6;
		if (requiredIndexCapacity > this.opaqueIndices.length)
			this.opaqueIndices = this.grow(this.opaqueIndices,
					Math.max(requiredIndexCapacity, this.opaqueIndices.length * 2));
	}

	private void ensureTransparentCapacity() {
		final int additionalVertices = 4;
		final int requiredVertexCapacity = this.transparentVertexCount + additionalVertices * 3;
		if (requiredVertexCapacity > this.transparentVertices.length) {
			final int newCapacity = Math.max(requiredVertexCapacity, this.transparentVertices.length * 2);
			this.transparentVertices = this.grow(this.transparentVertices, newCapacity);
			this.transparentNormals = this.grow(this.transparentNormals, newCapacity);
		}

		final int requiredColorCapacity = this.transparentColorCount + additionalVertices * 4;
		if (requiredColorCapacity > this.transparentColors.length)
			this.transparentColors = this.grow(this.transparentColors,
					Math.max(requiredColorCapacity, this.transparentColors.length * 2));

		final int requiredTexCoordCapacity = this.transparentTexCoordCount + additionalVertices * 2;
		if (requiredTexCoordCapacity > this.transparentTexCoords.length)
			this.transparentTexCoords = this.grow(this.transparentTexCoords,
					Math.max(requiredTexCoordCapacity, this.transparentTexCoords.length * 2));

		final int requiredIndexCapacity = this.transparentIndexCount + additionalVertices / 4 * 6;
		if (requiredIndexCapacity > this.transparentIndices.length)
			this.transparentIndices = this.grow(this.transparentIndices,
					Math.max(requiredIndexCapacity, this.transparentIndices.length * 2));
	}

	private float[] grow(final float[] array, final int newCapacity) {
		final float[] newArray = new float[newCapacity];
		System.arraycopy(array, 0, newArray, 0, array.length);
		return newArray;
	}

	private int[] grow(final int[] array, final int newCapacity) {
		final int[] newArray = new int[newCapacity];
		System.arraycopy(array, 0, newArray, 0, array.length);
		return newArray;
	}

	private void addTopFace(final float x, final float y, final float z, final BlockType block,
			final boolean isInternalLeaf, final boolean transparent) {
		if (transparent)
			this.ensureTransparentCapacity();
		else
			this.ensureOpaqueCapacity();

		final float[] uv = block.getTopUV();
		final float[] tint = block.getTopTint();
		final float leafData = this.getLeafData(block, isInternalLeaf);

		this.addVertex(x, y + 1, z + 1, transparent);
		this.addVertex(x + 1, y + 1, z + 1, transparent);
		this.addVertex(x + 1, y + 1, z, transparent);
		this.addVertex(x, y + 1, z, transparent);

		this.addTexCoord(uv[0], uv[1], transparent);
		this.addTexCoord(uv[6], uv[7], transparent);
		this.addTexCoord(uv[4], uv[5], transparent);
		this.addTexCoord(uv[2], uv[3], transparent);

		this.addNormal(0, 1, 0, transparent);
		this.addNormal(0, 1, 0, transparent);
		this.addNormal(0, 1, 0, transparent);
		this.addNormal(0, 1, 0, transparent);

		this.addColor(tint, leafData, transparent);
		this.addColor(tint, leafData, transparent);
		this.addColor(tint, leafData, transparent);
		this.addColor(tint, leafData, transparent);

		this.addQuadIndices(transparent);
	}

	private void addBottomFace(final float x, final float y, final float z, final BlockType block,
			final boolean isInternalLeaf,
			final boolean transparent) {
		if (transparent)
			this.ensureTransparentCapacity();
		else
			this.ensureOpaqueCapacity();

		final float[] uv = block.getBottomUV();
		final float[] tint = block.getBottomTint();
		final float leafData = this.getLeafData(block, isInternalLeaf);

		this.addVertex(x, y, z, transparent);
		this.addVertex(x + 1, y, z, transparent);
		this.addVertex(x + 1, y, z + 1, transparent);
		this.addVertex(x, y, z + 1, transparent);

		this.addTexCoord(uv[0], uv[1], transparent);
		this.addTexCoord(uv[6], uv[7], transparent);
		this.addTexCoord(uv[4], uv[5], transparent);
		this.addTexCoord(uv[2], uv[3], transparent);

		this.addNormal(0, -1, 0, transparent);
		this.addNormal(0, -1, 0, transparent);
		this.addNormal(0, -1, 0, transparent);
		this.addNormal(0, -1, 0, transparent);

		this.addColor(tint, leafData, transparent);
		this.addColor(tint, leafData, transparent);
		this.addColor(tint, leafData, transparent);
		this.addColor(tint, leafData, transparent);

		this.addQuadIndices(transparent);
	}

	private void addFrontFace(final float x, final float y, final float z, final BlockType block,
			final boolean isInternalLeaf, final boolean transparent) {
		if (transparent)
			this.ensureTransparentCapacity();
		else
			this.ensureOpaqueCapacity();

		final float[] uv = block.getSideUV();
		final float[] tint = block.getSideTint();
		final float leafData = this.getLeafData(block, isInternalLeaf);

		this.addVertex(x, y, z + 1, transparent);
		this.addVertex(x + 1, y, z + 1, transparent);
		this.addVertex(x + 1, y + 1, z + 1, transparent);
		this.addVertex(x, y + 1, z + 1, transparent);

		this.addTexCoord(uv[0], uv[3], transparent);
		this.addTexCoord(uv[6], uv[5], transparent);
		this.addTexCoord(uv[4], uv[7], transparent);
		this.addTexCoord(uv[2], uv[1], transparent);

		this.addNormal(0, 0, 1, transparent);
		this.addNormal(0, 0, 1, transparent);
		this.addNormal(0, 0, 1, transparent);
		this.addNormal(0, 0, 1, transparent);

		this.addColor(tint, leafData, transparent);
		this.addColor(tint, leafData, transparent);
		this.addColor(tint, leafData, transparent);
		this.addColor(tint, leafData, transparent);

		this.addQuadIndices(transparent);
	}

	private void addBackFace(final float x, final float y, final float z, final BlockType block,
			final boolean isInternalLeaf, final boolean transparent) {
		if (transparent)
			this.ensureTransparentCapacity();
		else
			this.ensureOpaqueCapacity();

		final float[] uv = block.getSideUV();
		final float[] tint = block.getSideTint();
		final float leafData = this.getLeafData(block, isInternalLeaf);

		this.addVertex(x + 1, y, z, transparent);
		this.addVertex(x, y, z, transparent);
		this.addVertex(x, y + 1, z, transparent);
		this.addVertex(x + 1, y + 1, z, transparent);

		this.addTexCoord(uv[0], uv[3], transparent);
		this.addTexCoord(uv[6], uv[5], transparent);
		this.addTexCoord(uv[4], uv[7], transparent);
		this.addTexCoord(uv[2], uv[1], transparent);

		this.addNormal(0, 0, -1, transparent);
		this.addNormal(0, 0, -1, transparent);
		this.addNormal(0, 0, -1, transparent);
		this.addNormal(0, 0, -1, transparent);

		this.addColor(tint, leafData, transparent);
		this.addColor(tint, leafData, transparent);
		this.addColor(tint, leafData, transparent);
		this.addColor(tint, leafData, transparent);

		this.addQuadIndices(transparent);
	}

	private void addRightFace(final float x, final float y, final float z, final BlockType block,
			final boolean isInternalLeaf, final boolean transparent) {
		if (transparent)
			this.ensureTransparentCapacity();
		else
			this.ensureOpaqueCapacity();

		final float[] uv = block.getSideUV();
		final float[] tint = block.getSideTint();
		final float leafData = this.getLeafData(block, isInternalLeaf);

		this.addVertex(x + 1, y, z + 1, transparent);
		this.addVertex(x + 1, y, z, transparent);
		this.addVertex(x + 1, y + 1, z, transparent);
		this.addVertex(x + 1, y + 1, z + 1, transparent);

		this.addTexCoord(uv[0], uv[3], transparent);
		this.addTexCoord(uv[6], uv[5], transparent);
		this.addTexCoord(uv[4], uv[7], transparent);
		this.addTexCoord(uv[2], uv[1], transparent);

		this.addNormal(1, 0, 0, transparent);
		this.addNormal(1, 0, 0, transparent);
		this.addNormal(1, 0, 0, transparent);
		this.addNormal(1, 0, 0, transparent);

		this.addColor(tint, leafData, transparent);
		this.addColor(tint, leafData, transparent);
		this.addColor(tint, leafData, transparent);
		this.addColor(tint, leafData, transparent);

		this.addQuadIndices(transparent);
	}

	private void addLeftFace(final float x, final float y, final float z, final BlockType block,
			final boolean isInternalLeaf, final boolean transparent) {
		if (transparent)
			this.ensureTransparentCapacity();
		else
			this.ensureOpaqueCapacity();

		final float[] uv = block.getSideUV();
		final float[] tint = block.getSideTint();
		final float leafData = this.getLeafData(block, isInternalLeaf);

		this.addVertex(x, y, z, transparent);
		this.addVertex(x, y, z + 1, transparent);
		this.addVertex(x, y + 1, z + 1, transparent);
		this.addVertex(x, y + 1, z, transparent);

		this.addTexCoord(uv[0], uv[3], transparent);
		this.addTexCoord(uv[6], uv[5], transparent);
		this.addTexCoord(uv[4], uv[7], transparent);
		this.addTexCoord(uv[2], uv[1], transparent);

		this.addNormal(-1, 0, 0, transparent);
		this.addNormal(-1, 0, 0, transparent);
		this.addNormal(-1, 0, 0, transparent);
		this.addNormal(-1, 0, 0, transparent);

		this.addColor(tint, leafData, transparent);
		this.addColor(tint, leafData, transparent);
		this.addColor(tint, leafData, transparent);
		this.addColor(tint, leafData, transparent);

		this.addQuadIndices(transparent);
	}

	private void addVertex(final float x, final float y, final float z, final boolean transparent) {
		if (transparent) {
			this.transparentVertices[this.transparentVertexCount++] = x;
			this.transparentVertices[this.transparentVertexCount++] = y;
			this.transparentVertices[this.transparentVertexCount++] = z;
		} else {
			this.opaqueVertices[this.opaqueVertexCount++] = x;
			this.opaqueVertices[this.opaqueVertexCount++] = y;
			this.opaqueVertices[this.opaqueVertexCount++] = z;
		}
	}

	private void addTexCoord(final float u, final float v, final boolean transparent) {
		if (transparent) {
			this.transparentTexCoords[this.transparentTexCoordCount++] = u;
			this.transparentTexCoords[this.transparentTexCoordCount++] = v;
		} else {
			this.opaqueTexCoords[this.opaqueTexCoordCount++] = u;
			this.opaqueTexCoords[this.opaqueTexCoordCount++] = v;
		}
	}

	private void addNormal(final float x, final float y, final float z, final boolean transparent) {
		if (transparent) {
			this.transparentNormals[this.transparentNormalCount++] = x;
			this.transparentNormals[this.transparentNormalCount++] = y;
			this.transparentNormals[this.transparentNormalCount++] = z;
		} else {
			this.opaqueNormals[this.opaqueNormalCount++] = x;
			this.opaqueNormals[this.opaqueNormalCount++] = y;
			this.opaqueNormals[this.opaqueNormalCount++] = z;
		}
	}

	private void addColor(final float[] tint, final float leafData, final boolean transparent) {
		if (transparent) {
			this.transparentColors[this.transparentColorCount++] = tint[0];
			this.transparentColors[this.transparentColorCount++] = tint[1];
			this.transparentColors[this.transparentColorCount++] = tint[2];
			this.transparentColors[this.transparentColorCount++] = leafData;
		} else {
			this.opaqueColors[this.opaqueColorCount++] = tint[0];
			this.opaqueColors[this.opaqueColorCount++] = tint[1];
			this.opaqueColors[this.opaqueColorCount++] = tint[2];
			this.opaqueColors[this.opaqueColorCount++] = leafData;
		}
	}

	private float getLeafData(final BlockType block, final boolean isInternalLeaf) {
		if (!block.isLeaves())
			return 0.0f;
		if (isInternalLeaf)
			return 0.5f;
		return 1.0f;
	}

	private void addQuadIndices(final boolean transparent) {
		if (transparent) {
			this.transparentIndices[this.transparentIndexCount++] = this.transparentVertexIndex;
			this.transparentIndices[this.transparentIndexCount++] = this.transparentVertexIndex + 1;
			this.transparentIndices[this.transparentIndexCount++] = this.transparentVertexIndex + 2;
			this.transparentIndices[this.transparentIndexCount++] = this.transparentVertexIndex + 2;
			this.transparentIndices[this.transparentIndexCount++] = this.transparentVertexIndex + 3;
			this.transparentIndices[this.transparentIndexCount++] = this.transparentVertexIndex;
			this.transparentVertexIndex += 4;
		} else {
			this.opaqueIndices[this.opaqueIndexCount++] = this.opaqueVertexIndex;
			this.opaqueIndices[this.opaqueIndexCount++] = this.opaqueVertexIndex + 1;
			this.opaqueIndices[this.opaqueIndexCount++] = this.opaqueVertexIndex + 2;
			this.opaqueIndices[this.opaqueIndexCount++] = this.opaqueVertexIndex + 2;
			this.opaqueIndices[this.opaqueIndexCount++] = this.opaqueVertexIndex + 3;
			this.opaqueIndices[this.opaqueIndexCount++] = this.opaqueVertexIndex;
			this.opaqueVertexIndex += 4;
		}
	}
}
