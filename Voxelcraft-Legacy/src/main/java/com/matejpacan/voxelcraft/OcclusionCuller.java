package com.matejpacan.voxelcraft;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class OcclusionCuller {

	private static final int BUFFER_WIDTH = 64;
	private static final int BUFFER_HEIGHT = 64;

	private final float[] depthBuffer = new float[BUFFER_WIDTH * BUFFER_HEIGHT];
	private final Matrix4f viewProjection = new Matrix4f();
	private final Vector4f tempVec = new Vector4f();

	public void beginFrame(Matrix4f projection, Matrix4f view) {
		projection.mul(view, this.viewProjection);

		for (int i = 0; i < this.depthBuffer.length; i++)
			this.depthBuffer[i] = 1.0f;
	}

	public List<Chunk> cullAndSort(Iterable<Chunk> chunks, Vector3f cameraPos, Frustum frustum) {
		final List<ChunkWithDistance> visibleChunks = new ArrayList<>();

		for (final Chunk chunk : chunks) {
			if (!chunk.isReadyToRender())
				continue;

			final int cx = chunk.getChunkX();
			final int cz = chunk.getChunkZ();

			if (!frustum.isChunkInFrustum(cx, cz, Chunk.SIZE, Chunk.HEIGHT))
				continue;

			final float centerX = cx * Chunk.SIZE + Chunk.SIZE * 0.5f;
			final float centerZ = cz * Chunk.SIZE + Chunk.SIZE * 0.5f;
			final float dx = centerX - cameraPos.x;
			final float dz = centerZ - cameraPos.z;
			final float distSq = dx * dx + dz * dz;

			if (distSq < 64 * 64) {
				visibleChunks.add(new ChunkWithDistance(chunk, distSq));
				continue;
			}

			if (!this.isChunkOccluded(chunk, cameraPos)) {
				visibleChunks.add(new ChunkWithDistance(chunk, distSq));
				this.rasterizeChunkToDepthBuffer(chunk);
			}
		}

		visibleChunks.sort(Comparator.comparingDouble(c -> c.distanceSq));

		final List<Chunk> result = new ArrayList<>(visibleChunks.size());
		for (final ChunkWithDistance cwd : visibleChunks)
			result.add(cwd.chunk);
		return result;
	}

	private boolean isChunkOccluded(Chunk chunk, Vector3f cameraPos) {
		final float minX = chunk.getChunkX() * Chunk.SIZE;
		final float maxX = minX + Chunk.SIZE;
		final float minZ = chunk.getChunkZ() * Chunk.SIZE;
		final float maxZ = minZ + Chunk.SIZE;

		final float minY = 0;
		final float maxY = Chunk.HEIGHT;

		final float[][] corners = {
				{ minX, minY, minZ },
				{ maxX, minY, minZ },
				{ minX, maxY, minZ },
				{ maxX, maxY, minZ },
				{ minX, minY, maxZ },
				{ maxX, minY, maxZ },
				{ minX, maxY, maxZ },
				{ maxX, maxY, maxZ }
		};

		float minScreenX = Float.MAX_VALUE;
		float maxScreenX = Float.MIN_VALUE;
		float minScreenY = Float.MAX_VALUE;
		float maxScreenY = Float.MIN_VALUE;
		float minDepth = Float.MAX_VALUE;

		boolean anyVisible = false;

		for (final float[] corner : corners) {
			this.tempVec.set(corner[0], corner[1], corner[2], 1.0f);
			this.viewProjection.transform(this.tempVec);

			if (this.tempVec.w <= 0)
				return false;

			final float invW = 1.0f / this.tempVec.w;
			final float ndcX = this.tempVec.x * invW;
			final float ndcY = this.tempVec.y * invW;
			final float ndcZ = this.tempVec.z * invW;

			if (ndcX >= -1 && ndcX <= 1 && ndcY >= -1 && ndcY <= 1)
				anyVisible = true;

			final float screenX = (ndcX + 1.0f) * 0.5f * BUFFER_WIDTH;
			final float screenY = (1.0f - ndcY) * 0.5f * BUFFER_HEIGHT;
			final float depth = (ndcZ + 1.0f) * 0.5f;

			minScreenX = Math.min(minScreenX, screenX);
			maxScreenX = Math.max(maxScreenX, screenX);
			minScreenY = Math.min(minScreenY, screenY);
			maxScreenY = Math.max(maxScreenY, screenY);
			minDepth = Math.min(minDepth, depth);
		}

		if (!anyVisible)
			return false;

		final int x0 = Math.max(0, (int) minScreenX);
		final int x1 = Math.min(BUFFER_WIDTH - 1, (int) maxScreenX);
		final int y0 = Math.max(0, (int) minScreenY);
		final int y1 = Math.min(BUFFER_HEIGHT - 1, (int) maxScreenY);

		for (int y = y0; y <= y1; y++)
			for (int x = x0; x <= x1; x++) {
				final int idx = y * BUFFER_WIDTH + x;
				if (minDepth < this.depthBuffer[idx])
					return false;
			}

		return true;
	}

	private void rasterizeChunkToDepthBuffer(Chunk chunk) {
		final float minX = chunk.getChunkX() * Chunk.SIZE;
		final float maxX = minX + Chunk.SIZE;
		final float minZ = chunk.getChunkZ() * Chunk.SIZE;
		final float maxZ = minZ + Chunk.SIZE;

		final float surfaceY = Chunk.SEA_LEVEL + 30;

		final float[][] topCorners = {
				{ minX, surfaceY, minZ },
				{ maxX, surfaceY, minZ },
				{ maxX, surfaceY, maxZ },
				{ minX, surfaceY, maxZ }
		};

		this.rasterizeQuadToDepthBuffer(topCorners);
	}

	private void rasterizeQuadToDepthBuffer(float[][] corners) {
		final float[] screenX = new float[4];
		final float[] screenY = new float[4];
		final float[] screenZ = new float[4];
		boolean allValid = true;

		for (int i = 0; i < 4; i++) {
			this.tempVec.set(corners[i][0], corners[i][1], corners[i][2], 1.0f);
			this.viewProjection.transform(this.tempVec);

			if (this.tempVec.w <= 0) {
				allValid = false;
				break;
			}

			final float invW = 1.0f / this.tempVec.w;
			final float ndcX = this.tempVec.x * invW;
			final float ndcY = this.tempVec.y * invW;
			final float ndcZ = this.tempVec.z * invW;

			screenX[i] = (ndcX + 1.0f) * 0.5f * BUFFER_WIDTH;
			screenY[i] = (1.0f - ndcY) * 0.5f * BUFFER_HEIGHT;
			screenZ[i] = (ndcZ + 1.0f) * 0.5f;
		}

		if (!allValid)
			return;

		this.rasterizeTriangle(
				screenX[0], screenY[0], screenZ[0],
				screenX[1], screenY[1], screenZ[1],
				screenX[2], screenY[2], screenZ[2]);
		this.rasterizeTriangle(
				screenX[0], screenY[0], screenZ[0],
				screenX[2], screenY[2], screenZ[2],
				screenX[3], screenY[3], screenZ[3]);
	}

	private void rasterizeTriangle(
			float x0, float y0, float z0,
			float x1, float y1, float z1,
			float x2, float y2, float z2) {
		final int minX = (int) Math.max(0, Math.min(x0, Math.min(x1, x2)));
		final int maxX = (int) Math.min(BUFFER_WIDTH - 1, Math.max(x0, Math.max(x1, x2)));
		final int minY = (int) Math.max(0, Math.min(y0, Math.min(y1, y2)));
		final int maxY = (int) Math.min(BUFFER_HEIGHT - 1, Math.max(y0, Math.max(y1, y2)));

		final float denom = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2);
		if (Math.abs(denom) < 0.0001f)
			return;

		final float invDenom = 1.0f / denom;

		for (int py = minY; py <= maxY; py++)
			for (int px = minX; px <= maxX; px++) {
				final float w0 = ((y1 - y2) * (px - x2) + (x2 - x1) * (py - y2)) * invDenom;
				final float w1 = ((y2 - y0) * (px - x2) + (x0 - x2) * (py - y2)) * invDenom;
				final float w2 = 1.0f - w0 - w1;

				if (w0 >= 0 && w1 >= 0 && w2 >= 0) {
					final float depth = w0 * z0 + w1 * z1 + w2 * z2;
					final int idx = py * BUFFER_WIDTH + px;
					if (depth < this.depthBuffer[idx])
						this.depthBuffer[idx] = depth;
				}
			}
	}

	private static class ChunkWithDistance {
		final Chunk chunk;
		final float distanceSq;

		ChunkWithDistance(Chunk chunk, float distanceSq) {
			this.chunk = chunk;
			this.distanceSq = distanceSq;
		}
	}
}
