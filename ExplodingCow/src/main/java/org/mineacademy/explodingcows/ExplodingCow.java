package org.mineacademy.explodingcows;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class ExplodingCow extends JavaPlugin implements Listener {

	@Override
	public void onEnable() {
		this.getServer().getPluginManager().registerEvents(this, this);
	}

	@Override
	public void onDisable() {
		// Plugin shutdown logic
	}

	@EventHandler
	public void onRightClickEntity(PlayerInteractAtEntityEvent event) {
		EntityType type = event.getRightClicked().getType();
		Player player = event.getPlayer();

		if (player.hasPermission("my.custom.permission"))
			if (type == EntityType.COW || type == EntityType.CHICKEN)
				event.getRightClicked().getLocation().createExplosion(20F);
	}
}
