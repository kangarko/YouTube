package com.matejpacan.voxelcraft;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import lombok.Getter;
import lombok.Setter;

public class Camera {

	private static final float PLAYER_WIDTH = 0.6f;
	private static final float PLAYER_HEIGHT = 1.8f;
	private static final float EYE_HEIGHT = 1.62f;
	private static final float SNEAK_EYE_HEIGHT = 1.50f;
	private static final float COLLISION_EPSILON = 0.001f;

	private static final float GRAVITY = 32.0f;
	private static final float JUMP_VELOCITY = 8.5f;
	private static final float TERMINAL_VELOCITY = 50.0f;
	private static final float WALK_SPEED = 4.3f;
	private static final float SNEAK_SPEED = 1.3f;
	private static final float FLY_SPEED = 10.0f;
	private static final float SPRINT_MULTIPLIER = 1.5f;
	private static final float FLY_SPRINT_MULTIPLIER = 3.0f;
	private static final float JUMP_COOLDOWN = 0.22f;

	private static final float DOUBLE_TAP_TIME = 0.3f;

	private static final float SPEED_TRANSITION_RATE = 8.0f;
	private static final float HEAD_BOB_FREQUENCY_WALK = 8.0f;
	private static final float HEAD_BOB_FREQUENCY_SPRINT = 12.0f;
	private static final float HEAD_BOB_VERTICAL_AMPLITUDE = 0.035f;
	private static final float HEAD_BOB_HORIZONTAL_AMPLITUDE = 0.015f;
	private static final float FLIGHT_TRANSITION_DURATION = 0.4f;
	private static final float FLIGHT_LIFT_HEIGHT = 0.3f;

	@Getter
	private final Vector3f position;
	private final Vector3f front;
	private final Vector3f up;
	private final Vector3f right;
	private final Vector3f worldUp;

	private final Vector3f velocity = new Vector3f(0, 0, 0);

	@Getter
	private float yaw;
	@Getter
	private float pitch;

	@Setter
	private World world;

	@Getter
	private boolean flying = false;
	@Getter
	private boolean sneaking = false;
	private boolean onGround = false;
	private float lastSpacePressTime = -1.0f;
	private float lastJumpTime = -1.0f;
	private float gameTime = 0.0f;
	private boolean spaceWasPressed = false;

	private float currentSpeed = 0.0f;
	private float targetSpeed = 0.0f;
	private float headBobPhase = 0.0f;
	private float headBobOffsetY = 0.0f;
	private float headBobOffsetX = 0.0f;
	private float currentEyeHeight = EYE_HEIGHT;

	private float flightTransitionProgress = 0.0f;
	private boolean flightTransitionActive = false;
	private boolean flightTransitionToFly = false;
	private float flightTransitionStartY = 0.0f;

	private static final float MOUSE_SENSITIVITY = 0.1f;
	private static final float MAX_PITCH = 89.0f;

	public Camera(Vector3f position, float yaw, float pitch) {
		this.position = new Vector3f(position);
		this.front = new Vector3f(0, 0, -1);
		this.up = new Vector3f(0, 1, 0);
		this.right = new Vector3f(1, 0, 0);
		this.worldUp = new Vector3f(0, 1, 0);
		this.yaw = yaw;
		this.pitch = pitch;

		this.updateCameraVectors();
	}

	private void updateCameraVectors() {
		final float yawRad = (float) Math.toRadians(this.yaw);
		final float pitchRad = (float) Math.toRadians(this.pitch);

		this.front.x = (float) (Math.cos(yawRad) * Math.cos(pitchRad));
		this.front.y = (float) Math.sin(pitchRad);
		this.front.z = (float) (Math.sin(yawRad) * Math.cos(pitchRad));
		this.front.normalize();

		this.front.cross(this.worldUp, this.right).normalize();
		this.right.cross(this.front, this.up).normalize();
	}

	public static float getEyeHeight() {
		return EYE_HEIGHT;
	}

	public void setFlying(boolean flying) {
		this.flying = flying;
		if (flying)
			this.velocity.y = 0;
	}

	public void processMouseMovement(float xOffset, float yOffset) {
		xOffset *= MOUSE_SENSITIVITY;
		yOffset *= MOUSE_SENSITIVITY;

		this.yaw += xOffset;
		this.pitch += yOffset;

		if (this.pitch > MAX_PITCH)
			this.pitch = MAX_PITCH;
		if (this.pitch < -MAX_PITCH)
			this.pitch = -MAX_PITCH;

		this.updateCameraVectors();
	}

	public void update(float deltaTime, boolean forward, boolean backward, boolean left, boolean rightKey,
			boolean jump, boolean sneak, boolean sprint) {
		this.gameTime += deltaTime;
		this.sneaking = sneak && !this.flying;

		final boolean jumpPressed = jump && !this.spaceWasPressed;

		if (jumpPressed && !this.flightTransitionActive)
			if (this.lastSpacePressTime > 0 && this.gameTime - this.lastSpacePressTime < DOUBLE_TAP_TIME) {
				this.startFlightTransition(!this.flying);
				this.lastSpacePressTime = -1.0f;
			} else
				this.lastSpacePressTime = this.gameTime;

		this.updateFlightTransition(deltaTime);

		float baseSpeed;
		if (this.flying) {
			baseSpeed = FLY_SPEED;
			if (sprint)
				baseSpeed *= FLY_SPRINT_MULTIPLIER;
		} else if (this.sneaking)
			baseSpeed = SNEAK_SPEED;
		else {
			baseSpeed = WALK_SPEED;
			if (sprint)
				baseSpeed *= SPRINT_MULTIPLIER;
		}

		final boolean isMoving = forward || backward || left || rightKey;

		if (isMoving) {
			if (this.currentSpeed < 0.1f)
				this.currentSpeed = baseSpeed;
			else {
				this.targetSpeed = baseSpeed;
				this.currentSpeed = this.lerp(this.currentSpeed, this.targetSpeed, Math.min(1.0f, SPEED_TRANSITION_RATE * deltaTime));
				if (Math.abs(this.currentSpeed - this.targetSpeed) < 0.05f)
					this.currentSpeed = this.targetSpeed;
			}
		} else
			this.currentSpeed = 0.0f;

		this.updateHeadBob(deltaTime, isMoving, sprint);
		this.updateSneakHeight(deltaTime);

		final Vector3f moveDir = new Vector3f(0, 0, 0);

		if (forward) {
			final Vector3f forwardDir = new Vector3f(this.front.x, 0, this.front.z).normalize();
			moveDir.add(forwardDir);
		}
		if (backward) {
			final Vector3f backDir = new Vector3f(-this.front.x, 0, -this.front.z).normalize();
			moveDir.add(backDir);
		}
		if (left)
			moveDir.add(-this.right.x, 0, -this.right.z);
		if (rightKey)
			moveDir.add(this.right.x, 0, this.right.z);

		if (moveDir.lengthSquared() > 0.001f && this.currentSpeed > 0.0f) {
			moveDir.normalize();
			final float dx = moveDir.x * this.currentSpeed * deltaTime;
			final float dz = moveDir.z * this.currentSpeed * deltaTime;

			if (this.sneaking && this.onGround) {
				final float newX = this.position.x + dx;
				final float newZ = this.position.z + dz;
				if (!this.wouldFallOff(newX, this.position.z))
					this.tryMoveX(dx);
				if (!this.wouldFallOff(this.position.x, newZ))
					this.tryMoveZ(dz);
			} else {
				this.tryMoveX(dx);
				this.tryMoveZ(dz);
			}
		}

		if (this.flying && !this.flightTransitionActive) {
			float verticalSpeed = FLY_SPEED;
			if (sprint)
				verticalSpeed *= FLY_SPRINT_MULTIPLIER;

			if (jump)
				this.tryMoveY(verticalSpeed * deltaTime);
			if (sneak)
				this.tryMoveY(-verticalSpeed * deltaTime);

			this.velocity.y = 0;
		} else if (!this.flying && !this.flightTransitionActive) {
			if (!this.onGround)
				this.velocity.y -= GRAVITY * deltaTime;

			this.velocity.y = Math.max(-TERMINAL_VELOCITY, this.velocity.y);

			final boolean jumpReady = this.gameTime - this.lastJumpTime >= JUMP_COOLDOWN;
			final boolean hasClearance = this.hasJumpClearance();
			if (this.onGround && jump && !this.sneaking && jumpReady && hasClearance) {
				this.velocity.y = JUMP_VELOCITY;
				this.onGround = false;
				this.lastJumpTime = this.gameTime;
			}

			final float dy = this.velocity.y * deltaTime;

			if (dy < 0) {
				final float feetY = this.position.y - EYE_HEIGHT;
				boolean collisionFound = false;
				float landingY = this.position.y + dy;

				if (Math.abs(dy) > 1.0f) {
					final int steps = (int) Math.ceil(Math.abs(dy));
					final float stepSize = dy / steps;
					for (int i = 1; i <= steps; i++) {
						final float testY = this.position.y + stepSize * i;
						if (this.checkCollisionAtPosition(this.position.x, testY, this.position.z)) {
							landingY = this.position.y + stepSize * (i - 1);
							collisionFound = true;
							break;
						}
					}
					if (!collisionFound)
						landingY = this.position.y + dy;
				}

				if (!collisionFound)
					collisionFound = this.checkCollisionAtPosition(this.position.x, landingY, this.position.z);

				if (collisionFound) {
					final int targetFeetBlockY = (int) Math.floor(feetY + dy);
					final int currentFeetBlockY = (int) Math.floor(feetY);
					int groundY = currentFeetBlockY;
					for (int checkY = targetFeetBlockY; checkY <= currentFeetBlockY; checkY++) {
						final float testY = checkY + 1 + EYE_HEIGHT + COLLISION_EPSILON;
						if (!this.checkCollisionAtPosition(this.position.x, testY, this.position.z)) {
							groundY = checkY;
							break;
						}
					}
					this.position.y = groundY + 1 + EYE_HEIGHT + COLLISION_EPSILON;

					int safetyCounter = 0;
					while (this.checkCollisionAtPosition(this.position.x, this.position.y, this.position.z) && safetyCounter < 10) {
						this.position.y += 1.0f;
						safetyCounter++;
					}

					this.velocity.y = 0;
					this.onGround = true;
				} else {
					this.position.y = landingY;
					this.onGround = false;
				}
			} else if (dy > 0) {
				final float newY = this.position.y + dy;
				if (this.checkCollisionAtPosition(this.position.x, newY, this.position.z))
					this.velocity.y = 0;
				else {
					this.position.y = newY;
					this.onGround = false;
				}
			}

			this.checkGroundContact();
		}
		this.spaceWasPressed = jump;
	}

	private void startFlightTransition(boolean toFly) {
		this.flightTransitionActive = true;
		this.flightTransitionToFly = toFly;
		this.flightTransitionProgress = 0.0f;
		this.flightTransitionStartY = this.position.y;

		if (toFly)
			this.velocity.y = 0;
	}

	private void updateFlightTransition(float deltaTime) {
		if (!this.flightTransitionActive)
			return;

		this.flightTransitionProgress += deltaTime / FLIGHT_TRANSITION_DURATION;

		if (this.flightTransitionProgress >= 1.0f) {
			this.flightTransitionProgress = 1.0f;
			this.flightTransitionActive = false;
			this.flying = this.flightTransitionToFly;

			if (!this.flying) {
				this.onGround = false;
				this.velocity.y = -1.0f;
			}
			return;
		}

		final float easedProgress = this.easeInOutCubic(this.flightTransitionProgress);

		if (this.flightTransitionToFly) {
			final float liftOffset = (float) Math.sin(easedProgress * Math.PI) * FLIGHT_LIFT_HEIGHT;
			this.position.y = this.flightTransitionStartY + liftOffset;

			if (this.flightTransitionProgress > 0.5f && !this.flying) {
				this.flying = true;
				this.velocity.y = 0;
			}
		} else {
			if (this.flightTransitionProgress < 0.3f) {
				final float dropProgress = this.flightTransitionProgress / 0.3f;
				final float dropEase = this.easeInQuad(dropProgress);
				this.position.y = this.flightTransitionStartY - dropEase * 0.1f;
			}

			if (this.flightTransitionProgress > 0.2f && this.flying)
				this.flying = false;
		}
	}

	private void updateSneakHeight(float deltaTime) {
		final float targetEyeHeight = this.sneaking ? SNEAK_EYE_HEIGHT : EYE_HEIGHT;
		this.currentEyeHeight = this.lerp(this.currentEyeHeight, targetEyeHeight, Math.min(1.0f, 12.0f * deltaTime));
	}

	private void updateHeadBob(float deltaTime, boolean isMoving, boolean sprinting) {
		if (!isMoving || this.flying || !this.onGround || this.flightTransitionActive) {
			this.headBobOffsetY = this.lerp(this.headBobOffsetY, 0.0f, Math.min(1.0f, 10.0f * deltaTime));
			this.headBobOffsetX = this.lerp(this.headBobOffsetX, 0.0f, Math.min(1.0f, 10.0f * deltaTime));

			if (Math.abs(this.headBobOffsetY) < 0.001f)
				this.headBobOffsetY = 0.0f;
			if (Math.abs(this.headBobOffsetX) < 0.001f)
				this.headBobOffsetX = 0.0f;
			return;
		}

		final float frequency = sprinting ? HEAD_BOB_FREQUENCY_SPRINT : HEAD_BOB_FREQUENCY_WALK;
		final float speedFactor = Math.min(1.0f, this.currentSpeed / WALK_SPEED);
		final float amplitudeMultiplier = sprinting ? 1.3f : 1.0f;

		this.headBobPhase += frequency * deltaTime * speedFactor;
		if (this.headBobPhase > Math.PI * 2)
			this.headBobPhase -= (float) (Math.PI * 2);

		final float targetBobY = (float) Math.sin(this.headBobPhase * 2) * HEAD_BOB_VERTICAL_AMPLITUDE * amplitudeMultiplier
				* speedFactor;
		final float targetBobX = (float) Math.sin(this.headBobPhase) * HEAD_BOB_HORIZONTAL_AMPLITUDE * amplitudeMultiplier
				* speedFactor;

		this.headBobOffsetY = this.lerp(this.headBobOffsetY, targetBobY, Math.min(1.0f, 15.0f * deltaTime));
		this.headBobOffsetX = this.lerp(this.headBobOffsetX, targetBobX, Math.min(1.0f, 15.0f * deltaTime));
	}

	private float lerp(float a, float b, float t) {
		return a + (b - a) * t;
	}

	private float easeInOutCubic(float t) {
		return t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
	}

	private float easeInQuad(float t) {
		return t * t;
	}

	private boolean wouldFallOff(float x, float z) {
		if (this.world == null)
			return false;

		final float feetY = this.position.y - EYE_HEIGHT;
		final float checkY = feetY - 0.5f;

		final float halfWidth = PLAYER_WIDTH / 2.0f;
		return !this.world.checkCollision(
				x - halfWidth, checkY, z - halfWidth,
				x + halfWidth, feetY - 0.1f, z + halfWidth);
	}

	private void checkGroundContact() {
		if (this.world == null)
			return;

		final float feetY = this.position.y - EYE_HEIGHT;
		final float checkY = feetY - 0.05f;

		final float halfWidth = PLAYER_WIDTH / 2.0f - COLLISION_EPSILON;
		final boolean groundBelow = this.world.checkCollision(
				this.position.x - halfWidth, checkY, this.position.z - halfWidth,
				this.position.x + halfWidth, feetY, this.position.z + halfWidth);

		if (groundBelow && this.velocity.y <= 0) {
			this.onGround = true;
			this.velocity.y = 0;
		} else if (!groundBelow)
			this.onGround = false;
	}

	public void moveForward(float amount) {
		final Vector3f moveDir = new Vector3f(this.front.x, 0, this.front.z).normalize();
		final float dx = moveDir.x * amount;
		final float dz = moveDir.z * amount;

		this.tryMoveX(dx);
		this.tryMoveZ(dz);
	}

	public void moveRight(float amount) {
		final float dx = this.right.x * amount;
		final float dz = this.right.z * amount;

		this.tryMoveX(dx);
		this.tryMoveZ(dz);
	}

	public void moveUp(float amount) {
		this.tryMoveY(amount);
	}

	private void tryMoveX(float dx) {
		if (this.world == null) {
			this.position.x += dx;
			return;
		}

		final float newX = this.position.x + dx;
		if (!this.checkCollisionAtPosition(newX, this.position.y, this.position.z))
			this.position.x = newX;
	}

	private void tryMoveY(float dy) {
		if (this.world == null) {
			this.position.y += dy;
			return;
		}

		final float newY = this.position.y + dy;
		if (!this.checkCollisionAtPosition(this.position.x, newY, this.position.z))
			this.position.y = newY;
	}

	private void tryMoveZ(float dz) {
		if (this.world == null) {
			this.position.z += dz;
			return;
		}

		final float newZ = this.position.z + dz;
		if (!this.checkCollisionAtPosition(this.position.x, this.position.y, newZ))
			this.position.z = newZ;
	}

	private boolean checkCollisionAtPosition(float x, float y, float z) {
		if (this.world == null)
			return false;

		final float halfWidth = PLAYER_WIDTH / 2.0f;
		final float feetY = y - EYE_HEIGHT;
		final float headY = feetY + PLAYER_HEIGHT;

		final float minX = x - halfWidth + COLLISION_EPSILON;
		final float maxX = x + halfWidth - COLLISION_EPSILON;
		final float minY = feetY + COLLISION_EPSILON;
		final float maxY = headY - COLLISION_EPSILON;
		final float minZ = z - halfWidth + COLLISION_EPSILON;
		final float maxZ = z + halfWidth - COLLISION_EPSILON;

		return this.world.checkCollision(minX, minY, minZ, maxX, maxY, maxZ);
	}

	private boolean hasJumpClearance() {
		if (this.world == null)
			return true;

		final float halfWidth = PLAYER_WIDTH / 2.0f - COLLISION_EPSILON;
		final float feetY = this.position.y - EYE_HEIGHT;
		final float headTarget = feetY + PLAYER_HEIGHT + 0.2f;

		return !this.world.checkCollision(
				this.position.x - halfWidth, feetY, this.position.z - halfWidth,
				this.position.x + halfWidth, headTarget, this.position.z + halfWidth);
	}

	public Matrix4f getViewMatrix(Matrix4f dest) {
		final Vector3f viewPos = new Vector3f(this.position);
		viewPos.y += this.headBobOffsetY;
		viewPos.y -= EYE_HEIGHT - this.currentEyeHeight;
		viewPos.x += this.right.x * this.headBobOffsetX;
		viewPos.z += this.right.z * this.headBobOffsetX;

		final Vector3f center = new Vector3f(viewPos).add(this.front);
		return dest.identity().lookAt(viewPos, center, this.up);
	}

	public Vector3f getFront() {
		return new Vector3f(this.front);
	}

	public boolean isOnGround() {
		return this.onGround;
	}
}
