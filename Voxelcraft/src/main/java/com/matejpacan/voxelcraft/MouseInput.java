package com.matejpacan.voxelcraft;

import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

public class MouseInput {

	private final Vector2f previousPos;
	private final Vector2f currentPos;
	private final Vector2f deltaPos;
	private boolean firstMouse = true;

	public MouseInput(final Window window) {
		this.previousPos = new Vector2f();
		this.currentPos = new Vector2f();
		this.deltaPos = new Vector2f();

		GLFW.glfwSetCursorPosCallback(window.getHandle(), (_, xpos, ypos) -> {
			this.currentPos.x = (float) xpos;
			this.currentPos.y = (float) ypos;
		});

		GLFW.glfwSetCursorEnterCallback(window.getHandle(), (_, entered) -> {
			if (entered)
				this.firstMouse = true;
		});
	}

	public void input() {
		if (this.firstMouse) {
			this.previousPos.set(this.currentPos);
			this.firstMouse = false;
			this.deltaPos.set(0, 0);
			return;
		}

		this.deltaPos.x = this.currentPos.x - this.previousPos.x;
		this.deltaPos.y = this.currentPos.y - this.previousPos.y;

		this.previousPos.set(this.currentPos);
	}

	public float getDeltaX() {
		return this.deltaPos.x;
	}

	public float getDeltaY() {
		return this.deltaPos.y;
	}
}
