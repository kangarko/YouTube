package org.mineacademy.meteoropus.remain;

import java.util.Arrays;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public enum CompMaterial {

    BLACK_PANE("BLACK_STAINED_GLASS_PANE", "STAINED_GLASS_PANE:15"),
    GRAY_PANE("GRAY_STAINED_GLASS_PANE", "STAINED_GLASS_PANE:7"),
    WHITE_PANE("WHITE_STAINED_GLASS_PANE", "STAINED_GLASS_PANE:0"),
    YELLOW_PANE("YELLOW_STAINED_GLASS_PANE", "STAINED_GLASS_PANE:4"),
    ORANGE_PANE("ORANGE_STAINED_GLASS_PANE", "STAINED_GLASS_PANE:1"),
    RED_PANE("RED_STAINED_GLASS_PANE", "STAINED_GLASS_PANE:14"),
    BROWN_PANE("BROWN_STAINED_GLASS_PANE", "STAINED_GLASS_PANE:12"),
    LIGHT_BLUE_PANE("LIGHT_BLUE_STAINED_GLASS_PANE", "STAINED_GLASS_PANE:3"),
    CYAN_PANE("CYAN_STAINED_GLASS_PANE", "STAINED_GLASS_PANE:9"),
    BLUE_PANE("BLUE_STAINED_GLASS_PANE", "STAINED_GLASS_PANE:11"),
    MAGENTA_PANE("MAGENTA_STAINED_GLASS_PANE", "STAINED_GLASS_PANE:2"),
    PURPLE_PANE("PURPLE_STAINED_GLASS_PANE", "STAINED_GLASS_PANE:10"),

    FIRE_CHARGE("FIRE_CHARGE", "FIREBALL"),
    BLUE_ICE("BLUE_ICE", "PACKED_ICE"),
    ENDER_EYE("ENDER_EYE", "EYE_OF_ENDER"),
    BLAZE_POWDER("BLAZE_POWDER"),
    SNOWBALL("SNOWBALL", "SNOW_BALL"),
    ENDER_PEARL("ENDER_PEARL"),
    NETHER_STAR("NETHER_STAR"),

    MAGMA_BLOCK("MAGMA_BLOCK", "MAGMA", "NETHERRACK"),
    NETHERRACK("NETHERRACK"),
    OBSIDIAN("OBSIDIAN"),
    CRYING_OBSIDIAN("CRYING_OBSIDIAN", "OBSIDIAN"),
    BLACKSTONE("BLACKSTONE", "COAL_BLOCK"),
    SHROOMLIGHT("SHROOMLIGHT", "GLOWSTONE"),
    ORANGE_TERRACOTTA("ORANGE_TERRACOTTA", "STAINED_CLAY:1"),
    RED_TERRACOTTA("RED_TERRACOTTA", "STAINED_CLAY:14"),
    COAL_BLOCK("COAL_BLOCK"),
    LAVA("LAVA", "STATIONARY_LAVA"),

    ICE("ICE"),
    PACKED_ICE("PACKED_ICE"),
    SNOW_BLOCK("SNOW_BLOCK"),
    LIGHT_BLUE_GLASS("LIGHT_BLUE_STAINED_GLASS", "STAINED_GLASS:3"),
    WHITE_GLASS("WHITE_STAINED_GLASS", "STAINED_GLASS:0"),

    BLACK_CONCRETE("BLACK_CONCRETE", "CONCRETE:15", "COAL_BLOCK"),
    PURPLE_GLASS("PURPLE_STAINED_GLASS", "STAINED_GLASS:10"),
    PURPLE_TERRACOTTA("PURPLE_TERRACOTTA", "STAINED_CLAY:10"),

    OAK_PLANKS("OAK_PLANKS", "WOOD"),
    WHITE_WOOL("WHITE_WOOL", "WOOL"),
    BONE_BLOCK("BONE_BLOCK", "QUARTZ_BLOCK"),
    PURPUR_BLOCK("PURPUR_BLOCK", "OBSIDIAN"),
    TERRACOTTA("TERRACOTTA", "HARD_CLAY");

    private final Material material;
    private final byte data;
    private BlockLook look;

    CompMaterial(final String... candidates) {
        final String winner = findCandidate(candidates);
        final int colon     = winner.indexOf(':');

        this.material = Material.getMaterial(colon == -1 ? winner : winner.substring(0, colon));
        this.data     = colon == -1 ? 0 : Byte.parseByte(winner.substring(colon + 1));
    }

    public Material getMaterial() {
        return this.material;
    }

    public byte getData() {
        return this.data;
    }

    public BlockLook getLook() {
        if (this.look == null)
            this.look = BlockLook.fromMaterial(this.material, this.data);

        return this.look;
    }

    @SuppressWarnings("deprecation")
    public ItemStack toItem(final int amount) {
        return this.data == 0 ? new ItemStack(this.material, amount) : new ItemStack(this.material, amount, this.data);
    }

    private static String findCandidate(final String[] candidates) {
        for (final String candidate : candidates) {
            final int colon = candidate.indexOf(':');

            if (Material.getMaterial(colon == -1 ? candidate : candidate.substring(0, colon)) != null)
                return candidate;
        }

        throw new IllegalStateException("None of " + Arrays.toString(candidates) + " exists on this server");
    }
}
