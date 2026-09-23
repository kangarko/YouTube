package org.mineacademy.meteor;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

final class BukkitVisuals {

	enum Dust {

		COLOR,
		FLAME,
		SNOW,
		PORTAL,
		SMOKE,
		SPARK,
		CLOUD,
		EXPLOSION
	}

	private final Class<?> particleClass;
	private final Method spawnParticle;
	private final Method forceParticle;
	private final Method minHeight;
	private final Method isItem;
	private final Method title;
	final Method addChunkTicket;
	final Method removeChunkTicket;
	final Method isForceLoaded;
	final Method setForceLoaded;
	private final Constructor<?> dustConstructor;
	private final Map<Dust, Object> particles = new EnumMap<>(Dust.class);
	private final Map<Element, Object> colors = new EnumMap<>(Element.class);
	private final Map<Material, Boolean> itemTypes = new EnumMap<>(Material.class);
	private final Inventory itemProbe;
	final boolean flattened;

	BukkitVisuals() {
		this.particleClass = findClass("org.bukkit.Particle");
		this.flattened = findClass("org.bukkit.block.data.BlockData") != null;
		this.minHeight = findMethod(World.class, "getMinHeight");
		this.isItem = findMethod(Material.class, "isItem");
		this.title = findMethod(Player.class, "sendTitle", String.class, String.class, int.class, int.class, int.class);
		this.addChunkTicket = findMethod(Chunk.class, "addPluginChunkTicket", Plugin.class);
		this.removeChunkTicket = findMethod(Chunk.class, "removePluginChunkTicket", Plugin.class);
		this.isForceLoaded = findMethod(Chunk.class, "isForceLoaded");
		this.setForceLoaded = findMethod(Chunk.class, "setForceLoaded", boolean.class);
		this.itemProbe = this.isItem == null ? Bukkit.createInventory(null, 9) : null;

		if (this.particleClass == null) {
			this.spawnParticle = null;
			this.forceParticle = null;
			this.dustConstructor = null;

			return;
		}

		this.spawnParticle = getMethod(Player.class, "spawnParticle", this.particleClass, Location.class, int.class, double.class, double.class, double.class, double.class, Object.class);
		this.forceParticle = findMethod(Player.class, "spawnParticle", this.particleClass, Location.class, int.class, double.class, double.class, double.class, double.class, Object.class, boolean.class);
		this.particles.put(Dust.COLOR, this.particle("DUST", "REDSTONE"));
		this.particles.put(Dust.FLAME, this.particle("FLAME", "FLAME"));
		this.particles.put(Dust.SNOW, this.particle("SNOWFLAKE", "SNOW_SHOVEL"));
		this.particles.put(Dust.PORTAL, this.particle("PORTAL", "PORTAL"));
		this.particles.put(Dust.SMOKE, this.particle("SMOKE", "SMOKE_NORMAL"));
		this.particles.put(Dust.SPARK, this.particle("FIREWORK", "FIREWORKS_SPARK"));
		this.particles.put(Dust.CLOUD, this.particle("CLOUD", "CLOUD"));
		this.particles.put(Dust.EXPLOSION, this.particle("EXPLOSION", "EXPLOSION_LARGE"));
		final Class<?> dust = findClass("org.bukkit.Particle$DustOptions");

		if (dust == null) {
			this.dustConstructor = null;

			return;
		}

		try {
			this.dustConstructor = dust.getConstructor(Color.class, float.class);

			for (final Element element : Element.values())
				this.colors.put(element, this.dustConstructor.newInstance(Color.fromRGB(element.red, element.green, element.blue), 1.65F));
		} catch (final ReflectiveOperationException exception) {
			throw new IllegalStateException("Cannot initialize Bukkit dust colors", exception);
		}
	}

	private Object particle(String current, String earlier) {
		try {
			return this.particleClass.getField(current).get(null);
		} catch (final NoSuchFieldException absent) {
			try {
				return this.particleClass.getField(earlier).get(null);
			} catch (final ReflectiveOperationException exception) {
				throw new IllegalStateException("Missing Bukkit particle " + current + " / " + earlier, exception);
			}
		} catch (final IllegalAccessException exception) {
			throw new IllegalStateException("Cannot access Bukkit particle " + current, exception);
		}
	}

	int minHeight(World world) {
		if (this.minHeight == null)
			return 0;

		return (Integer) invoke(this.minHeight, world);
	}

	ItemStack pane(int dye) {
		if (!this.flattened)
			return new ItemStack(Material.valueOf("STAINED_GLASS_PANE"), 1, (short) dye);

		final String[] colors = { "WHITE", "ORANGE", "MAGENTA", "LIGHT_BLUE", "YELLOW", "LIME", "PINK", "GRAY", "LIGHT_GRAY", "CYAN", "PURPLE", "BLUE", "BROWN", "GREEN", "RED", "BLACK" };

		return new ItemStack(Material.valueOf(colors[dye] + "_STAINED_GLASS_PANE"));
	}

	ItemStack debris(BlockState state, Element element) {
		final Material material = state.getType();

		if (this.isItem != null && !(Boolean) invoke(this.isItem, material))
			return new ItemStack(element.rock);

		if (this.isItem == null) {
			Boolean usable = this.itemTypes.get(material);

			if (usable == null) {
				this.itemProbe.setItem(0, new ItemStack(material));
				final ItemStack converted = this.itemProbe.getItem(0);
				usable = converted != null && converted.getType() != Material.AIR;
				this.itemTypes.put(material, usable);
				this.itemProbe.clear();
			}

			if (!usable)
				return new ItemStack(element.rock);
		}

		if (material.name().contains("WATER") || material.name().contains("LAVA") || !material.isSolid())
			return new ItemStack(element.core);

		if (this.flattened)
			return new ItemStack(material);

		return new ItemStack(material, 1, (short) (state.getRawData() & 255));
	}

	void title(Player player, String heading, String subtitle, int duration) {
		if (this.title == null)
			player.sendTitle(heading, subtitle);
		else
			invoke(this.title, player, heading, subtitle, 0, duration, 8);
	}

	void sound(Player player, Location location, String modern, String legacy, float volume, float pitch) {
		String name = modern;

		if (this.particleClass == null)
			name = legacy;
		else if (!this.flattened)
			name = modern.replace("block.note_block.", "block.note.").replace("entity.firework_rocket.", "entity.firework.").replace("entity.ender_dragon.", "entity.enderdragon.").replace("entity.lightning_bolt.", "entity.lightning.").replace("entity.enderman.", "entity.endermen.");

		player.playSound(location, name, volume, pitch);
	}

	void particle(Player player, Location location, Dust dust, Element element, int count, double spread, double speed) {
		if (this.particleClass == null) {
			Effect effect;

			switch (dust) {
				case COLOR:
					effect = Effect.COLOURED_DUST;
					break;
				case FLAME:
					effect = Effect.FLAME;
					break;
				case SNOW:
					effect = Effect.SNOW_SHOVEL;
					break;
				case PORTAL:
					effect = Effect.PORTAL;
					break;
				case SMOKE:
					effect = Effect.PARTICLE_SMOKE;
					break;
				case SPARK:
					effect = Effect.FIREWORKS_SPARK;
					break;
				case CLOUD:
					effect = Effect.CLOUD;
					break;
				case EXPLOSION:
					effect = Effect.EXPLOSION_LARGE;
					break;
				default:
					throw new IllegalStateException("Unknown effect " + dust);
			}

			if (dust == Dust.COLOR)
				player.spigot().playEffect(location, effect, 0, 0, element.red / 255F, element.green / 255F, element.blue / 255F, 1, 0, 256);
			else
				player.spigot().playEffect(location, effect, 0, 0, (float) spread, (float) spread, (float) spread, (float) speed, count, 256);

			return;
		}

		if (dust == Dust.COLOR && this.dustConstructor == null) {
			this.spawn(player, location, dust, 0, element.red / 255D, element.green / 255D, element.blue / 255D, 1D, null);

			return;
		}

		Object data = null;

		if (dust == Dust.COLOR)
			data = this.colors.get(element);

		this.spawn(player, location, dust, count, spread, spread, spread, speed, data);
	}

	private void spawn(Player player, Location location, Dust dust, int count, double x, double y, double z, double speed, Object data) {
		if (this.forceParticle != null)
			invoke(this.forceParticle, player, this.particles.get(dust), location, count, x, y, z, speed, data, true);
		else
			invoke(this.spawnParticle, player, this.particles.get(dust), location, count, x, y, z, speed, data);
	}

	static Method findMethod(Class<?> type, String name, Class<?>... parameters) {
		try {
			return type.getMethod(name, parameters);
		} catch (final NoSuchMethodException absent) {
			return null;
		}
	}

	static Method getMethod(Class<?> type, String name, Class<?>... parameters) {
		final Method method = findMethod(type, name, parameters);

		if (method == null)
			throw new IllegalStateException("Missing Bukkit method " + type.getName() + "." + name);

		return method;
	}

	static Object invoke(Method method, Object receiver, Object... arguments) {
		try {
			return method.invoke(receiver, arguments);
		} catch (final InvocationTargetException exception) {
			throw new IllegalStateException("Bukkit call failed: " + method, exception.getCause());
		} catch (final ReflectiveOperationException exception) {
			throw new IllegalStateException("Cannot invoke Bukkit method " + method, exception);
		}
	}

	private static Class<?> findClass(String name) {
		try {
			return Class.forName(name);
		} catch (final ClassNotFoundException absent) {
			return null;
		}
	}
}
