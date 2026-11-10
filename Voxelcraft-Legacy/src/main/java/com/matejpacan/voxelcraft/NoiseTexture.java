package com.matejpacan.voxelcraft;

import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_REPEAT;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_RGBA8;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_3D;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_WRAP_R;
import static org.lwjgl.opengl.GL12.glTexImage3D;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;

public class NoiseTexture {

	private final int texture3D;

	public NoiseTexture(int size) {
		this.texture3D = this.generate3DNoiseTexture(size);
	}

	private int generate3DNoiseTexture(int size) {
		final ByteBuffer data = BufferUtils.createByteBuffer(size * size * size * 4);

		for (int z = 0; z < size; z++)
			for (int y = 0; y < size; y++)
				for (int x = 0; x < size; x++) {
					final float fx = x / (float) size;
					final float fy = y / (float) size;
					final float fz = z / (float) size;

					final float valueNoise = this.valueNoise3D(fx * 4, fy * 4, fz * 4);
					final float fbmNoise = this.fbm3D(fx * 2, fy * 2, fz * 2, 4);
					final float worleyNoise = this.worleyNoise3D(fx * 3, fy * 3, fz * 3);
					final float detailNoise = this.fbm3D(fx * 8, fy * 8, fz * 8, 3);

					final int r = (int) (this.clamp(valueNoise, 0, 1) * 255);
					final int g = (int) (this.clamp(fbmNoise, 0, 1) * 255);
					final int b = (int) (this.clamp(worleyNoise, 0, 1) * 255);
					final int a = (int) (this.clamp(detailNoise, 0, 1) * 255);

					data.put((byte) r);
					data.put((byte) g);
					data.put((byte) b);
					data.put((byte) a);
				}
		data.flip();

		final int texture = glGenTextures();
		glBindTexture(GL_TEXTURE_3D, texture);
		glTexImage3D(GL_TEXTURE_3D, 0, GL_RGBA8, size, size, size, 0, GL_RGBA, GL_UNSIGNED_BYTE, data);

		glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_REPEAT);
		glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_REPEAT);
		glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_REPEAT);

		glBindTexture(GL_TEXTURE_3D, 0);

		return texture;
	}

	private float hash3D(float x, float y, float z) {
		final float n = (float) Math.sin(x * 127.1f + y * 311.7f + z * 74.7f) * 43758.5453f;
		return n - (float) Math.floor(n);
	}

	private float smoothstep(float t) {
		return t * t * (3 - 2 * t);
	}

	private float lerp(float a, float b, float t) {
		return a + t * (b - a);
	}

	private float clamp(float v, float min, float max) {
		return Math.max(min, Math.min(max, v));
	}

	private float valueNoise3D(float x, float y, float z) {
		final int ix = (int) Math.floor(x);
		final int iy = (int) Math.floor(y);
		final int iz = (int) Math.floor(z);
		final float fx = x - ix;
		final float fy = y - iy;
		final float fz = z - iz;

		final float sx = this.smoothstep(fx);
		final float sy = this.smoothstep(fy);
		final float sz = this.smoothstep(fz);

		final float v000 = this.hash3D(ix, iy, iz);
		final float v100 = this.hash3D(ix + 1, iy, iz);
		final float v010 = this.hash3D(ix, iy + 1, iz);
		final float v110 = this.hash3D(ix + 1, iy + 1, iz);
		final float v001 = this.hash3D(ix, iy, iz + 1);
		final float v101 = this.hash3D(ix + 1, iy, iz + 1);
		final float v011 = this.hash3D(ix, iy + 1, iz + 1);
		final float v111 = this.hash3D(ix + 1, iy + 1, iz + 1);

		final float x00 = this.lerp(v000, v100, sx);
		final float x10 = this.lerp(v010, v110, sx);
		final float x01 = this.lerp(v001, v101, sx);
		final float x11 = this.lerp(v011, v111, sx);

		final float y0 = this.lerp(x00, x10, sy);
		final float y1 = this.lerp(x01, x11, sy);

		return this.lerp(y0, y1, sz);
	}

	private float fbm3D(float x, float y, float z, int octaves) {
		float value = 0;
		float amplitude = 0.5f;
		float frequency = 1;
		float maxValue = 0;

		for (int i = 0; i < octaves; i++) {
			value += amplitude * this.valueNoise3D(x * frequency, y * frequency, z * frequency);
			maxValue += amplitude;
			amplitude *= 0.5f;
			frequency *= 2;
		}

		return value / maxValue;
	}

	private float worleyNoise3D(float x, float y, float z) {
		final int ix = (int) Math.floor(x);
		final int iy = (int) Math.floor(y);
		final int iz = (int) Math.floor(z);
		final float fx = x - ix;
		final float fy = y - iy;
		final float fz = z - iz;

		float minDist = 1.0f;

		for (int dz = -1; dz <= 1; dz++)
			for (int dy = -1; dy <= 1; dy++)
				for (int dx = -1; dx <= 1; dx++) {
					final float px = this.hash3D(ix + dx, iy + dy, iz + dz);
					final float py = this.hash3D(ix + dx + 31, iy + dy + 31, iz + dz + 31);
					final float pz = this.hash3D(ix + dx + 57, iy + dy + 57, iz + dz + 57);

					final float diffX = dx + px - fx;
					final float diffY = dy + py - fy;
					final float diffZ = dz + pz - fz;

					final float dist = (float) Math.sqrt(diffX * diffX + diffY * diffY + diffZ * diffZ);
					minDist = Math.min(minDist, dist);
				}

		return minDist;
	}

	public void bind(int unit) {
		glActiveTexture(GL_TEXTURE0 + unit);
		glBindTexture(GL_TEXTURE_3D, this.texture3D);
	}

	public void cleanup() {
		glDeleteTextures(this.texture3D);
	}
}
