package com.matejpacan.voxelcraft;

import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDepthMask;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glBufferSubData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20.glGetProgrami;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderi;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

public class ParticleSystem {

	private static final int MAX_PARTICLES = 500;
	private static final float PARTICLE_SIZE = 0.08f;
	private static final float GRAVITY = -15.0f;
	private static final float LIFETIME = 1.2f;
	private static final int PARTICLES_PER_BLOCK = 20;

	private final List<Particle> particles = new ArrayList<>();
	private final Random random = new Random();
	private final TextureAtlas textureAtlas;

	private int vao;
	private int vbo;
	private int shaderProgram;
	private int uProjection;
	private int uView;
	private int uTexture;

	public ParticleSystem(TextureAtlas textureAtlas) {
		this.textureAtlas = textureAtlas;
		this.createShader();
		this.createBuffers();
	}

	private void createShader() {
		final String vertexSource = """
									#version 410 core
									layout(location = 0) in vec3 aPosition;
									layout(location = 1) in vec2 aTexCoord;
									layout(location = 2) in float aAlpha;

									uniform mat4 uProjection;
									uniform mat4 uView;

									out vec2 vTexCoord;
									out float vAlpha;

									void main() {
									    vTexCoord = aTexCoord;
									    vAlpha = aAlpha;
									    gl_Position = uProjection * uView * vec4(aPosition, 1.0);
									}
									""";

		final String fragmentSource = """
										#version 410 core
										in vec2 vTexCoord;
										in float vAlpha;

										uniform sampler2D uTexture;

										layout(location = 0) out vec4 FragColor;
										layout(location = 1) out vec4 GBuffer;

										void main() {
										    vec4 texColor = texture(uTexture, vTexCoord);
										    if (texColor.a < 0.1)
										        discard;
										    FragColor = vec4(texColor.rgb, texColor.a * vAlpha);
										    GBuffer = vec4(0.0, 0.0, 0.0, 1.0);
										}
										""";

		final int vertexShader = glCreateShader(GL_VERTEX_SHADER);
		glShaderSource(vertexShader, vertexSource);
		glCompileShader(vertexShader);
		if (glGetShaderi(vertexShader, GL_COMPILE_STATUS) == GL_FALSE)
			throw new RuntimeException("Particle vertex shader failed: " + glGetShaderInfoLog(vertexShader));

		final int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(fragmentShader, fragmentSource);
		glCompileShader(fragmentShader);
		if (glGetShaderi(fragmentShader, GL_COMPILE_STATUS) == GL_FALSE)
			throw new RuntimeException("Particle fragment shader failed: " + glGetShaderInfoLog(fragmentShader));

		this.shaderProgram = glCreateProgram();
		glAttachShader(this.shaderProgram, vertexShader);
		glAttachShader(this.shaderProgram, fragmentShader);
		glLinkProgram(this.shaderProgram);
		if (glGetProgrami(this.shaderProgram, GL_LINK_STATUS) == GL_FALSE)
			throw new RuntimeException("Particle shader link failed: " + glGetProgramInfoLog(this.shaderProgram));

		glDeleteShader(vertexShader);
		glDeleteShader(fragmentShader);

		this.uProjection = glGetUniformLocation(this.shaderProgram, "uProjection");
		this.uView = glGetUniformLocation(this.shaderProgram, "uView");
		this.uTexture = glGetUniformLocation(this.shaderProgram, "uTexture");
	}

	private void createBuffers() {
		this.vao = glGenVertexArrays();
		this.vbo = glGenBuffers();

		glBindVertexArray(this.vao);
		glBindBuffer(GL_ARRAY_BUFFER, this.vbo);

		final int floatsPerVertex = 6;
		final int verticesPerParticle = 6;
		glBufferData(GL_ARRAY_BUFFER, (long) MAX_PARTICLES * verticesPerParticle * floatsPerVertex * Float.BYTES,
				GL_DYNAMIC_DRAW);

		glVertexAttribPointer(0, 3, GL_FLOAT, false, floatsPerVertex * Float.BYTES, 0);
		glEnableVertexAttribArray(0);
		glVertexAttribPointer(1, 2, GL_FLOAT, false, floatsPerVertex * Float.BYTES, 3 * Float.BYTES);
		glEnableVertexAttribArray(1);
		glVertexAttribPointer(2, 1, GL_FLOAT, false, floatsPerVertex * Float.BYTES, 5 * Float.BYTES);
		glEnableVertexAttribArray(2);

		glBindVertexArray(0);
	}

	public void spawnBlockBreakParticles(BlockType blockType, int x, int y, int z) {
		if (blockType == BlockType.AIR || blockType == BlockType.WATER)
			return;

		int[] tile = blockType.getSideTile();
		if (tile == null)
			tile = blockType.getTopTile();
		if (tile == null)
			return;

		final float[] uv = this.textureAtlas.getTileUV(tile[0], tile[1]);

		final int count = Math.min(PARTICLES_PER_BLOCK, MAX_PARTICLES - this.particles.size());

		for (int i = 0; i < count; i++) {
			final float px = x + 0.2f + this.random.nextFloat() * 0.6f;
			final float py = y + 0.2f + this.random.nextFloat() * 0.6f;
			final float pz = z + 0.2f + this.random.nextFloat() * 0.6f;

			final float vx = (this.random.nextFloat() - 0.5f) * 4.0f;
			final float vy = this.random.nextFloat() * 5.0f + 2.0f;
			final float vz = (this.random.nextFloat() - 0.5f) * 4.0f;

			final float subU1 = uv[0] + this.random.nextFloat() * (uv[2] - uv[0]) * 0.5f;
			final float subV1 = uv[1] + this.random.nextFloat() * (uv[3] - uv[1]) * 0.5f;
			final float subU2 = subU1 + (uv[2] - uv[0]) * 0.25f;
			final float subV2 = subV1 + (uv[3] - uv[1]) * 0.25f;

			final float size = PARTICLE_SIZE * (0.7f + this.random.nextFloat() * 0.6f);
			final float lifetime = LIFETIME * (0.6f + this.random.nextFloat() * 0.4f);

			this.particles.add(new Particle(px, py, pz, vx, vy, vz, subU1, subV1, subU2, subV2, size, lifetime));
		}
	}

	public void update(float deltaTime) {
		final Iterator<Particle> it = this.particles.iterator();
		while (it.hasNext()) {
			final Particle p = it.next();
			p.age += deltaTime;

			if (p.age >= p.lifetime) {
				it.remove();
				continue;
			}

			p.vy += GRAVITY * deltaTime;

			p.x += p.vx * deltaTime;
			p.y += p.vy * deltaTime;
			p.z += p.vz * deltaTime;

			if (p.y < 0) {
				p.y = 0;
				p.vy *= -0.3f;
				p.vx *= 0.8f;
				p.vz *= 0.8f;
			}

			p.vx *= 1.0f - 2.0f * deltaTime;
			p.vz *= 1.0f - 2.0f * deltaTime;
		}
	}

	public void render(Matrix4f projection, Matrix4f view, Vector3f cameraPos) {
		if (this.particles.isEmpty())
			return;

		final int floatsPerVertex = 6;
		final int verticesPerParticle = 6;
		final FloatBuffer buffer = BufferUtils.createFloatBuffer(this.particles.size() * verticesPerParticle * floatsPerVertex);

		final Vector3f camRight = new Vector3f();
		final Vector3f camUp = new Vector3f();

		view.positiveX(camRight);
		view.positiveY(camUp);

		for (final Particle p : this.particles) {
			float alpha = 1.0f - p.age / p.lifetime;
			alpha = alpha * alpha;

			final float halfSize = p.size * 0.5f;

			final float x0 = p.x - camRight.x * halfSize - camUp.x * halfSize;
			final float y0 = p.y - camRight.y * halfSize - camUp.y * halfSize;
			final float z0 = p.z - camRight.z * halfSize - camUp.z * halfSize;

			final float x1 = p.x + camRight.x * halfSize - camUp.x * halfSize;
			final float y1 = p.y + camRight.y * halfSize - camUp.y * halfSize;
			final float z1 = p.z + camRight.z * halfSize - camUp.z * halfSize;

			final float x2 = p.x + camRight.x * halfSize + camUp.x * halfSize;
			final float y2 = p.y + camRight.y * halfSize + camUp.y * halfSize;
			final float z2 = p.z + camRight.z * halfSize + camUp.z * halfSize;

			final float x3 = p.x - camRight.x * halfSize + camUp.x * halfSize;
			final float y3 = p.y - camRight.y * halfSize + camUp.y * halfSize;
			final float z3 = p.z - camRight.z * halfSize + camUp.z * halfSize;

			buffer.put(x0).put(y0).put(z0).put(p.u1).put(p.v1).put(alpha);
			buffer.put(x1).put(y1).put(z1).put(p.u2).put(p.v1).put(alpha);
			buffer.put(x2).put(y2).put(z2).put(p.u2).put(p.v2).put(alpha);

			buffer.put(x0).put(y0).put(z0).put(p.u1).put(p.v1).put(alpha);
			buffer.put(x2).put(y2).put(z2).put(p.u2).put(p.v2).put(alpha);
			buffer.put(x3).put(y3).put(z3).put(p.u1).put(p.v2).put(alpha);
		}

		buffer.flip();

		glBindBuffer(GL_ARRAY_BUFFER, this.vbo);
		glBufferSubData(GL_ARRAY_BUFFER, 0, buffer);

		glUseProgram(this.shaderProgram);
		glUniformMatrix4fv(this.uProjection, false, projection.get(new float[16]));
		glUniformMatrix4fv(this.uView, false, view.get(new float[16]));
		glUniform1i(this.uTexture, 0);

		this.textureAtlas.bind(0);

		glDisable(GL_CULL_FACE);
		glDepthMask(false);

		glBindVertexArray(this.vao);
		glDrawArrays(GL_TRIANGLES, 0, this.particles.size() * 6);
		glBindVertexArray(0);

		glDepthMask(true);
		glEnable(GL_CULL_FACE);
		glUseProgram(0);
	}

	public void cleanup() {
		glDeleteBuffers(this.vbo);
		glDeleteVertexArrays(this.vao);
		glDeleteProgram(this.shaderProgram);
	}

	private static class Particle {
		float x, y, z;
		float vx, vy, vz;
		float u1, v1, u2, v2;
		float size;
		float lifetime;
		float age;

		Particle(float x, float y, float z, float vx, float vy, float vz,
				float u1, float v1, float u2, float v2, float size, float lifetime) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.vx = vx;
			this.vy = vy;
			this.vz = vz;
			this.u1 = u1;
			this.v1 = v1;
			this.u2 = u2;
			this.v2 = v2;
			this.size = size;
			this.lifetime = lifetime;
			this.age = 0;
		}
	}
}
