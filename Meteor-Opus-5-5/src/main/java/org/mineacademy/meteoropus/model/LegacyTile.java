package org.mineacademy.meteoropus.model;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.SkullType;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.Chest;
import org.bukkit.block.CommandBlock;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Furnace;
import org.bukkit.block.Jukebox;
import org.bukkit.block.NoteBlock;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;
import org.bukkit.block.banner.Pattern;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

final class LegacyTile {

    private ItemStack[] contents;
    private String[] lines;
    private EntityType spawnedType;
    private int spawnDelay;
    private SkullType skullType;
    private BlockFace skullRotation;
    private String skullOwner;
    private DyeColor bannerColor;
    private List<Pattern> bannerPatterns;
    private String command;
    private String commandName;
    private Note note;
    private short burnTime;
    private short cookTime;
    private int brewingTime;
    private Material record;

    private LegacyTile() {
    }

    void apply(final Block block) {
        final BlockState fresh = block.getState();

        if (this.contents != null && fresh instanceof InventoryHolder)
            findInventory(fresh).setContents(this.contents);

        if (this.lines != null && fresh instanceof Sign)
            for (int line = 0; line < this.lines.length; line++)
                ((Sign) fresh).setLine(line, this.lines[line]);

        if (this.spawnedType != null && fresh instanceof CreatureSpawner) {
            ((CreatureSpawner) fresh).setSpawnedType(this.spawnedType);
            ((CreatureSpawner) fresh).setDelay(this.spawnDelay);
        }

        if (this.skullType != null && fresh instanceof Skull)
            this.applySkull((Skull) fresh);

        if (this.bannerColor != null && fresh instanceof Banner) {
            ((Banner) fresh).setBaseColor(this.bannerColor);
            ((Banner) fresh).setPatterns(this.bannerPatterns);
        }

        if (this.command != null && fresh instanceof CommandBlock) {
            ((CommandBlock) fresh).setCommand(this.command);
            ((CommandBlock) fresh).setName(this.commandName);
        }

        if (this.note != null && fresh instanceof NoteBlock)
            ((NoteBlock) fresh).setNote(this.note);

        if (fresh instanceof Furnace) {
            ((Furnace) fresh).setBurnTime(this.burnTime);
            ((Furnace) fresh).setCookTime(this.cookTime);
        }

        if (fresh instanceof BrewingStand)
            ((BrewingStand) fresh).setBrewingTime(this.brewingTime);

        if (this.record != null && fresh instanceof Jukebox)
            ((Jukebox) fresh).setPlaying(this.record);

        fresh.update(true, false);
    }

    private void applySkull(final Skull skull) {
        skull.setSkullType(this.skullType);
        skull.setRotation(this.skullRotation);

        if (this.skullOwner != null)
            skull.setOwner(this.skullOwner);
    }

    static LegacyTile capture(final BlockState state) {
        final LegacyTile tile = new LegacyTile();

        if (state instanceof InventoryHolder)
            tile.contents = cloneContents(findInventory(state).getContents());

        if (state instanceof Sign)
            tile.lines = ((Sign) state).getLines().clone();

        if (state instanceof CreatureSpawner) {
            tile.spawnedType = ((CreatureSpawner) state).getSpawnedType();
            tile.spawnDelay  = ((CreatureSpawner) state).getDelay();
        }

        if (state instanceof Skull) {
            final Skull skull = (Skull) state;

            tile.skullType     = skull.getSkullType();
            tile.skullRotation = skull.getRotation();
            tile.skullOwner    = skull.hasOwner() ? skull.getOwner() : null;
        }

        if (state instanceof Banner) {
            tile.bannerColor    = ((Banner) state).getBaseColor();
            tile.bannerPatterns = new ArrayList<>(((Banner) state).getPatterns());
        }

        if (state instanceof CommandBlock) {
            tile.command     = ((CommandBlock) state).getCommand();
            tile.commandName = ((CommandBlock) state).getName();
        }

        if (state instanceof NoteBlock)
            tile.note = ((NoteBlock) state).getNote();

        if (state instanceof Furnace) {
            tile.burnTime = ((Furnace) state).getBurnTime();
            tile.cookTime = ((Furnace) state).getCookTime();
        }

        if (state instanceof BrewingStand)
            tile.brewingTime = ((BrewingStand) state).getBrewingTime();

        if (state instanceof Jukebox && ((Jukebox) state).isPlaying())
            tile.record = ((Jukebox) state).getPlaying();

        return tile;
    }

    private static Inventory findInventory(final BlockState state) {
        return state instanceof Chest ? ((Chest) state).getBlockInventory() : ((InventoryHolder) state).getInventory();
    }

    private static ItemStack[] cloneContents(final ItemStack[] contents) {
        final ItemStack[] copy = new ItemStack[contents.length];

        for (int slot = 0; slot < contents.length; slot++)
            copy[slot] = contents[slot] == null ? null : contents[slot].clone();

        return copy;
    }
}
