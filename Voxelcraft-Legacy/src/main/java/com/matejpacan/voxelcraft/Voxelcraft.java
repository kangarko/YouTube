package com.matejpacan.voxelcraft;

import java.nio.IntBuffer;
import java.util.logging.Level;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import lombok.Getter;
import lombok.extern.java.Log;

@Log
public class Voxelcraft {

	@Getter
	private long window;
	@Getter
	private int windowWidth = 1280;
	@Getter
	private int windowHeight = 720;
	private int framebufferWidth;
	private int framebufferHeight;
	private float windowToFramebufferRatio = 1.0f;

	@Getter
	private Camera camera;
	private World world;
	private ShaderProgram blockShader;
	private ShaderProgram skyShader;
	private ShaderProgram sunShader;
	private ShaderProgram shadowShader;
	private TextureAtlas textureAtlas;
	private SkyRenderer skyRenderer;
	private ShadowMap shadowMap;
	private VolumetricClouds volumetricClouds;
	private PostProcess postProcess;
	private GodRays godRays;
	private NoiseTexture noiseTexture;
	private final Vector3f moonDirection = new Vector3f();
	private final Vector3f moonColor = new Vector3f(0.75f, 0.82f, 0.95f);

	private GameUI gameUI;
	private CommandHandler commandHandler;
	private BlockSelection blockSelection;
	private Inventory inventory;
	private ParticleSystem particleSystem;
	private ItemSystem itemSystem;
	private HandRenderer handRenderer;
	private Frustum frustum;
	private OcclusionCuller occlusionCuller;
	private ChunkBatchRenderer chunkBatchRenderer;

	private final boolean[] keys = new boolean[GLFW.GLFW_KEY_LAST + 1];
	private boolean mouseCaptured = false;
	private double lastMouseX, lastMouseY;
	private boolean firstMouse = true;
	private boolean windowFocused = true;

	private double lastFrameTime;
	private float deltaTime;

	@Getter
	private long worldTicks = 0;
	@Getter
	private long dayTimeTicks = 6000;
	private boolean dayCycleEnabled = true;
	private static final int TICKS_PER_DAY = 24000;
	private static final float TICKS_PER_SECOND = 100.0f;
	private float tickAccumulator = 0;
	private final Vector3f sunDirection = new Vector3f();
	private final Vector3f sunColor = new Vector3f();
	private final Vector3f ambientColor = new Vector3f();

	private final Matrix4f projection = new Matrix4f();
	private final Matrix4f view = new Matrix4f();
	private float nearPlane = 0.1f;
	private float farPlane = 600.0f;
	private final Vector3f lastCameraPos = new Vector3f();
	private float handWalkPhase = 0.0f;
	private float handImpulse = 0.0f;
	private float handSwing = 0.0f;
	private float handBob = 0.0f;

	public void run() {
		try {
			this.init();
			this.loop();
		} catch (final Exception e) {
			Voxelcraft.log.log(Level.SEVERE, "Fatal error in VoxelDemo", e);
			e.printStackTrace();
		} finally {
			this.cleanup();
		}
	}

	private void init() {
		Voxelcraft.log.info("Initializing VoxelDemo...");

		GLFWErrorCallback.createPrint(System.err).set();

		if (!GLFW.glfwInit())
			throw new IllegalStateException("Unable to initialize GLFW");

		GLFW.glfwDefaultWindowHints();
		GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
		GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);

		GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
		GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 1);
		GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
		GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);

		GLFW.glfwWindowHint(GLFW.GLFW_SAMPLES, 2);
		GLFW.glfwWindowHint(GLFW.GLFW_DEPTH_BITS, 24);

		this.window = GLFW.glfwCreateWindow(this.windowWidth, this.windowHeight,
				"Voxelcraft - Minecraft Style (WASD + Mouse, ESC for menu)", MemoryUtil.NULL, MemoryUtil.NULL);
		if (this.window == MemoryUtil.NULL)
			throw new RuntimeException("Failed to create GLFW window");

		this.setupCallbacks();

		try (MemoryStack stack = MemoryStack.stackPush()) {
			final IntBuffer pWidth = stack.mallocInt(1);
			final IntBuffer pHeight = stack.mallocInt(1);
			GLFW.glfwGetWindowSize(this.window, pWidth, pHeight);
			final GLFWVidMode vidmode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
			if (vidmode != null)
				GLFW.glfwSetWindowPos(this.window,
						(vidmode.width() - pWidth.get(0)) / 2,
						(vidmode.height() - pHeight.get(0)) / 2);
		}

		GLFW.glfwMakeContextCurrent(this.window);
		GLFW.glfwSwapInterval(0);
		GLFW.glfwShowWindow(this.window);
		this.windowFocused = GLFW.glfwGetWindowAttrib(this.window, GLFW.GLFW_FOCUSED) == GLFW.GLFW_TRUE;

		try (MemoryStack stack = MemoryStack.stackPush()) {
			final IntBuffer fbWidth = stack.mallocInt(1);
			final IntBuffer fbHeight = stack.mallocInt(1);
			GLFW.glfwGetFramebufferSize(this.window, fbWidth, fbHeight);
			this.framebufferWidth = fbWidth.get(0);
			this.framebufferHeight = fbHeight.get(0);
		}

		GL.createCapabilities();

		GL11.glViewport(0, 0, this.framebufferWidth, this.framebufferHeight);

		System.out.println("===========================================");
		System.out.println("  Voxel Demo - Minecraft Style");
		System.out.println("===========================================");
		System.out.println("OpenGL Version: " + GL11.glGetString(GL11.GL_VERSION));
		System.out.println("GLSL Version: " + GL11.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION));
		System.out.println("Renderer: " + GL11.glGetString(GL11.GL_RENDERER));
		System.out.println("===========================================");
		System.out.println("Controls:");
		System.out.println("  WASD        - Move");
		System.out.println("  Mouse       - Look around");
		System.out.println("  Left Click  - Break block");
		System.out.println("  Right Click - Place block");
		System.out.println("  1-9         - Select hotbar slot");
		System.out.println("  Scroll      - Cycle hotbar slots");
		System.out.println("  E           - Open creative inventory");
		System.out.println("  Space       - Jump / Fly up");
		System.out.println("  Shift       - Sneak / Fly down");
		System.out.println("  Ctrl        - Sprint");
		System.out.println("  Space x2    - Toggle flight mode");
		System.out.println("  T           - Open chat");
		System.out.println("  ESC         - Pause menu");
		System.out.println("===========================================");

		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDepthFunc(GL11.GL_LEQUAL);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glCullFace(GL11.GL_BACK);
		GL11.glFrontFace(GL11.GL_CCW);
		GL11.glEnable(GL13.GL_MULTISAMPLE);

		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

		try {
			this.textureAtlas = new TextureAtlas("terrain.png");
			this.blockShader = this.createBlockShader();
			this.skyShader = this.createSkyShader();
			this.sunShader = this.createSunShader();
			this.shadowShader = this.createShadowShader();
			this.shadowMap = new ShadowMap(2048);
			final float spawnX = 8.0f;
			final float spawnZ = 8.0f;
			this.world = new World(this.textureAtlas);
			this.camera = new Camera(new Vector3f(spawnX, 0, spawnZ), -90.0f, -20.0f);
			this.camera.setWorld(this.world);
			this.prepareSpawnArea(spawnX, spawnZ);
			final float spawnY = this.calculateSpawnY(spawnX, spawnZ);
			this.camera.getPosition().set(spawnX, spawnY, spawnZ);
			this.camera.setFlying(false);
			this.skyRenderer = new SkyRenderer();
			this.volumetricClouds = new VolumetricClouds();
			this.postProcess = new PostProcess(this.framebufferWidth, this.framebufferHeight);
			this.godRays = new GodRays();
			this.noiseTexture = new NoiseTexture(128);

			this.gameUI = new GameUI();
			this.gameUI.setScreenSize(this.framebufferWidth, this.framebufferHeight);
			this.updateWindowToFramebufferRatio();
			this.gameUI.setUiScale(this.windowToFramebufferRatio);
			this.commandHandler = new CommandHandler(this);
			this.blockSelection = new BlockSelection();
			this.inventory = new Inventory(this.textureAtlas, this.gameUI.getTextRenderer());
			this.inventory.setScreenSize(this.framebufferWidth, this.framebufferHeight);
			this.inventory.setUiScale(this.windowToFramebufferRatio);
			this.particleSystem = new ParticleSystem(this.textureAtlas);
			this.itemSystem = new ItemSystem(this.textureAtlas, this.inventory);
			this.handRenderer = new HandRenderer(this.textureAtlas);
			this.frustum = new Frustum();
			this.occlusionCuller = new OcclusionCuller();
			this.chunkBatchRenderer = new ChunkBatchRenderer();
			this.lastCameraPos.set(this.camera.getPosition());

			this.gameUI.addSystemMessage("Welcome! Press E for inventory, T to chat.");
			this.gameUI.addSystemMessage("Left-click to break, right-click to place.");
			this.gameUI.addSystemMessage("Double-tap SPACE to fly. Type /help for commands.");
			this.gameUI.openPauseMenu();
			this.releaseMouse();
		} catch (final Exception e) {
			throw new RuntimeException("Failed to initialize components", e);
		}

		this.updateProjection();
		this.lastFrameTime = GLFW.glfwGetTime();

		Voxelcraft.log.info("VoxelDemo initialized successfully!");
	}

	private void setupCallbacks() {
		GLFW.glfwSetKeyCallback(this.window, (win, key, scancode, action, mods) -> {
			if (key < 0 || key > GLFW.GLFW_KEY_LAST)
				return;

			if (action == GLFW.GLFW_PRESS)
				this.keys[key] = true;
			else if (action == GLFW.GLFW_RELEASE)
				this.keys[key] = false;

			if (action != GLFW.GLFW_PRESS && action != GLFW.GLFW_REPEAT)
				return;

			final GameUI.GameState state = this.gameUI.getState();

			if (state == GameUI.GameState.CHAT) {
				this.handleChatKeyInput(key, mods);
				return;
			}

			if (state == GameUI.GameState.PAUSED) {
				this.handlePauseKeyInput(key);
				return;
			}

			if (state == GameUI.GameState.INVENTORY) {
				if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_E) {
					this.inventory.closeCreativeMenu();
					this.gameUI.setState(GameUI.GameState.PLAYING);
					this.captureMouse();
				}
				return;
			}

			if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
				this.gameUI.openPauseMenu();
				this.releaseMouse();
				return;
			}

			if (key == GLFW.GLFW_KEY_E && action == GLFW.GLFW_PRESS) {
				this.inventory.openCreativeMenu();
				this.gameUI.setState(GameUI.GameState.INVENTORY);
				this.releaseMouse();
				return;
			}

			if (key == GLFW.GLFW_KEY_T && action == GLFW.GLFW_PRESS) {
				this.gameUI.openChat();
				this.releaseMouse();
				return;
			}

			if (key == GLFW.GLFW_KEY_SLASH && action == GLFW.GLFW_PRESS) {
				this.gameUI.openChat();
				this.gameUI.addChatCharacter('/');
				this.releaseMouse();
				return;
			}

			if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_9 && action == GLFW.GLFW_PRESS)
				this.inventory.selectSlot(key - GLFW.GLFW_KEY_1);
		});

		GLFW.glfwSetCharCallback(this.window, (win, codepoint) -> {
			if (this.gameUI.getState() == GameUI.GameState.CHAT && !this.gameUI.shouldIgnoreNextChar())
				this.gameUI.addChatCharacter((char) codepoint);
			this.gameUI.clearIgnoreNextChar();
		});

		GLFW.glfwSetCursorPosCallback(this.window, (win, xpos, ypos) -> {
			if (this.gameUI.getState() == GameUI.GameState.PAUSED) {
				this.gameUI.updateMousePosition(xpos, ypos, this.windowToFramebufferRatio);
				return;
			}

			if (this.gameUI.getState() == GameUI.GameState.INVENTORY) {
				this.inventory.updateMousePosition(xpos * this.windowToFramebufferRatio,
						ypos * this.windowToFramebufferRatio);
				return;
			}

			if (!this.mouseCaptured || this.gameUI.getState() != GameUI.GameState.PLAYING)
				return;

			if (this.firstMouse) {
				this.lastMouseX = xpos;
				this.lastMouseY = ypos;
				this.firstMouse = false;
				return;
			}

			final double xOffset = xpos - this.lastMouseX;
			final double yOffset = this.lastMouseY - ypos;
			this.lastMouseX = xpos;
			this.lastMouseY = ypos;

			this.camera.processMouseMovement((float) xOffset, (float) yOffset);
		});

		GLFW.glfwSetMouseButtonCallback(this.window, (win, button, action, mods) -> {
			if (action == GLFW.GLFW_PRESS) {
				if (this.gameUI.getState() == GameUI.GameState.INVENTORY) {
					final double[] xpos = new double[1];
					final double[] ypos = new double[1];
					GLFW.glfwGetCursorPos(this.window, xpos, ypos);
					this.inventory.handleClick(xpos[0] * this.windowToFramebufferRatio,
							ypos[0] * this.windowToFramebufferRatio,
							button);
					return;
				}

				if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
					if (this.gameUI.getState() == GameUI.GameState.PAUSED) {
						final double[] xpos = new double[1];
						final double[] ypos = new double[1];
						GLFW.glfwGetCursorPos(this.window, xpos, ypos);

						final int clicked = this.gameUI.handleMouseClick(xpos[0], ypos[0],
								this.windowToFramebufferRatio);
						if (clicked == 0) {
							this.gameUI.closePauseMenu();
							this.captureMouse();
						} else if (clicked == 1)
							GLFW.glfwSetWindowShouldClose(this.window, true);
					} else if (this.gameUI.getState() == GameUI.GameState.PLAYING && this.mouseCaptured) {
						final var selected = this.blockSelection.getSelectedBlock();
						if (selected != null) {
							final BlockType brokenBlock = this.world.getBlockAt(selected.x, selected.y, selected.z);
							final BlockType aboveBlock = this.world.getBlockAt(selected.x, selected.y + 1, selected.z);
							if (this.blockSelection.breakBlock(this.world)) {
								this.handImpulse = 0.7f;
								if (this.shouldDrop(brokenBlock))
									this.itemSystem.spawnDrop(this.world, brokenBlock, selected.x + 0.5f,
											selected.y + 0.35f,
											selected.z + 0.5f);
								if (aboveBlock != null && aboveBlock.isPlant() && this.shouldDrop(aboveBlock))
									this.itemSystem.spawnDrop(this.world, aboveBlock, selected.x + 0.5f,
											selected.y + 1.2f,
											selected.z + 0.5f);
								this.particleSystem.spawnBlockBreakParticles(brokenBlock, selected.x, selected.y,
										selected.z);
							}
						}
					}
				} else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT)
					if (this.gameUI.getState() == GameUI.GameState.PLAYING && this.mouseCaptured) {
						final BlockType selectedBlock = this.inventory.getSelectedBlock();
						if (this.blockSelection.placeBlock(this.world, selectedBlock, this.camera.getPosition(),
								this.camera.getFront()))
							this.handImpulse = 0.55f;
					}
			}
		});

		GLFW.glfwSetWindowSizeCallback(this.window, (win, width, height) -> {
			if (width > 0 && height > 0) {
				this.windowWidth = width;
				this.windowHeight = height;
				this.updateWindowToFramebufferRatio();
			}
		});

		GLFW.glfwSetFramebufferSizeCallback(this.window, (win, width, height) -> {
			if (width > 0 && height > 0) {
				this.framebufferWidth = width;
				this.framebufferHeight = height;
				GL11.glViewport(0, 0, width, height);
				this.updateProjection();
				this.gameUI.setScreenSize(width, height);
				if (this.inventory != null)
					this.inventory.setScreenSize(width, height);
				this.updateWindowToFramebufferRatio();
				if (this.postProcess != null)
					this.postProcess.resize(width, height);
			}
		});

		GLFW.glfwSetWindowFocusCallback(this.window, (win, focused) -> {
			this.windowFocused = focused;
			if (this.gameUI == null)
				return;
			if (focused) {
				if (this.gameUI.getState() == GameUI.GameState.PLAYING)
					this.captureMouse();
				return;
			}
			if (this.mouseCaptured)
				this.releaseMouse();
		});

		GLFW.glfwSetScrollCallback(this.window, (win, xoffset, yoffset) -> {
			if (this.inventory != null)
				this.inventory.handleScroll(yoffset);
		});
	}

	private void updateWindowToFramebufferRatio() {
		if (this.windowWidth > 0) {
			this.windowToFramebufferRatio = (float) this.framebufferWidth / this.windowWidth;
			if (this.gameUI != null)
				this.gameUI.setUiScale(this.windowToFramebufferRatio);
			if (this.inventory != null)
				this.inventory.setUiScale(this.windowToFramebufferRatio);
		}
	}

	private void handleChatKeyInput(final int key, final int mods) {
		if (key == GLFW.GLFW_KEY_ESCAPE) {
			if (this.gameUI.dismissAutocomplete())
				return;
			this.gameUI.closeChat();
			this.captureMouse();
			return;
		}

		if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
			final String input = this.gameUI.getChatInputText();
			if (!input.isEmpty()) {
				this.gameUI.saveInputToHistory(input);
				this.commandHandler.executeCommand(input, this.gameUI);
			}
			this.gameUI.clearChatInput();
			this.gameUI.closeChat();
			this.captureMouse();
			return;
		}

		if (key == GLFW.GLFW_KEY_BACKSPACE) {
			if ((mods & GLFW.GLFW_MOD_CONTROL) != 0)
				this.gameUI.removePreviousWord();
			else
				this.gameUI.removeChatCharacter();
			return;
		}

		if (key == GLFW.GLFW_KEY_LEFT) {
			if ((mods & GLFW.GLFW_MOD_CONTROL) != 0)
				this.gameUI.moveCursorWordLeft();
			else
				this.gameUI.moveCursorLeft();
			return;
		}

		if (key == GLFW.GLFW_KEY_RIGHT) {
			if ((mods & GLFW.GLFW_MOD_CONTROL) != 0)
				this.gameUI.moveCursorWordRight();
			else
				this.gameUI.moveCursorRight();
			return;
		}

		if (key == GLFW.GLFW_KEY_TAB) {
			if ((mods & GLFW.GLFW_MOD_SHIFT) != 0)
				this.gameUI.cycleSuggestion(false);
			else
				this.gameUI.tabComplete();
			return;
		}

		if (key == GLFW.GLFW_KEY_UP) {
			if (this.gameUI.hasCommandSuggestions()) {
				this.gameUI.selectPreviousSuggestion();
				return;
			}
			this.gameUI.navigateHistoryUp();
			return;
		}

		if (key == GLFW.GLFW_KEY_DOWN) {
			if (this.gameUI.hasCommandSuggestions()) {
				this.gameUI.selectNextSuggestion();
				return;
			}
			this.gameUI.navigateHistoryDown();
		}
	}

	private void handlePauseKeyInput(final int key) {
		if (key == GLFW.GLFW_KEY_ESCAPE) {
			this.gameUI.closePauseMenu();
			this.captureMouse();
			return;
		}

		if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
			final int selection = this.gameUI.getPauseSelection();
			if (selection == 0) {
				this.gameUI.closePauseMenu();
				this.captureMouse();
			} else if (selection == 1)
				GLFW.glfwSetWindowShouldClose(this.window, true);
			return;
		}

		this.gameUI.handlePauseInput(key);
	}

	private void captureMouse() {
		this.windowFocused = true;
		this.firstMouse = true;
		GLFW.glfwSetInputMode(this.window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
		GLFW.glfwPollEvents();
		this.mouseCaptured = true;
	}

	private void releaseMouse() {
		this.mouseCaptured = false;
		GLFW.glfwSetInputMode(this.window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
	}

	public void teleportPlayer(final float x, final float y, final float z) {
		this.camera.getPosition().set(x, y, z);
	}

	public void setDayTime(final long ticks) {
		this.dayTimeTicks = Math.max(0, Math.min(Voxelcraft.TICKS_PER_DAY - 1, ticks));
	}

	public float getDayTimeNormalized() {
		return (float) this.dayTimeTicks / Voxelcraft.TICKS_PER_DAY;
	}

	public boolean isDayCycleEnabled() {
		return this.dayCycleEnabled;
	}

	public void setDayCycleEnabled(final boolean enabled) {
		this.dayCycleEnabled = enabled;
	}

	private void enforceMouseCaptureState() {
		if (this.gameUI == null)
			throw new IllegalStateException("Game UI missing during cursor enforcement");
		final boolean shouldCapture = this.gameUI.getState() == GameUI.GameState.PLAYING;
		if (shouldCapture) {
			if (!this.mouseCaptured && this.windowFocused)
				this.captureMouse();
			return;
		}
		if (this.mouseCaptured)
			this.releaseMouse();
	}

	private void updateProjection() {
		final float aspect = (float) this.framebufferWidth / Math.max(this.framebufferHeight, 1);
		this.nearPlane = 0.1f;
		this.farPlane = World.RENDER_DISTANCE * Chunk.SIZE * 1.5f;
		this.projection.setPerspective((float) Math.toRadians(70.0f), aspect, this.nearPlane, this.farPlane);
	}

	private boolean isChunkInShadowRange(final Chunk chunk, final Vector3f camPos) {
		final float centerX = chunk.getChunkX() * Chunk.SIZE + Chunk.SIZE * 0.5f;
		final float centerZ = chunk.getChunkZ() * Chunk.SIZE + Chunk.SIZE * 0.5f;
		final float dx = centerX - camPos.x;
		final float dz = centerZ - camPos.z;
		final float range = ShadowMap.SHADOW_DISTANCE;
		return dx * dx + dz * dz <= range * range;
	}

	private ShaderProgram createBlockShader() {
		final String vertexSource = """
									#version 410 core

									layout(location = 0) in vec3 aPosition;
									layout(location = 1) in vec2 aTexCoord;
									layout(location = 2) in vec3 aNormal;
									layout(location = 3) in vec3 aColor;
									layout(location = 4) in float aAO;
									layout(location = 5) in float aSkyLight;
									layout(location = 6) in float aBlockLight;

									uniform mat4 uProjection;
									uniform mat4 uView;
									uniform mat4 uLightSpaceMatrix;
									uniform vec3 uCameraPos;
									uniform vec3 uSunDirection;
									uniform vec3 uMoonDirection;
									uniform vec3 uAmbientColor;
									uniform float uFogStart;
									uniform float uFogEnd;
									uniform float uShadowDistance;
									uniform float uSkyLightIntensity;
									uniform vec3 uMoonColor;

									out vec2 vTexCoord;
									out vec3 vNormal;
									out vec3 vViewNormal;
									out vec3 vWorldPos;
									out vec3 vColor;
									out vec4 vLightSpacePos;
									out float vAO;
									out float vSkyLight;
									out float vBlockLight;
									out float vDistanceToCamera;
									out float vFogAmount;
									out float vFaceShade;
									out float vShadowFade;
									out vec3 vAmbientLight;

									void main() {
									    vTexCoord = aTexCoord;
									    vNormal = aNormal;
									    vViewNormal = mat3(uView) * aNormal;
									    vColor = aColor;
									    vAO = aAO;
									    vSkyLight = aSkyLight;
									    vBlockLight = aBlockLight;
									    vWorldPos = aPosition;
									    vLightSpacePos = uLightSpaceMatrix * vec4(aPosition, 1.0);
									    vDistanceToCamera = length(aPosition - uCameraPos);

									    float fogRange = uFogEnd - uFogStart;
									    float normalizedDist = max(0.0, vDistanceToCamera - uFogStart) / fogRange;
									    vFogAmount = 1.0 - exp(-normalizedDist * normalizedDist * 3.0);
									    vFogAmount = clamp(vFogAmount, 0.0, 0.75);

									    if (aNormal.y < -0.5) vFaceShade = 0.5;
									    else if (aNormal.y > 0.5) vFaceShade = 1.0;
									    else if (abs(aNormal.x) > 0.5) vFaceShade = 0.78;
									    else vFaceShade = 0.72;

									    float shadowFadeStart = uShadowDistance * 0.7;
									    vShadowFade = 1.0 - smoothstep(shadowFadeStart, uShadowDistance, vDistanceToCamera);

									    float smoothAO = aAO * aAO * (3.0 - 2.0 * aAO);
									    float aoFactor = 0.4 + 0.6 * smoothAO;
									    float skyAccess = max(0.0, aNormal.y * 0.5 + 0.5);
									    float sunAngleFactor = smoothstep(-0.1, 0.4, uSunDirection.y);
									    float directionalAmbient = mix(0.3, 1.0, skyAccess * sunAngleFactor);
									    vAmbientLight = uAmbientColor * vFaceShade * aoFactor * directionalAmbient * 0.55 * uSkyLightIntensity;

									    gl_Position = uProjection * uView * vec4(aPosition, 1.0);
									}
									""";

		final String fragmentSource = """
										#version 410 core

										in vec2 vTexCoord;
										in vec3 vNormal;
										in vec3 vViewNormal;
										in vec3 vWorldPos;
										in vec3 vColor;
										in vec4 vLightSpacePos;
										in float vAO;
										in float vSkyLight;
										in float vBlockLight;
										in float vDistanceToCamera;
										in float vFogAmount;
										in float vFaceShade;
										in float vShadowFade;
										in vec3 vAmbientLight;

										uniform sampler2D uTexture;
										uniform sampler2DShadow uShadowMap;
										uniform sampler3D uNoiseTexture;
										uniform vec3 uSunDirection;
										uniform vec3 uSunColor;
										uniform vec3 uMoonDirection;
										uniform vec3 uMoonColor;
										uniform vec3 uAmbientColor;
										uniform vec3 uCameraPos;
										uniform mat4 uView;
										uniform float uFogStart;
										uniform float uFogEnd;
										uniform vec3 uFogColor;
										uniform float uChunkOpacity;
										uniform float uShadowDistance;
										uniform float uSkyLightIntensity;
										uniform float uSmoothLightingStrength;
										uniform float uTime;
										uniform bool uCameraUnderwater;

										layout(location = 0) out vec4 FragColor;
										layout(location = 1) out vec4 GBuffer;

										float calculateShadow(vec4 lightSpacePos, vec3 normal, vec3 lightDir, float shadowFade, bool isFoliage, float distToCamera) {
										    vec3 projCoords = lightSpacePos.xyz / lightSpacePos.w;
										    projCoords = projCoords * 0.5 + 0.5;

										    if (projCoords.z > 1.0)
										        return 0.0;

										    if (shadowFade < 0.01)
										        return 0.0;

										    float edgeFade = 1.0;
										    edgeFade *= smoothstep(0.0, 0.05, projCoords.x) * smoothstep(1.0, 0.95, projCoords.x);
										    edgeFade *= smoothstep(0.0, 0.05, projCoords.y) * smoothstep(1.0, 0.95, projCoords.y);
										    edgeFade *= shadowFade;

										    if (edgeFade < 0.01)
										        return 0.0;

										    float NdotL = max(dot(normal, lightDir), 0.0);
										    float bias = isFoliage ? 0.002 : (0.001 + 0.002 * (1.0 - NdotL));

										    float texelSize = 1.0 / 2048.0;
										    float shadow = 0.0;

										    if (distToCamera > 60.0) {
										        shadow = texture(uShadowMap, vec3(projCoords.xy, projCoords.z - bias));
										    } else {
										        vec2 offsets[5] = vec2[](
										            vec2(0.0, 0.0),
										            vec2(-1.0, 0.0) * texelSize,
										            vec2(1.0, 0.0) * texelSize,
										            vec2(0.0, -1.0) * texelSize,
										            vec2(0.0, 1.0) * texelSize
										        );

										        for (int i = 0; i < 5; i++) {
										            shadow += texture(uShadowMap, vec3(projCoords.xy + offsets[i], projCoords.z - bias));
										        }
										        shadow /= 5.0;
										    }

										    return (1.0 - shadow) * edgeFade * 0.85;
										}

										vec3 desaturate(vec3 color, float amount) {
										    float gray = dot(color, vec3(0.299, 0.587, 0.114));
										    return mix(color, vec3(gray), amount);
										}

										float simpleNoise(vec2 p) {
										    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
										}

										float hash(vec2 p) {
										    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
										}

										float hash3(vec3 p) {
										    return fract(sin(dot(p, vec3(127.1, 311.7, 74.7))) * 43758.5453123);
										}

										vec4 sampleNoise3D(vec3 p) {
										    return texture(uNoiseTexture, p);
										}

										float getValueNoise(vec3 p) {
										    return sampleNoise3D(p * 0.25).r;
										}

										float getFbmNoise(vec3 p) {
										    return sampleNoise3D(p * 0.125).g;
										}

										float getWorleyNoise(vec3 p) {
										    return sampleNoise3D(p * 0.1667).b;
										}

										float getDetailNoise(vec3 p) {
										    return sampleNoise3D(p * 0.5).a;
										}

										vec3 applyBiomeWeathering(vec3 color, vec3 worldPos, vec3 normal, vec3 biomeTint, float ao, float distToCamera) {
										    float avgTint = (biomeTint.r + biomeTint.g + biomeTint.b) / 3.0;
										    if (avgTint > 0.95 || distToCamera > 50.0)
										        return color;

										    vec4 n = sampleNoise3D(worldPos * 0.02);

										    float weatherStrength = (1.0 - distToCamera / 50.0) * 0.15;
										    float variation = n.r * weatherStrength;
										    float aoEffect = (1.0 - ao) * n.a * 0.05;

										    vec3 weatheredColor = color * (1.0 - variation - aoEffect);
										    weatheredColor += color * ao * n.g * 0.03;

										    return weatheredColor;
										}

										vec3 calculateFogColor(vec3 baseFogColor, vec3 sunDir, vec3 viewDir, float dayFactor) {
										    float sunAmount = max(dot(viewDir, sunDir), 0.0);
										    float sunScatter = pow(sunAmount, 8.0) * 0.15 * dayFactor;

										    vec3 nightFog = vec3(0.0, 0.0, 0.0);
										    vec3 dayFog = vec3(0.6, 0.7, 0.85);
										    vec3 horizonColor = mix(nightFog, dayFog, dayFactor);

										    vec3 sunTintedFog = mix(horizonColor, vec3(1.0, 0.95, 0.85), sunScatter);
										    return sunTintedFog;
										}

										void main() {
										    vec4 texColor = texture(uTexture, vTexCoord);

										    bool isWater = (vColor.b > 0.9 && vColor.r < 0.2);
										    bool isFoliageEarly = (vColor.g > vColor.r * 1.05 && vColor.g > vColor.b * 1.05);
										    bool isLog = (vColor.b > vColor.r * 1.01 && vColor.b > vColor.g * 1.01 && vColor.r > 0.95 && vColor.g > 0.95);

										    float foliageOcclusion = 0.0;
										    if (isFoliageEarly) {
										        float avgColor = (vColor.r + vColor.g + vColor.b) / 3.0;
										        float expectedAvg = 0.52;
										        foliageOcclusion = clamp(1.0 - avgColor / expectedAvg, 0.0, 1.0);

										        if (texColor.a < 0.1)
										            discard;
										    } else if (isLog) {
										        if (texColor.a < 0.1)
										            discard;
										    } else if (!isWater) {
										        if (texColor.a < 0.5)
										            discard;
										    }

										    bool cameraUnderwater = uCameraUnderwater;

										    if (isWater) {
										        float dayFactor = smoothstep(-0.1, 0.3, uSunDirection.y);
										        float moonFactor = smoothstep(-0.35, 0.05, -uSunDirection.y);
										        vec3 nightWater = vec3(0.01, 0.02, 0.05);
										        vec3 dayWater = vec3(0.06, 0.15, 0.30);
										        vec3 waterColor = mix(nightWater, dayWater, dayFactor);

										        vec3 V = normalize(uCameraPos - vWorldPos);

										        vec2 gDir1 = normalize(vec2(1.0, 0.3));
										        vec2 gDir2 = normalize(vec2(-0.6, 0.8));
										        vec2 gDir3 = normalize(vec2(0.2, -1.0));
										        float amp1 = 0.35;
										        float amp2 = 0.22;
										        float amp3 = 0.18;
										        float freq1 = 0.18;
										        float freq2 = 0.27;
										        float freq3 = 0.13;
										        float speed1 = 0.9;
										        float speed2 = 0.65;
										        float speed3 = 0.45;

										        vec3 displaced = vWorldPos;
										        vec2 grad = vec2(0.0);

										        float p1 = dot(gDir1, vWorldPos.xz) * freq1 + uTime * speed1;
										        float s1 = sin(p1);
										        float c1 = cos(p1);
										        displaced.x += gDir1.x * c1 * amp1 * 0.6;
										        displaced.z += gDir1.y * c1 * amp1 * 0.6;
										        displaced.y += s1 * amp1;
										        grad += gDir1 * c1 * amp1 * freq1;

										        float p2 = dot(gDir2, vWorldPos.xz) * freq2 + uTime * speed2;
										        float s2 = sin(p2);
										        float c2 = cos(p2);
										        displaced.x += gDir2.x * c2 * amp2 * 0.6;
										        displaced.z += gDir2.y * c2 * amp2 * 0.6;
										        displaced.y += s2 * amp2;
										        grad += gDir2 * c2 * amp2 * freq2;

										        float p3 = dot(gDir3, vWorldPos.xz) * freq3 + uTime * speed3;
										        float s3 = sin(p3);
										        float c3 = cos(p3);
										        displaced.x += gDir3.x * c3 * amp3 * 0.6;
										        displaced.z += gDir3.y * c3 * amp3 * 0.6;
										        displaced.y += s3 * amp3;
										        grad += gDir3 * c3 * amp3 * freq3;

										        vec3 N = normalize(vec3(-grad.x, 1.0, -grad.y));

										        float fresnel = pow(1.0 - max(dot(V, N), 0.0), 3.0);
										        fresnel = mix(0.05, 0.58, fresnel);

										        vec3 nightSky = vec3(0.01, 0.015, 0.03);
										        vec3 daySky = vec3(0.45, 0.55, 0.7);
										        vec3 skyReflect = mix(nightSky, daySky, dayFactor);
										        skyReflect = mix(skyReflect, uMoonColor, moonFactor * 0.25);
										        vec3 baseColor = mix(waterColor, skyReflect, fresnel * 0.6);

										        vec3 Ls = normalize(uSunDirection);
										        vec3 Lm = normalize(uMoonDirection);
										        vec3 R = reflect(-V, N);
										        float specSun = pow(max(dot(R, Ls), 0.0), 52.0) * 0.35 * dayFactor;
										        float specMoon = pow(max(dot(R, Lm), 0.0), 40.0) * 0.22 * moonFactor;
										        baseColor += vec3(1.0, 0.95, 0.9) * specSun;
										        baseColor += uMoonColor * specMoon;

										        float ripple = simpleNoise(displaced.xz * 0.6 + uTime * 0.25);
										        baseColor = mix(baseColor, baseColor + vec3(0.05, 0.07, 0.1) * ripple, 0.25);

										        float distToCamera = length(displaced - uCameraPos);
										        float fogRange = uFogEnd - uFogStart;
										        float normalizedDist = max(0.0, distToCamera - uFogStart) / fogRange;
										        float fogAmount = 1.0 - exp(-normalizedDist * normalizedDist * 2.2);
										        fogAmount = clamp(fogAmount, 0.0, 0.65);
										        vec3 scatterColor = mix(waterColor, vec3(0.05, 0.12, 0.2), 0.6);
										        vec3 finalColor = mix(baseColor, scatterColor, fogAmount);

										        float foam = smoothstep(0.22, 0.6, abs(s1 - s3));
										        finalColor += vec3(0.85, 0.9, 0.95) * foam * 0.06 * dayFactor;

										        finalColor = pow(finalColor, vec3(1.0 / 2.2));
										        FragColor = vec4(finalColor, 0.72);
										        return;
										    }

										    vec3 baseColor = texColor.rgb * vColor;
										    if (isFoliageEarly) {
										        float depthDarken = mix(1.0, 0.6, foliageOcclusion * foliageOcclusion);
										        baseColor *= depthDarken;
										    }

										    vec3 N = normalize(vNormal);

										    if (!isFoliageEarly && !isLog && vDistanceToCamera < 50.0)
										        baseColor = applyBiomeWeathering(baseColor, vWorldPos, N, vColor, vAO, vDistanceToCamera);

										    vec3 L = normalize(uSunDirection);
										    bool isFoliage = isFoliageEarly;

										    float smoothAO = vAO * vAO * (3.0 - 2.0 * vAO);
										    float aoFactor = 0.4 + 0.6 * smoothAO;

										    if (isFoliage)
										        aoFactor *= mix(1.0, 0.7, foliageOcclusion);

										    float smoothSkyLight = vSkyLight * vSkyLight * (3.0 - 2.0 * vSkyLight);
										    float smoothBlockLight = vBlockLight * vBlockLight * (3.0 - 2.0 * vBlockLight);

										    float shadow = calculateShadow(vLightSpacePos, N, L, vShadowFade, isFoliage, vDistanceToCamera);
										    float shadowDarkness = mix(0.95, 0.8, smoothstep(-0.1, 0.4, uSunDirection.y));
										    float shadowFactor = 1.0 - shadow * shadowDarkness * uSkyLightIntensity;

										    if (isFoliage)
										        shadowFactor *= (1.0 - foliageOcclusion * 0.35);

										    float combinedLight = aoFactor * mix(1.0, 0.02 + 0.98 * max(smoothSkyLight, smoothBlockLight),
										            uSmoothLightingStrength);

										    vec3 ambient = vAmbientLight * combinedLight;
										    float diffuseStrength = max(dot(N, L), 0.0) * 0.85 + 0.15;
										    vec3 diffuse = uSunColor * diffuseStrength * vFaceShade * shadowFactor * combinedLight * uSkyLightIntensity * 0.95;

										    if (isFoliage) {
										        float backLitAmount = max(dot(N, -L), 0.0);
										        vec3 subsurfaceColor = vec3(0.7, 0.9, 0.3) * baseColor;
										        float ssStr = mix(0.5, 0.15, foliageOcclusion);
										        diffuse += subsurfaceColor * backLitAmount * uSkyLightIntensity * ssStr;
										    }

										    vec3 lighting = ambient + diffuse;

										    vec3 torchColor = vec3(1.0, 0.65, 0.3);
										    float torchIntensity = smoothBlockLight * smoothBlockLight * 2.8;
										    vec3 torchLight = torchColor * torchIntensity * aoFactor;
										    lighting += torchLight;

										    float minLight = 0.001;
										    if (isFoliage)
										        minLight *= mix(1.0, 0.5, foliageOcclusion);
										    lighting = max(lighting, vec3(minLight));

										    vec3 finalColor = baseColor * lighting;

										    float seaLevel = 62.0;
										    bool isUnderwater = vWorldPos.y < seaLevel && !cameraUnderwater;
										    if (isUnderwater) {
										        float underwaterDepth = seaLevel - vWorldPos.y;
										        float underwaterFogAmount = 1.0 - exp(-underwaterDepth * 0.2);
										        vec3 underwaterFogColor = vec3(0.02, 0.05, 0.14);
										        finalColor = mix(finalColor, underwaterFogColor, underwaterFogAmount * 0.8);
										        finalColor *= mix(1.0, 0.35, underwaterFogAmount);
										    }

										    float dist = length(vWorldPos - uCameraPos);
										    vec3 viewDir = normalize(vWorldPos - uCameraPos);
										    float dayFactor = smoothstep(-0.1, 0.3, uSunDirection.y);
										    float fogAmount = vFogAmount;
										    if (isFoliage || isLog)
										        fogAmount *= 0.8;
										    vec3 atmosphericFogColor = calculateFogColor(uFogColor, uSunDirection, viewDir, dayFactor);
										    finalColor = mix(finalColor, atmosphericFogColor, fogAmount);
										    finalColor = pow(finalColor, vec3(1.0 / 2.2));

										    if (cameraUnderwater) {
										        vec3 underwaterFog = vec3(0.02, 0.08, 0.15);
										        float uwDist = min(vDistanceToCamera / 25.0, 1.0);
										        finalColor = mix(finalColor, underwaterFog, uwDist * 0.85);
										        finalColor *= vec3(0.4, 0.6, 0.8);
										    }

										    finalColor = mix(uFogColor, finalColor, uChunkOpacity);

										    FragColor = vec4(finalColor, 1.0);

										    float roughness = 0.55;
										    if (isWater)
										        roughness = 0.08;
										    else if (isFoliageEarly)
										        roughness = 0.7;
										    else if (isLog)
										        roughness = 0.6;

										    vec3 viewNormal = normalize(vViewNormal);
										    if (isWater)
										        viewNormal = normalize(mat3(uView) * N);

										    GBuffer = vec4(viewNormal * 0.5 + 0.5, clamp(roughness, 0.04, 1.0));
										}
										""";

		return new ShaderProgram(vertexSource, fragmentSource);
	}

	private ShaderProgram createSkyShader() {
		final String vertexSource = """
									#version 410 core

									layout(location = 0) in vec3 aPosition;

									uniform mat4 uProjection;
									uniform mat4 uView;

									out vec3 vPosition;

									void main() {
									    vPosition = aPosition;
									    mat4 viewNoTranslation = mat4(mat3(uView));
									    vec4 pos = uProjection * viewNoTranslation * vec4(aPosition, 1.0);
									    gl_Position = pos.xyww;
									}
									""";

		final String fragmentSource = """
										#version 410 core

										in vec3 vPosition;

										uniform vec3 uSunDirection;
										uniform float uDayTime;

										layout(location = 0) out vec4 FragColor;
										layout(location = 1) out vec4 GBuffer;

										float hash(vec3 p) {
										    p = fract(p * 0.3183099 + 0.1);
										    p *= 17.0;
										    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
										}

										float noise3D(vec3 p) {
										    vec3 i = floor(p);
										    vec3 f = fract(p);
										    f = f * f * (3.0 - 2.0 * f);
										    return mix(
										        mix(mix(hash(i), hash(i + vec3(1,0,0)), f.x),
										            mix(hash(i + vec3(0,1,0)), hash(i + vec3(1,1,0)), f.x), f.y),
										        mix(mix(hash(i + vec3(0,0,1)), hash(i + vec3(1,0,1)), f.x),
										            mix(hash(i + vec3(0,1,1)), hash(i + vec3(1,1,1)), f.x), f.y), f.z);
										}

										vec3 getSkyColor(vec3 direction, vec3 sunDir, float dayTime) {
										    vec3 dir = normalize(direction);
										    float sunHeight = sunDir.y;

										    float dayFactor = smoothstep(-0.15, 0.35, sunHeight);
										    float sunriseFactor = smoothstep(-0.1, 0.05, sunHeight) * smoothstep(0.25, 0.0, sunHeight);
										    float sunsetFactor = sunriseFactor;

										    float elevation = abs(dir.y);
										    float horizonFactor = 1.0 - pow(elevation, 0.4);
										    float zenithFactor = pow(elevation, 0.8);
										    bool isAboveHorizon = dir.y >= 0.0;

										    vec3 dayZenith = vec3(0.22, 0.42, 0.75);
										    vec3 dayMid = vec3(0.40, 0.58, 0.82);
										    vec3 dayHorizon = vec3(0.65, 0.75, 0.88);

										    vec3 nightZenith = vec3(0.002, 0.005, 0.015);
										    vec3 nightMid = vec3(0.008, 0.012, 0.028);
										    vec3 nightHorizon = vec3(0.015, 0.02, 0.04);

										    vec3 nightNadir = vec3(0.001, 0.002, 0.006);

										    vec3 sunriseZenith = vec3(0.25, 0.35, 0.55);
										    vec3 sunriseMid = vec3(0.55, 0.45, 0.45);
										    vec3 sunriseHorizon = vec3(0.9, 0.55, 0.35);

										    vec3 dayColor = mix(dayHorizon, mix(dayMid, dayZenith, zenithFactor), elevation);
										    vec3 nightColorUp = mix(nightHorizon, mix(nightMid, nightZenith, zenithFactor), elevation);
										    vec3 nightColorDown = mix(nightHorizon, nightNadir, elevation);
										    vec3 nightColor = isAboveHorizon ? nightColorUp : nightColorDown;
										    vec3 sunriseColor = mix(sunriseHorizon, mix(sunriseMid, sunriseZenith, zenithFactor), elevation);

										    vec3 skyColor = mix(nightColor, dayColor, dayFactor);
										    skyColor = mix(skyColor, sunriseColor, sunriseFactor * 0.8);

										    float sunAngle = dot(dir, sunDir);
										    float sunProximity = pow(max(sunAngle, 0.0), 4.0);

										    vec3 sunAtmosphere = mix(vec3(1.0, 0.62, 0.34), vec3(1.0, 0.93, 0.78), dayFactor);
										    skyColor += sunAtmosphere * sunProximity * 0.2 * smoothstep(-0.1, 0.2, sunHeight);

										    float atmosphericDepth = 1.0 / max(abs(dir.y) + 0.15, 0.05);
										    atmosphericDepth = min(atmosphericDepth, 8.0);
										    vec3 scatterColor = mix(vec3(0.9, 0.5, 0.3), vec3(0.7, 0.8, 0.95), dayFactor);
										    float scatter = (1.0 - exp(-atmosphericDepth * 0.08)) * horizonFactor;
										    skyColor = mix(skyColor, scatterColor, scatter * 0.4 * dayFactor);

										    float sunDisc = smoothstep(0.998, 0.9996, sunAngle);
										    float sunLimb = smoothstep(0.9965, 0.998, sunAngle);
										    vec3 sunCore = vec3(1.0, 0.98, 0.9) * 2.2;
										    vec3 sunEdge = vec3(1.0, 0.88, 0.58) * 1.6;
										    vec3 sunResult = mix(sunEdge, sunCore, sunDisc);
										    float sunVisibility = smoothstep(-0.05, 0.1, sunHeight);
										    skyColor += sunResult * sunLimb * sunVisibility;

										    float sunGlow = pow(max(sunAngle, 0.0), 8.0);
										    vec3 glowColor = mix(vec3(1.0, 0.5, 0.2), vec3(1.0, 0.9, 0.7), dayFactor);
										    skyColor += glowColor * sunGlow * 0.08 * sunVisibility;

										    vec3 moonDir = -sunDir;
										    float moonAngle = dot(dir, moonDir);
										    float moonDisc = smoothstep(0.998, 0.9995, moonAngle);
										    float moonGlow = pow(max(moonAngle, 0.0), 32.0);
										    float moonVisibility = smoothstep(0.1, -0.1, sunHeight);
										    vec3 moonColor = vec3(0.7, 0.78, 0.9);
										    skyColor += moonColor * moonDisc * 0.42 * moonVisibility;
										    skyColor += moonColor * moonGlow * 0.03 * moonVisibility;

										    if (dayFactor < 0.3) {
										        float starDensity = 520.0;
										        vec3 starCoord = floor(dir * starDensity);
										        float starHash = hash(starCoord);
										        float colorHash = hash(starCoord + vec3(12.3, 3.4, 7.8));
										        float sizeHash = hash(starCoord + vec3(5.1, 9.7, 2.2));

										        float starThreshold = 0.9945;
										        float horizonCutoff = smoothstep(-0.15, 0.05, dir.y);
										        if (starHash > starThreshold) {
										            float baseBrightness = (starHash - starThreshold) / (1.0 - starThreshold);
										            float twinkle = 0.65 + 0.35 * sin(starHash * 120.0 + uDayTime * 25.0);
										            vec3 coolStar = vec3(0.82, 0.86, 1.0);
										            vec3 warmStar = vec3(1.0, 0.9, 0.82);
										            vec3 starColorTint = mix(coolStar, warmStar, colorHash);
										            float intensity = mix(0.35, 0.95, sizeHash);
										            float starFade = (1.0 - dayFactor / 0.3);
										            skyColor += starColorTint * baseBrightness * twinkle * intensity * starFade * horizonCutoff * 1.1;
										        }
										    }

										    float horizonHaze = pow(horizonFactor, 3.0);
										    vec3 hazeColor = mix(vec3(0.01, 0.015, 0.025), vec3(0.6, 0.65, 0.75), dayFactor);
										    skyColor = mix(skyColor, hazeColor, horizonHaze * 0.5);

										    return skyColor;
										}

										void main() {
										    vec3 color = getSkyColor(vPosition, uSunDirection, uDayTime);
										    color = pow(color, vec3(1.0 / 2.2));
										    FragColor = vec4(color, 1.0);
										    GBuffer = vec4(0.0, 0.0, 0.0, 1.0);
										}
										""";

		return new ShaderProgram(vertexSource, fragmentSource);
	}

	private ShaderProgram createSunShader() {
		final String vertexSource = """
									#version 410 core

									layout(location = 0) in vec3 aPosition;
									layout(location = 1) in vec2 aTexCoord;

									uniform mat4 uProjection;
									uniform mat4 uView;
									uniform vec3 uSunDirection;
									uniform float uSunSize;
									uniform vec3 uSunColor;
									uniform float uSunIntensity;

									out vec2 vUV;
									out vec3 vSunColor;
									out float vSunIntensity;

									void main() {
									    vUV = aTexCoord;
									    vSunColor = uSunColor;
									    vSunIntensity = uSunIntensity;

									    vec3 sunPos = uSunDirection * 90.0;

									    mat4 viewNoTranslation = mat4(mat3(uView));
									    vec3 right = vec3(viewNoTranslation[0][0], viewNoTranslation[1][0], viewNoTranslation[2][0]);
									    vec3 up = vec3(viewNoTranslation[0][1], viewNoTranslation[1][1], viewNoTranslation[2][1]);

									    vec3 worldPos = sunPos + (right * aPosition.x + up * aPosition.y) * uSunSize;

									    gl_Position = uProjection * viewNoTranslation * vec4(worldPos, 1.0);
									}
									""";

		final String fragmentSource = """
										#version 410 core

										in vec2 vUV;
										in vec3 vSunColor;
										in float vSunIntensity;

										layout(location = 0) out vec4 FragColor;
										layout(location = 1) out vec4 GBuffer;

										void main() {
										    vec2 center = vUV - vec2(0.5);
										    float dist = length(center) * 2.0;

										    float core = 1.0 - smoothstep(0.0, 0.4, dist);
										    float edge = 1.0 - smoothstep(0.3, 1.0, dist);

										    vec3 coreColor = vSunColor * 1.4;
										    vec3 edgeColor = mix(vSunColor, vec3(1.0, 0.82, 0.45), 0.5);

										    vec3 color = mix(edgeColor, coreColor, core) * edge * vSunIntensity;
										    float alpha = edge * vSunIntensity;

										    if (alpha < 0.01) discard;

										    FragColor = vec4(color, alpha);
										    GBuffer = vec4(0.0, 0.0, 0.0, 1.0);
										}
										""";

		return new ShaderProgram(vertexSource, fragmentSource);
	}

	private ShaderProgram createShadowShader() {
		final String vertexSource = """
									#version 410 core

									layout(location = 0) in vec3 aPosition;
									layout(location = 1) in vec2 aTexCoord;
									layout(location = 2) in vec3 aNormal;
									layout(location = 3) in vec3 aColor;

									uniform mat4 uLightSpaceMatrix;

									out vec2 vTexCoord;
									out float vIsFoliage;

									void main() {
									    vTexCoord = aTexCoord;
									    vIsFoliage = (aColor.g > aColor.r * 1.1 && aColor.g > aColor.b * 1.1) ? 1.0 : 0.0;

									    vec4 lightSpacePos = uLightSpaceMatrix * vec4(aPosition, 1.0);

									    if (vIsFoliage > 0.5) {
									        lightSpacePos.z -= 0.001 * lightSpacePos.w;
									    }

									    gl_Position = lightSpacePos;
									}
									""";

		final String fragmentSource = """
										#version 410 core

										in vec2 vTexCoord;
										in float vIsFoliage;

										uniform sampler2D uTexture;

										void main() {
										    vec4 texColor = texture(uTexture, vTexCoord);

										    float alphaThreshold = vIsFoliage > 0.5 ? 0.4 : 0.5;
										    if (texColor.a < alphaThreshold)
										        discard;
										}
										""";

		return new ShaderProgram(vertexSource, fragmentSource);
	}

	private void prepareSpawnArea(final float spawnX, final float spawnZ) {
		final int spawnCX = Math.floorDiv((int) spawnX, Chunk.SIZE);
		final int spawnCZ = Math.floorDiv((int) spawnZ, Chunk.SIZE);

		this.world.loadChunkSync(spawnCX, spawnCZ);
		GLFW.glfwPollEvents();
	}

	private float calculateSpawnY(final float spawnX, final float spawnZ) {
		final int x = (int) Math.floor(spawnX);
		final int z = (int) Math.floor(spawnZ);

		for (int y = Chunk.HEIGHT - 3; y >= 1; y--) {
			final BlockType ground = this.world.getBlockAt(x, y, z);
			if (!this.isBlockingSpawnSpace(ground))
				continue;
			final BlockType head = this.world.getBlockAt(x, y + 1, z);
			final BlockType roof = this.world.getBlockAt(x, y + 2, z);
			if (this.isBlockingSpawnSpace(head) || this.isBlockingSpawnSpace(roof))
				continue;
			return y + 3 + Camera.getEyeHeight();
		}
		throw new IllegalStateException("No safe spawn position found at " + x + "," + z);
	}

	private boolean isBlockingSpawnSpace(final BlockType type) {
		if (type == null)
			throw new IllegalStateException("Block type missing during spawn computation");
		return type.isSolid() && type != BlockType.WATER && !type.isPlant();
	}

	private void updateHandMotion() {
		final Vector3f camPos = this.camera.getPosition();
		final float moveDist = camPos.distance(this.lastCameraPos);
		final float moveSpeed = this.deltaTime > 0 ? moveDist / this.deltaTime : 0;
		this.lastCameraPos.set(camPos);

		final boolean shouldSwing = this.camera.isOnGround() && !this.camera.isFlying();

		if (shouldSwing) {
			this.handWalkPhase += moveSpeed * this.deltaTime * 5.0f;
			final float walkSwingMag = Math.min(1.0f, moveSpeed * 0.08f);
			final float swingWave = (float) Math.sin(this.handWalkPhase) * 0.15f * walkSwingMag;
			final float bobWave = (float) Math.abs(Math.cos(this.handWalkPhase * 2.0f)) * 0.1f * walkSwingMag;
			this.handSwing = swingWave + this.handImpulse * 0.3f;
			this.handBob = bobWave + this.handImpulse * 0.15f;
		} else {
			this.handSwing = this.handImpulse * 0.3f;
			this.handBob = this.handImpulse * 0.15f;
		}

		if (this.handImpulse > 0.0f)
			this.handImpulse = Math.max(0.0f, this.handImpulse - this.deltaTime * 2.5f);
	}

	private boolean shouldDrop(final BlockType type) {
		if (type == null)
			throw new IllegalArgumentException("Drop type missing");
		if ((type == BlockType.AIR) || type == BlockType.WATER || type == BlockType.WATER_FLOWING)
			return false;
		if (type == BlockType.LAVA || type == BlockType.LAVA_FLOWING)
			return false;
		if (type == BlockType.BEDROCK)
			return false;
		return true;
	}

	private void updateSunPosition() {
		final float normalizedTime = (float) this.dayTimeTicks / Voxelcraft.TICKS_PER_DAY;
		final float angle = (normalizedTime - 0.25f) * (float) (Math.PI * 2);
		this.sunDirection.set(
				(float) Math.cos(angle),
				(float) Math.sin(angle),
				0.3f).normalize();

		final float sunHeight = this.sunDirection.y;
		if (sunHeight > 0.1f)
			this.sunColor.set(0.95f, 0.92f, 0.85f);
		else if (sunHeight > -0.1f) {
			final float t = (sunHeight + 0.1f) / 0.2f;
			this.sunColor.set(0.95f, 0.55f + t * 0.37f, 0.35f + t * 0.5f);
		} else
			this.sunColor.set(0.08f, 0.08f, 0.15f);

		final float ambientIntensity = Math.max(0.08f, sunHeight * 0.35f + 0.25f);
		this.ambientColor.set(ambientIntensity * 0.95f, ambientIntensity * 0.92f, ambientIntensity * 0.88f);
	}

	private float calculateSkyLightIntensity() {
		final float sunHeight = this.sunDirection.y;

		if (sunHeight > 0.2f)
			return 1.0f;
		if (sunHeight > -0.1f) {
			final float t = (sunHeight + 0.1f) / 0.3f;
			return 0.15f + 0.85f * t * t * (3.0f - 2.0f * t);
		}
		return 0.15f;
	}

	private Biome getCurrentBiome() {
		final Vector3f pos = this.camera.getPosition();
		final int x = (int) Math.floor(pos.x);
		final int z = (int) Math.floor(pos.z);
		final int cx = Math.floorDiv(x, Chunk.SIZE);
		final int cz = Math.floorDiv(z, Chunk.SIZE);
		final Chunk chunk = this.world.getChunk(cx, cz);
		if (chunk == null)
			return null;
		final int lx = Math.floorMod(x, Chunk.SIZE);
		final int lz = Math.floorMod(z, Chunk.SIZE);
		return chunk.getBiomeAt(lx, lz);
	}

	private void processInput() {
		if (this.gameUI.getState() != GameUI.GameState.PLAYING)
			return;

		final boolean forward = this.keys[GLFW.GLFW_KEY_W];
		final boolean backward = this.keys[GLFW.GLFW_KEY_S];
		final boolean left = this.keys[GLFW.GLFW_KEY_A];
		final boolean right = this.keys[GLFW.GLFW_KEY_D];
		final boolean jump = this.keys[GLFW.GLFW_KEY_SPACE];
		final boolean sneak = this.keys[GLFW.GLFW_KEY_LEFT_SHIFT] || this.keys[GLFW.GLFW_KEY_RIGHT_SHIFT];
		final boolean sprint = this.keys[GLFW.GLFW_KEY_LEFT_CONTROL] || this.keys[GLFW.GLFW_KEY_RIGHT_CONTROL];

		this.camera.update(this.deltaTime, forward, backward, left, right, jump, sneak, sprint);
	}

	private void loop() {
		while (!GLFW.glfwWindowShouldClose(this.window)) {
			final double currentTime = GLFW.glfwGetTime();
			this.deltaTime = (float) (currentTime - this.lastFrameTime);
			this.lastFrameTime = currentTime;

			this.deltaTime = Math.min(this.deltaTime, 0.1f);

			this.enforceMouseCaptureState();
			this.processInput();

			if (this.gameUI.getState() == GameUI.GameState.PLAYING) {
				this.world.update(this.camera.getPosition().x, this.camera.getPosition().z, this.deltaTime);
				this.blockSelection.update(this.world, this.camera.getPosition(), this.camera.getFront());
				this.itemSystem.update(this.deltaTime, this.world, this.camera.getPosition());
				this.updateHandMotion();
			}

			this.particleSystem.update(this.deltaTime);

			this.gameUI.update(this.deltaTime);
			this.gameUI.setPlayerFlying(this.camera.isFlying());
			this.gameUI.setPlayerSneaking(this.camera.isSneaking());
			this.gameUI.setCurrentBiome(this.getCurrentBiome());

			this.tickAccumulator += this.deltaTime * Voxelcraft.TICKS_PER_SECOND;
			final int ticksToProcess = (int) this.tickAccumulator;
			if (ticksToProcess > 0) {
				this.tickAccumulator -= ticksToProcess;
				this.worldTicks += ticksToProcess;

				if (this.dayCycleEnabled) {
					this.dayTimeTicks += ticksToProcess;
					if (this.dayTimeTicks >= Voxelcraft.TICKS_PER_DAY)
						this.dayTimeTicks -= Voxelcraft.TICKS_PER_DAY;
				}
			}

			this.updateSunPosition();

			final float dayFactor = Math.max(0, Math.min(1, (this.sunDirection.y + 0.1f) / 0.4f));
			final Vector3f dayFogColor = new Vector3f(0.6f, 0.7f, 0.85f);
			final Vector3f nightFogColor = new Vector3f(0.0f, 0.0f, 0.0f);
			final Vector3f fogColor = new Vector3f(nightFogColor).lerp(dayFogColor, dayFactor);

			if (this.sunDirection.y > -0.1f
					&& this.shadowMap.needsUpdate(this.sunDirection, this.camera.getPosition())) {
				this.shadowMap.updateLightSpaceMatrix(this.sunDirection, this.camera.getPosition());

				this.shadowMap.bindForWriting();

				GL11.glDisable(GL11.GL_CULL_FACE);

				this.shadowShader.use();
				this.shadowShader.setMatrix4f("uLightSpaceMatrix", this.shadowMap.getLightSpaceMatrix());
				this.shadowShader.setInt("uTexture", 0);
				this.textureAtlas.bind(0);

				for (final Chunk chunk : this.world.getChunks()) {
					if (!chunk.isReadyToRender() || !this.isChunkInShadowRange(chunk, this.camera.getPosition()))
						continue;
					chunk.renderOpaque();
				}

				GL11.glEnable(GL11.GL_CULL_FACE);
				this.shadowMap.unbind();
			}

			GL11.glViewport(0, 0, this.framebufferWidth, this.framebufferHeight);

			this.postProcess.bindSceneFBO();
			GL11.glClearColor(fogColor.x, fogColor.y, fogColor.z, 1.0f);
			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

			this.camera.getViewMatrix(this.view);
			this.frustum.update(this.projection, this.view);
			this.occlusionCuller.beginFrame(this.projection, this.view);
			final java.util.List<Chunk> visibleChunks = this.occlusionCuller.cullAndSort(this.world.getChunks(),
					this.camera.getPosition(),
					this.frustum);

			GL11.glDepthMask(false);
			GL11.glDisable(GL11.GL_CULL_FACE);
			GL11.glDisable(GL11.GL_DEPTH_TEST);
			this.skyShader.use();
			this.skyShader.setMatrix4f("uProjection", this.projection);
			this.skyShader.setMatrix4f("uView", this.view);
			this.skyShader.setVec3("uSunDirection", this.sunDirection);
			this.skyShader.setFloat("uDayTime", this.getDayTimeNormalized());
			this.skyRenderer.render();
			GL11.glEnable(GL11.GL_DEPTH_TEST);
			GL11.glDepthFunc(GL11.GL_LEQUAL);

			this.volumetricClouds.update(this.deltaTime);
			this.volumetricClouds.render(this.projection, this.view, this.camera.getPosition(), this.sunDirection,
					this.getDayTimeNormalized());

			GL11.glEnable(GL11.GL_CULL_FACE);
			GL11.glDepthMask(true);

			final float skyLightIntensity = this.calculateSkyLightIntensity();
			this.moonDirection.set(this.sunDirection).negate().normalize();

			this.blockShader.use();
			this.blockShader.setMatrix4f("uProjection", this.projection);
			this.blockShader.setMatrix4f("uView", this.view);
			this.blockShader.setMatrix4f("uLightSpaceMatrix", this.shadowMap.getLightSpaceMatrix());
			this.blockShader.setVec3("uSunDirection", this.sunDirection);
			this.blockShader.setVec3("uMoonDirection", this.moonDirection);
			this.blockShader.setVec3("uSunColor", this.sunColor);
			this.blockShader.setVec3("uAmbientColor", this.ambientColor);
			this.blockShader.setVec3("uCameraPos", this.camera.getPosition());
			this.blockShader.setFloat("uSkyLightIntensity", skyLightIntensity);
			this.blockShader.setFloat("uSmoothLightingStrength", 1.0f);
			this.blockShader.setFloat("uTime", (float) GLFW.glfwGetTime());

			final Vector3f camPos = this.camera.getPosition();
			final BlockType camBlock = this.world.getBlockAt((int) Math.floor(camPos.x), (int) Math.floor(camPos.y),
					(int) Math.floor(camPos.z));
			this.blockShader.setBool("uCameraUnderwater", camBlock == BlockType.WATER);

			final float renderDist = World.RENDER_DISTANCE * Chunk.SIZE;
			this.blockShader.setFloat("uFogStart", renderDist * 0.15f);
			this.blockShader.setFloat("uFogEnd", renderDist * 1.1f);
			this.blockShader.setVec3("uFogColor", fogColor);
			this.blockShader.setFloat("uShadowDistance", ShadowMap.SHADOW_DISTANCE);
			this.blockShader.setInt("uTexture", 0);
			this.blockShader.setInt("uShadowMap", 1);
			this.blockShader.setInt("uNoiseTexture", 2);
			this.blockShader.setVec3("uMoonColor", this.moonColor);

			GL11.glDisable(GL11.GL_CULL_FACE);
			GL11.glEnable(GL11.GL_DEPTH_TEST);
			GL11.glDepthFunc(GL11.GL_LEQUAL);
			GL11.glDepthMask(true);

			this.blockShader.use();
			this.textureAtlas.bind(0);
			this.shadowMap.bindForReading(1);
			this.noiseTexture.bind(2);

			GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
			GL11.glPolygonOffset(0.5f, 0.5f);
			GL11.glDisable(GL11.GL_BLEND);
			GL11.glDepthMask(true);
			GL11.glDisable(GL11.GL_CULL_FACE);

			this.chunkBatchRenderer.beginFrame();
			this.chunkBatchRenderer.renderOpaqueChunks(visibleChunks, this.blockShader);

			GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);

			this.itemSystem.render(this.projection, this.view);
			this.handRenderer.render(this.inventory.getSelectedBlock(), this.projection, this.camera.getYaw(),
					this.camera.getPitch(),
					this.handSwing, this.handBob, this.handImpulse);

			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			GL11.glEnable(GL11.GL_DEPTH_TEST);
			GL11.glDepthFunc(GL11.GL_LEQUAL);
			GL11.glDepthMask(false);
			GL11.glDisable(GL11.GL_CULL_FACE);

			this.blockShader.use();
			this.textureAtlas.bind(0);

			this.chunkBatchRenderer.renderWaterChunks(visibleChunks, this.blockShader);
			this.chunkBatchRenderer.endFrame();

			GL11.glDepthMask(true);
			GL11.glDisable(GL11.GL_BLEND);
			GL11.glEnable(GL11.GL_CULL_FACE);

			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
			GL11.glEnable(GL11.GL_DEPTH_TEST);
			GL11.glDepthMask(false);
			GL11.glDisable(GL11.GL_CULL_FACE);
			this.sunShader.use();
			this.sunShader.setMatrix4f("uProjection", this.projection);
			this.sunShader.setMatrix4f("uView", this.view);
			this.sunShader.setVec3("uSunDirection", this.sunDirection);
			this.sunShader.setVec3("uSunColor", this.sunColor);
			this.sunShader.setFloat("uSunSize", 4.5f);
			this.sunShader.setFloat("uSunIntensity", 0.7f);
			this.skyRenderer.renderSun();
			if (this.moonDirection.y > -0.2f) {
				final float moonIntensity = Math.max(0.0f, this.moonDirection.y + 0.2f) * 0.6f;
				this.sunShader.setVec3("uSunDirection", this.moonDirection);
				this.sunShader.setVec3("uSunColor", this.moonColor);
				this.sunShader.setFloat("uSunSize", 3.2f);
				this.sunShader.setFloat("uSunIntensity", moonIntensity);
				this.skyRenderer.renderSun();
			}
			GL11.glDepthMask(true);
			GL11.glEnable(GL11.GL_CULL_FACE);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			GL11.glDisable(GL11.GL_BLEND);

			GL13.glActiveTexture(GL13.GL_TEXTURE1);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
			GL13.glActiveTexture(GL13.GL_TEXTURE0);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
			GL20.glUseProgram(0);

			GL11.glDisable(GL11.GL_DEPTH_TEST);
			this.blockSelection.render(this.world, this.projection, this.view);
			GL11.glEnable(GL11.GL_DEPTH_TEST);

			this.particleSystem.render(this.projection, this.view, this.camera.getPosition());

			this.postProcess.unbind();
			GL11.glViewport(0, 0, this.framebufferWidth, this.framebufferHeight);
			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

			GL11.glDisable(GL11.GL_DEPTH_TEST);

			float godRayIntensity = 0.6f;
			if (camBlock == BlockType.WATER)
				godRayIntensity *= 0.4f;

			this.godRays.render(this.postProcess, this.projection, this.view, this.camera.getPosition(),
					this.sunDirection, this.sunColor,
					godRayIntensity, (float) GLFW.glfwGetTime());

			GL11.glEnable(GL11.GL_DEPTH_TEST);

			this.gameUI.render(this.camera.getPosition(), this.camera.getYaw(), this.camera.getPitch());
			this.inventory.render();

			GLFW.glfwSwapBuffers(this.window);
			GLFW.glfwPollEvents();
		}
	}

	private void cleanup() {
		Voxelcraft.log.info("Cleaning up...");

		try {
			if (this.world != null)
				this.world.cleanup();
			if (this.textureAtlas != null)
				this.textureAtlas.cleanup();
			if (this.blockShader != null)
				this.blockShader.cleanup();
			if (this.skyShader != null)
				this.skyShader.cleanup();
			if (this.sunShader != null)
				this.sunShader.cleanup();
			if (this.shadowShader != null)
				this.shadowShader.cleanup();
			if (this.shadowMap != null)
				this.shadowMap.cleanup();
			if (this.skyRenderer != null)
				this.skyRenderer.cleanup();
			if (this.volumetricClouds != null)
				this.volumetricClouds.cleanup();
			if (this.postProcess != null)
				this.postProcess.cleanup();
			if (this.godRays != null)
				this.godRays.cleanup();
			if (this.noiseTexture != null)
				this.noiseTexture.cleanup();
			if (this.gameUI != null)
				this.gameUI.cleanup();
			if (this.blockSelection != null)
				this.blockSelection.cleanup();
			if (this.inventory != null)
				this.inventory.cleanup();
			if (this.particleSystem != null)
				this.particleSystem.cleanup();
			if (this.handRenderer != null)
				this.handRenderer.cleanup();
			if (this.itemSystem != null)
				this.itemSystem.cleanup();
		} catch (final Exception e) {
			Voxelcraft.log.log(Level.WARNING, "Error during cleanup", e);
		}

		if (this.window != 0) {
			Callbacks.glfwFreeCallbacks(this.window);
			GLFW.glfwDestroyWindow(this.window);
		}
		GLFW.glfwTerminate();

		final GLFWErrorCallback callback = GLFW.glfwSetErrorCallback(null);
		if (callback != null)
			callback.free();
	}

	public static void main(final String[] args) {
		new Voxelcraft().run();
	}
}
