package com.matejpacan.voxelcraft;

import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
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
import static org.lwjgl.opengl.GL20.glUniform3f;
import static org.lwjgl.opengl.GL20.glUniform4f;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

import org.joml.Matrix4f;

public class EntityRenderer {

	private final TextureAtlas textureAtlas;
	private final int vao;
	private final int vbo;
	private final int ebo;
	private final int crossVao;
	private final int crossVbo;
	private final int crossEbo;
	private final int shader;
	private final int crossShader;
	private final int uProjection;
	private final int uView;
	private final int uModel;
	private final int uTopUV;
	private final int uBottomUV;
	private final int uFrontUV;
	private final int uBackUV;
	private final int uLeftUV;
	private final int uRightUV;
	private final int uTint;
	private final int uTintTop;
	private final int uTintAll;
	private final int uTexture;
	private final int uCrossProjection;
	private final int uCrossView;
	private final int uCrossModel;
	private final int uCrossUV;
	private final int uCrossTint;
	private final int uCrossTexture;

	public EntityRenderer(TextureAtlas textureAtlas) {
		if (textureAtlas == null)
			throw new IllegalArgumentException("Texture atlas missing for entity renderer");
		this.textureAtlas = textureAtlas;
		this.shader = this.createShader();
		this.crossShader = this.createCrossShader();
		this.vao = glGenVertexArrays();
		this.vbo = glGenBuffers();
		this.ebo = glGenBuffers();
		this.crossVao = glGenVertexArrays();
		this.crossVbo = glGenBuffers();
		this.crossEbo = glGenBuffers();
		this.createMesh();
		this.createCrossMesh();
		this.uProjection = glGetUniformLocation(this.shader, "uProjection");
		this.uView = glGetUniformLocation(this.shader, "uView");
		this.uModel = glGetUniformLocation(this.shader, "uModel");
		this.uTopUV = glGetUniformLocation(this.shader, "uTopUV");
		this.uBottomUV = glGetUniformLocation(this.shader, "uBottomUV");
		this.uFrontUV = glGetUniformLocation(this.shader, "uFrontUV");
		this.uBackUV = glGetUniformLocation(this.shader, "uBackUV");
		this.uLeftUV = glGetUniformLocation(this.shader, "uLeftUV");
		this.uRightUV = glGetUniformLocation(this.shader, "uRightUV");
		this.uTint = glGetUniformLocation(this.shader, "uTint");
		this.uTintTop = glGetUniformLocation(this.shader, "uTintTop");
		this.uTintAll = glGetUniformLocation(this.shader, "uTintAll");
		this.uTexture = glGetUniformLocation(this.shader, "uTexture");
		this.uCrossProjection = glGetUniformLocation(this.crossShader, "uProjection");
		this.uCrossView = glGetUniformLocation(this.crossShader, "uView");
		this.uCrossModel = glGetUniformLocation(this.crossShader, "uModel");
		this.uCrossUV = glGetUniformLocation(this.crossShader, "uUV");
		this.uCrossTint = glGetUniformLocation(this.crossShader, "uTint");
		this.uCrossTexture = glGetUniformLocation(this.crossShader, "uTexture");
	}

	public void renderBlock(BlockType block, Matrix4f projection, Matrix4f view, Matrix4f model,
			float[] tint, boolean tintTop, boolean tintAll) {
		this.renderBlockInternal(block, projection, view, model, tint, tintTop, tintAll, false);
	}

	public void renderBlockWithCulling(BlockType block, Matrix4f projection, Matrix4f view, Matrix4f model,
			float[] tint, boolean tintTop, boolean tintAll) {
		this.renderBlockInternal(block, projection, view, model, tint, tintTop, tintAll, true);
	}

	public void renderPlant(BlockType block, Matrix4f projection, Matrix4f view, Matrix4f model) {
		if (block == null || projection == null || view == null || model == null)
			throw new IllegalArgumentException("Render parameters missing");
		final int[] tile = block.getSideTile();
		if (tile == null)
			return;

		final float[] uv = this.textureAtlas.getTileUV(tile[0], tile[1]);
		float[] tint = new float[] { 1.0f, 1.0f, 1.0f };
		if (block.needsGrassTint())
			tint = BlockType.getGrassColor(null);

		glDisable(GL_CULL_FACE);
		glUseProgram(this.crossShader);
		glUniformMatrix4fv(this.uCrossProjection, false, projection.get(new float[16]));
		glUniformMatrix4fv(this.uCrossView, false, view.get(new float[16]));
		glUniformMatrix4fv(this.uCrossModel, false, model.get(new float[16]));
		glUniform4f(this.uCrossUV, uv[0], uv[1], uv[2], uv[3]);
		glUniform3f(this.uCrossTint, tint[0], tint[1], tint[2]);
		glActiveTexture(GL_TEXTURE0);
		this.textureAtlas.bind(0);
		glUniform1i(this.uCrossTexture, 0);

		glBindVertexArray(this.crossVao);
		glDrawElements(GL_TRIANGLES, 24, GL_UNSIGNED_INT, 0);
		glBindVertexArray(0);
		glUseProgram(0);
		glEnable(GL_CULL_FACE);
	}

	private void renderBlockInternal(BlockType block, Matrix4f projection, Matrix4f view, Matrix4f model,
			float[] tint, boolean tintTop, boolean tintAll, boolean keepCullingEnabled) {
		if (block == null || projection == null || view == null || model == null)
			throw new IllegalArgumentException("Render parameters missing");
		final int[] sideTile = block.getSideTile();
		if (sideTile == null)
			return;
		int[] topTile = block.getTopTile();
		int[] bottomTile = block.getBottomTile();
		if (topTile == null)
			topTile = sideTile;
		if (bottomTile == null)
			bottomTile = sideTile;

		final int[] frontTile = sideTile;
		final int[] backTile = sideTile;
		final int[] leftTile = sideTile;
		final int[] rightTile = sideTile;

		final float[] topUV = this.textureAtlas.getTileUV(topTile[0], topTile[1]);
		final float[] bottomUV = this.textureAtlas.getTileUV(bottomTile[0], bottomTile[1]);
		final float[] frontUV = this.textureAtlas.getTileUV(frontTile[0], frontTile[1]);
		final float[] backUV = this.textureAtlas.getTileUV(backTile[0], backTile[1]);
		final float[] leftUV = this.textureAtlas.getTileUV(leftTile[0], leftTile[1]);
		final float[] rightUV = this.textureAtlas.getTileUV(rightTile[0], rightTile[1]);

		float[] resolvedTint = tint != null ? tint : new float[] { 1.0f, 1.0f, 1.0f };
		boolean resolvedTintTop = tintTop;
		boolean resolvedTintAll = tintAll;

		if (tint == null)
			if (block.needsFoliageTint()) {
				resolvedTint = BlockType.getFoliageColor(null);
				resolvedTintAll = true;
			} else if (block.needsTint(BlockType.Face.TOP)) {
				resolvedTint = BlockType.getGrassColor(null);
				resolvedTintTop = true;
			}

		if (!keepCullingEnabled)
			glDisable(GL_CULL_FACE);

		glUseProgram(this.shader);
		glUniformMatrix4fv(this.uProjection, false, projection.get(new float[16]));
		glUniformMatrix4fv(this.uView, false, view.get(new float[16]));
		glUniformMatrix4fv(this.uModel, false, model.get(new float[16]));
		glUniform4f(this.uTopUV, topUV[0], topUV[1], topUV[2], topUV[3]);
		glUniform4f(this.uBottomUV, bottomUV[0], bottomUV[1], bottomUV[2], bottomUV[3]);
		glUniform4f(this.uFrontUV, frontUV[0], frontUV[1], frontUV[2], frontUV[3]);
		glUniform4f(this.uBackUV, backUV[0], backUV[1], backUV[2], backUV[3]);
		glUniform4f(this.uLeftUV, leftUV[0], leftUV[1], leftUV[2], leftUV[3]);
		glUniform4f(this.uRightUV, rightUV[0], rightUV[1], rightUV[2], rightUV[3]);
		glUniform3f(this.uTint, resolvedTint[0], resolvedTint[1], resolvedTint[2]);
		glUniform1i(this.uTintTop, resolvedTintTop ? 1 : 0);
		glUniform1i(this.uTintAll, resolvedTintAll ? 1 : 0);
		glActiveTexture(GL_TEXTURE0);
		this.textureAtlas.bind(0);
		glUniform1i(this.uTexture, 0);

		glBindVertexArray(this.vao);
		glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_INT, 0);
		glBindVertexArray(0);
		glUseProgram(0);

		if (!keepCullingEnabled)
			glEnable(GL_CULL_FACE);
	}

	public void cleanup() {
		glDeleteBuffers(this.vbo);
		glDeleteBuffers(this.ebo);
		glDeleteVertexArrays(this.vao);
		glDeleteBuffers(this.crossVbo);
		glDeleteBuffers(this.crossEbo);
		glDeleteVertexArrays(this.crossVao);
		glDeleteProgram(this.shader);
		glDeleteProgram(this.crossShader);
	}

	private void createMesh() {
		final float s = 0.4f;
		final float[] vertices = {
				-s, -s, s, 0, 0, 1, 0, 0, 2,
				s, -s, s, 0, 0, 1, 1, 0, 2,
				s, s, s, 0, 0, 1, 1, 1, 2,
				-s, s, s, 0, 0, 1, 0, 1, 2,

				s, -s, -s, 0, 0, -1, 0, 0, 3,
				-s, -s, -s, 0, 0, -1, 1, 0, 3,
				-s, s, -s, 0, 0, -1, 1, 1, 3,
				s, s, -s, 0, 0, -1, 0, 1, 3,

				-s, s, s, 0, 1, 0, 0, 0, 0,
				s, s, s, 0, 1, 0, 1, 0, 0,
				s, s, -s, 0, 1, 0, 1, 1, 0,
				-s, s, -s, 0, 1, 0, 0, 1, 0,

				-s, -s, -s, 0, -1, 0, 0, 1, 1,
				s, -s, -s, 0, -1, 0, 1, 1, 1,
				s, -s, s, 0, -1, 0, 1, 0, 1,
				-s, -s, s, 0, -1, 0, 0, 0, 1,

				s, -s, s, 1, 0, 0, 0, 0, 4,
				s, -s, -s, 1, 0, 0, 1, 0, 4,
				s, s, -s, 1, 0, 0, 1, 1, 4,
				s, s, s, 1, 0, 0, 0, 1, 4,

				-s, -s, -s, -1, 0, 0, 0, 0, 5,
				-s, -s, s, -1, 0, 0, 1, 0, 5,
				-s, s, s, -1, 0, 0, 1, 1, 5,
				-s, s, -s, -1, 0, 0, 0, 1, 5
		};

		final int[] indices = {
				0, 1, 2, 2, 3, 0,
				4, 5, 6, 6, 7, 4,
				8, 9, 10, 10, 11, 8,
				12, 13, 14, 14, 15, 12,
				16, 17, 18, 18, 19, 16,
				20, 21, 22, 22, 23, 20
		};

		glBindVertexArray(this.vao);
		glBindBuffer(GL_ARRAY_BUFFER, this.vbo);
		glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, this.ebo);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

		final int stride = 9 * Float.BYTES;
		glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
		glEnableVertexAttribArray(0);
		glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3L * Float.BYTES);
		glEnableVertexAttribArray(1);
		glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6L * Float.BYTES);
		glEnableVertexAttribArray(2);
		glVertexAttribPointer(3, 1, GL_FLOAT, false, stride, 8L * Float.BYTES);
		glEnableVertexAttribArray(3);

		glBindVertexArray(0);
	}

	private void createCrossMesh() {
		final float s = 0.4f;
		final float[] vertices = {
				-s, -s, 0, 0, 0,
				s, -s, 0, 1, 0,
				s, s, 0, 1, 1,
				-s, s, 0, 0, 1,

				0, -s, -s, 0, 0,
				0, -s, s, 1, 0,
				0, s, s, 1, 1,
				0, s, -s, 0, 1,

				s, -s, 0, 0, 0,
				-s, -s, 0, 1, 0,
				-s, s, 0, 1, 1,
				s, s, 0, 0, 1,

				0, -s, s, 0, 0,
				0, -s, -s, 1, 0,
				0, s, -s, 1, 1,
				0, s, s, 0, 1
		};

		final int[] indices = {
				0, 1, 2, 2, 3, 0,
				4, 5, 6, 6, 7, 4,
				8, 9, 10, 10, 11, 8,
				12, 13, 14, 14, 15, 12
		};

		glBindVertexArray(this.crossVao);
		glBindBuffer(GL_ARRAY_BUFFER, this.crossVbo);
		glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, this.crossEbo);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

		final int stride = 5 * Float.BYTES;
		glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
		glEnableVertexAttribArray(0);
		glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3L * Float.BYTES);
		glEnableVertexAttribArray(1);

		glBindVertexArray(0);
	}

	private int createCrossShader() {
		final String vertexSource = """
									#version 410 core
									layout(location = 0) in vec3 aPosition;
									layout(location = 1) in vec2 aTexCoord;

									uniform mat4 uProjection;
									uniform mat4 uView;
									uniform mat4 uModel;

									out vec2 vTexCoord;

									void main() {
									    vTexCoord = aTexCoord;
									    gl_Position = uProjection * uView * uModel * vec4(aPosition, 1.0);
									}
									""";

		final String fragmentSource = """
										#version 410 core
										in vec2 vTexCoord;

										uniform sampler2D uTexture;
										uniform vec4 uUV;
										uniform vec3 uTint;

										out vec4 FragColor;

										void main() {
										    vec2 uv = vec2(
										        mix(uUV.x, uUV.z, vTexCoord.x),
										        mix(uUV.y, uUV.w, vTexCoord.y)
										    );
										    vec4 texColor = texture(uTexture, uv);
										    if (texColor.a < 0.5)
										        discard;
										    vec3 color = texColor.rgb * uTint;
										    float ambient = 0.7;
										    color *= ambient;
										    color = pow(color, vec3(1.0 / 2.2));
										    FragColor = vec4(color, 1.0);
										}
										""";

		final int vertexShader = glCreateShader(GL_VERTEX_SHADER);
		glShaderSource(vertexShader, vertexSource);
		glCompileShader(vertexShader);
		if (glGetShaderi(vertexShader, GL_COMPILE_STATUS) == GL_FALSE)
			throw new IllegalStateException(
					"Cross renderer vertex shader failed: " + glGetShaderInfoLog(vertexShader));

		final int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(fragmentShader, fragmentSource);
		glCompileShader(fragmentShader);
		if (glGetShaderi(fragmentShader, GL_COMPILE_STATUS) == GL_FALSE)
			throw new IllegalStateException(
					"Cross renderer fragment shader failed: " + glGetShaderInfoLog(fragmentShader));

		final int program = glCreateProgram();
		glAttachShader(program, vertexShader);
		glAttachShader(program, fragmentShader);
		glLinkProgram(program);
		if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE)
			throw new IllegalStateException("Cross renderer shader link failed: " + glGetProgramInfoLog(program));

		glDeleteShader(vertexShader);
		glDeleteShader(fragmentShader);
		return program;
	}

	private int createShader() {
		final String vertexSource = """
									#version 410 core
									layout(location = 0) in vec3 aPosition;
									layout(location = 1) in vec3 aNormal;
									layout(location = 2) in vec2 aTexCoord;
									layout(location = 3) in float aFaceId;

									uniform mat4 uProjection;
									uniform mat4 uView;
									uniform mat4 uModel;

									out vec3 vNormal;
									out vec2 vTexCoord;
									flat out float vFaceId;

									void main() {
									    vNormal = mat3(uModel) * aNormal;
									    vTexCoord = aTexCoord;
									    vFaceId = aFaceId;
									    gl_Position = uProjection * uView * uModel * vec4(aPosition, 1.0);
									}
									""";

		final String fragmentSource = """
										#version 410 core
										in vec3 vNormal;
										in vec2 vTexCoord;
										flat in float vFaceId;

										uniform sampler2D uTexture;
										uniform vec4 uTopUV;
										uniform vec4 uBottomUV;
										uniform vec4 uFrontUV;
										uniform vec4 uBackUV;
										uniform vec4 uLeftUV;
										uniform vec4 uRightUV;
										uniform vec3 uTint;
										uniform bool uTintTop;
										uniform bool uTintAll;

										out vec4 FragColor;

										void main() {
										    vec4 uvs;
										    bool applyTint = uTintAll;
										    int faceId = int(vFaceId + 0.5);
										    if (faceId == 0) {
										        uvs = uTopUV;
										        if (uTintTop)
										            applyTint = true;
										    } else if (faceId == 1) {
										        uvs = uBottomUV;
										    } else if (faceId == 2) {
										        uvs = uFrontUV;
										    } else if (faceId == 3) {
										        uvs = uBackUV;
										    } else if (faceId == 4) {
										        uvs = uRightUV;
										    } else {
										        uvs = uLeftUV;
										    }
										    vec2 uv = vec2(
										        mix(uvs.x, uvs.z, vTexCoord.x),
										        mix(uvs.y, uvs.w, vTexCoord.y)
										    );
										    vec4 texColor = texture(uTexture, uv);

										    if (texColor.a < 0.1)
										        discard;

										    vec3 color = texColor.rgb;

										    if (applyTint)
										        color *= uTint;
										    vec3 N = normalize(vNormal);
										    vec3 lightDir = normalize(vec3(0.35, 0.9, 0.4));
										    float diff = max(dot(N, lightDir), 0.0);
										    float ambient = 0.5;
										    color *= ambient + diff * 0.5;
										    color = pow(color, vec3(1.0 / 2.2));
										    FragColor = vec4(color, 1.0);
										}
										""";

		final int vertexShader = glCreateShader(GL_VERTEX_SHADER);
		glShaderSource(vertexShader, vertexSource);
		glCompileShader(vertexShader);
		if (glGetShaderi(vertexShader, GL_COMPILE_STATUS) == GL_FALSE)
			throw new IllegalStateException(
					"Entity renderer vertex shader failed: " + glGetShaderInfoLog(vertexShader));

		final int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(fragmentShader, fragmentSource);
		glCompileShader(fragmentShader);
		if (glGetShaderi(fragmentShader, GL_COMPILE_STATUS) == GL_FALSE)
			throw new IllegalStateException(
					"Entity renderer fragment shader failed: " + glGetShaderInfoLog(fragmentShader));

		final int program = glCreateProgram();
		glAttachShader(program, vertexShader);
		glAttachShader(program, fragmentShader);
		glLinkProgram(program);
		if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE)
			throw new IllegalStateException("Entity renderer shader link failed: " + glGetProgramInfoLog(program));

		glDeleteShader(vertexShader);
		glDeleteShader(fragmentShader);
		return program;
	}
}
