package com.matejpacan.voxelcraft;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;

public class ShadowMap {

	private static final int SHADOW_MAP_SIZE = 4096;
	private static final float SHADOW_DISTANCE = 150.0f;

	private final int fboId;
	private final int depthTextureId;
	private final Shader depthShader;
	private final Matrix4f lightViewMatrix = new Matrix4f();
	private final Matrix4f lightProjectionMatrix = new Matrix4f();
	private final Matrix4f lightSpaceMatrix = new Matrix4f();

	public ShadowMap(final String vertexShaderCode, final String fragmentShaderCode) {
		this.fboId = GL30.glGenFramebuffers();
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.fboId);

		this.depthTextureId = GL11.glGenTextures();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.depthTextureId);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL14.GL_DEPTH_COMPONENT24,
				ShadowMap.SHADOW_MAP_SIZE, ShadowMap.SHADOW_MAP_SIZE, 0,
				GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, 0);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL13.GL_CLAMP_TO_BORDER);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL13.GL_CLAMP_TO_BORDER);
		GL11.glTexParameterfv(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_BORDER_COLOR, new float[] { 1.0f, 1.0f, 1.0f, 1.0f });
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_MODE, GL14.GL_COMPARE_R_TO_TEXTURE);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_FUNC, GL11.GL_LEQUAL);

		GL32.glFramebufferTexture(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, this.depthTextureId, 0);
		GL11.glDrawBuffer(GL11.GL_NONE);
		GL11.glReadBuffer(GL11.GL_NONE);

		final int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
		if (status != GL30.GL_FRAMEBUFFER_COMPLETE)
			throw new RuntimeException("Shadow map framebuffer is not complete: " + status);

		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);

		this.depthShader = new Shader();
		this.depthShader.createVertexShader(vertexShaderCode);
		this.depthShader.createFragmentShader(fragmentShaderCode);
		this.depthShader.link();
		this.depthShader.createUniform("lightSpaceMatrix");
		this.depthShader.createUniform("modelMatrix");
		this.depthShader.createUniform("textureSampler");
	}

	public void updateLightSpaceMatrix(final Vector3f sunDirection, final Vector3f cameraPosition) {
		final Vector3f lightDir = new Vector3f(sunDirection).normalize();

		final Vector3f lightPos = new Vector3f(cameraPosition)
				.sub(new Vector3f(lightDir).mul(ShadowMap.SHADOW_DISTANCE * 0.5f));

		final Vector3f up = Math.abs(lightDir.y) > 0.99f
				? new Vector3f(0, 0, 1)
				: new Vector3f(0, 1, 0);

		this.lightViewMatrix.identity().lookAt(
				lightPos,
				new Vector3f(cameraPosition),
				up);

		final float orthoSize = ShadowMap.SHADOW_DISTANCE;
		this.lightProjectionMatrix.identity().ortho(
				-orthoSize, orthoSize,
				-orthoSize, orthoSize,
				0.1f, ShadowMap.SHADOW_DISTANCE * 2.0f);

		this.lightProjectionMatrix.mul(this.lightViewMatrix, this.lightSpaceMatrix);
	}

	public void bind() {
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.fboId);
		GL11.glViewport(0, 0, ShadowMap.SHADOW_MAP_SIZE, ShadowMap.SHADOW_MAP_SIZE);
		GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
	}

	public void unbind() {
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
	}

	public void bindDepthTexture(final int textureUnit) {
		GL13.glActiveTexture(GL13.GL_TEXTURE0 + textureUnit);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.depthTextureId);
	}

	public Shader getDepthShader() {
		return this.depthShader;
	}

	public Matrix4f getLightSpaceMatrix() {
		return this.lightSpaceMatrix;
	}

	public void cleanup() {
		GL11.glDeleteTextures(this.depthTextureId);
		GL30.glDeleteFramebuffers(this.fboId);
		if (this.depthShader != null)
			this.depthShader.cleanup();
	}
}
