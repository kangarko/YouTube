package com.matejpacan.voxelcraft;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

public class Framebuffer {

	private final int fboId;
	private final int colorTextureId;
	private final int depthRenderbufferId;
	private int width;
	private int height;

	public Framebuffer(final int width, final int height) {
		this.width = width;
		this.height = height;

		this.fboId = GL30.glGenFramebuffers();
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.fboId);

		this.colorTextureId = GL11.glGenTextures();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.colorTextureId);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE,
				0);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_WRAP_R, GL12.GL_CLAMP_TO_EDGE);
		GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D,
				this.colorTextureId, 0);

		this.depthRenderbufferId = GL30.glGenRenderbuffers();
		GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, this.depthRenderbufferId);
		GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL14.GL_DEPTH_COMPONENT24, width, height);
		GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER,
				this.depthRenderbufferId);

		final int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
		if (status != GL30.GL_FRAMEBUFFER_COMPLETE)
			throw new RuntimeException("Framebuffer is not complete: " + status);

		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
		GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, 0);
	}

	public void bind() {
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.fboId);
		GL11.glViewport(0, 0, this.width, this.height);
	}

	public void unbind() {
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
	}

	public void resize(final int newWidth, final int newHeight) {
		this.width = newWidth;
		this.height = newHeight;

		GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.colorTextureId);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, newWidth, newHeight, 0, GL11.GL_RGBA,
				GL11.GL_UNSIGNED_BYTE, 0);

		GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, this.depthRenderbufferId);
		GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL14.GL_DEPTH_COMPONENT24, newWidth, newHeight);

		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
		GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, 0);
	}

	public void bindColorTexture(final int textureUnit) {
		GL13.glActiveTexture(GL13.GL_TEXTURE0 + textureUnit);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.colorTextureId);
	}

	public void cleanup() {
		GL11.glDeleteTextures(this.colorTextureId);
		GL30.glDeleteRenderbuffers(this.depthRenderbufferId);
		GL30.glDeleteFramebuffers(this.fboId);
	}
}
