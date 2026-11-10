package com.matejpacan.voxelcraft;

import static org.lwjgl.opengl.GL11.GL_DEPTH_COMPONENT;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT24;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_DEPTH_ATTACHMENT;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_COMPLETE;
import static org.lwjgl.opengl.GL30.GL_RGBA16F;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glCheckFramebufferStatus;
import static org.lwjgl.opengl.GL30.glDeleteFramebuffers;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glFramebufferTexture2D;
import static org.lwjgl.opengl.GL30.glGenFramebuffers;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

public class PostProcess {

	private final int sceneFBO;
	private final int sceneColorTexture;
	private final int sceneDepthTexture;

	private final int godRayFBO;
	private final int godRayTexture;

	private final int quadVAO;
	private final int quadVBO;

	private int width;
	private int height;

	public PostProcess(int width, int height) {
		this.width = width;
		this.height = height;

		final float[] quadVertices = {
				-1.0f, -1.0f, 0.0f, 0.0f,
				1.0f, -1.0f, 1.0f, 0.0f,
				1.0f, 1.0f, 1.0f, 1.0f,
				-1.0f, -1.0f, 0.0f, 0.0f,
				1.0f, 1.0f, 1.0f, 1.0f,
				-1.0f, 1.0f, 0.0f, 1.0f,
		};

		this.quadVAO = glGenVertexArrays();
		this.quadVBO = glGenBuffers();

		glBindVertexArray(this.quadVAO);
		glBindBuffer(GL_ARRAY_BUFFER, this.quadVBO);

		final FloatBuffer buffer = BufferUtils.createFloatBuffer(quadVertices.length);
		buffer.put(quadVertices).flip();
		glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

		glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
		glEnableVertexAttribArray(0);
		glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2L * Float.BYTES);
		glEnableVertexAttribArray(1);

		glBindVertexArray(0);

		this.sceneFBO = glGenFramebuffers();
		this.sceneColorTexture = this.createColorTexture(width, height);
		this.sceneDepthTexture = this.createDepthTexture(width, height);

		glBindFramebuffer(GL_FRAMEBUFFER, this.sceneFBO);
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.sceneColorTexture, 0);
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, this.sceneDepthTexture, 0);

		if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
			throw new RuntimeException("Scene framebuffer is not complete!");

		glBindFramebuffer(GL_FRAMEBUFFER, 0);

		final int quarterWidth = Math.max(1, width / 4);
		final int quarterHeight = Math.max(1, height / 4);

		this.godRayFBO = glGenFramebuffers();
		this.godRayTexture = this.createColorTexture(quarterWidth, quarterHeight);

		glBindFramebuffer(GL_FRAMEBUFFER, this.godRayFBO);
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.godRayTexture, 0);

		if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
			throw new RuntimeException("God ray framebuffer is not complete!");

		glBindFramebuffer(GL_FRAMEBUFFER, 0);
	}

	private int createColorTexture(int w, int h) {
		final int texture = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, texture);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, w, h, 0, GL_RGBA, GL_FLOAT, 0);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		return texture;
	}

	private int createDepthTexture(int w, int h) {
		final int texture = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, texture);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, w, h, 0, GL_DEPTH_COMPONENT, GL_FLOAT, 0);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		return texture;
	}

	public void resize(int newWidth, int newHeight) {
		if (newWidth == this.width && newHeight == this.height)
			return;

		this.width = newWidth;
		this.height = newHeight;

		glBindTexture(GL_TEXTURE_2D, this.sceneColorTexture);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, this.width, this.height, 0, GL_RGBA, GL_FLOAT, 0);

		glBindTexture(GL_TEXTURE_2D, this.sceneDepthTexture);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, this.width, this.height, 0, GL_DEPTH_COMPONENT, GL_FLOAT, 0);

		final int quarterWidth = Math.max(1, this.width / 4);
		final int quarterHeight = Math.max(1, this.height / 4);

		glBindTexture(GL_TEXTURE_2D, this.godRayTexture);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, quarterWidth, quarterHeight, 0, GL_RGBA, GL_FLOAT, 0);

		glBindTexture(GL_TEXTURE_2D, 0);
	}

	public void bindSceneFBO() {
		glBindFramebuffer(GL_FRAMEBUFFER, this.sceneFBO);
		glViewport(0, 0, this.width, this.height);
	}

	public void bindGodRayFBO() {
		glBindFramebuffer(GL_FRAMEBUFFER, this.godRayFBO);
		glViewport(0, 0, Math.max(1, this.width / 4), Math.max(1, this.height / 4));
	}

	public void unbind() {
		glBindFramebuffer(GL_FRAMEBUFFER, 0);
	}

	public void bindSceneTexture(int unit) {
		glActiveTexture(GL_TEXTURE0 + unit);
		glBindTexture(GL_TEXTURE_2D, this.sceneColorTexture);
	}

	public void bindDepthTexture(int unit) {
		glActiveTexture(GL_TEXTURE0 + unit);
		glBindTexture(GL_TEXTURE_2D, this.sceneDepthTexture);
	}

	public void bindGodRayTexture(int unit) {
		glActiveTexture(GL_TEXTURE0 + unit);
		glBindTexture(GL_TEXTURE_2D, this.godRayTexture);
	}

	public void renderQuad() {
		glBindVertexArray(this.quadVAO);
		glDrawArrays(GL_TRIANGLES, 0, 6);
		glBindVertexArray(0);
	}

	public int getWidth() {
		return this.width;
	}

	public int getHeight() {
		return this.height;
	}

	public void cleanup() {
		glDeleteFramebuffers(this.sceneFBO);
		glDeleteFramebuffers(this.godRayFBO);
		glDeleteTextures(this.sceneColorTexture);
		glDeleteTextures(this.sceneDepthTexture);
		glDeleteTextures(this.godRayTexture);
		glDeleteVertexArrays(this.quadVAO);
		glDeleteBuffers(this.quadVBO);
	}
}
