package com.matejpacan.voxelcraft;

import org.joml.Vector3f;

public abstract class Entity {

	protected final Vector3f position = new Vector3f();
	protected final Vector3f velocity = new Vector3f();
	protected float age = 0.0f;
	protected boolean removed = false;
	protected boolean renderEnabled = true;
	protected boolean updateEnabled = true;

	public Vector3f getPosition() {
		return this.position;
	}

	public Vector3f getVelocity() {
		return this.velocity;
	}

	public float getAge() {
		return this.age;
	}

	public boolean isRemoved() {
		return this.removed;
	}

	public boolean isRenderEnabled() {
		return this.renderEnabled;
	}

	public boolean isUpdateEnabled() {
		return this.updateEnabled;
	}

	public void setRenderEnabled(boolean renderEnabled) {
		this.renderEnabled = renderEnabled;
	}

	public void setUpdateEnabled(boolean updateEnabled) {
		this.updateEnabled = updateEnabled;
	}

	public void markRemoved() {
		this.removed = true;
	}

	public void step(float deltaTime) {
		if (!this.updateEnabled)
			return;
		this.age += deltaTime;
		this.onUpdate(deltaTime);
	}

	protected abstract void onUpdate(float deltaTime);

	public abstract void render(org.joml.Matrix4f projection, org.joml.Matrix4f view);
}
