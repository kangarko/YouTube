package com.matejpacan.voxelcraft;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.system.MemoryUtil;

public class Mesh {

	private final int vaoId;
	private final int posVboId;
	private final int texCoordsVboId;
	private final int normalsVboId;
	private final int colorsVboId;
	private final int idxVboId;
	private final int vertexCount;

	public Mesh(float[] positions, float[] texCoords, float[] normals, float[] colors, int[] indices) {
		this.vertexCount = indices.length;

		this.vaoId = glGenVertexArrays();
		glBindVertexArray(this.vaoId);

		this.posVboId = this.createVbo(positions, 0, 3);
		this.texCoordsVboId = this.createVbo(texCoords, 1, 2);
		this.normalsVboId = this.createVbo(normals, 2, 3);
		this.colorsVboId = this.createVbo(colors, 3, 4);

		this.idxVboId = glGenBuffers();
		IntBuffer indicesBuffer = null;
		try {
			indicesBuffer = MemoryUtil.memAllocInt(indices.length);
			indicesBuffer.put(indices).flip();
			glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, this.idxVboId);
			glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW);
		} finally {
			if (indicesBuffer != null)
				MemoryUtil.memFree(indicesBuffer);
		}

		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glBindVertexArray(0);
	}

	private int createVbo(float[] data, int attributeIndex, int size) {
		final int vboId = glGenBuffers();
		FloatBuffer buffer = null;
		try {
			buffer = MemoryUtil.memAllocFloat(data.length);
			buffer.put(data).flip();
			glBindBuffer(GL_ARRAY_BUFFER, vboId);
			glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
			glEnableVertexAttribArray(attributeIndex);
			glVertexAttribPointer(attributeIndex, size, GL_FLOAT, false, 0, 0);
		} finally {
			if (buffer != null)
				MemoryUtil.memFree(buffer);
		}
		return vboId;
	}

	public void render() {
		glBindVertexArray(this.vaoId);
		glDrawElements(GL_TRIANGLES, this.vertexCount, GL_UNSIGNED_INT, 0);
		glBindVertexArray(0);
	}

	public void cleanup() {
		glDisableVertexAttribArray(0);
		glDisableVertexAttribArray(1);
		glDisableVertexAttribArray(2);
		glDisableVertexAttribArray(3);

		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glDeleteBuffers(this.posVboId);
		glDeleteBuffers(this.texCoordsVboId);
		glDeleteBuffers(this.normalsVboId);
		glDeleteBuffers(this.colorsVboId);
		glDeleteBuffers(this.idxVboId);

		glBindVertexArray(0);
		glDeleteVertexArrays(this.vaoId);
	}
}
