package org.mineacademy.meteor;

import org.bukkit.Material;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.CommandBlock;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Jukebox;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

final class BlockRecord {

    private final BlockState state;
    private final Material type;
    private final int x;
    private final int y;
    private final int z;
    private final boolean air;
    private final boolean liquid;
    private final boolean special;
    private final boolean ephemeral;
    private final ItemStack[] contents;
    private final String[] lines;

    BlockRecord(Block block) {
        this.state = block.getState();
        this.type = block.getType();
        this.x = block.getX();
        this.y = block.getY();
        this.z = block.getZ();
        this.air = Compat.isAir(this.type);
        this.liquid = block.isLiquid();
        this.contents = this.state instanceof InventoryHolder ? copyContents(inventoryOf(this.state).getContents()) : null;
        this.lines = this.state instanceof Sign ? ((Sign) this.state).getLines().clone() : null;
        this.special = this.contents != null || this.lines != null || isSpecialState(this.state) || isSpecialName(this.type.name());
        this.ephemeral = isEphemeralName(this.type.name());
    }

    int getX() {
        return this.x;
    }

    int getY() {
        return this.y;
    }

    int getZ() {
        return this.z;
    }

    Material getType() {
        return this.type;
    }

    BlockState getState() {
        return this.state;
    }

    Block getBlock() {
        return this.state.getBlock();
    }

    boolean isAir() {
        return this.air;
    }

    boolean isLiquid() {
        return this.liquid;
    }

    boolean isSpecial() {
        return this.special;
    }

    boolean canFly() {
        return !this.air && !this.liquid && !this.ephemeral && !this.special && this.type.isBlock();
    }

    boolean canSubstitute() {
        return !this.air && !this.liquid && !this.ephemeral && this.special;
    }

    boolean isOccluding() {
        return !this.air && !this.special && this.type.isOccluding();
    }

    void clearContents() {
        if (this.contents == null)
            return;

        if (!(this.state instanceof InventoryHolder))
            return;

        inventoryOf(this.state).clear();
    }

    void restore() {
        this.state.update(true, false);

        if (this.contents == null && this.lines == null)
            return;

        BlockState fresh = this.getBlock().getState();

        if (this.contents != null && fresh instanceof InventoryHolder) {
            Inventory inventory = inventoryOf(fresh);

            if (inventory.getSize() == this.contents.length)
                inventory.setContents(copyContents(this.contents));
        }

        if (this.lines != null && fresh instanceof Sign) {
            Sign sign = (Sign) fresh;

            for (int line = 0; line < this.lines.length && line < 4; line++)
                sign.setLine(line, this.lines[line]);

            sign.update(true, false);
        }
    }

    private static Inventory inventoryOf(BlockState state) {
        if (state instanceof Chest)
            return ((Chest) state).getBlockInventory();

        return ((InventoryHolder) state).getInventory();
    }

    private static ItemStack[] copyContents(ItemStack[] contents) {
        ItemStack[] copy = new ItemStack[contents.length];

        for (int slot = 0; slot < contents.length; slot++)
            copy[slot] = contents[slot] == null ? null : contents[slot].clone();

        return copy;
    }

    private static boolean isSpecialState(BlockState state) {
        return state instanceof CreatureSpawner || state instanceof Skull || state instanceof Banner || state instanceof Jukebox || state instanceof CommandBlock;
    }

    private static boolean isSpecialName(String name) {
        return name.contains("CHEST") || name.contains("SIGN") || name.contains("SKULL") || name.endsWith("_HEAD") || name.contains("BANNER")
                || name.contains("SPAWNER") || name.contains("SHULKER") || name.contains("BED") || name.contains("CONDUIT") || name.contains("BELL")
                || name.contains("PISTON") || name.contains("DECORATED_POT") || name.contains("NOTE_BLOCK");
    }

    private static boolean isEphemeralName(String name) {
        return name.contains("FIRE") || name.contains("PORTAL") || name.equals("BARRIER") || name.equals("LIGHT") || name.equals("STRUCTURE_VOID");
    }
}
