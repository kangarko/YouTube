package com.matejpacan.voxelcraft;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LEQUAL;
import static org.lwjgl.opengl.GL11.GL_LESS;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glDepthFunc;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
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
import static org.lwjgl.opengl.GL20.glUniform3f;
import static org.lwjgl.opengl.GL20.glUniform4f;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import lombok.Getter;
import lombok.Setter;

public class Inventory {

	private static final int HOTBAR_SIZE = 9;
	private static final float SLOT_SIZE = 50;
	private static final float SLOT_PADDING = 4;

	@Getter
	private final BlockType[] hotbar = new BlockType[HOTBAR_SIZE];
	@Getter
	@Setter
	private int selectedSlot = 0;

	@Getter
	private boolean creativeMenuOpen = false;
	private int creativeMenuScroll = 0;
	private int hoveredCreativeSlot = -1;
	private int hoveredHotbarSlot = -1;

	private final List<BlockType> creativeBlocks = new ArrayList<>();

	private int screenWidth;
	private int screenHeight;
	private float uiScale = 2.0f;

	private int blockVao;
	private int blockVbo;
	private int blockIbo;
	private int blockShader;
	private int spriteVao;
	private int spriteVbo;
	private int spriteIbo;
	private int spriteShader;
	private int uiVao;
	private int uiVbo;
	private int uiShader;

	private final TextureAtlas textureAtlas;
	private final TextRenderer textRenderer;

	private double mouseX;
	private double mouseY;

	public Inventory(TextureAtlas textureAtlas, TextRenderer textRenderer) {
		this.textureAtlas = textureAtlas;
		this.textRenderer = textRenderer;

		for (final BlockType type : BlockType.values())
			if (type != BlockType.AIR && type != BlockType.WATER_FLOWING
					&& type != BlockType.LAVA_FLOWING && type != BlockType.FURNACE_LIT)
				this.creativeBlocks.add(type);

		this.hotbar[0] = BlockType.GRASS;
		this.hotbar[1] = BlockType.DIRT;
		this.hotbar[2] = BlockType.STONE;
		this.hotbar[3] = BlockType.COBBLESTONE;
		this.hotbar[4] = BlockType.OAK_LOG;
		this.hotbar[5] = BlockType.OAK_PLANKS;
		this.hotbar[6] = BlockType.BRICKS;
		this.hotbar[7] = BlockType.GLASS;
		this.hotbar[8] = BlockType.GLOWSTONE;

		this.createBlockMesh();
		this.createBlockShader();
		this.createSpriteMesh();
		this.createSpriteShader();
		this.createUIElements();
	}

	private void createBlockMesh() {
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

		this.blockVao = glGenVertexArrays();
		this.blockVbo = glGenBuffers();
		this.blockIbo = glGenBuffers();

		glBindVertexArray(this.blockVao);

		final FloatBuffer vertexData = BufferUtils.createFloatBuffer(vertices.length);
		vertexData.put(vertices).flip();

		glBindBuffer(GL_ARRAY_BUFFER, this.blockVbo);
		glBufferData(GL_ARRAY_BUFFER, vertexData, GL_STATIC_DRAW);

		glVertexAttribPointer(0, 3, GL_FLOAT, false, 9 * Float.BYTES, 0);
		glEnableVertexAttribArray(0);
		glVertexAttribPointer(1, 3, GL_FLOAT, false, 9 * Float.BYTES, 3 * Float.BYTES);
		glEnableVertexAttribArray(1);
		glVertexAttribPointer(2, 2, GL_FLOAT, false, 9 * Float.BYTES, 6 * Float.BYTES);
		glEnableVertexAttribArray(2);
		glVertexAttribPointer(3, 1, GL_FLOAT, false, 9 * Float.BYTES, 8 * Float.BYTES);
		glEnableVertexAttribArray(3);

		final IntBuffer indexData = BufferUtils.createIntBuffer(indices.length);
		indexData.put(indices).flip();

		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, this.blockIbo);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexData, GL_STATIC_DRAW);

		glBindVertexArray(0);
	}

	private void createBlockShader() {
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
									flat out int vFaceId;

									void main() {
									    vec3 worldPos = vec3(uModel * vec4(aPosition, 1.0));
									    vNormal = mat3(uModel) * aNormal;
									    vTexCoord = aTexCoord;
									    vFaceId = int(aFaceId);
									    gl_Position = uProjection * uView * vec4(worldPos, 1.0);
									}
									""";

		final String fragmentSource = """
										#version 410 core
										in vec3 vNormal;
										in vec2 vTexCoord;
										flat in int vFaceId;

										uniform sampler2D uTexture;
										uniform vec4 uTopUV;
										uniform vec4 uBottomUV;
										uniform vec4 uFrontUV;
										uniform vec4 uBackUV;
										uniform vec4 uLeftUV;
										uniform vec4 uRightUV;
										uniform vec4 uSideUV;
										uniform vec3 uTint;
										uniform bool uTintTop;
										uniform bool uTintAll;

										out vec4 FragColor;

										void main() {
										    vec3 N = normalize(vNormal);
										    vec4 uvs;
										    bool applyTint = uTintAll;

										    if (vFaceId == 0) {
										        uvs = uTopUV;
										        if (uTintTop)
										            applyTint = true;
										    } else if (vFaceId == 1) {
										        uvs = uBottomUV;
										    } else if (vFaceId == 2) {
										        uvs = uFrontUV;
										    } else if (vFaceId == 3) {
										        uvs = uBackUV;
										    } else if (vFaceId == 4) {
										        uvs = uRightUV;
										    } else if (vFaceId == 5) {
										        uvs = uLeftUV;
										    } else {
										        uvs = uSideUV;
										    }

										    vec2 texCoord = vec2(
										        mix(uvs.x, uvs.z, vTexCoord.x),
										        mix(uvs.y, uvs.w, vTexCoord.y)
										    );

										    vec4 texColor = texture(uTexture, texCoord);
										    if (texColor.a < 0.1)
										        discard;

										    vec3 color = texColor.rgb;
										    if (applyTint)
										        color *= uTint;

										    vec3 lightDir = normalize(vec3(0.5, 1.0, 0.3));
										    float diff = max(dot(N, lightDir), 0.0);
										    float ambient = 0.4;
										    float lighting = ambient + diff * 0.6;

										    color *= lighting;
										    color = pow(color, vec3(1.0 / 2.2));

										    FragColor = vec4(color, texColor.a);
										}
										""";

		final int vertexShader = glCreateShader(GL_VERTEX_SHADER);
		glShaderSource(vertexShader, vertexSource);
		glCompileShader(vertexShader);
		if (glGetShaderi(vertexShader, GL_COMPILE_STATUS) == GL_FALSE)
			throw new RuntimeException("Block vertex shader compilation failed: " + glGetShaderInfoLog(vertexShader));

		final int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(fragmentShader, fragmentSource);
		glCompileShader(fragmentShader);
		if (glGetShaderi(fragmentShader, GL_COMPILE_STATUS) == GL_FALSE)
			throw new RuntimeException(
					"Block fragment shader compilation failed: " + glGetShaderInfoLog(fragmentShader));

		this.blockShader = glCreateProgram();
		glAttachShader(this.blockShader, vertexShader);
		glAttachShader(this.blockShader, fragmentShader);
		glLinkProgram(this.blockShader);
		if (glGetProgrami(this.blockShader, GL_LINK_STATUS) == GL_FALSE)
			throw new RuntimeException("Block shader linking failed: " + glGetProgramInfoLog(this.blockShader));

		glDeleteShader(vertexShader);
		glDeleteShader(fragmentShader);
	}

	private void createSpriteMesh() {
		final float s = 0.5f;
		final float[] vertices = {
				-s, -s, 0, 0, 0,
				s, -s, 0, 1, 0,
				s, s, 0, 1, 1,
				-s, s, 0, 0, 1
		};

		final int[] indices = { 0, 1, 2, 2, 3, 0 };

		this.spriteVao = glGenVertexArrays();
		this.spriteVbo = glGenBuffers();
		this.spriteIbo = glGenBuffers();

		glBindVertexArray(this.spriteVao);

		final FloatBuffer vertexData = BufferUtils.createFloatBuffer(vertices.length);
		vertexData.put(vertices).flip();

		glBindBuffer(GL_ARRAY_BUFFER, this.spriteVbo);
		glBufferData(GL_ARRAY_BUFFER, vertexData, GL_STATIC_DRAW);

		glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0);
		glEnableVertexAttribArray(0);
		glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);
		glEnableVertexAttribArray(1);

		final IntBuffer indexData = BufferUtils.createIntBuffer(indices.length);
		indexData.put(indices).flip();

		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, this.spriteIbo);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexData, GL_STATIC_DRAW);

		glBindVertexArray(0);
	}

	private void createSpriteShader() {
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
										uniform bool uApplyTint;

										out vec4 FragColor;

										void main() {
										    vec2 texCoord = vec2(
										        mix(uUV.x, uUV.z, vTexCoord.x),
										        mix(uUV.y, uUV.w, vTexCoord.y)
										    );

										    vec4 texColor = texture(uTexture, texCoord);
										    if (texColor.a < 0.1)
										        discard;

										    vec3 color = texColor.rgb;
										    if (uApplyTint)
										        color *= uTint;

										    color = pow(color, vec3(1.0 / 2.2));

										    FragColor = vec4(color, texColor.a);
										}
										""";

		final int vertexShader = glCreateShader(GL_VERTEX_SHADER);
		glShaderSource(vertexShader, vertexSource);
		glCompileShader(vertexShader);
		if (glGetShaderi(vertexShader, GL_COMPILE_STATUS) == GL_FALSE)
			throw new RuntimeException("Sprite vertex shader compilation failed: " + glGetShaderInfoLog(vertexShader));

		final int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(fragmentShader, fragmentSource);
		glCompileShader(fragmentShader);
		if (glGetShaderi(fragmentShader, GL_COMPILE_STATUS) == GL_FALSE)
			throw new RuntimeException(
					"Sprite fragment shader compilation failed: " + glGetShaderInfoLog(fragmentShader));

		this.spriteShader = glCreateProgram();
		glAttachShader(this.spriteShader, vertexShader);
		glAttachShader(this.spriteShader, fragmentShader);
		glLinkProgram(this.spriteShader);
		if (glGetProgrami(this.spriteShader, GL_LINK_STATUS) == GL_FALSE)
			throw new RuntimeException("Sprite shader linking failed: " + glGetProgramInfoLog(this.spriteShader));

		glDeleteShader(vertexShader);
		glDeleteShader(fragmentShader);
	}

	private void createUIElements() {
		final String vertexSource = """
									#version 410 core
									layout(location = 0) in vec2 aPosition;
									uniform mat4 uProjection;
									void main() {
									    gl_Position = uProjection * vec4(aPosition, 0.0, 1.0);
									}
									""";

		final String fragmentSource = """
										#version 410 core
										uniform vec4 uColor;
										out vec4 FragColor;
										void main() {
										    FragColor = uColor;
										}
										""";

		final int vertexShader = glCreateShader(GL_VERTEX_SHADER);
		glShaderSource(vertexShader, vertexSource);
		glCompileShader(vertexShader);
		if (glGetShaderi(vertexShader, GL_COMPILE_STATUS) == GL_FALSE)
			throw new RuntimeException("UI vertex shader compilation failed: " + glGetShaderInfoLog(vertexShader));

		final int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(fragmentShader, fragmentSource);
		glCompileShader(fragmentShader);
		if (glGetShaderi(fragmentShader, GL_COMPILE_STATUS) == GL_FALSE)
			throw new RuntimeException("UI fragment shader compilation failed: " + glGetShaderInfoLog(fragmentShader));

		this.uiShader = glCreateProgram();
		glAttachShader(this.uiShader, vertexShader);
		glAttachShader(this.uiShader, fragmentShader);
		glLinkProgram(this.uiShader);
		if (glGetProgrami(this.uiShader, GL_LINK_STATUS) == GL_FALSE)
			throw new RuntimeException("UI shader linking failed: " + glGetProgramInfoLog(this.uiShader));

		glDeleteShader(vertexShader);
		glDeleteShader(fragmentShader);

		this.uiVao = glGenVertexArrays();
		this.uiVbo = glGenBuffers();

		glBindVertexArray(this.uiVao);
		glBindBuffer(GL_ARRAY_BUFFER, this.uiVbo);
		glBufferData(GL_ARRAY_BUFFER, 6 * 2 * Float.BYTES, GL_DYNAMIC_DRAW);
		glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0);
		glEnableVertexAttribArray(0);
		glBindVertexArray(0);
	}

	public void setScreenSize(int width, int height) {
		this.screenWidth = width;
		this.screenHeight = height;
	}

	public void setUiScale(float scale) {
		this.uiScale = scale;
	}

	public void openCreativeMenu() {
		this.creativeMenuOpen = true;
		this.creativeMenuScroll = 0;
		this.hoveredCreativeSlot = -1;
	}

	public void closeCreativeMenu() {
		this.creativeMenuOpen = false;
	}

	public void toggleCreativeMenu() {
		if (this.creativeMenuOpen)
			this.closeCreativeMenu();
		else
			this.openCreativeMenu();
	}

	public BlockType getSelectedBlock() {
		return this.hotbar[this.selectedSlot];
	}

	public boolean addBlock(BlockType block) {
		if (block == null)
			throw new IllegalArgumentException("Cannot add null block to inventory");
		if (block == BlockType.AIR)
			throw new IllegalArgumentException("Cannot add air to inventory");
		for (int i = 0; i < HOTBAR_SIZE; i++)
			if (this.hotbar[i] == block)
				return true;
		for (int i = 0; i < HOTBAR_SIZE; i++)
			if (this.hotbar[i] == null) {
				this.hotbar[i] = block;
				return true;
			}
		this.hotbar[this.selectedSlot] = block;
		return true;
	}

	public void selectSlot(int slot) {
		if (slot < 0 || slot >= HOTBAR_SIZE)
			throw new IllegalArgumentException("Invalid slot index: " + slot);
		this.selectedSlot = slot;
	}

	public void scrollSlot(int direction) {
		this.selectedSlot = (this.selectedSlot + direction + HOTBAR_SIZE) % HOTBAR_SIZE;
	}

	public void updateMousePosition(double mouseX, double mouseY) {
		this.mouseX = mouseX;
		this.mouseY = mouseY;

		if (!this.creativeMenuOpen) {
			this.hoveredHotbarSlot = -1;
			this.hoveredCreativeSlot = -1;
			return;
		}

		final float slotSizeScaled = SLOT_SIZE * this.uiScale;
		final float paddingScaled = SLOT_PADDING * this.uiScale;
		final float totalSlotSize = slotSizeScaled + paddingScaled;

		final int cols = 9;
		final int rows = (int) Math.ceil((double) this.creativeBlocks.size() / cols);
		final float menuWidth = cols * totalSlotSize + paddingScaled;
		final float menuHeight = Math.min(rows, 6) * totalSlotSize + paddingScaled + 40 * this.uiScale;
		final float menuX = (this.screenWidth - menuWidth) / 2;
		final float menuY = (this.screenHeight - menuHeight) / 2;

		final float gridStartX = menuX + paddingScaled;
		final float gridStartY = menuY + 35 * this.uiScale;

		this.hoveredCreativeSlot = -1;
		for (int i = 0; i < this.creativeBlocks.size(); i++) {
			final int row = i / cols;
			final int col = i % cols;

			final int displayRow = row - this.creativeMenuScroll;
			if (displayRow < 0 || displayRow >= 6)
				continue;

			final float slotX = gridStartX + col * totalSlotSize;
			final float slotY = gridStartY + displayRow * totalSlotSize;

			if (mouseX >= slotX && mouseX < slotX + slotSizeScaled &&
					mouseY >= slotY && mouseY < slotY + slotSizeScaled) {
				this.hoveredCreativeSlot = i;
				break;
			}
		}

		final float hotbarY = this.screenHeight - 70 * this.uiScale;
		final float hotbarWidth = HOTBAR_SIZE * totalSlotSize + paddingScaled;
		final float hotbarX = (this.screenWidth - hotbarWidth) / 2;

		this.hoveredHotbarSlot = -1;
		for (int i = 0; i < HOTBAR_SIZE; i++) {
			final float slotX = hotbarX + paddingScaled + i * totalSlotSize;
			if (mouseX >= slotX && mouseX < slotX + slotSizeScaled &&
					mouseY >= hotbarY && mouseY < hotbarY + slotSizeScaled) {
				this.hoveredHotbarSlot = i;
				break;
			}
		}
	}

	public boolean handleClick(double mouseX, double mouseY, int button) {
		if (!this.creativeMenuOpen)
			return false;

		this.updateMousePosition(mouseX, mouseY);

		if (button == 0) {
			if (this.hoveredCreativeSlot >= 0 && this.hoveredCreativeSlot < this.creativeBlocks.size()) {
				final BlockType block = this.creativeBlocks.get(this.hoveredCreativeSlot);
				this.hotbar[this.selectedSlot] = block;
				return true;
			}
			if (this.hoveredHotbarSlot >= 0) {
				this.selectedSlot = this.hoveredHotbarSlot;
				return true;
			}
		} else if (button == 1)
			if (this.hoveredHotbarSlot >= 0) {
				this.hotbar[this.hoveredHotbarSlot] = null;
				return true;
			}

		return false;
	}

	public void handleScroll(double yOffset) {
		if (this.creativeMenuOpen) {
			final int maxScroll = Math.max(0, (int) Math.ceil((double) this.creativeBlocks.size() / 9) - 6);
			this.creativeMenuScroll = Math.max(0, Math.min(maxScroll, this.creativeMenuScroll - (int) yOffset));
		} else
			this.scrollSlot((int) -yOffset);
	}

	public void render() {
		glDisable(GL_DEPTH_TEST);
		glDisable(GL_CULL_FACE);
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

		this.renderHotbar();
		this.renderActionBar();

		if (this.creativeMenuOpen) {
			this.renderCreativeMenu();
			this.renderTooltip();
		}

		glEnable(GL_CULL_FACE);
		glEnable(GL_DEPTH_TEST);
	}

	private void renderActionBar() {
		final BlockType selected = this.hotbar[this.selectedSlot];
		if (selected == null)
			return;

		final String name = this.formatBlockName(selected);
		final float hotbarY = this.screenHeight - 70 * this.uiScale;

		final float textScale = this.uiScale * 0.85f;
		final float textWidth = this.textRenderer.getTextWidth(name) * textScale;
		final float textX = (this.screenWidth - textWidth) / 2;
		final float textY = hotbarY - 40 * this.uiScale;

		this.textRenderer.drawTextWithShadow(name, textX, textY, 1.0f, 1.0f, 1.0f, 1.0f, textScale);
	}

	private void renderTooltip() {
		BlockType hoveredBlock = null;

		if (this.hoveredCreativeSlot >= 0 && this.hoveredCreativeSlot < this.creativeBlocks.size())
			hoveredBlock = this.creativeBlocks.get(this.hoveredCreativeSlot);
		else if (this.hoveredHotbarSlot >= 0 && this.hotbar[this.hoveredHotbarSlot] != null)
			hoveredBlock = this.hotbar[this.hoveredHotbarSlot];

		if (hoveredBlock == null)
			return;

		final String name = this.formatBlockName(hoveredBlock);

		final float textScale = this.uiScale * 0.75f;
		final float textWidth = this.textRenderer.getTextWidth(name) * textScale;
		final float textHeight = this.textRenderer.getFontSize() * textScale;

		final float tooltipPadding = 8 * this.uiScale;
		final float tooltipWidth = textWidth + tooltipPadding * 2;
		final float tooltipHeight = textHeight + tooltipPadding;

		float tooltipX = (float) this.mouseX + 12 * this.uiScale;
		float tooltipY = (float) this.mouseY - 8 * this.uiScale;

		if (tooltipX + tooltipWidth > this.screenWidth)
			tooltipX = (float) this.mouseX - tooltipWidth - 4 * this.uiScale;
		if (tooltipY + tooltipHeight > this.screenHeight)
			tooltipY = this.screenHeight - tooltipHeight;
		if (tooltipY < 0)
			tooltipY = 0;

		this.drawRect(tooltipX - 1, tooltipY - 1, tooltipX + tooltipWidth + 1, tooltipY + tooltipHeight + 1,
				0.1f, 0.0f, 0.2f, 1.0f);
		this.drawRect(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + tooltipHeight,
				0.1f, 0.1f, 0.15f, 0.95f);

		this.textRenderer.drawText(name, tooltipX + tooltipPadding, tooltipY + tooltipPadding * 0.5f,
				1.0f, 1.0f, 1.0f, 1.0f, textScale);
	}

	private String formatBlockName(BlockType block) {
		final String name = block.name();
		final StringBuilder result = new StringBuilder();
		final String[] words = name.split("_");
		for (int i = 0; i < words.length; i++) {
			if (i > 0)
				result.append(" ");
			final String word = words[i];
			result.append(Character.toUpperCase(word.charAt(0)));
			if (word.length() > 1)
				result.append(word.substring(1).toLowerCase());
		}
		return result.toString();
	}

	private void renderHotbar() {
		final float slotSizeScaled = SLOT_SIZE * this.uiScale;
		final float paddingScaled = SLOT_PADDING * this.uiScale;
		final float totalSlotSize = slotSizeScaled + paddingScaled;
		final float hotbarWidth = HOTBAR_SIZE * totalSlotSize + paddingScaled;
		final float hotbarX = (this.screenWidth - hotbarWidth) / 2;
		final float hotbarY = this.screenHeight - 70 * this.uiScale;

		this.drawRect(hotbarX, hotbarY, hotbarX + hotbarWidth, hotbarY + slotSizeScaled + paddingScaled * 2, 0.1f, 0.1f,
				0.1f, 0.85f);

		for (int i = 0; i < HOTBAR_SIZE; i++) {
			final float slotX = hotbarX + paddingScaled + i * totalSlotSize;
			final float slotY = hotbarY + paddingScaled;

			if (i == this.selectedSlot)
				this.drawRect(slotX - 2, slotY - 2, slotX + slotSizeScaled + 2, slotY + slotSizeScaled + 2, 1.0f, 1.0f, 1.0f,
						1.0f);

			this.drawRect(slotX, slotY, slotX + slotSizeScaled, slotY + slotSizeScaled, 0.2f, 0.2f, 0.2f, 0.9f);

			if (this.hotbar[i] != null)
				this.renderBlockPreview(this.hotbar[i], slotX + slotSizeScaled / 2, slotY + slotSizeScaled / 2,
						slotSizeScaled * 0.7f);
		}
	}

	private void renderCreativeMenu() {
		final float slotSizeScaled = SLOT_SIZE * this.uiScale;
		final float paddingScaled = SLOT_PADDING * this.uiScale;
		final float totalSlotSize = slotSizeScaled + paddingScaled;

		final int cols = 9;
		final int rows = (int) Math.ceil((double) this.creativeBlocks.size() / cols);
		final int visibleRows = Math.min(rows, 6);

		final float menuWidth = cols * totalSlotSize + paddingScaled * 2;
		final float menuHeight = visibleRows * totalSlotSize + paddingScaled * 2 + 40 * this.uiScale;
		final float menuX = (this.screenWidth - menuWidth) / 2;
		final float menuY = (this.screenHeight - menuHeight) / 2;

		this.drawRect(0, 0, this.screenWidth, this.screenHeight, 0, 0, 0, 0.5f);

		this.drawRect(menuX, menuY, menuX + menuWidth, menuY + menuHeight, 0.15f, 0.15f, 0.15f, 0.95f);
		this.drawRect(menuX, menuY, menuX + menuWidth, menuY + 3, 0.4f, 0.4f, 0.4f, 1);
		this.drawRect(menuX, menuY + menuHeight - 3, menuX + menuWidth, menuY + menuHeight, 0.05f, 0.05f, 0.05f, 1);

		final float gridStartX = menuX + paddingScaled;
		final float gridStartY = menuY + 35 * this.uiScale;

		for (int i = 0; i < this.creativeBlocks.size(); i++) {
			final int row = i / cols;
			final int col = i % cols;

			final int displayRow = row - this.creativeMenuScroll;
			if (displayRow < 0 || displayRow >= visibleRows)
				continue;

			final float slotX = gridStartX + col * totalSlotSize;
			final float slotY = gridStartY + displayRow * totalSlotSize;

			final boolean hovered = i == this.hoveredCreativeSlot;
			if (hovered)
				this.drawRect(slotX - 2, slotY - 2, slotX + slotSizeScaled + 2, slotY + slotSizeScaled + 2, 0.8f, 0.8f, 0.3f,
						1.0f);

			this.drawRect(slotX, slotY, slotX + slotSizeScaled, slotY + slotSizeScaled, 0.25f, 0.25f, 0.25f, 0.9f);

			final BlockType block = this.creativeBlocks.get(i);
			this.renderBlockPreview(block, slotX + slotSizeScaled / 2, slotY + slotSizeScaled / 2, slotSizeScaled * 0.65f);
		}

		if (rows > 6) {
			final float scrollbarHeight = menuHeight - 45 * this.uiScale;
			final float scrollbarX = menuX + menuWidth - 12 * this.uiScale;
			final float scrollbarY = menuY + 35 * this.uiScale;

			this.drawRect(scrollbarX, scrollbarY, scrollbarX + 8 * this.uiScale, scrollbarY + scrollbarHeight, 0.1f, 0.1f, 0.1f,
					0.8f);

			final float thumbHeight = scrollbarHeight * (6.0f / rows);
			final float thumbY = scrollbarY + (scrollbarHeight - thumbHeight) * (this.creativeMenuScroll / (float) (rows - 6));
			this.drawRect(scrollbarX, thumbY, scrollbarX + 8 * this.uiScale, thumbY + thumbHeight, 0.5f, 0.5f, 0.5f, 0.9f);
		}
	}

	private void renderBlockPreview(BlockType block, float centerX, float centerY, float size) {
		if (block == null)
			return;

		final int[] sideTile = block.getSideTile();
		if (sideTile == null)
			return;

		final boolean isSprite = !block.isSolid() && block != BlockType.AIR;

		if (isSprite) {
			this.renderSpritePreview(block, centerX, centerY, size);
			return;
		}

		int[] topTile = block.getTopTile();
		int[] bottomTile = block.getBottomTile();

		if (topTile == null)
			topTile = sideTile;
		if (bottomTile == null)
			bottomTile = sideTile;

		final boolean isFurnace = block == BlockType.FURNACE || block == BlockType.FURNACE_LIT;
		final int[] stoneSide = BlockType.STONE.getSideTile();
		final int[] frontTile = isFurnace ? sideTile : sideTile;
		final int[] backTile = isFurnace ? stoneSide : sideTile;
		final int[] leftTile = isFurnace ? stoneSide : sideTile;
		final int[] rightTile = isFurnace ? stoneSide : sideTile;

		final float[] topUV = this.textureAtlas.getTileUV(topTile[0], topTile[1]);
		final float[] bottomUV = this.textureAtlas.getTileUV(bottomTile[0], bottomTile[1]);
		final float[] frontUV = this.textureAtlas.getTileUV(frontTile[0], frontTile[1]);
		final float[] backUV = this.textureAtlas.getTileUV(backTile[0], backTile[1]);
		final float[] leftUV = this.textureAtlas.getTileUV(leftTile[0], leftTile[1]);
		final float[] rightUV = this.textureAtlas.getTileUV(rightTile[0], rightTile[1]);
		final float[] sideUV = this.textureAtlas.getTileUV(sideTile[0], sideTile[1]);

		float[] tint = new float[] { 1, 1, 1 };
		boolean tintTop = false;
		boolean tintAll = false;
		if (block.needsFoliageTint()) {
			tint = BlockType.getGrassColor();
			tintAll = true;
		} else if (block.needsTint(BlockType.Face.TOP)) {
			tint = BlockType.getGrassColor();
			tintTop = true;
		}

		glEnable(GL_DEPTH_TEST);
		glDepthFunc(GL_LESS);
		glClear(GL_DEPTH_BUFFER_BIT);

		final float viewportSize = size * 1.5f;
		final int vpX = (int) (centerX - viewportSize / 2);
		final int vpY = (int) (this.screenHeight - centerY - viewportSize / 2);

		glViewport(vpX, vpY, (int) viewportSize, (int) viewportSize);

		final Matrix4f projection = new Matrix4f().ortho(-1, 1, -1, 1, -10, 10);
		final Matrix4f view = new Matrix4f()
				.lookAt(new Vector3f(1.2f, 1.0f, 1.2f), new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));
		final Matrix4f model = new Matrix4f().rotateY((float) Math.toRadians(-25));

		glUseProgram(this.blockShader);
		glUniformMatrix4fv(glGetUniformLocation(this.blockShader, "uProjection"), false, projection.get(new float[16]));
		glUniformMatrix4fv(glGetUniformLocation(this.blockShader, "uView"), false, view.get(new float[16]));
		glUniformMatrix4fv(glGetUniformLocation(this.blockShader, "uModel"), false, model.get(new float[16]));

		glUniform4f(glGetUniformLocation(this.blockShader, "uTopUV"), topUV[0], topUV[1], topUV[2], topUV[3]);
		glUniform4f(glGetUniformLocation(this.blockShader, "uBottomUV"), bottomUV[0], bottomUV[1], bottomUV[2], bottomUV[3]);
		glUniform4f(glGetUniformLocation(this.blockShader, "uFrontUV"), frontUV[0], frontUV[1], frontUV[2], frontUV[3]);
		glUniform4f(glGetUniformLocation(this.blockShader, "uBackUV"), backUV[0], backUV[1], backUV[2], backUV[3]);
		glUniform4f(glGetUniformLocation(this.blockShader, "uLeftUV"), leftUV[0], leftUV[1], leftUV[2], leftUV[3]);
		glUniform4f(glGetUniformLocation(this.blockShader, "uRightUV"), rightUV[0], rightUV[1], rightUV[2], rightUV[3]);
		glUniform4f(glGetUniformLocation(this.blockShader, "uSideUV"), sideUV[0], sideUV[1], sideUV[2], sideUV[3]);
		glUniform3f(glGetUniformLocation(this.blockShader, "uTint"), tint[0], tint[1], tint[2]);
		glUniform1i(glGetUniformLocation(this.blockShader, "uTintTop"), tintTop ? 1 : 0);
		glUniform1i(glGetUniformLocation(this.blockShader, "uTintAll"), tintAll ? 1 : 0);

		this.textureAtlas.bind(0);
		glUniform1i(glGetUniformLocation(this.blockShader, "uTexture"), 0);

		glEnable(GL_CULL_FACE);
		glBindVertexArray(this.blockVao);
		glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_INT, 0);
		glBindVertexArray(0);

		glViewport(0, 0, this.screenWidth, this.screenHeight);
		glDepthFunc(GL_LEQUAL);
		glDisable(GL_DEPTH_TEST);
		glDisable(GL_CULL_FACE);
	}

	private void renderSpritePreview(BlockType block, float centerX, float centerY, float size) {
		final int[] tile = block.getSideTile();

		final float[] uv = this.textureAtlas.getTileUV(tile[0], tile[1]);

		float[] tint = new float[] { 1, 1, 1 };
		final boolean applyTint = block.needsTint(BlockType.Face.NORTH) || block.needsGrassTint() || block.needsFoliageTint();
		if (applyTint)
			tint = BlockType.getGrassColor();

		glEnable(GL_DEPTH_TEST);
		glDepthFunc(GL_LESS);
		glClear(GL_DEPTH_BUFFER_BIT);

		final float viewportSize = size * 1.5f;
		final int vpX = (int) (centerX - viewportSize / 2);
		final int vpY = (int) (this.screenHeight - centerY - viewportSize / 2);

		glViewport(vpX, vpY, (int) viewportSize, (int) viewportSize);

		final Matrix4f projection = new Matrix4f().ortho(-1, 1, -1, 1, -10, 10);
		final Matrix4f view = new Matrix4f().identity();
		final Matrix4f model = new Matrix4f().identity();

		glUseProgram(this.spriteShader);
		glUniformMatrix4fv(glGetUniformLocation(this.spriteShader, "uProjection"), false, projection.get(new float[16]));
		glUniformMatrix4fv(glGetUniformLocation(this.spriteShader, "uView"), false, view.get(new float[16]));
		glUniformMatrix4fv(glGetUniformLocation(this.spriteShader, "uModel"), false, model.get(new float[16]));

		glUniform4f(glGetUniformLocation(this.spriteShader, "uUV"), uv[0], uv[1], uv[2], uv[3]);
		glUniform3f(glGetUniformLocation(this.spriteShader, "uTint"), tint[0], tint[1], tint[2]);
		glUniform1i(glGetUniformLocation(this.spriteShader, "uApplyTint"), applyTint ? 1 : 0);

		this.textureAtlas.bind(0);
		glUniform1i(glGetUniformLocation(this.spriteShader, "uTexture"), 0);

		glDisable(GL_CULL_FACE);
		glBindVertexArray(this.spriteVao);
		glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
		glBindVertexArray(0);

		glViewport(0, 0, this.screenWidth, this.screenHeight);
		glDepthFunc(GL_LEQUAL);
		glDisable(GL_DEPTH_TEST);
	}

	private void drawRect(float x1, float y1, float x2, float y2, float r, float g, float b, float a) {
		final float[] vertices = {
				x1, y1, x2, y1, x2, y2,
				x1, y1, x2, y2, x1, y2
		};

		glBindBuffer(GL_ARRAY_BUFFER, this.uiVbo);
		final FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
		buffer.put(vertices).flip();
		glBufferSubData(GL_ARRAY_BUFFER, 0, buffer);

		final Matrix4f ortho = new Matrix4f().ortho(0, this.screenWidth, this.screenHeight, 0, -1, 1);
		glUseProgram(this.uiShader);
		glUniformMatrix4fv(glGetUniformLocation(this.uiShader, "uProjection"), false, ortho.get(new float[16]));
		glUniform4f(glGetUniformLocation(this.uiShader, "uColor"), r, g, b, a);

		glBindVertexArray(this.uiVao);
		glDrawArrays(GL_TRIANGLES, 0, 6);
		glBindVertexArray(0);
	}

	public void cleanup() {
		glDeleteBuffers(this.blockVbo);
		glDeleteBuffers(this.blockIbo);
		glDeleteVertexArrays(this.blockVao);
		glDeleteProgram(this.blockShader);
		glDeleteBuffers(this.spriteVbo);
		glDeleteBuffers(this.spriteIbo);
		glDeleteVertexArrays(this.spriteVao);
		glDeleteProgram(this.spriteShader);
		glDeleteBuffers(this.uiVbo);
		glDeleteVertexArrays(this.uiVao);
		glDeleteProgram(this.uiShader);
	}
}
