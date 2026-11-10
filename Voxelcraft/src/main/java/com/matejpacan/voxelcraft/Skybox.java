package com.matejpacan.voxelcraft;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LEQUAL;
import static org.lwjgl.opengl.GL11.GL_LESS;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glDepthFunc;
import static org.lwjgl.opengl.GL11.glDepthMask;
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

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

public class Skybox {

	private static final float SIZE = 500.0f;

	private static final float[] VERTICES = {
			-SIZE, SIZE, -SIZE,
			-SIZE, -SIZE, -SIZE,
			SIZE, -SIZE, -SIZE,
			SIZE, SIZE, -SIZE,

			-SIZE, -SIZE, SIZE,
			-SIZE, -SIZE, -SIZE,
			-SIZE, SIZE, -SIZE,
			-SIZE, SIZE, SIZE,

			SIZE, -SIZE, -SIZE,
			SIZE, -SIZE, SIZE,
			SIZE, SIZE, SIZE,
			SIZE, SIZE, -SIZE,

			-SIZE, -SIZE, SIZE,
			-SIZE, SIZE, SIZE,
			SIZE, SIZE, SIZE,
			SIZE, -SIZE, SIZE,

			-SIZE, SIZE, -SIZE,
			SIZE, SIZE, -SIZE,
			SIZE, SIZE, SIZE,
			-SIZE, SIZE, SIZE,

			-SIZE, -SIZE, -SIZE,
			-SIZE, -SIZE, SIZE,
			SIZE, -SIZE, SIZE,
			SIZE, -SIZE, -SIZE
	};

	private static final int[] INDICES = {
			0, 1, 2, 2, 3, 0,
			4, 5, 6, 6, 7, 4,
			8, 9, 10, 10, 11, 8,
			12, 13, 14, 14, 15, 12,
			16, 17, 18, 18, 19, 16,
			20, 21, 22, 22, 23, 20
	};

	private final int vaoId;
	private final int posVboId;
	private final int idxVboId;
	private final int vertexCount;

	private final Shader shader;
	private final Vector3f sunDirection;

	public Skybox(String vertexShaderCode, String fragmentShaderCode) {
		this.vertexCount = INDICES.length;

		this.shader = new Shader();
		this.shader.createVertexShader(vertexShaderCode);
		this.shader.createFragmentShader(fragmentShaderCode);
		this.shader.link();

		this.shader.createUniform("projectionMatrix");
		this.shader.createUniform("viewMatrix");
		this.shader.createUniform("sunDirection");

		this.sunDirection = new Vector3f();
		this.setSunAtDawn();

		this.vaoId = glGenVertexArrays();
		glBindVertexArray(this.vaoId);

		FloatBuffer verticesBuffer = null;
		try {
			verticesBuffer = MemoryUtil.memAllocFloat(VERTICES.length);
			verticesBuffer.put(VERTICES).flip();
			this.posVboId = glGenBuffers();
			glBindBuffer(GL_ARRAY_BUFFER, this.posVboId);
			glBufferData(GL_ARRAY_BUFFER, verticesBuffer, GL_STATIC_DRAW);
			glEnableVertexAttribArray(0);
			glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		} finally {
			if (verticesBuffer != null)
				MemoryUtil.memFree(verticesBuffer);
		}

		IntBuffer indicesBuffer = null;
		try {
			indicesBuffer = MemoryUtil.memAllocInt(INDICES.length);
			indicesBuffer.put(INDICES).flip();
			this.idxVboId = glGenBuffers();
			glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, this.idxVboId);
			glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW);
		} finally {
			if (indicesBuffer != null)
				MemoryUtil.memFree(indicesBuffer);
		}

		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glBindVertexArray(0);
	}

	private void setSunAtDawn() {
		final float elevation = (float) Math.toRadians(15.0);
		final float azimuth = (float) Math.toRadians(90.0);

		this.sunDirection.x = (float) (Math.cos(elevation) * Math.sin(azimuth));
		this.sunDirection.y = (float) Math.sin(elevation);
		this.sunDirection.z = (float) (Math.cos(elevation) * Math.cos(azimuth));
		this.sunDirection.normalize();
	}

	public void render(Matrix4f projectionMatrix, Matrix4f viewMatrix) {
		glDepthFunc(GL_LEQUAL);
		glDepthMask(false);

		this.shader.bind();

		final Matrix4f skyboxViewMatrix = new Matrix4f(viewMatrix);
		skyboxViewMatrix.m30(0);
		skyboxViewMatrix.m31(0);
		skyboxViewMatrix.m32(0);

		this.shader.setUniform("projectionMatrix", projectionMatrix);
		this.shader.setUniform("viewMatrix", skyboxViewMatrix);
		this.shader.setUniform("sunDirection", this.sunDirection);

		glBindVertexArray(this.vaoId);
		glDrawElements(GL_TRIANGLES, this.vertexCount, GL_UNSIGNED_INT, 0);
		glBindVertexArray(0);

		this.shader.unbind();

		glDepthMask(true);
		glDepthFunc(GL_LESS);
	}

	public Vector3f getSunDirection() {
		return new Vector3f(this.sunDirection);
	}

	public int getVaoId() {
		return this.vaoId;
	}

	public int getVertexCount() {
		return this.vertexCount;
	}

	public void cleanup() {
		glDisableVertexAttribArray(0);

		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glDeleteBuffers(this.posVboId);
		glDeleteBuffers(this.idxVboId);

		glBindVertexArray(0);
		glDeleteVertexArrays(this.vaoId);

		if (this.shader != null)
			this.shader.cleanup();
	}
}
