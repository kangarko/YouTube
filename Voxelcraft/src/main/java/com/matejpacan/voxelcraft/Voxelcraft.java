package com.matejpacan.voxelcraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

public class Voxelcraft {

	private static final int RENDER_DISTANCE = 24;

	private Window window;
	private Shader shader;
	private Skybox skybox;
	private Texture terrainTexture;
	private MouseInput mouseInput;
	private Camera camera;
	private World world;
	private Frustum frustum;

	private Framebuffer sceneFBO;
	private Framebuffer occlusionFBO;
	private GodRays godRays;
	private ShadowMap shadowMap;

	private final Matrix4f projectionMatrix = new Matrix4f();
	private final Matrix4f viewMatrix = new Matrix4f();
	private final Matrix4f modelMatrix = new Matrix4f();

	private long lastFrameTime;
	private float deltaTime;
	private boolean cursorLocked = true;

	static void main(final String[] args) {
		new Voxelcraft().run();
	}

	public void run() {
		this.init();
		this.loop();
		this.cleanup();
	}

	private void init() {
		this.window = new Window(1280, 720, "VoxelCraft");
		this.window.init();

		GL.createCapabilities();

		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glCullFace(GL11.GL_BACK);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

		this.mouseInput = new MouseInput(this.window);
		this.camera = new Camera();
		this.camera.setMoveSpeed(15.0f);
		this.frustum = new Frustum();

		this.shader = new Shader();
		this.shader.createVertexShader(this.loadResource("/shaders/vertex.glsl"));
		this.shader.createFragmentShader(this.loadResource("/shaders/fragment.glsl"));
		this.shader.link();

		this.shader.createUniform("projectionMatrix");
		this.shader.createUniform("viewMatrix");
		this.shader.createUniform("modelMatrix");
		this.shader.createUniform("textureSampler");
		this.shader.createUniform("lightSpaceMatrix");
		this.shader.createUniform("shadowMap");

		this.skybox = new Skybox(
				this.loadResource("/shaders/skybox_vertex.glsl"),
				this.loadResource("/shaders/skybox_fragment.glsl"));

		this.terrainTexture = Texture.loadTexture("/terrain.png");

		final int fbWidth = this.window.getFramebufferWidth();
		final int fbHeight = this.window.getFramebufferHeight();
		this.sceneFBO = new Framebuffer(fbWidth, fbHeight);
		this.occlusionFBO = new Framebuffer(fbWidth / 2, fbHeight / 2);

		this.godRays = new GodRays(
				this.loadResource("/shaders/godrays_occlusion_vertex.glsl"),
				this.loadResource("/shaders/godrays_occlusion_fragment.glsl"),
				this.loadResource("/shaders/godrays_skybox_fragment.glsl"),
				this.loadResource("/shaders/godrays_composite_vertex.glsl"),
				this.loadResource("/shaders/godrays_composite_fragment.glsl"));

		this.shadowMap = new ShadowMap(
				this.loadResource("/shaders/shadow_vertex.glsl"),
				this.loadResource("/shaders/shadow_fragment.glsl"));

		this.world = new World(Voxelcraft.RENDER_DISTANCE);

		this.spawnPlayerAboveTerrain();

		this.updateProjectionMatrix();
		this.lastFrameTime = System.nanoTime();

		GLFW.glfwSetInputMode(this.window.getHandle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
	}

	private void spawnPlayerAboveTerrain() {
		final int spawnX = 8;
		final int spawnZ = 8;

		this.world.update(spawnX, spawnZ);

		final int surfaceY = this.world.getSurfaceHeight(spawnX, spawnZ);
		this.camera.setPosition(spawnX, surfaceY + 2.0f, spawnZ);
	}

	private void updateProjectionMatrix() {
		final float aspectRatio = (float) this.window.getFramebufferWidth() / this.window.getFramebufferHeight();
		this.projectionMatrix.setPerspective(
				(float) Math.toRadians(70.0f),
				aspectRatio,
				0.1f,
				1000.0f);
	}

	private void loop() {
		while (!this.window.shouldClose()) {
			final long currentTime = System.nanoTime();
			this.deltaTime = (currentTime - this.lastFrameTime) / 1_000_000_000.0f;
			this.lastFrameTime = currentTime;

			this.deltaTime = Math.min(this.deltaTime, 0.1f);

			this.input();
			this.update();
			this.render();
		}
	}

	private void input() {
		this.mouseInput.input();

		if (GLFW.glfwGetKey(this.window.getHandle(), GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS)
			if (this.cursorLocked) {
				GLFW.glfwSetInputMode(this.window.getHandle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
				this.cursorLocked = false;
			} else
				GLFW.glfwSetWindowShouldClose(this.window.getHandle(), true);

		if (GLFW.glfwGetMouseButton(this.window.getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS
				&& !this.cursorLocked) {
			GLFW.glfwSetInputMode(this.window.getHandle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
			this.cursorLocked = true;
		}

		if (this.cursorLocked)
			this.camera.rotate(this.mouseInput.getDeltaX(), this.mouseInput.getDeltaY());

		float moveX = 0, moveY = 0, moveZ = 0;

		if (GLFW.glfwGetKey(this.window.getHandle(), GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS)
			moveZ += 1;
		if (GLFW.glfwGetKey(this.window.getHandle(), GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS)
			moveZ -= 1;
		if (GLFW.glfwGetKey(this.window.getHandle(), GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS)
			moveX -= 1;
		if (GLFW.glfwGetKey(this.window.getHandle(), GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS)
			moveX += 1;
		if (GLFW.glfwGetKey(this.window.getHandle(), GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS)
			moveY += 1;
		if (GLFW.glfwGetKey(this.window.getHandle(), GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS)
			moveY -= 1;

		final boolean sprint = GLFW.glfwGetKey(this.window.getHandle(), GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS;
		if (sprint)
			this.camera.setMoveSpeed(30.0f);
		else
			this.camera.setMoveSpeed(15.0f);

		this.camera.move(moveX, moveY, moveZ, this.deltaTime, this.world);
	}

	private void update() {
		if (this.window.isResized()) {
			this.updateProjectionMatrix();
			final int fbWidth = this.window.getFramebufferWidth();
			final int fbHeight = this.window.getFramebufferHeight();
			this.sceneFBO.resize(fbWidth, fbHeight);
			this.occlusionFBO.resize(fbWidth / 2, fbHeight / 2);
			this.window.setResized(false);
		}

		this.world.update(this.camera.getPosition().x, this.camera.getPosition().z);
	}

	private void render() {
		this.camera.getViewMatrix(this.viewMatrix);
		this.frustum.update(this.projectionMatrix, this.viewMatrix);

		final Vector3f sunDirection = this.skybox.getSunDirection();
		final Vector2f sunScreenPos = this.godRays.computeSunScreenPosition(sunDirection, this.projectionMatrix,
				this.viewMatrix);

		this.renderShadowPass(sunDirection);
		this.renderOcclusionPass(sunDirection);
		this.renderScenePass();
		this.renderCompositePass(sunScreenPos);

		this.window.update();
	}

	private void renderShadowPass(final Vector3f sunDirection) {
		this.shadowMap.updateLightSpaceMatrix(sunDirection, this.camera.getPosition());
		this.shadowMap.bind();

		GL11.glCullFace(GL11.GL_FRONT);

		final Shader depthShader = this.shadowMap.getDepthShader();
		depthShader.bind();

		this.modelMatrix.identity();
		depthShader.setUniform("lightSpaceMatrix", this.shadowMap.getLightSpaceMatrix());
		depthShader.setUniform("modelMatrix", this.modelMatrix);
		depthShader.setUniform("textureSampler", 0);

		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		this.terrainTexture.bind();

		final Map<Long, Mesh> opaqueMeshes = this.world.getOpaqueChunkMeshes();
		for (final Mesh mesh : opaqueMeshes.values())
			mesh.render();

		depthShader.unbind();

		GL11.glCullFace(GL11.GL_BACK);
		this.shadowMap.unbind();
	}

	private void renderOcclusionPass(final Vector3f sunDirection) {
		this.occlusionFBO.bind();
		GL11.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

		this.godRays.renderSkyboxOcclusion(this.projectionMatrix, this.viewMatrix, sunDirection,
				this.skybox.getVaoId(), this.skybox.getVertexCount());

		final Shader occlusionShader = this.godRays.getOcclusionShader();
		occlusionShader.bind();

		this.modelMatrix.identity();
		occlusionShader.setUniform("projectionMatrix", this.projectionMatrix);
		occlusionShader.setUniform("viewMatrix", this.viewMatrix);
		occlusionShader.setUniform("modelMatrix", this.modelMatrix);
		occlusionShader.setUniform("textureSampler", 0);

		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		this.terrainTexture.bind();

		final Map<Long, Chunk> chunks = this.world.getChunks();
		final Map<Long, Mesh> opaqueMeshes = this.world.getOpaqueChunkMeshes();
		final Map<Long, Mesh> transparentMeshes = this.world.getTransparentChunkMeshes();

		for (final Map.Entry<Long, Mesh> entry : opaqueMeshes.entrySet()) {
			final Chunk chunk = chunks.get(entry.getKey());
			if (chunk == null || this.frustum.isChunkHidden(chunk.getChunkX(), chunk.getChunkZ()))
				continue;

			occlusionShader.setUniform("isLeaves", chunk.hasLeaves() ? 1 : 0);
			entry.getValue().render();
		}

		for (final Map.Entry<Long, Mesh> entry : transparentMeshes.entrySet()) {
			final Chunk chunk = chunks.get(entry.getKey());
			if (chunk == null || this.frustum.isChunkHidden(chunk.getChunkX(), chunk.getChunkZ()))
				continue;

			occlusionShader.setUniform("isLeaves", chunk.hasLeaves() ? 1 : 0);
			entry.getValue().render();
		}

		occlusionShader.unbind();
		this.occlusionFBO.unbind();
	}

	private void renderScenePass() {
		this.sceneFBO.bind();
		GL11.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

		final int[] framebufferSize = this.window.getFramebufferSize();
		GL11.glViewport(0, 0, framebufferSize[0], framebufferSize[1]);

		this.skybox.render(this.projectionMatrix, this.viewMatrix);

		this.shader.bind();

		this.modelMatrix.identity();

		this.shader.setUniform("projectionMatrix", this.projectionMatrix);
		this.shader.setUniform("viewMatrix", this.viewMatrix);
		this.shader.setUniform("modelMatrix", this.modelMatrix);
		this.shader.setUniform("textureSampler", 0);
		this.shader.setUniform("lightSpaceMatrix", this.shadowMap.getLightSpaceMatrix());
		this.shader.setUniform("shadowMap", 1);

		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		this.terrainTexture.bind();

		this.shadowMap.bindDepthTexture(1);

		final Map<Long, Chunk> chunks = this.world.getChunks();
		final Map<Long, Mesh> opaqueMeshes = this.world.getOpaqueChunkMeshes();
		final Map<Long, Mesh> transparentMeshes = this.world.getTransparentChunkMeshes();

		final Vector3f cameraPos = this.camera.getPosition();

		GL11.glDepthMask(true);
		for (final Map.Entry<Long, Mesh> entry : opaqueMeshes.entrySet()) {
			final Chunk chunk = chunks.get(entry.getKey());
			if (chunk == null || this.frustum.isChunkHidden(chunk.getChunkX(), chunk.getChunkZ()))
				continue;

			entry.getValue().render();
		}

		final List<Map.Entry<Long, Mesh>> sortedTransparent = new ArrayList<>();
		for (final Map.Entry<Long, Mesh> entry : transparentMeshes.entrySet())
			if (chunks.containsKey(entry.getKey()))
				sortedTransparent.add(entry);

		sortedTransparent.sort((a, b) -> {
			final Chunk chunkA = chunks.get(a.getKey());
			final Chunk chunkB = chunks.get(b.getKey());

			final float centerAX = chunkA.getWorldX() + Chunk.WIDTH / 2.0f;
			final float centerAZ = chunkA.getWorldZ() + Chunk.DEPTH / 2.0f;
			final float centerBX = chunkB.getWorldX() + Chunk.WIDTH / 2.0f;
			final float centerBZ = chunkB.getWorldZ() + Chunk.DEPTH / 2.0f;

			final float distA = (centerAX - cameraPos.x) * (centerAX - cameraPos.x)
					+ (centerAZ - cameraPos.z) * (centerAZ - cameraPos.z);
			final float distB = (centerBX - cameraPos.x) * (centerBX - cameraPos.x)
					+ (centerBZ - cameraPos.z) * (centerBZ - cameraPos.z);

			return Float.compare(distB, distA);
		});

		GL11.glDepthMask(false);
		for (final Map.Entry<Long, Mesh> entry : sortedTransparent) {
			final Chunk chunk = chunks.get(entry.getKey());
			if (chunk == null || this.frustum.isChunkHidden(chunk.getChunkX(), chunk.getChunkZ()))
				continue;

			entry.getValue().render();
		}
		GL11.glDepthMask(true);

		this.shader.unbind();
		this.sceneFBO.unbind();
	}

	private void renderCompositePass(final Vector2f sunScreenPos) {
		final int[] framebufferSize = this.window.getFramebufferSize();
		GL11.glViewport(0, 0, framebufferSize[0], framebufferSize[1]);
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

		this.godRays.renderComposite(this.sceneFBO, this.occlusionFBO, sunScreenPos);
	}

	private void cleanup() {
		if (this.world != null)
			this.world.cleanup();
		if (this.shader != null)
			this.shader.cleanup();
		if (this.skybox != null)
			this.skybox.cleanup();
		if (this.terrainTexture != null)
			this.terrainTexture.cleanup();
		if (this.godRays != null)
			this.godRays.cleanup();
		if (this.shadowMap != null)
			this.shadowMap.cleanup();
		if (this.sceneFBO != null)
			this.sceneFBO.cleanup();
		if (this.occlusionFBO != null)
			this.occlusionFBO.cleanup();
		if (this.window != null)
			this.window.cleanup();
	}

	private String loadResource(final String path) {
		try (var inputStream = this.getClass().getResourceAsStream(path)) {
			if (inputStream == null)
				throw new RuntimeException("Resource not found: " + path);
			return new String(inputStream.readAllBytes());
		} catch (final Exception e) {
			throw new RuntimeException("Failed to load resource: " + path, e);
		}
	}
}
