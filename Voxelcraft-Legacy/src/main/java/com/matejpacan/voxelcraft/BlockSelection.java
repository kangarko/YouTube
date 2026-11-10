package com.matejpacan.voxelcraft;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glLineWidth;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glBufferSubData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUniform3f;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3i;

import lombok.Getter;

public class BlockSelection {

	private static final float REACH_DISTANCE = 5.0f;
	private static final float RAY_STEP = 0.05f;
	private static final float OFFSET = 0.003f;
	private static final float LINE_WIDTH = 3.0f;

	private static final int FACE_TOP = 0;
	private static final int FACE_BOTTOM = 1;
	private static final int FACE_FRONT = 2;
	private static final int FACE_BACK = 3;
	private static final int FACE_LEFT = 4;
	private static final int FACE_RIGHT = 5;

	@Getter
	private Vector3i selectedBlock = null;
	@Getter
	private Vector3i adjacentBlock = null;

	private int vao;
	private int vbo;
	private int shaderProgram;
	private int uProjection;
	private int uView;
	private int uBlockPos;
	private int currentVertexCount = 0;

	public BlockSelection() {
		this.createShader();
		this.createBuffers();
	}

	private void createShader() {
		final String vertexSource = """
									#version 410 core
									layout(location = 0) in vec3 aPosition;
									uniform mat4 uProjection;
									uniform mat4 uView;
									uniform vec3 uBlockPos;
									void main() {
									    vec3 worldPos = aPosition + uBlockPos;
									    gl_Position = uProjection * uView * vec4(worldPos, 1.0);
									}
									""";

		final String fragmentSource = """
										#version 410 core
										layout(location = 0) out vec4 FragColor;
										layout(location = 1) out vec4 GBuffer;
										void main() {
										    FragColor = vec4(0.1, 0.1, 0.1, 0.8);
										    GBuffer = vec4(0.0, 0.0, 0.0, 1.0);
										}
										""";

		final int vertexShader = glCreateShader(GL_VERTEX_SHADER);
		glShaderSource(vertexShader, vertexSource);
		glCompileShader(vertexShader);

		final int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(fragmentShader, fragmentSource);
		glCompileShader(fragmentShader);

		this.shaderProgram = glCreateProgram();
		glAttachShader(this.shaderProgram, vertexShader);
		glAttachShader(this.shaderProgram, fragmentShader);
		glLinkProgram(this.shaderProgram);

		glDeleteShader(vertexShader);
		glDeleteShader(fragmentShader);

		this.uProjection = glGetUniformLocation(this.shaderProgram, "uProjection");
		this.uView = glGetUniformLocation(this.shaderProgram, "uView");
		this.uBlockPos = glGetUniformLocation(this.shaderProgram, "uBlockPos");
	}

	private void createBuffers() {
		this.vao = glGenVertexArrays();
		this.vbo = glGenBuffers();

		glBindVertexArray(this.vao);
		glBindBuffer(GL_ARRAY_BUFFER, this.vbo);
		glBufferData(GL_ARRAY_BUFFER, 24 * 3 * Float.BYTES, GL_DYNAMIC_DRAW);

		glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		glEnableVertexAttribArray(0);

		glBindVertexArray(0);
	}

	private void updateMesh(boolean[] faceVisible) {
		final List<Float> vertices = new ArrayList<>();
		final float o = OFFSET;

		if (faceVisible[FACE_BOTTOM] || faceVisible[FACE_BACK])
			this.addEdge(vertices, -o, -o, -o, 1 + o, -o, -o);
		if (faceVisible[FACE_BOTTOM] || faceVisible[FACE_FRONT])
			this.addEdge(vertices, -o, -o, 1 + o, 1 + o, -o, 1 + o);
		if (faceVisible[FACE_BOTTOM] || faceVisible[FACE_LEFT])
			this.addEdge(vertices, -o, -o, -o, -o, -o, 1 + o);
		if (faceVisible[FACE_BOTTOM] || faceVisible[FACE_RIGHT])
			this.addEdge(vertices, 1 + o, -o, -o, 1 + o, -o, 1 + o);

		if (faceVisible[FACE_TOP] || faceVisible[FACE_BACK])
			this.addEdge(vertices, -o, 1 + o, -o, 1 + o, 1 + o, -o);
		if (faceVisible[FACE_TOP] || faceVisible[FACE_FRONT])
			this.addEdge(vertices, -o, 1 + o, 1 + o, 1 + o, 1 + o, 1 + o);
		if (faceVisible[FACE_TOP] || faceVisible[FACE_LEFT])
			this.addEdge(vertices, -o, 1 + o, -o, -o, 1 + o, 1 + o);
		if (faceVisible[FACE_TOP] || faceVisible[FACE_RIGHT])
			this.addEdge(vertices, 1 + o, 1 + o, -o, 1 + o, 1 + o, 1 + o);

		if (faceVisible[FACE_BACK] || faceVisible[FACE_LEFT])
			this.addEdge(vertices, -o, -o, -o, -o, 1 + o, -o);
		if (faceVisible[FACE_BACK] || faceVisible[FACE_RIGHT])
			this.addEdge(vertices, 1 + o, -o, -o, 1 + o, 1 + o, -o);
		if (faceVisible[FACE_FRONT] || faceVisible[FACE_LEFT])
			this.addEdge(vertices, -o, -o, 1 + o, -o, 1 + o, 1 + o);
		if (faceVisible[FACE_FRONT] || faceVisible[FACE_RIGHT])
			this.addEdge(vertices, 1 + o, -o, 1 + o, 1 + o, 1 + o, 1 + o);

		final float[] vertexArray = new float[vertices.size()];
		for (int i = 0; i < vertices.size(); i++)
			vertexArray[i] = vertices.get(i);

		this.currentVertexCount = vertices.size() / 3;

		if (this.currentVertexCount > 0) {
			glBindBuffer(GL_ARRAY_BUFFER, this.vbo);
			final FloatBuffer buffer = org.lwjgl.BufferUtils.createFloatBuffer(vertexArray.length);
			buffer.put(vertexArray).flip();
			glBufferSubData(GL_ARRAY_BUFFER, 0, buffer);
		}
	}

	private void addEdge(List<Float> vertices, float x1, float y1, float z1, float x2, float y2, float z2) {
		vertices.add(x1);
		vertices.add(y1);
		vertices.add(z1);
		vertices.add(x2);
		vertices.add(y2);
		vertices.add(z2);
	}

	private boolean isTransparent(BlockType block) {
		return block == null || block == BlockType.AIR || block == BlockType.WATER || block.isPlant();
	}

	public void update(World world, Vector3f cameraPos, Vector3f cameraFront) {
		this.selectedBlock = null;
		this.adjacentBlock = null;

		final Vector3f rayPos = new Vector3f(cameraPos);
		final Vector3f rayDir = new Vector3f(cameraFront).normalize();
		final Vector3f step = new Vector3f(rayDir).mul(RAY_STEP);

		int lastAirX = Integer.MIN_VALUE;
		int lastAirY = Integer.MIN_VALUE;
		int lastAirZ = Integer.MIN_VALUE;

		float distance = 0;
		while (distance < REACH_DISTANCE) {
			final int bx = (int) Math.floor(rayPos.x);
			final int by = (int) Math.floor(rayPos.y);
			final int bz = (int) Math.floor(rayPos.z);

			final BlockType block = world.getBlockAt(bx, by, bz);

			if (block != BlockType.AIR && block != BlockType.WATER) {
				this.selectedBlock = new Vector3i(bx, by, bz);
				if (lastAirX != Integer.MIN_VALUE)
					this.adjacentBlock = new Vector3i(lastAirX, lastAirY, lastAirZ);
				return;
			}

			lastAirX = bx;
			lastAirY = by;
			lastAirZ = bz;

			rayPos.add(step);
			distance += RAY_STEP;
		}
	}

	public void render(World world, Matrix4f projection, Matrix4f view) {
		if (this.selectedBlock == null)
			return;

		final int bx = this.selectedBlock.x;
		final int by = this.selectedBlock.y;
		final int bz = this.selectedBlock.z;

		final boolean[] faceVisible = new boolean[6];
		faceVisible[FACE_TOP] = this.isTransparent(world.getBlockAt(bx, by + 1, bz));
		faceVisible[FACE_BOTTOM] = this.isTransparent(world.getBlockAt(bx, by - 1, bz));
		faceVisible[FACE_FRONT] = this.isTransparent(world.getBlockAt(bx, by, bz + 1));
		faceVisible[FACE_BACK] = this.isTransparent(world.getBlockAt(bx, by, bz - 1));
		faceVisible[FACE_LEFT] = this.isTransparent(world.getBlockAt(bx - 1, by, bz));
		faceVisible[FACE_RIGHT] = this.isTransparent(world.getBlockAt(bx + 1, by, bz));

		this.updateMesh(faceVisible);

		if (this.currentVertexCount == 0)
			return;

		glUseProgram(this.shaderProgram);
		glUniformMatrix4fv(this.uProjection, false, projection.get(new float[16]));
		glUniformMatrix4fv(this.uView, false, view.get(new float[16]));
		glUniform3f(this.uBlockPos, this.selectedBlock.x, this.selectedBlock.y, this.selectedBlock.z);

		glBindVertexArray(this.vao);
		glLineWidth(LINE_WIDTH);
		glDrawArrays(GL_LINES, 0, this.currentVertexCount);
		glBindVertexArray(0);

		glUseProgram(0);
	}

	public boolean breakBlock(World world) {
		if (this.selectedBlock == null)
			return false;

		final BlockType block = world.getBlockAt(this.selectedBlock.x, this.selectedBlock.y, this.selectedBlock.z);
		if (block == BlockType.BEDROCK)
			return false;

		world.setBlockAt(this.selectedBlock.x, this.selectedBlock.y, this.selectedBlock.z, BlockType.AIR);
		return true;
	}

	public boolean placeBlock(World world, BlockType blockType, Vector3f playerPos, Vector3f cameraFront) {
		if ((this.adjacentBlock == null) || blockType == null || blockType == BlockType.AIR)
			return false;
		if (cameraFront == null)
			throw new IllegalArgumentException("Camera direction missing");

		final int px = (int) Math.floor(playerPos.x);
		final int py = (int) Math.floor(playerPos.y);
		final int pz = (int) Math.floor(playerPos.z);

		if (this.adjacentBlock.x == px && this.adjacentBlock.z == pz &&
				(this.adjacentBlock.y == py || this.adjacentBlock.y == py - 1))
			return false;

		final BlockType existing = world.getBlockAt(this.adjacentBlock.x, this.adjacentBlock.y, this.adjacentBlock.z);
		if (existing != BlockType.AIR && existing != BlockType.WATER)
			return false;

		int metadata = 0;
		if (this.isLog(blockType))
			metadata = this.determineLogAxis();
		else if (this.requiresFacing(blockType))
			metadata = this.determineFacing(cameraFront);

		world.setBlockAt(this.adjacentBlock.x, this.adjacentBlock.y, this.adjacentBlock.z, blockType, metadata);
		return true;
	}

	public void cleanup() {
		glDeleteBuffers(this.vbo);
		glDeleteVertexArrays(this.vao);
		glDeleteProgram(this.shaderProgram);
	}

	private boolean isLog(BlockType type) {
		return type == BlockType.OAK_LOG || type == BlockType.SPRUCE_LOG
				|| type == BlockType.BIRCH_LOG || type == BlockType.JUNGLE_LOG;
	}

	private boolean requiresFacing(BlockType type) {
		return type == BlockType.FURNACE || type == BlockType.FURNACE_LIT || type == BlockType.DISPENSER;
	}

	private int determineLogAxis() {
		if (this.selectedBlock == null)
			throw new IllegalStateException("No selected block to derive axis");
		final Vector3i normal = new Vector3i(this.adjacentBlock).sub(this.selectedBlock);
		final boolean hasX = normal.x != 0;
		final boolean hasY = normal.y != 0;
		final boolean hasZ = normal.z != 0;
		final int components = (hasX ? 1 : 0) + (hasY ? 1 : 0) + (hasZ ? 1 : 0);
		if (components != 1)
			throw new IllegalStateException("Invalid placement normal " + normal);
		if (hasX)
			return 1;
		if (hasZ)
			return 2;
		return 0;
	}

	private int determineFacing(Vector3f cameraFront) {
		final Vector3f horizontal = new Vector3f(cameraFront.x, 0, cameraFront.z);
		final float lenSq = horizontal.lengthSquared();
		if (lenSq < 1e-6f)
			throw new IllegalStateException("Cannot derive facing from zero vector");
		horizontal.normalize();
		if (Math.abs(horizontal.x) >= Math.abs(horizontal.z))
			return horizontal.x >= 0 ? 3 : 2;
		return horizontal.z >= 0 ? 0 : 1;
	}
}
