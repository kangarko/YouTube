package com.matejpacan.voxelcraft;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera {

	private static final float PLAYER_WIDTH = 0.6f;
	private static final float PLAYER_HEIGHT = 1.8f;
	private static final float EYE_HEIGHT = 1.62f;

	private final Vector3f position;
	private final Vector3f rotation;
	private final Vector3f front;
	private final Vector3f up;
	private final Vector3f right;

	private float moveSpeed = 10.0f;

	public Camera() {
		this.position = new Vector3f(8.0f, 10.0f, 8.0f);
		this.rotation = new Vector3f(20.0f, 0.0f, 0.0f);
		this.front = new Vector3f(0.0f, 0.0f, -1.0f);
		this.up = new Vector3f(0.0f, 1.0f, 0.0f);
		this.right = new Vector3f(1.0f, 0.0f, 0.0f);
		this.updateVectors();
	}

	public void move(final float dx, final float dy, final float dz, final float deltaTime, final World world) {
		final float velocity = this.moveSpeed * deltaTime;

		final Vector3f frontFlat = new Vector3f(this.front.x, 0, this.front.z).normalize();
		final Vector3f rightFlat = new Vector3f(this.right.x, 0, this.right.z).normalize();

		final float feetY = this.position.y - Camera.EYE_HEIGHT;

		final float newX = this.position.x + frontFlat.x * dz * velocity + rightFlat.x * dx * velocity;
		if (this.canMoveAt(newX, feetY, this.position.z, world))
			this.position.x = newX;

		final float newZ = this.position.z + frontFlat.z * dz * velocity + rightFlat.z * dx * velocity;
		if (this.canMoveAt(this.position.x, feetY, newZ, world))
			this.position.z = newZ;

		final float newFeetY = feetY + dy * velocity;
		if (this.canMoveAt(this.position.x, newFeetY, this.position.z, world))
			this.position.y = newFeetY + Camera.EYE_HEIGHT;
	}

	private boolean canMoveAt(final float x, final float feetY, final float z, final World world) {
		final float halfWidth = Camera.PLAYER_WIDTH / 2.0f;

		final float minX = x - halfWidth;
		final float maxX = x + halfWidth;
		final float maxY = feetY + Camera.PLAYER_HEIGHT;
		final float minZ = z - halfWidth;
		final float maxZ = z + halfWidth;

		final int blockMinX = (int) Math.floor(minX);
		final int blockMaxX = (int) Math.floor(maxX);
		final int blockMinY = (int) Math.floor(feetY);
		final int blockMaxY = (int) Math.floor(maxY);
		final int blockMinZ = (int) Math.floor(minZ);
		final int blockMaxZ = (int) Math.floor(maxZ);

		for (int bx = blockMinX; bx <= blockMaxX; bx++)
			for (int by = blockMinY; by <= blockMaxY; by++)
				for (int bz = blockMinZ; bz <= blockMaxZ; bz++)
					if (world.getBlock(bx, by, bz).blocksMovement())
						return false;

		return true;
	}

	public void rotate(final float deltaX, final float deltaY) {
		final float mouseSensitivity = 0.1f;
		this.rotation.y += deltaX * mouseSensitivity;
		this.rotation.x += deltaY * mouseSensitivity;

		this.rotation.x = Math.max(-89.0f, Math.min(89.0f, this.rotation.x));

		this.updateVectors();
	}

	private void updateVectors() {
		final float yaw = (float) Math.toRadians(this.rotation.y);
		final float pitch = (float) Math.toRadians(this.rotation.x);

		this.front.x = (float) (Math.cos(pitch) * Math.sin(yaw));
		this.front.y = (float) -Math.sin(pitch);
		this.front.z = (float) -(Math.cos(pitch) * Math.cos(yaw));
		this.front.normalize();

		this.right.set(this.front).cross(new Vector3f(0, 1, 0)).normalize();
		this.up.set(this.right).cross(this.front).normalize();
	}

	public void getViewMatrix(final Matrix4f dest) {
		final Vector3f target = new Vector3f(this.position).add(this.front);
		dest.identity().lookAt(this.position, target, this.up);
	}

	public Vector3f getPosition() {
		return this.position;
	}

	public void setPosition(final float x, final float y, final float z) {
		this.position.set(x, y, z);
	}

	public void setMoveSpeed(final float speed) {
		this.moveSpeed = speed;
	}
}
