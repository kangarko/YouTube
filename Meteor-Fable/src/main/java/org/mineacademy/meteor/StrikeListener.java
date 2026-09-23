package org.mineacademy.meteor;

import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.hanging.HangingBreakEvent;

public final class StrikeListener implements Listener {

    private final MeteorPlugin plugin;

    StrikeListener(MeteorPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent event) {
        if (this.isProtected(event.getEntity()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCombust(EntityCombustEvent event) {
        if (this.isProtected(event.getEntity()))
            event.setCancelled(true);
    }

    private boolean isProtected(Entity entity) {
        if (this.plugin.isImmune(entity.getUniqueId()))
            return true;

        Strike strike = this.plugin.getStrike();

        return strike != null && strike.isProtectedEntity(entity);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onChangeBlock(EntityChangeBlockEvent event) {
        Strike strike = this.plugin.getStrike();

        if (strike == null || !strike.isOwnedEntity(event.getEntity().getUniqueId()))
            return;

        event.setCancelled(true);

        if (event.getEntity() instanceof FallingBlock)
            strike.handleDebrisLanding((FallingBlock) event.getEntity());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onItemSpawn(ItemSpawnEvent event) {
        Strike strike = this.plugin.getStrike();

        if (strike != null && strike.isAtCrater(event.getLocation()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFromTo(BlockFromToEvent event) {
        Strike strike = this.plugin.getStrike();

        if (strike != null && (strike.isNearCrater(event.getBlock().getLocation()) || strike.isNearCrater(event.getToBlock().getLocation())))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onIgnite(BlockIgniteEvent event) {
        Strike strike = this.plugin.getStrike();

        if (strike != null && strike.isNearCrater(event.getBlock().getLocation()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBurn(BlockBurnEvent event) {
        Strike strike = this.plugin.getStrike();

        if (strike != null && strike.isNearCrater(event.getBlock().getLocation()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSpread(BlockSpreadEvent event) {
        Strike strike = this.plugin.getStrike();

        if (strike != null && (strike.isNearCrater(event.getBlock().getLocation()) || strike.isNearCrater(event.getSource().getLocation())))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFade(BlockFadeEvent event) {
        Strike strike = this.plugin.getStrike();

        if (strike != null && strike.isNearCrater(event.getBlock().getLocation()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPhysics(BlockPhysicsEvent event) {
        Strike strike = this.plugin.getStrike();

        if (strike != null && strike.isNearCrater(event.getBlock().getLocation()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onHangingBreak(HangingBreakEvent event) {
        Strike strike = this.plugin.getStrike();

        if (strike != null && strike.isNearCrater(event.getEntity().getLocation()))
            event.setCancelled(true);
    }
}
