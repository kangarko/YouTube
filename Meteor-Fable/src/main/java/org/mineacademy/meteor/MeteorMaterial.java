package org.mineacademy.meteor;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

public enum MeteorMaterial {

    MAGMA("MAGMA_BLOCK", "MAGMA", "NETHERRACK"),
    NETHERRACK("NETHERRACK"),
    OBSIDIAN("OBSIDIAN"),
    CRYING_OBSIDIAN("CRYING_OBSIDIAN", "OBSIDIAN"),
    GLOWSTONE("GLOWSTONE"),
    LAVA("STATIONARY_LAVA", "LAVA"),
    FIRE("FIRE"),
    PACKED_ICE("PACKED_ICE"),
    BLUE_ICE("BLUE_ICE", "PACKED_ICE"),
    ICE("ICE"),
    SNOW_BLOCK("SNOW_BLOCK"),
    SNOW_LAYER("SNOW"),
    SEA_LANTERN("SEA_LANTERN"),
    COAL_BLOCK("COAL_BLOCK"),
    BLACK_CONCRETE("BLACK_CONCRETE", "COAL_BLOCK"),
    PURPUR("PURPUR_BLOCK", "OBSIDIAN"),
    END_STONE("END_STONE", "ENDER_STONE"),
    STONE("STONE"),
    BLAZE_POWDER("BLAZE_POWDER"),
    ENDER_PEARL("ENDER_PEARL"),
    EMERALD_BLOCK("EMERALD_BLOCK"),
    TNT("TNT"),
    FIRE_CHARGE("FIRE_CHARGE", "FIREBALL"),
    ENDER_EYE("ENDER_EYE", "EYE_OF_ENDER"),
    PANE_ORANGE("ORANGE_STAINED_GLASS_PANE", "STAINED_GLASS_PANE:1"),
    PANE_RED("RED_STAINED_GLASS_PANE", "STAINED_GLASS_PANE:14"),
    PANE_YELLOW("YELLOW_STAINED_GLASS_PANE", "STAINED_GLASS_PANE:4"),
    PANE_LIGHT_BLUE("LIGHT_BLUE_STAINED_GLASS_PANE", "STAINED_GLASS_PANE:3"),
    PANE_WHITE("WHITE_STAINED_GLASS_PANE", "STAINED_GLASS_PANE:0"),
    PANE_CYAN("CYAN_STAINED_GLASS_PANE", "STAINED_GLASS_PANE:9"),
    PANE_PURPLE("PURPLE_STAINED_GLASS_PANE", "STAINED_GLASS_PANE:10"),
    PANE_MAGENTA("MAGENTA_STAINED_GLASS_PANE", "STAINED_GLASS_PANE:2"),
    PANE_BLACK("BLACK_STAINED_GLASS_PANE", "STAINED_GLASS_PANE:15"),
    PANE_GRAY("GRAY_STAINED_GLASS_PANE", "STAINED_GLASS_PANE:7");

    private final String[] candidates;
    private Material material;
    private short data;

    MeteorMaterial(String... candidates) {
        this.candidates = candidates;
    }

    public Material getMaterial() {
        return this.material;
    }

    public ItemStack toItem(int amount) {
        if (this.data == 0)
            return new ItemStack(this.material, amount);

        return new ItemStack(this.material, amount, this.data);
    }

    public void place(Block block) {
        block.setType(this.material, false);
    }

    private void resolve() {
        for (String candidate : this.candidates) {
            int colon = candidate.indexOf(':');
            String name = colon == -1 ? candidate : candidate.substring(0, colon);
            Material found = Material.getMaterial(name);

            if (found == null)
                continue;

            this.material = found;
            this.data = colon == -1 ? 0 : Short.parseShort(candidate.substring(colon + 1));

            return;
        }

        throw new IllegalStateException("Report / None of the materials " + String.join(", ", this.candidates) + " exist on this server");
    }

    public static void load() {
        for (MeteorMaterial material : values())
            material.resolve();
    }
}
