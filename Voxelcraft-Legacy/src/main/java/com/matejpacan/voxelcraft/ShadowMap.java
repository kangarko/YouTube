package com.matejpacan.voxelcraft;

import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_COMPONENT;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LEQUAL;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_NONE;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_BORDER_COLOR;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glDrawBuffer;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glReadBuffer;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameterfv;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL13.GL_CLAMP_TO_BORDER;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT24;
import static org.lwjgl.opengl.GL14.GL_TEXTURE_COMPARE_FUNC;
import static org.lwjgl.opengl.GL14.GL_TEXTURE_COMPARE_MODE;
import static org.lwjgl.opengl.GL30.GL_COMPARE_REF_TO_TEXTURE;
import static org.lwjgl.opengl.GL30.GL_DEPTH_ATTACHMENT;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_COMPLETE;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glCheckFramebufferStatus;
import static org.lwjgl.opengl.GL30.glDeleteFramebuffers;
import static org.lwjgl.opengl.GL30.glFramebufferTexture2D;
import static org.lwjgl.opengl.GL30.glGenFramebuffers;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import lombok.Getter;

public class ShadowMap {

	@Getter
	private final int shadowMapSize;

	private final int depthMapFBO;

	@Getter
	private final int depthMapTexture;

	@Getter
	private final Matrix4f lightSpaceMatrix = new Matrix4f();

	public static final float SHADOW_DISTANCE = 100.0f;

	private final Vector3f lastUpdatePos = new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
	private final Vector3f lastSunDir = new Vector3f(0, 1, 0);
	private static final float UPDATE_THRESHOLD = 16.0f;
	private static final float SUN_THRESHOLD = 0.02f;
	private int framesSinceUpdate = 0;
	private static final int MIN_UPDATE_INTERVAL = 3;

	public ShadowMap(int size) {
		this.shadowMapSize = size;

		this.depthMapTexture = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, this.depthMapTexture);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, size, size, 0, GL_DEPTH_COMPONENT, GL_FLOAT, 0);

		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);

		final float[] borderColor = { 1.0f, 1.0f, 1.0f, 1.0f };
		glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, borderColor);

		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);

		this.depthMapFBO = glGenFramebuffers();
		glBindFramebuffer(GL_FRAMEBUFFER, this.depthMapFBO);
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, this.depthMapTexture, 0);

		glDrawBuffer(GL_NONE);
		glReadBuffer(GL_NONE);

		if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
			throw new RuntimeException("Shadow map framebuffer is not complete!");

		glBindFramebuffer(GL_FRAMEBUFFER, 0);
	}

	public boolean needsUpdate(Vector3f sunDirection, Vector3f cameraPosition) {
		this.framesSinceUpdate++;
		if (this.framesSinceUpdate < MIN_UPDATE_INTERVAL)
			return false;

		final float camDist = this.lastUpdatePos.distance(cameraPosition);
		final float sunDist = this.lastSunDir.distance(sunDirection);
		final boolean shouldUpdate = camDist > UPDATE_THRESHOLD || sunDist > SUN_THRESHOLD;

		if (shouldUpdate)
			this.framesSinceUpdate = 0;

		return shouldUpdate;
	}

	public void updateLightSpaceMatrix(Vector3f sunDirection, Vector3f cameraPosition) {
		this.lastUpdatePos.set(cameraPosition);
		this.lastSunDir.set(sunDirection);

		final float gridSize = 32.0f;
		final float snappedX = (float) Math.floor(cameraPosition.x / gridSize) * gridSize;
		final float snappedY = (float) Math.floor(cameraPosition.y / gridSize) * gridSize;
		final float snappedZ = (float) Math.floor(cameraPosition.z / gridSize) * gridSize;
		final Vector3f center = new Vector3f(snappedX, snappedY, snappedZ);

		final Vector3f lightDir = new Vector3f(sunDirection).normalize();
		final Vector3f lightPos = new Vector3f(center).add(new Vector3f(lightDir).mul(SHADOW_DISTANCE));

		final Vector3f up = new Vector3f(0, 1, 0);
		if (Math.abs(lightDir.y) > 0.99f)
			up.set(0, 0, 1);

		final Matrix4f lightView = new Matrix4f().lookAt(lightPos, center, up);
		final Matrix4f lightProjection = new Matrix4f().ortho(
				-SHADOW_DISTANCE, SHADOW_DISTANCE,
				-SHADOW_DISTANCE, SHADOW_DISTANCE,
				0.1f, SHADOW_DISTANCE * 2.5f);

		lightProjection.mul(lightView, this.lightSpaceMatrix);
	}

	public void bindForWriting() {
		glViewport(0, 0, this.shadowMapSize, this.shadowMapSize);
		glBindFramebuffer(GL_FRAMEBUFFER, this.depthMapFBO);
		glClear(GL_DEPTH_BUFFER_BIT);
	}

	public void unbind() {
		glBindFramebuffer(GL_FRAMEBUFFER, 0);
	}

	public void bindForReading(int textureUnit) {
		glActiveTexture(GL_TEXTURE0 + textureUnit);
		glBindTexture(GL_TEXTURE_2D, this.depthMapTexture);
	}

	public void cleanup() {
		glDeleteFramebuffers(this.depthMapFBO);
		glDeleteTextures(this.depthMapTexture);
	}
}
