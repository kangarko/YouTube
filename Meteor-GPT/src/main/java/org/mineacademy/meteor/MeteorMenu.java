package org.mineacademy.meteor;

import java.util.Arrays;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

final class MeteorMenu implements InventoryHolder {

    private static final int[] EDGE = {0, 1, 2, 3, 4, 5, 6, 7, 8, 17, 26, 35, 44, 43, 42, 41, 39, 38, 37, 36, 27, 18, 9};
    final Player player;
    final Location target;
    final Inventory inventory;
    Element element = Element.FIRE;
    int power       = 3;
    boolean confirmed;
    private final MeteorPlugin plugin;

    MeteorMenu(MeteorPlugin plugin, Player player, Location target) {
        this.plugin    = plugin;
        this.player    = player;
        this.target    = target;
        this.inventory = Bukkit.createInventory(this, 45, ChatColor.DARK_GRAY + "Meteor " + ChatColor.GRAY + "| Choose your strike");
        this.draw();
        this.animate(0);
    }

    void draw() {
        Element[] elements = Element.values();

        for (int index = 0; index < elements.length; index++) {
            Element choice  = elements[index];
            String selected = choice == this.element ? ChatColor.WHITE + "Selected" : ChatColor.GRAY + "Click to select";
            this.inventory.setItem(11 + index * 2, item(new ItemStack(choice.icon), choice.color + "" + ChatColor.BOLD + choice.label, ChatColor.GRAY + choice.finale, "", selected));
        }

        for (int value = 1; value <= 5; value++) {
            ItemStack item = new ItemStack(value == this.power ? Material.GLOWSTONE : Material.COAL_BLOCK, value);
            this.inventory.setItem(28 + value, item(item, this.element.color + "Power " + value, ChatColor.GRAY + "Crater radius: " + (2 + value) + " blocks", value == this.power ? ChatColor.WHITE + "Selected" : ChatColor.GRAY + "Click to select"));
        }

        this.inventory.setItem(22, item(new ItemStack(Material.COMPASS), ChatColor.WHITE + "Target", ChatColor.GRAY + this.target.getWorld().getName(), ChatColor.GRAY + "X " + this.target.getBlockX() + "   Y " + this.target.getBlockY() + "   Z " + this.target.getBlockZ()));
        this.inventory.setItem(40, item(new ItemStack(Material.NETHER_STAR), this.element.color + "" + ChatColor.BOLD + "Confirm strike", ChatColor.WHITE + this.element.label + "   Power " + this.power, "", ChatColor.GRAY + "Close the menu to cancel."));
    }

    void animate(int frame) {
        for (int index = 0; index < EDGE.length; index++) {
            int distance = (index - frame % EDGE.length + EDGE.length) % EDGE.length;
            int dye      = distance < 3 ? this.element.dye : 15;
            this.inventory.setItem(EDGE[index], item(this.plugin.visuals.pane(dye), " "));
        }
    }

    void click(int slot) {
        if (this.confirmed)
            return;

        if (slot == 11 || slot == 13 || slot == 15) {
            this.element = Element.values()[(slot - 11) / 2];
            this.draw();
        } else if (slot >= 29 && slot <= 33) {
            this.power = slot - 28;
            this.draw();
        } else if (slot == 40) {
            this.plugin.confirm(this);

            return;
        } else
            return;

        this.plugin.visuals.sound(this.player, this.player.getLocation(), "block.note_block.pling", "note.pling", 0.7F, 1.2F + this.power * 0.1F);
    }

    private static ItemStack item(ItemStack item, String name, String... lore) {
        ItemMeta meta = item.getItemMeta();

        if (meta == null)
            throw new IllegalStateException("Menu item has no metadata: " + item.getType());

        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);

        return item;
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }
}
