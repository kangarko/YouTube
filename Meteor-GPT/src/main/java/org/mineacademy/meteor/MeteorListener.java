package org.mineacademy.meteor;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Cancellable;
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
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

final class MeteorListener implements Listener {

    private final MeteorPlugin plugin;

    MeteorListener(MeteorPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void click(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof MeteorMenu) {
            event.setCancelled(true);
            MeteorMenu menu = (MeteorMenu) event.getInventory().getHolder();

            if (event.getWhoClicked().getUniqueId().equals(menu.player.getUniqueId()) && (event.getClick() == org.bukkit.event.inventory.ClickType.LEFT || event.getClick() == org.bukkit.event.inventory.ClickType.RIGHT))
                menu.click(event.getRawSlot());
        } else if (this.locked(event.getInventory()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void drag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof MeteorMenu || this.locked(event.getInventory()))
            event.setCancelled(true);
    }

    @EventHandler
    public void close(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof MeteorMenu)
            this.plugin.menus.remove(event.getPlayer().getUniqueId(), event.getInventory().getHolder());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void open(InventoryOpenEvent event) {
        if (this.locked(event.getInventory()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void move(InventoryMoveItemEvent event) {
        if (event.getSource().getHolder() instanceof MeteorMenu || event.getDestination().getHolder() instanceof MeteorMenu || this.locked(event.getSource()) || this.locked(event.getDestination()))
            event.setCancelled(true);
    }

    private boolean locked(Inventory inventory) {
        if (inventory instanceof DoubleChestInventory) {
            DoubleChestInventory chest = (DoubleChestInventory) inventory;

            return this.locked(chest.getLeftSide()) || this.locked(chest.getRightSide());
        }

        InventoryHolder holder = inventory.getHolder();

        return holder instanceof BlockState && this.protectedBlock(((BlockState) holder).getBlock());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void drop(PlayerDropItemEvent event) {
        if (this.plugin.menus.containsKey(event.getPlayer().getUniqueId()) || this.protectedEntity(event.getPlayer()))
            event.setCancelled(true);
    }

    @EventHandler
    public void quit(PlayerQuitEvent event) {
        this.plugin.menus.remove(event.getPlayer().getUniqueId());

        if (this.plugin.strike != null)
            this.plugin.strike.forget(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void damage(EntityDamageEvent event) {
        if (this.protectedEntity(event.getEntity())) {
            event.setCancelled(true);
            event.getEntity().setFallDistance(0);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void combust(EntityCombustEvent event) {
        if (this.protectedEntity(event.getEntity()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void entityInteract(PlayerInteractEntityEvent event) {
        if (event.getRightClicked().hasMetadata("MeteorGPT_Visual"))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void armorStand(PlayerArmorStandManipulateEvent event) {
        if (event.getRightClicked().hasMetadata("MeteorGPT_Visual"))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void interact(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null && this.protectedBlock(event.getClickedBlock()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void breakBlock(BlockBreakEvent event) {
        if (this.protectedBlock(event.getBlock()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void place(BlockPlaceEvent event) {
        if (this.protectedBlock(event.getBlock()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void physics(BlockPhysicsEvent event) {
        if (this.protectedBlock(event.getBlock()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void flow(BlockFromToEvent event) {
        if (this.protectedBlock(event.getBlock()) || this.protectedBlock(event.getToBlock()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void burn(BlockBurnEvent event) {
        if (this.protectedBlock(event.getBlock()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void fade(BlockFadeEvent event) {
        if (this.protectedBlock(event.getBlock()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void form(BlockFormEvent event) {
        if (this.protectedBlock(event.getBlock()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void grow(BlockGrowEvent event) {
        if (this.protectedBlock(event.getBlock()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void spread(BlockSpreadEvent event) {
        if (this.protectedBlock(event.getBlock()) || this.protectedBlock(event.getSource()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void ignite(BlockIgniteEvent event) {
        if (this.protectedBlock(event.getBlock()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void leaves(LeavesDecayEvent event) {
        if (this.protectedBlock(event.getBlock()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void redstone(BlockRedstoneEvent event) {
        if (this.protectedBlock(event.getBlock()))
            event.setNewCurrent(event.getOldCurrent());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void pistonExtend(BlockPistonExtendEvent event) {
        if (this.protectedBlock(event.getBlock())) {
            event.setCancelled(true);

            return;
        }

        for (Block block : event.getBlocks())
            if (this.protectedBlock(block) || this.protectedBlock(block.getRelative(event.getDirection())))
                event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void pistonRetract(BlockPistonRetractEvent event) {
        if (this.protectedBlock(event.getBlock())) {
            event.setCancelled(true);

            return;
        }

        for (Block block : event.getBlocks())
            if (this.protectedBlock(block) || this.protectedBlock(block.getRelative(event.getDirection())))
                event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void structure(StructureGrowEvent event) {
        for (BlockState block : event.getBlocks())
            if (this.protectedBlock(block.getBlock()))
                event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void change(EntityChangeBlockEvent event) {
        if (event.getEntity().hasMetadata("MeteorGPT_Visual") || this.protectedBlock(event.getBlock()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void entityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(this::protectedBlock);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void blockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(this::protectedBlock);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void hanging(HangingBreakEvent event) {
        if (this.protectedEntity(event.getEntity()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void item(ItemSpawnEvent event) {
        if (this.protectedEntity(event.getEntity()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void unload(ChunkUnloadEvent event) {
        if (!(event instanceof Cancellable))
            return;

        MeteorStrike strike = this.plugin.strike;

        if (strike == null || !event.getWorld().equals(strike.world))
            return;

        if (Math.abs(event.getChunk().getX() * 16 + 8 - strike.target.getX()) < strike.radius + 24 && Math.abs(event.getChunk().getZ() * 16 + 8 - strike.target.getZ()) < strike.radius + 24)
            ((Cancellable) event).setCancelled(true);

        for (Entity entity : event.getChunk().getEntities())
            if (entity.hasMetadata("MeteorGPT_Visual"))
                ((Cancellable) event).setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void worldUnload(WorldUnloadEvent event) {
        if (this.plugin.strike != null && this.plugin.strike.world.equals(event.getWorld()))
            this.plugin.strike.finish(true);
    }

    private boolean protectedBlock(Block block) {
        return this.plugin.strike != null && this.plugin.strike.contains(block.getLocation(), 3);
    }

    private boolean protectedEntity(Entity entity) {
        return this.plugin.strike != null && this.plugin.strike.protects(entity);
    }
}
