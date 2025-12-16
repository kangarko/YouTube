package com.matejpacan.voxelcraft;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

public class Texture {

	private final int id;

	public Texture(final int id) {
		this.id = id;
	}

	public static Texture loadTexture(final String resourcePath) {
		ByteBuffer imageBuffer;
		try {
			imageBuffer = Texture.loadResourceToBuffer(resourcePath);
		} catch (final IOException e) {
			throw new RuntimeException("Failed to load texture: " + resourcePath, e);
		}

		int width, height;
		ByteBuffer pixels;

		try (MemoryStack stack = MemoryStack.stackPush()) {
			final IntBuffer w = stack.mallocInt(1);
			final IntBuffer h = stack.mallocInt(1);
			final IntBuffer channels = stack.mallocInt(1);

			STBImage.stbi_set_flip_vertically_on_load(false);
			pixels = STBImage.stbi_load_from_memory(imageBuffer, w, h, channels, 4);

			if (pixels == null)
				throw new RuntimeException(
						"Failed to decode texture: " + resourcePath + " - " + STBImage.stbi_failure_reason());

			width = w.get(0);
			height = h.get(0);
		}

		org.lwjgl.system.MemoryUtil.memFree(imageBuffer);

		final int textureId = GL11.glGenTextures();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST_MIPMAP_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE,
				pixels);
		GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);

		STBImage.stbi_image_free(pixels);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

		return new Texture(textureId);
	}

	private static ByteBuffer loadResourceToBuffer(final String resourcePath) throws IOException {
		try (InputStream is = Texture.class.getResourceAsStream(resourcePath)) {
			if (is == null)
				throw new IOException("Resource not found: " + resourcePath);

			final byte[] bytes = is.readAllBytes();
			final ByteBuffer buffer = org.lwjgl.system.MemoryUtil.memAlloc(bytes.length);
			buffer.put(bytes).flip();
			return buffer;
		}
	}

	public void bind() {
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.id);
	}

	public void cleanup() {
		GL11.glDeleteTextures(this.id);
	}
}
