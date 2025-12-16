package com.matejpacan.voxelcraft;

import java.nio.IntBuffer;

import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public class Window {

	private final int width;
	private final int height;
	private final String title;
	private long handle;
	private boolean resized;
	private int framebufferWidth;
	private int framebufferHeight;
	private GLFWFramebufferSizeCallback framebufferSizeCallback;

	public Window(final int width, final int height, final String title) {
		this.width = width;
		this.height = height;
		this.title = title;
		this.resized = false;
	}

	public void init() {
		GLFWErrorCallback.createPrint(System.err).set();

		if (!GLFW.glfwInit())
			throw new RuntimeException("Unable to initialize GLFW");

		GLFW.glfwDefaultWindowHints();
		GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
		GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);

		GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
		GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 1);
		GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
		GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);

		GLFW.glfwWindowHint(GLFW.GLFW_COCOA_RETINA_FRAMEBUFFER, GLFW.GLFW_TRUE);

		GLFW.glfwWindowHint(GLFW.GLFW_SAMPLES, 4);

		this.handle = GLFW.glfwCreateWindow(this.width, this.height, this.title, MemoryUtil.NULL, MemoryUtil.NULL);
		if (this.handle == MemoryUtil.NULL)
			throw new RuntimeException("Failed to create the GLFW window");

		this.framebufferSizeCallback = GLFWFramebufferSizeCallback.create((_, w, h) -> {
			this.framebufferWidth = w;
			this.framebufferHeight = h;
			this.resized = true;
		});
		GLFW.glfwSetFramebufferSizeCallback(this.handle, this.framebufferSizeCallback);

		try (MemoryStack stack = MemoryStack.stackPush()) {
			final IntBuffer pWidth = stack.mallocInt(1);
			final IntBuffer pHeight = stack.mallocInt(1);

			GLFW.glfwGetWindowSize(this.handle, pWidth, pHeight);

			final GLFWVidMode vidMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
			if (vidMode == null)
				throw new RuntimeException("Failed to get video mode");

			GLFW.glfwSetWindowPos(
					this.handle,
					(vidMode.width() - pWidth.get(0)) / 2,
					(vidMode.height() - pHeight.get(0)) / 2);

			final IntBuffer fbWidth = stack.mallocInt(1);
			final IntBuffer fbHeight = stack.mallocInt(1);
			GLFW.glfwGetFramebufferSize(this.handle, fbWidth, fbHeight);
			this.framebufferWidth = fbWidth.get(0);
			this.framebufferHeight = fbHeight.get(0);
		}

		GLFW.glfwMakeContextCurrent(this.handle);
		GLFW.glfwSwapInterval(1);
		GLFW.glfwShowWindow(this.handle);
	}

	public void update() {
		GLFW.glfwSwapBuffers(this.handle);
		GLFW.glfwPollEvents();
	}

	public boolean shouldClose() {
		return GLFW.glfwWindowShouldClose(this.handle);
	}

	public void cleanup() {
		Callbacks.glfwFreeCallbacks(this.handle);
		GLFW.glfwDestroyWindow(this.handle);
		GLFW.glfwTerminate();

		final GLFWErrorCallback errorCallback = GLFW.glfwSetErrorCallback(null);
		if (errorCallback != null)
			errorCallback.free();

		if (this.framebufferSizeCallback != null)
			try {
				this.framebufferSizeCallback.free();
			} catch (final NullPointerException e) {
				// Ignore
			}
	}

	public long getHandle() {
		return this.handle;
	}

	public int getFramebufferWidth() {
		return this.framebufferWidth;
	}

	public int getFramebufferHeight() {
		return this.framebufferHeight;
	}

	public int[] getFramebufferSize() {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			final IntBuffer w = stack.mallocInt(1);
			final IntBuffer h = stack.mallocInt(1);
			GLFW.glfwGetFramebufferSize(this.handle, w, h);
			return new int[] { w.get(0), h.get(0) };
		}
	}

	public boolean isResized() {
		return this.resized;
	}

	public void setResized(final boolean resized) {
		this.resized = resized;
	}
}
