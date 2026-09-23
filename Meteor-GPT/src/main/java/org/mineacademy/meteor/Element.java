package org.mineacademy.meteor;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

enum Element {

    FIRE("Fire", ChatColor.GOLD, "BLAZE_POWDER", "NETHERRACK", "GLOWSTONE", 1, "Molten heart", 255, 86, 12),
    ICE("Ice", ChatColor.AQUA, "PACKED_ICE", "PACKED_ICE", "ICE", 3, "Glacial crown", 85, 215, 255),
    VOID("Void", ChatColor.LIGHT_PURPLE, "ENDER_PEARL", "OBSIDIAN", "COAL_BLOCK", 10, "Event horizon", 165, 38, 235);

    final String label;
    final ChatColor color;
    final Material icon;
    final Material rock;
    final Material core;
    final int dye;
    final String finale;
    final int red;
    final int green;
    final int blue;

    Element(String label, ChatColor color, String icon, String rock, String core, int dye, String finale, int red, int green, int blue) {
        this.label  = label;
        this.color  = color;
        this.icon   = Material.valueOf(icon);
        this.rock   = Material.valueOf(rock);
        this.core   = Material.valueOf(core);
        this.dye    = dye;
        this.finale = finale;
        this.red    = red;
        this.green  = green;
        this.blue   = blue;
    }

    ItemStack rock(int index) {
        return new ItemStack(index % 5 == 0 ? this.core : this.rock);
    }
}
