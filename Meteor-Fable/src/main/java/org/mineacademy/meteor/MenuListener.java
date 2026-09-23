package org.mineacademy.meteor;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class MenuListener implements Listener {

    private final MeteorPlugin plugin;

    MenuListener(MeteorPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MeteorMenu))
            return;

        event.setCancelled(true);

        MeteorMenu menu = (MeteorMenu) event.getInventory().getHolder();
        int slot = event.getRawSlot();

        if (slot < 0 || slot >= menu.getInventory().getSize())
            return;

        if (event.getWhoClicked() != menu.getPlayer())
            return;

        menu.handleClick(slot);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof MeteorMenu)
            event.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof MeteorMenu)
            this.plugin.unregisterMenu(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.plugin.unregisterMenu(event.getPlayer().getUniqueId());
    }
}
