package com.matejpacan.voxelcraft;

import static org.lwjgl.opengl.GL11.GL_BACK;
import static org.lwjgl.opengl.GL11.GL_CCW;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.glCullFace;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glFrontFace;

import org.joml.Matrix4f;

public class HandRenderer {

	private final EntityRenderer renderer;

	public HandRenderer(TextureAtlas textureAtlas) {
		if (textureAtlas == null)
			throw new IllegalArgumentException("Texture atlas missing for hand renderer");
		this.renderer = new EntityRenderer(textureAtlas);
	}

	public void render(BlockType block, Matrix4f projection, float yaw, float pitch, float swing, float bob,
			float impulse) {
		if (block == null || block == BlockType.AIR)
			return;
		if (projection == null)
			throw new IllegalArgumentException("Projection missing for hand render");

		glDisable(GL_DEPTH_TEST);
		glEnable(GL_CULL_FACE);
		glCullFace(GL_BACK);
		glFrontFace(GL_CCW);

		final Matrix4f view = new Matrix4f().identity();
		final float swingOffsetX = swing * 0.03f;
		final float swingOffsetY = bob * 0.04f;
		final float swingOffsetZ = -impulse * 0.1f;

		final Matrix4f model = new Matrix4f()
				.translate(0.65f + swingOffsetX, -0.7f + swingOffsetY, -1.2f + swingOffsetZ)
				.rotateY((float) Math.toRadians(45))
				.rotateX((float) Math.toRadians(impulse * 20))
				.scale(0.55f);

		if (block.isPlant())
			this.renderer.renderPlant(block, projection, view, model);
		else
			this.renderer.renderBlockWithCulling(block, projection, view, model, null, false, false);

		glEnable(GL_DEPTH_TEST);
	}

	public void cleanup() {
		this.renderer.cleanup();
	}
}
