package com.matejpacan.voxelcraft;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

public class GodRays {

	private static final float[] QUAD_POSITIONS = {
			-1.0f, -1.0f,
			1.0f, -1.0f,
			1.0f, 1.0f,
			-1.0f, 1.0f
	};

	private static final float[] QUAD_TEX_COORDS = {
			0.0f, 0.0f,
			1.0f, 0.0f,
			1.0f, 1.0f,
			0.0f, 1.0f
	};

	private static final int[] QUAD_INDICES = {
			0, 1, 2,
			2, 3, 0
	};

	private final int quadVaoId;
	private final int posVboId;
	private final int texCoordVboId;
	private final int indexVboId;

	private final Shader occlusionShader;
	private final Shader skyboxOcclusionShader;
	private final Shader compositeShader;

	private static final float DENSITY = 1.0f;
	private static final float WEIGHT = 0.01f;
	private static final float DECAY = 0.975f;
	private static final float EXPOSURE = 0.8f;
	private static final int NUM_SAMPLES = 100;

	private final Vector2f sunScreenPos = new Vector2f();
	private final Vector4f tempVec4 = new Vector4f();

	public GodRays(final String occlusionVertexCode, final String occlusionFragmentCode,
			final String skyboxOcclusionFragmentCode,
			final String compositeVertexCode, final String compositeFragmentCode) {

		this.occlusionShader = new Shader();
		this.occlusionShader.createVertexShader(occlusionVertexCode);
		this.occlusionShader.createFragmentShader(occlusionFragmentCode);
		this.occlusionShader.link();
		this.occlusionShader.createUniform("projectionMatrix");
		this.occlusionShader.createUniform("viewMatrix");
		this.occlusionShader.createUniform("modelMatrix");
		this.occlusionShader.createUniform("textureSampler");
		this.occlusionShader.createUniform("isLeaves");

		this.skyboxOcclusionShader = new Shader();
		this.skyboxOcclusionShader.createVertexShader("""
														#version 410 core

														layout (location = 0) in vec3 position;

														out vec3 fragPos;

														uniform mat4 projectionMatrix;
														uniform mat4 viewMatrix;

														void main() {
														    fragPos = position;
														    vec4 pos = projectionMatrix * viewMatrix * vec4(position, 1.0);
														    gl_Position = pos.xyww;
														}
														""");
		this.skyboxOcclusionShader.createFragmentShader(skyboxOcclusionFragmentCode);
		this.skyboxOcclusionShader.link();
		this.skyboxOcclusionShader.createUniform("projectionMatrix");
		this.skyboxOcclusionShader.createUniform("viewMatrix");
		this.skyboxOcclusionShader.createUniform("sunDirection");

		this.compositeShader = new Shader();
		this.compositeShader.createVertexShader(compositeVertexCode);
		this.compositeShader.createFragmentShader(compositeFragmentCode);
		this.compositeShader.link();
		this.compositeShader.createUniform("sceneTexture");
		this.compositeShader.createUniform("occlusionTexture");
		this.compositeShader.createUniform("sunScreenPos");
		this.compositeShader.createUniform("density");
		this.compositeShader.createUniform("weight");
		this.compositeShader.createUniform("decay");
		this.compositeShader.createUniform("exposure");
		this.compositeShader.createUniform("numSamples");

		this.quadVaoId = GL30.glGenVertexArrays();
		GL30.glBindVertexArray(this.quadVaoId);

		this.posVboId = this.createFloatVbo(GodRays.QUAD_POSITIONS, 0);
		this.texCoordVboId = this.createFloatVbo(GodRays.QUAD_TEX_COORDS, 1);

		this.indexVboId = GL15.glGenBuffers();
		IntBuffer indexBuffer = null;
		try {
			indexBuffer = MemoryUtil.memAllocInt(GodRays.QUAD_INDICES.length);
			indexBuffer.put(GodRays.QUAD_INDICES).flip();
			GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.indexVboId);
			GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL15.GL_STATIC_DRAW);
		} finally {
			if (indexBuffer != null)
				MemoryUtil.memFree(indexBuffer);
		}

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		GL30.glBindVertexArray(0);
	}

	private int createFloatVbo(final float[] data, final int attributeIndex) {
		final int vboId = GL15.glGenBuffers();
		FloatBuffer buffer = null;
		try {
			buffer = MemoryUtil.memAllocFloat(data.length);
			buffer.put(data).flip();
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
			GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
			GL20.glEnableVertexAttribArray(attributeIndex);
			GL20.glVertexAttribPointer(attributeIndex, 2, GL11.GL_FLOAT, false, 0, 0);
		} finally {
			if (buffer != null)
				MemoryUtil.memFree(buffer);
		}
		return vboId;
	}

	public Vector2f computeSunScreenPosition(final Vector3f sunDirection, final Matrix4f projectionMatrix,
			final Matrix4f viewMatrix) {
		this.tempVec4.set(sunDirection.x * 1000.0f, sunDirection.y * 1000.0f, sunDirection.z * 1000.0f, 1.0f);

		viewMatrix.transform(this.tempVec4);
		projectionMatrix.transform(this.tempVec4);

		if (this.tempVec4.w <= 0.0f) {
			this.sunScreenPos.set(-10.0f, -10.0f);
			return this.sunScreenPos;
		}

		final float ndcX = this.tempVec4.x / this.tempVec4.w;
		final float ndcY = this.tempVec4.y / this.tempVec4.w;

		this.sunScreenPos.x = (ndcX + 1.0f) * 0.5f;
		this.sunScreenPos.y = (ndcY + 1.0f) * 0.5f;

		return this.sunScreenPos;
	}

	public Shader getOcclusionShader() {
		return this.occlusionShader;
	}

	public void renderSkyboxOcclusion(final Matrix4f projectionMatrix, final Matrix4f viewMatrix,
			final Vector3f sunDirection,
			final int skyboxVaoId, final int skyboxVertexCount) {
		GL11.glDepthFunc(GL11.GL_LEQUAL);
		GL11.glDepthMask(false);

		this.skyboxOcclusionShader.bind();

		final Matrix4f skyboxViewMatrix = new Matrix4f(viewMatrix);
		skyboxViewMatrix.m30(0);
		skyboxViewMatrix.m31(0);
		skyboxViewMatrix.m32(0);

		this.skyboxOcclusionShader.setUniform("projectionMatrix", projectionMatrix);
		this.skyboxOcclusionShader.setUniform("viewMatrix", skyboxViewMatrix);
		this.skyboxOcclusionShader.setUniform("sunDirection", sunDirection);

		GL30.glBindVertexArray(skyboxVaoId);
		GL11.glDrawElements(GL11.GL_TRIANGLES, skyboxVertexCount, GL11.GL_UNSIGNED_INT, 0);
		GL30.glBindVertexArray(0);

		this.skyboxOcclusionShader.unbind();

		GL11.glDepthMask(true);
		GL11.glDepthFunc(GL11.GL_LESS);
	}

	public void renderComposite(final Framebuffer sceneFBO, final Framebuffer occlusionFBO, final Vector2f sunPos) {
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_BLEND);

		this.compositeShader.bind();

		sceneFBO.bindColorTexture(0);
		occlusionFBO.bindColorTexture(1);

		this.compositeShader.setUniform("sceneTexture", 0);
		this.compositeShader.setUniform("occlusionTexture", 1);
		this.compositeShader.setUniform("sunScreenPos", sunPos);
		this.compositeShader.setUniform("density", GodRays.DENSITY);
		this.compositeShader.setUniform("weight", GodRays.WEIGHT);
		this.compositeShader.setUniform("decay", GodRays.DECAY);
		this.compositeShader.setUniform("exposure", GodRays.EXPOSURE);
		this.compositeShader.setUniform("numSamples", GodRays.NUM_SAMPLES);

		GL30.glBindVertexArray(this.quadVaoId);
		GL11.glDrawElements(GL11.GL_TRIANGLES, 6, GL11.GL_UNSIGNED_INT, 0);
		GL30.glBindVertexArray(0);

		this.compositeShader.unbind();

		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_BLEND);
	}

	public void cleanup() {
		GL20.glDisableVertexAttribArray(0);
		GL20.glDisableVertexAttribArray(1);

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		GL15.glDeleteBuffers(this.posVboId);
		GL15.glDeleteBuffers(this.texCoordVboId);
		GL15.glDeleteBuffers(this.indexVboId);

		GL30.glBindVertexArray(0);
		GL30.glDeleteVertexArrays(this.quadVaoId);

		if (this.occlusionShader != null)
			this.occlusionShader.cleanup();
		if (this.skyboxOcclusionShader != null)
			this.skyboxOcclusionShader.cleanup();
		if (this.compositeShader != null)
			this.compositeShader.cleanup();
	}
}
