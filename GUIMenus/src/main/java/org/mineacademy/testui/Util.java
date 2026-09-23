package org.mineacademy.testui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public final class Util {

	private Util() {
	}

	public static void give(Player player, Material material) {
		if (player.getInventory().addItem(new ItemStack(material)).isEmpty())
			player.sendActionBar(toComponent("<aqua>Received <lang:" + material.translationKey() + ">"));
		else
			player.sendActionBar(toComponent("<red>Your inventory is full"));
	}

	public static Component toComponent(String message) {
		return MiniMessage.miniMessage().deserialize(message);
	}

	public static void setCamera(Player player, Entity target) {
		((CraftPlayer) player).getHandle().connection.send(new ClientboundSetCameraPacket(((CraftEntity) target).getHandle()));
	}

	public static void playSound(Player player, Sound sound, float pitch) {
		player.playSound(player, sound, 0.6F, pitch);
	}

	public static Location toWorld(Location eye, double x, double y, double distance) {
		final Vector forward = eye.getDirection();
		final Vector right = forward.clone().rotateAroundY(-Math.PI / 2);

		return eye.clone().add(forward.multiply(distance)).add(right.multiply(x)).add(0, y, 0);
	}

	public static double project(float angle, double distance, double edge) {
		final float turned = Location.normalizeYaw(angle);

		if (Math.abs(turned) > 80)
			return turned > 0 ? edge : -edge;

		return Math.max(-edge, Math.min(edge, Math.tan(Math.toRadians(turned)) * distance));
	}
}
