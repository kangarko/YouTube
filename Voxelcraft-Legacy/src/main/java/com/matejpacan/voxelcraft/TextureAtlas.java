package com.matejpacan.voxelcraft;

import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_NEAREST_MIPMAP_NEAREST;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glGetFloat;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameterf;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_MAX_LEVEL;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL14.GL_TEXTURE_LOD_BIAS;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import lombok.Getter;

/**
 * Texture atlas handler for Minecraft-style terrain.png.
 *
 * The terrain.png is a 256x256 image containing 16x16 tiles arranged in a 16x16
 * grid.
 * Each tile is 16x16 pixels.
 *
 * Notable tile positions (row, col from top-left, 0-indexed):
 * - (0, 0): Grass top (gray, needs green tint)
 * - (0, 1): Stone
 * - (0, 2): Dirt
 * - (0, 3): Grass side (with dirt bottom and grass overlay)
 * - (1, 0): Cobblestone
 * - (1, 1): Bedrock
 * - (0, 4): Planks
 */
public class TextureAtlas {

	private static final int ATLAS_SIZE = 256;
	private static final int TILE_SIZE = 16;
	private static final int TILES_PER_ROW = ATLAS_SIZE / TILE_SIZE; // 16

	@Getter
	private final int textureId;

	@Getter
	private final int atlasWidth;

	@Getter
	private final int atlasHeight;

	public TextureAtlas(String resourcePath) {
		ByteBuffer imageBuffer = null;

		try (MemoryStack stack = MemoryStack.stackPush()) {
			final IntBuffer widthBuf = stack.mallocInt(1);
			final IntBuffer heightBuf = stack.mallocInt(1);
			final IntBuffer channelsBuf = stack.mallocInt(1);

			// Load image from resources
			final ByteBuffer rawData = this.loadResource(resourcePath);

			// Flip image vertically for OpenGL
			STBImage.stbi_set_flip_vertically_on_load(true);

			imageBuffer = STBImage.stbi_load_from_memory(rawData, widthBuf, heightBuf, channelsBuf, 4);
			if (imageBuffer == null)
				throw new RuntimeException(
						"Failed to load texture: " + resourcePath + "\n" + STBImage.stbi_failure_reason());

			this.atlasWidth = widthBuf.get(0);
			this.atlasHeight = heightBuf.get(0);

			System.out.println("Loaded texture atlas: " + resourcePath + " (" + this.atlasWidth + "x" + this.atlasHeight + ")");

			// Create OpenGL texture
			this.textureId = glGenTextures();
			glBindTexture(GL_TEXTURE_2D, this.textureId);

			// Set texture parameters for pixel art (no filtering, wrap mode)
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_NEAREST);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

			// Upload texture data
			glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, this.atlasWidth, this.atlasHeight, 0,
					GL_RGBA, GL_UNSIGNED_BYTE, imageBuffer);

			glGenerateMipmap(GL_TEXTURE_2D);

			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 2);

			glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_LOD_BIAS, -0.5f);

			final int GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT = 0x84FF;
			final int GL_TEXTURE_MAX_ANISOTROPY_EXT = 0x84FE;
			final float maxAnisotropy = glGetFloat(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
			if (maxAnisotropy > 0)
				glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY_EXT, Math.min(8.0f, maxAnisotropy));

			glBindTexture(GL_TEXTURE_2D, 0);

		} catch (final Exception e) {
			throw new RuntimeException("Failed to load texture atlas: " + resourcePath, e);
		} finally {
			if (imageBuffer != null)
				STBImage.stbi_image_free(imageBuffer);
		}
	}

	private ByteBuffer loadResource(String resourcePath) {
		try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(resourcePath)) {
			if (is == null)
				throw new RuntimeException("Resource not found: " + resourcePath);

			final byte[] bytes = is.readAllBytes();
			final ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
			buffer.put(bytes).flip();
			return buffer;

		} catch (final IOException e) {
			throw new RuntimeException("Failed to load resource: " + resourcePath, e);
		}
	}

	/**
	 * Bind this texture to a texture unit.
	 */
	public void bind(int unit) {
		glActiveTexture(GL_TEXTURE0 + unit);
		glBindTexture(GL_TEXTURE_2D, this.textureId);
	}

	/**
	 * Get UV coordinates for a tile at the given grid position.
	 * Row and column are 0-indexed from top-left of the atlas.
	 *
	 * @param row Tile row (0-15, from top)
	 * @param col Tile column (0-15, from left)
	 * @return float[4] = {u1, v1, u2, v2} where (u1,v1) is bottom-left and (u2,v2)
	 *         is top-right
	 */
	public float[] getTileUV(int row, int col) {
		final float u1 = (float) col / TILES_PER_ROW;
		final float v1 = (float) (TILES_PER_ROW - row - 1) / TILES_PER_ROW;
		final float u2 = (float) (col + 1) / TILES_PER_ROW;
		final float v2 = (float) (TILES_PER_ROW - row) / TILES_PER_ROW;

		final float epsilon = 0.5f / ATLAS_SIZE;
		return new float[] { u1 + epsilon, v1 + epsilon, u2 - epsilon, v2 - epsilon };
	}

	/**
	 * Cleanup OpenGL resources.
	 */
	public void cleanup() {
		if (this.textureId != 0)
			glDeleteTextures(this.textureId);
	}
}
