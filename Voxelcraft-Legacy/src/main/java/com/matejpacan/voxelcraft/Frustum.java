package com.matejpacan.voxelcraft;

import org.joml.Matrix4f;
import org.joml.Vector4f;

public class Frustum {

	private final Vector4f[] planes = new Vector4f[6];
	private final Matrix4f viewProjection = new Matrix4f();

	public Frustum() {
		for (int i = 0; i < 6; i++)
			this.planes[i] = new Vector4f();
	}

	public void update(Matrix4f projection, Matrix4f view) {
		projection.mul(view, this.viewProjection);
		this.extractPlanes();
	}

	private void extractPlanes() {
		this.planes[0].set(
				this.viewProjection.m03() + this.viewProjection.m00(),
				this.viewProjection.m13() + this.viewProjection.m10(),
				this.viewProjection.m23() + this.viewProjection.m20(),
				this.viewProjection.m33() + this.viewProjection.m30());
		this.normalizePlane(this.planes[0]);

		this.planes[1].set(
				this.viewProjection.m03() - this.viewProjection.m00(),
				this.viewProjection.m13() - this.viewProjection.m10(),
				this.viewProjection.m23() - this.viewProjection.m20(),
				this.viewProjection.m33() - this.viewProjection.m30());
		this.normalizePlane(this.planes[1]);

		this.planes[2].set(
				this.viewProjection.m03() + this.viewProjection.m01(),
				this.viewProjection.m13() + this.viewProjection.m11(),
				this.viewProjection.m23() + this.viewProjection.m21(),
				this.viewProjection.m33() + this.viewProjection.m31());
		this.normalizePlane(this.planes[2]);

		this.planes[3].set(
				this.viewProjection.m03() - this.viewProjection.m01(),
				this.viewProjection.m13() - this.viewProjection.m11(),
				this.viewProjection.m23() - this.viewProjection.m21(),
				this.viewProjection.m33() - this.viewProjection.m31());
		this.normalizePlane(this.planes[3]);

		this.planes[4].set(
				this.viewProjection.m03() + this.viewProjection.m02(),
				this.viewProjection.m13() + this.viewProjection.m12(),
				this.viewProjection.m23() + this.viewProjection.m22(),
				this.viewProjection.m33() + this.viewProjection.m32());
		this.normalizePlane(this.planes[4]);

		this.planes[5].set(
				this.viewProjection.m03() - this.viewProjection.m02(),
				this.viewProjection.m13() - this.viewProjection.m12(),
				this.viewProjection.m23() - this.viewProjection.m22(),
				this.viewProjection.m33() - this.viewProjection.m32());
		this.normalizePlane(this.planes[5]);
	}

	private void normalizePlane(Vector4f plane) {
		final float length = (float) Math.sqrt(plane.x * plane.x + plane.y * plane.y + plane.z * plane.z);
		if (length > 0.0001f) {
			plane.x /= length;
			plane.y /= length;
			plane.z /= length;
			plane.w /= length;
		}
	}

	public boolean isChunkInFrustum(int chunkX, int chunkZ, int chunkSize, int chunkHeight) {
		final float minX = chunkX * chunkSize;
		final float maxX = minX + chunkSize;
		final float minY = 0;
		final float maxY = chunkHeight;
		final float minZ = chunkZ * chunkSize;
		final float maxZ = minZ + chunkSize;

		return this.isAABBInFrustum(minX, minY, minZ, maxX, maxY, maxZ);
	}

	public boolean isAABBInFrustum(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
		for (int i = 0; i < 6; i++) {
			final Vector4f plane = this.planes[i];

			final float px = plane.x > 0 ? maxX : minX;
			final float py = plane.y > 0 ? maxY : minY;
			final float pz = plane.z > 0 ? maxZ : minZ;

			if (plane.x * px + plane.y * py + plane.z * pz + plane.w < 0)
				return false;
		}
		return true;
	}

	public boolean isSphereInFrustum(float x, float y, float z, float radius) {
		for (int i = 0; i < 6; i++) {
			final Vector4f plane = this.planes[i];
			final float distance = plane.x * x + plane.y * y + plane.z * z + plane.w;
			if (distance < -radius)
				return false;
		}
		return true;
	}
}
