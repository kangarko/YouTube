package com.matejpacan.voxelcraft;

import org.joml.Matrix4f;
import org.joml.Vector4f;

public class Frustum {

	private final Vector4f[] planes = new Vector4f[6];

	public Frustum() {
		for (int i = 0; i < 6; i++)
			this.planes[i] = new Vector4f();
	}

	public void update(final Matrix4f projectionMatrix, final Matrix4f viewMatrix) {
		final Matrix4f clipMatrix = new Matrix4f();
		projectionMatrix.mul(viewMatrix, clipMatrix);

		this.planes[0].set(
				clipMatrix.m03() + clipMatrix.m00(),
				clipMatrix.m13() + clipMatrix.m10(),
				clipMatrix.m23() + clipMatrix.m20(),
				clipMatrix.m33() + clipMatrix.m30());

		this.planes[1].set(
				clipMatrix.m03() - clipMatrix.m00(),
				clipMatrix.m13() - clipMatrix.m10(),
				clipMatrix.m23() - clipMatrix.m20(),
				clipMatrix.m33() - clipMatrix.m30());

		this.planes[2].set(
				clipMatrix.m03() + clipMatrix.m01(),
				clipMatrix.m13() + clipMatrix.m11(),
				clipMatrix.m23() + clipMatrix.m21(),
				clipMatrix.m33() + clipMatrix.m31());

		this.planes[3].set(
				clipMatrix.m03() - clipMatrix.m01(),
				clipMatrix.m13() - clipMatrix.m11(),
				clipMatrix.m23() - clipMatrix.m21(),
				clipMatrix.m33() - clipMatrix.m31());

		this.planes[4].set(
				clipMatrix.m03() + clipMatrix.m02(),
				clipMatrix.m13() + clipMatrix.m12(),
				clipMatrix.m23() + clipMatrix.m22(),
				clipMatrix.m33() + clipMatrix.m32());

		this.planes[5].set(
				clipMatrix.m03() - clipMatrix.m02(),
				clipMatrix.m13() - clipMatrix.m12(),
				clipMatrix.m23() - clipMatrix.m22(),
				clipMatrix.m33() - clipMatrix.m32());

		for (int i = 0; i < 6; i++) {
			final float length = (float) Math.sqrt(
					this.planes[i].x * this.planes[i].x +
							this.planes[i].y * this.planes[i].y +
							this.planes[i].z * this.planes[i].z);
			this.planes[i].div(length);
		}
	}

	public boolean isChunkHidden(final int chunkX, final int chunkZ) {
		final float minX = chunkX * Chunk.WIDTH;
		final float maxX = minX + Chunk.WIDTH;
		final float minZ = chunkZ * Chunk.DEPTH;
		final float maxZ = minZ + Chunk.DEPTH;

		return this.isBoxHidden(minX, minZ, maxX, maxZ);
	}

	private boolean isBoxHidden(final float minX, final float minZ, final float maxX, final float maxZ) {
		final float minY = 0.0f;
		final float maxY = Chunk.HEIGHT;
		for (int i = 0; i < 6; i++) {
			final Vector4f plane = this.planes[i];

			final float px = plane.x > 0 ? maxX : minX;
			final float py = plane.y > 0 ? maxY : minY;
			final float pz = plane.z > 0 ? maxZ : minZ;

			if (plane.x * px + plane.y * py + plane.z * pz + plane.w < 0)
				return true;
		}
		return false;
	}
}
