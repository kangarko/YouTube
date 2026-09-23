package org.mineacademy.meteoropus.listener;

import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.InventoryHolder;
import org.mineacademy.meteoropus.menu.MeteorMenu;
import org.mineacademy.meteoropus.model.MovingBlock;
import org.mineacademy.meteoropus.model.Protection;
import org.mineacademy.meteoropus.remain.Remain;

public final class MeteorListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(final InventoryClickEvent event) {
        final InventoryHolder holder = event.getInventory().getHolder();

        if (!(holder instanceof MeteorMenu))
            return;

        event.setCancelled(true);

        final MeteorMenu menu = (MeteorMenu) holder;
        final ClickType click = event.getClick();

        menu.resync();

        if (event.getRawSlot() >= 0 && event.getRawSlot() < event.getInventory().getSize() && (click == ClickType.LEFT || click == ClickType.RIGHT || click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT))
            menu.onClick(event.getRawSlot());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(final InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof MeteorMenu) {
            event.setCancelled(true);

            ((MeteorMenu) event.getInventory().getHolder()).resync();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(final InventoryCloseEvent event) {
        final InventoryHolder holder = event.getInventory().getHolder();
        final HumanEntity viewer     = event.getPlayer();

        if (holder instanceof MeteorMenu && viewer instanceof Player)
            ((MeteorMenu) holder).onClose();
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        final MeteorMenu menu = MeteorMenu.findOpen(event.getPlayer());

        if (menu != null)
            menu.onClose();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityDamage(final EntityDamageEvent event) {
        final Entity entity = event.getEntity();

        if (Protection.isProtected(entity)) {
            event.setCancelled(true);

            entity.setFallDistance(0);
        }
    }

    @EventHandler
    public void onChunkLoad(final ChunkLoadEvent event) {
        if (Remain.canSetPersistent())
            return;

        for (final Entity entity : event.getChunk().getEntities())
            if (MovingBlock.isLeftover(entity))
                entity.remove();
    }
}
