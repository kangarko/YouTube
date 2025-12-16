package com.matejpacan.voxelcraft;

import static org.lwjgl.opengl.GL11.GL_BACK;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.glCullFace;
import static org.lwjgl.opengl.GL11.glDepthMask;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class ItemSystem {

	private static final float GRAVITY = -18.0f;
	private static final float FRICTION = 6.0f;
	private static final float BOUNCE_DAMPING = 0.35f;
	private static final float PICKUP_RADIUS_SQ = 2.56f;
	private static final float PICKUP_DELAY = 1.5f;
	private static final float SCALE = 0.35f;
	private static final float MAX_AGE = 300.0f;

	private final List<ItemEntity> items = new ArrayList<>();
	private final Inventory inventory;
	private final Random random = new Random();

	private final EntityRenderer renderer;

	public ItemSystem(TextureAtlas textureAtlas, Inventory inventory) {
		if (textureAtlas == null)
			throw new IllegalArgumentException("Texture atlas missing for item system");
		if (inventory == null)
			throw new IllegalArgumentException("Inventory missing for item system");
		this.inventory = inventory;
		this.renderer = new EntityRenderer(textureAtlas);
	}

	public void spawnDrop(World world, BlockType type, float x, float y, float z) {
		if (type == null)
			throw new IllegalArgumentException("Cannot drop null item");
		if (type == BlockType.AIR)
			throw new IllegalArgumentException("Cannot drop air");
		if (world == null)
			throw new IllegalArgumentException("World missing for drop spawn");
		final float offsetRadius = 0.28f;
		final float theta = this.random.nextFloat() * (float) Math.PI * 2.0f;
		final float ox = (float) Math.cos(theta) * offsetRadius * this.random.nextFloat();
		final float oz = (float) Math.sin(theta) * offsetRadius * this.random.nextFloat();
		final int topY = world.getTopBlockY((int) Math.floor(x + ox), (int) Math.floor(z + oz));
		final float baseY = topY >= 0 ? topY + 1.05f : y + 0.35f;
		final float spawnY = Math.max(baseY, y + 0.35f);
		final Vector3f position = new Vector3f(x + ox, spawnY, z + oz);
		final Vector3f velocity = new Vector3f((this.random.nextFloat() - 0.5f) * 1.35f,
				this.random.nextFloat() * 2.5f + 0.4f,
				(this.random.nextFloat() - 0.5f) * 1.35f);
		this.items.add(new ItemEntity(type, position, velocity, PICKUP_DELAY));
	}

	public void update(float deltaTime, World world, Vector3f playerPos) {
		if (world == null)
			throw new IllegalArgumentException("World missing for item update");
		if (playerPos == null)
			throw new IllegalArgumentException("Player position missing for item update");
		final Iterator<ItemEntity> it = this.items.iterator();
		while (it.hasNext()) {
			final ItemEntity item = it.next();
			item.step(deltaTime);
			if (item.age > MAX_AGE) {
				it.remove();
				continue;
			}
			item.rotation += deltaTime * 2.4f;
			item.velocity.y += GRAVITY * deltaTime;
			item.position.x += item.velocity.x * deltaTime;
			item.position.y += item.velocity.y * deltaTime;
			item.position.z += item.velocity.z * deltaTime;
			this.dampHorizontal(item.velocity, deltaTime);
			if (this.resolveGroundCollision(item, world))
				this.dampHorizontal(item.velocity, deltaTime * 2.5f);
			final float distSq = this.distanceSquared(item.position, playerPos);
			if (item.pickupDelay <= 0.0f && distSq <= PICKUP_RADIUS_SQ)
				if (this.inventory.addBlock(item.type))
					it.remove();
		}
	}

	public void render(Matrix4f projection, Matrix4f view) {
		if (this.items.isEmpty())
			return;
		if (projection == null || view == null)
			throw new IllegalArgumentException("Matrices missing for item render");

		glEnable(GL_DEPTH_TEST);
		glDepthMask(true);
		glEnable(GL_CULL_FACE);
		glCullFace(GL_BACK);

		for (final ItemEntity item : this.items) {
			final BlockType type = item.type;
			final Matrix4f model = new Matrix4f()
					.translate(item.position)
					.rotateY(item.rotation)
					.scale(SCALE);
			if (type.isPlant())
				this.renderer.renderPlant(type, projection, view, model);
			else
				this.renderer.renderBlock(type, projection, view, model, null, false, false);
		}

		glDisable(GL_CULL_FACE);
	}

	public void cleanup() {
		this.renderer.cleanup();
	}

	private boolean resolveGroundCollision(ItemEntity item, World world) {
		final int gx = (int) Math.floor(item.position.x);
		final int gy = (int) Math.floor(item.position.y - 0.05f);
		final int gz = (int) Math.floor(item.position.z);
		final BlockType ground = world.getBlockAt(gx, gy, gz);
		if ((ground == null) || !ground.isSolid())
			return false;
		final float targetY = gy + 1.02f;
		if (item.position.y >= targetY)
			return false;
		item.position.y = targetY;
		if (item.velocity.y < 0)
			item.velocity.y *= -BOUNCE_DAMPING;
		item.velocity.x *= 0.7f;
		item.velocity.z *= 0.7f;
		return true;
	}

	private void dampHorizontal(Vector3f velocity, float deltaTime) {
		final float factor = Math.max(0.0f, 1.0f - FRICTION * deltaTime);
		velocity.x *= factor;
		velocity.z *= factor;
	}

	private float distanceSquared(Vector3f a, Vector3f b) {
		final float dx = a.x - b.x;
		final float dy = a.y - b.y;
		final float dz = a.z - b.z;
		return dx * dx + dy * dy + dz * dz;
	}

	private static final class ItemEntity extends Entity {
		private final BlockType type;
		private float rotation;
		private float pickupDelay;

		ItemEntity(BlockType type, Vector3f position, Vector3f velocity, float pickupDelay) {
			this.type = type;
			this.position.set(position);
			this.velocity.set(velocity);
			this.pickupDelay = pickupDelay;
		}

		@Override
		protected void onUpdate(float deltaTime) {
			this.pickupDelay = Math.max(0.0f, this.pickupDelay - deltaTime);
		}

		@Override
		public void render(org.joml.Matrix4f projection, org.joml.Matrix4f view) {
		}
	}
}
