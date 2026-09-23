package org.mineacademy.meteoropus.listener;

import java.util.Iterator;
import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.mineacademy.meteoropus.model.MovingBlock;
import org.mineacademy.meteoropus.model.Strike;

public final class StrikeListener implements Listener {

    private final Strike strike;

    public StrikeListener(final Strike strike) {
        this.strike = strike;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityChangeBlock(final EntityChangeBlockEvent event) {
        final Entity entity     = event.getEntity();
        final MovingBlock block = MovingBlock.findByEntity(entity);

        if (block != null) {
            event.setCancelled(true);

            block.markLanded();

            return;
        }

        if (MovingBlock.isLeftover(entity)) {
            event.setCancelled(true);

            entity.remove();

            return;
        }

        if (this.strike.isNear(event.getBlock(), 1))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemSpawn(final ItemSpawnEvent event) {
        if (this.strike.isSuppressingDrops())
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPhysics(final BlockPhysicsEvent event) {
        if (this.strike.isNear(event.getBlock(), 2))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockFromTo(final BlockFromToEvent event) {
        if (this.strike.isNear(event.getToBlock(), 1) || this.strike.isNear(event.getBlock(), 1))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent event) {
        if (this.strike.isInCrater(event.getBlock()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent event) {
        if (this.strike.isInCrater(event.getBlock()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBucketEmpty(final PlayerBucketEmptyEvent event) {
        if (this.strike.isNear(event.getBlockClicked().getRelative(event.getBlockFace()), 1))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBucketFill(final PlayerBucketFillEvent event) {
        if (this.strike.isInCrater(event.getBlockClicked().getRelative(event.getBlockFace())) || this.strike.isInCrater(event.getBlockClicked()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockIgnite(final BlockIgniteEvent event) {
        this.cancelIfNear(event, event.getBlock(), 3);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBurn(final BlockBurnEvent event) {
        this.cancelIfNear(event, event.getBlock(), 3);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockSpread(final BlockSpreadEvent event) {
        this.cancelIfNear(event, event.getBlock(), 2);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockForm(final BlockFormEvent event) {
        this.cancelIfNear(event, event.getBlock(), 2);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockFade(final BlockFadeEvent event) {
        this.cancelIfNear(event, event.getBlock(), 2);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockGrow(final BlockGrowEvent event) {
        this.cancelIfNear(event, event.getBlock(), 2);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onLeavesDecay(final LeavesDecayEvent event) {
        this.cancelIfNear(event, event.getBlock(), 8);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPistonExtend(final BlockPistonExtendEvent event) {
        if (this.isPistonTouchingCrater(event.getBlock(), event.getBlocks(), event.getDirection()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPistonRetract(final BlockPistonRetractEvent event) {
        if (this.isPistonTouchingCrater(event.getBlock(), event.getBlocks(), event.getDirection()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityExplode(final EntityExplodeEvent event) {
        this.removeCraterBlocks(event.blockList());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockExplode(final BlockExplodeEvent event) {
        this.removeCraterBlocks(event.blockList());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onStructureGrow(final StructureGrowEvent event) {
        for (final Iterator<BlockState> iterator = event.getBlocks().iterator(); iterator.hasNext();)
            if (this.strike.isNear(iterator.next().getBlock(), 1))
                iterator.remove();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onHangingBreak(final HangingBreakEvent event) {
        if (this.strike.isNear(event.getEntity().getLocation(), 3))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityDamage(final EntityDamageEvent event) {
        if (this.strike.isNear(event.getEntity().getLocation(), this.strike.getDamageRange())) {
            event.setCancelled(true);

            event.getEntity().setFallDistance(0);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityCombust(final EntityCombustEvent event) {
        if (this.strike.isNear(event.getEntity().getLocation(), this.strike.getDamageRange()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onVehicleDamage(final VehicleDamageEvent event) {
        if (this.strike.isNear(event.getVehicle().getLocation(), this.strike.getDamageRange()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onVehicleDestroy(final VehicleDestroyEvent event) {
        if (this.strike.isNear(event.getVehicle().getLocation(), this.strike.getDamageRange()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChunkUnload(final ChunkUnloadEvent event) {
        if (event instanceof Cancellable && this.strike.isChunkHeld(event.getChunk()))
            ((Cancellable) event).setCancelled(true);
    }

    private void cancelIfNear(final Cancellable event, final Block block, final double margin) {
        if (this.strike.isNear(block, margin))
            event.setCancelled(true);
    }

    private boolean isPistonTouchingCrater(final Block piston, final List<Block> moved, final BlockFace direction) {
        if (this.strike.isInCrater(piston) || this.strike.isInCrater(piston.getRelative(direction)))
            return true;

        for (final Block block : moved)
            if (this.strike.isInCrater(block) || this.strike.isInCrater(block.getRelative(direction)))
                return true;

        return false;
    }

    private void removeCraterBlocks(final List<Block> blocks) {
        for (final Iterator<Block> iterator = blocks.iterator(); iterator.hasNext();)
            if (this.strike.isNear(iterator.next(), 1))
                iterator.remove();
    }
}
