package com.matejpacan.voxelcraft;

public class ChunkMeshData {
	public float[] vertices;
	public int[] indices;
	public int vertexCount;
	public int indexCount;
	public float[] waterVertices;
	public int[] waterIndices;
	public int waterVertexCount;
	public int waterIndexCount;
	public volatile boolean ready;
}
