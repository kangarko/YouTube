package com.matejpacan.voxelcraft;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

import java.util.ArrayList;
import java.util.List;

public class ChunkBatchRenderer {

	private int lastBoundVao = -1;
	private int drawCallsThisFrame = 0;
	private int chunksRenderedThisFrame = 0;
	private int stateChangesThisFrame = 0;

	public void beginFrame() {
		this.lastBoundVao = -1;
		this.drawCallsThisFrame = 0;
		this.chunksRenderedThisFrame = 0;
		this.stateChangesThisFrame = 0;
	}

	public void renderOpaqueChunks(List<Chunk> chunks, ShaderProgram shader) {
		if (chunks.isEmpty())
			return;

		final List<RenderBatch> batches = this.createBatches(chunks, false);

		for (final RenderBatch batch : batches)
			for (final Chunk chunk : batch.chunks) {
				shader.setFloat("uChunkOpacity", chunk.getFadeProgress());
				this.renderChunkOpaque(chunk);
			}
	}

	public void renderWaterChunks(List<Chunk> chunks, ShaderProgram shader) {
		if (chunks.isEmpty())
			return;

		final List<Chunk> waterChunks = new ArrayList<>();
		for (final Chunk chunk : chunks)
			if (chunk.hasWaterToRender())
				waterChunks.add(chunk);

		if (waterChunks.isEmpty())
			return;

		final List<RenderBatch> batches = this.createBatches(waterChunks, true);

		for (final RenderBatch batch : batches)
			for (final Chunk chunk : batch.chunks) {
				shader.setFloat("uChunkOpacity", chunk.getFadeProgress());
				this.renderChunkWater(chunk);
			}
	}

	private void renderChunkOpaque(Chunk chunk) {
		final int vao = chunk.getOpaqueVao();
		if (vao != this.lastBoundVao) {
			glBindVertexArray(vao);
			this.lastBoundVao = vao;
			this.stateChangesThisFrame++;
		}
		glDrawElements(GL_TRIANGLES, chunk.getOpaqueIndexCount(), GL_UNSIGNED_INT, 0);
		this.drawCallsThisFrame++;
		this.chunksRenderedThisFrame++;
	}

	private void renderChunkWater(Chunk chunk) {
		final int vao = chunk.getWaterVao();
		if (vao != this.lastBoundVao) {
			glBindVertexArray(vao);
			this.lastBoundVao = vao;
			this.stateChangesThisFrame++;
		}
		glDrawElements(GL_TRIANGLES, chunk.getWaterIndexCount(), GL_UNSIGNED_INT, 0);
		this.drawCallsThisFrame++;
	}

	private List<RenderBatch> createBatches(List<Chunk> chunks, boolean waterPass) {
		final List<RenderBatch> batches = new ArrayList<>();

		RenderBatch currentBatch = new RenderBatch();
		batches.add(currentBatch);

		for (final Chunk chunk : chunks) {
			if (currentBatch.chunks.size() >= 64) {
				currentBatch = new RenderBatch();
				batches.add(currentBatch);
			}
			currentBatch.chunks.add(chunk);
		}

		return batches;
	}

	public void endFrame() {
		if (this.lastBoundVao != -1) {
			glBindVertexArray(0);
			this.lastBoundVao = -1;
		}
	}

	public int getDrawCallCount() {
		return this.drawCallsThisFrame;
	}

	public int getChunksRendered() {
		return this.chunksRenderedThisFrame;
	}

	public int getStateChangeCount() {
		return this.stateChangesThisFrame;
	}

	private static class RenderBatch {
		final List<Chunk> chunks = new ArrayList<>();
	}
}
