package com.matejpacan.voxelcraft;

public class ChunkMeshData {

	private final int chunkX;
	private final int chunkZ;

	private final float[] opaqueVertices;
	private final float[] opaqueTexCoords;
	private final float[] opaqueNormals;
	private final float[] opaqueColors;
	private final int[] opaqueIndices;

	private final float[] transparentVertices;
	private final float[] transparentTexCoords;
	private final float[] transparentNormals;
	private final float[] transparentColors;
	private final int[] transparentIndices;

	public ChunkMeshData(final int chunkX, final int chunkZ,
			final float[] opaqueVertices, final float[] opaqueTexCoords, final float[] opaqueNormals,
			final float[] opaqueColors,
			final int[] opaqueIndices,
			final float[] transparentVertices, final float[] transparentTexCoords, final float[] transparentNormals,
			final float[] transparentColors, final int[] transparentIndices) {
		this.chunkX = chunkX;
		this.chunkZ = chunkZ;
		this.opaqueVertices = opaqueVertices;
		this.opaqueTexCoords = opaqueTexCoords;
		this.opaqueNormals = opaqueNormals;
		this.opaqueColors = opaqueColors;
		this.opaqueIndices = opaqueIndices;
		this.transparentVertices = transparentVertices;
		this.transparentTexCoords = transparentTexCoords;
		this.transparentNormals = transparentNormals;
		this.transparentColors = transparentColors;
		this.transparentIndices = transparentIndices;
	}

	public int getChunkX() {
		return this.chunkX;
	}

	public int getChunkZ() {
		return this.chunkZ;
	}

	public boolean isOpaqueEmpty() {
		return this.opaqueVertices == null || this.opaqueVertices.length == 0;
	}

	public boolean isTransparentEmpty() {
		return this.transparentVertices == null || this.transparentVertices.length == 0;
	}

	public Mesh createOpaqueMesh() {
		if (this.isOpaqueEmpty())
			return null;
		return new Mesh(this.opaqueVertices, this.opaqueTexCoords, this.opaqueNormals, this.opaqueColors,
				this.opaqueIndices);
	}

	public Mesh createTransparentMesh() {
		if (this.isTransparentEmpty())
			return null;
		return new Mesh(this.transparentVertices, this.transparentTexCoords, this.transparentNormals,
				this.transparentColors, this.transparentIndices);
	}
}
