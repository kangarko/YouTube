package org.mineacademy.meteor;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.Chest;
import org.bukkit.block.Furnace;
import org.bukkit.block.Sign;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

final class SavedBlock {

    final BlockState state;
    final Location center;
    final boolean occupied;
    final ItemStack[] contents;
    private final short burnTime;
    private final short cookTime;
    private final int brewingTime;
    private final String[] signLines;
    boolean changed;
    DisplayPiece debris;

    SavedBlock(Block block, BukkitVisuals visuals) {
        this.state          = block.getState();
        this.center         = block.getLocation().add(0.5, 0.5, 0.5);
        this.occupied       = block.getType() != Material.AIR && !block.getType().name().endsWith("_AIR");
        this.signLines      = !visuals.flattened && this.state instanceof Sign ? ((Sign) this.state).getLines().clone() : null;
        Inventory inventory = inventory(this.state);

        if (inventory == null)
            this.contents = null;
        else {
            this.contents = inventory.getContents();

            for (int index = 0; index < this.contents.length; index++)
                if (this.contents[index] != null)
                    this.contents[index] = this.contents[index].clone();
        }

        if (this.state instanceof Furnace) {
            Furnace furnace = (Furnace) this.state;
            this.burnTime   = furnace.getBurnTime();
            this.cookTime   = furnace.getCookTime();
        } else {
            this.burnTime = 0;
            this.cookTime = 0;
        }

        if (this.state instanceof BrewingStand)
            this.brewingTime = ((BrewingStand) this.state).getBrewingTime();
        else
            this.brewingTime = 0;
    }

    void change(Material material) {
        Block block = this.state.getBlock();

        if (block.getType() == material)
            return;

        this.changed = true;

        if (this.contents != null) {
            Inventory inventory = inventory(block.getState());

            if (inventory != null)
                inventory.clear();
        }

        block.setType(material, false);
    }

    void restore() {
        if (!this.changed)
            return;

        if (!this.state.update(true, false))
            throw new IllegalStateException("Cannot restore block " + this.center);

        BlockState current = this.state.getBlock().getState();

        if (this.signLines != null) {
            Sign sign = (Sign) current;

            for (int index = 0; index < this.signLines.length; index++)
                sign.setLine(index, this.signLines[index]);

            sign.update(true, false);
        }

        if (this.contents != null) {
            Inventory inventory = inventory(current);

            if (inventory == null)
                throw new IllegalStateException("Restored container has no inventory at " + this.center);

            inventory.setContents(this.contents);
        }

        if (current instanceof Furnace) {
            Furnace furnace = (Furnace) current;
            furnace.setBurnTime(this.burnTime);
            furnace.setCookTime(this.cookTime);
            furnace.update(true, false);
        }

        if (current instanceof BrewingStand) {
            BrewingStand stand = (BrewingStand) current;
            stand.setBrewingTime(this.brewingTime);
            stand.update(true, false);
        }

        this.changed = false;
    }

    static Inventory inventory(BlockState state) {
        if (state instanceof Chest)
            return ((Chest) state).getBlockInventory();

        if (state instanceof InventoryHolder)
            return ((InventoryHolder) state).getInventory();

        return null;
    }
}
