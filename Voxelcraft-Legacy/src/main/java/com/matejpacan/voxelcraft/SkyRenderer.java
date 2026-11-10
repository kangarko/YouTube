package com.matejpacan.voxelcraft;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;

/**
 * Renders a sky dome/box for the background, including a visible sun disc.
 */
public class SkyRenderer {

	// Sky dome
	private final int skyVao;
	private final int skyVbo;
	private final int skyEbo;
	private final int skyIndexCount;

	// Sun billboard
	private final int sunVao;
	private final int sunVbo;

	public SkyRenderer() {
		// Create a simple sky dome (inverted sphere)
		final int segments = 64;
		final int rings = 32;

		final float[] vertices = new float[(segments + 1) * (rings + 1) * 3];
		final int[] indices = new int[segments * rings * 6];

		int vi = 0;
		for (int r = 0; r <= rings; r++) {
			final float phi = (float) (Math.PI * r / rings);
			final float y = (float) Math.cos(phi);
			final float ringRadius = (float) Math.sin(phi);

			for (int s = 0; s <= segments; s++) {
				final float theta = (float) (2 * Math.PI * s / segments);
				final float x = ringRadius * (float) Math.cos(theta);
				final float z = ringRadius * (float) Math.sin(theta);

				// Scale to large size
				vertices[vi++] = x * 1500;
				vertices[vi++] = y * 1500;
				vertices[vi++] = z * 1500;
			}
		}

		int ii = 0;
		for (int r = 0; r < rings; r++)
			for (int s = 0; s < segments; s++) {
				final int current = r * (segments + 1) + s;
				final int next = current + segments + 1;

				// Reversed winding for inside-out sphere
				indices[ii++] = current;
				indices[ii++] = current + 1;
				indices[ii++] = next;

				indices[ii++] = next;
				indices[ii++] = current + 1;
				indices[ii++] = next + 1;
			}

		this.skyIndexCount = ii;

		// Create VAO/VBO/EBO for sky dome
		this.skyVao = glGenVertexArrays();
		this.skyVbo = glGenBuffers();
		this.skyEbo = glGenBuffers();

		glBindVertexArray(this.skyVao);

		final FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.length);
		vertexBuffer.put(vertices).flip();
		glBindBuffer(GL_ARRAY_BUFFER, this.skyVbo);
		glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

		final IntBuffer indexBuffer = BufferUtils.createIntBuffer(indices.length);
		indexBuffer.put(indices).flip();
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, this.skyEbo);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);

		// Position attribute only
		glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);
		glEnableVertexAttribArray(0);

		glBindVertexArray(0);

		// Create sun billboard quad
		this.sunVao = glGenVertexArrays();
		this.sunVbo = glGenBuffers();

		// Simple quad vertices (position + UV)
		final float[] sunQuad = {
				// Position (3) + UV (2)
				-1, -1, 0, 0, 0,
				1, -1, 0, 1, 0,
				1, 1, 0, 1, 1,
				-1, -1, 0, 0, 0,
				1, 1, 0, 1, 1,
				-1, 1, 0, 0, 1,
		};

		glBindVertexArray(this.sunVao);
		glBindBuffer(GL_ARRAY_BUFFER, this.sunVbo);

		final FloatBuffer sunBuffer = BufferUtils.createFloatBuffer(sunQuad.length);
		sunBuffer.put(sunQuad).flip();
		glBufferData(GL_ARRAY_BUFFER, sunBuffer, GL_STATIC_DRAW);

		// Position
		glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0);
		glEnableVertexAttribArray(0);
		// UV
		glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3L * Float.BYTES);
		glEnableVertexAttribArray(1);

		glBindVertexArray(0);
	}

	/**
	 * Render the sky dome.
	 */
	public void render() {
		glBindVertexArray(this.skyVao);
		glDrawElements(GL_TRIANGLES, this.skyIndexCount, GL_UNSIGNED_INT, 0);
		glBindVertexArray(0);
	}

	/**
	 * Render the sun billboard.
	 * Call this with an appropriate sun shader that handles billboarding.
	 */
	public void renderSun() {
		glBindVertexArray(this.sunVao);
		glDrawArrays(GL_TRIANGLES, 0, 6);
		glBindVertexArray(0);
	}

	/**
	 * Cleanup OpenGL resources.
	 */
	public void cleanup() {
		glDeleteVertexArrays(this.skyVao);
		glDeleteBuffers(this.skyVbo);
		glDeleteBuffers(this.skyEbo);
		glDeleteVertexArrays(this.sunVao);
		glDeleteBuffers(this.sunVbo);
	}
}
