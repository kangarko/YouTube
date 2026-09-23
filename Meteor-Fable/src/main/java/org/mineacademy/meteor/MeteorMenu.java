package org.mineacademy.meteor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class MeteorMenu implements InventoryHolder {

    private static final int SIZE = 27;
    private static final int[] ELEMENT_SLOTS = { 10, 12, 14 };
    private static final int CONFIRM_SLOT = 16;
    private static final int FIRST_POWER_SLOT = 20;
    private static final int MAX_POWER = 5;
    private static final int[] BORDER_SLOTS = { 9, 18, 17, 26 };
    private static final int SKY_STEPS = 12;

    private final MeteorPlugin plugin;
    private final Player player;
    private final Block target;
    private final Inventory inventory;
    private Element element = Element.FIRE;
    private int power = 3;
    private int frame;

    MeteorMenu(MeteorPlugin plugin, Player player, Block target) {
        this.plugin = plugin;
        this.player = player;
        this.target = target;
        this.inventory = Bukkit.createInventory(this, SIZE, ChatColor.DARK_RED.toString() + ChatColor.BOLD + "Meteor Strike");

        this.redraw();
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    public Player getPlayer() {
        return this.player;
    }

    void open() {
        this.player.openInventory(this.inventory);
        this.plugin.registerMenu(this);
    }

    void animate() {
        this.frame++;

        this.drawSky();
        this.drawBorder();
    }

    void handleClick(int slot) {
        for (int index = 0; index < ELEMENT_SLOTS.length; index++)
            if (slot == ELEMENT_SLOTS[index]) {
                this.element = Element.values()[index];

                this.element.playMenuClick(this.player);
                this.redraw();

                return;
            }

        if (slot >= FIRST_POWER_SLOT && slot < FIRST_POWER_SLOT + MAX_POWER) {
            this.power = slot - FIRST_POWER_SLOT + 1;

            MeteorSound.CLICK.play(this.player, 1F, 0.7F + this.power * 0.12F);
            this.redraw();

            return;
        }

        if (slot == CONFIRM_SLOT)
            this.confirm();
    }

    private void confirm() {
        if (this.plugin.hasStrike()) {
            MeteorPlugin.tell(this.player, ChatColor.RED + "Another meteor strike is already in progress.");
            this.closeLater();

            return;
        }

        MeteorSound.CLICK.play(this.player, 1F, 1.4F);
        this.closeLater();
        this.plugin.startStrike(this.player, this.target, this.element, this.power);
    }

    private void closeLater() {
        Bukkit.getScheduler().runTask(this.plugin, this.player::closeInventory);
    }

    private void redraw() {
        ItemStack filler = makeItem(MeteorMaterial.PANE_GRAY, 1, " ", false);

        for (int slot = 0; slot < SIZE; slot++)
            this.inventory.setItem(slot, filler);

        this.drawSky();
        this.drawBorder();

        for (int index = 0; index < ELEMENT_SLOTS.length; index++) {
            Element candidate = Element.values()[index];
            boolean selected = candidate == this.element;

            this.inventory.setItem(ELEMENT_SLOTS[index], makeItem(candidate.getIcon(), 1, candidate.getColor().toString() + ChatColor.BOLD + candidate.getDisplayName(), selected,
                    ChatColor.GRAY + "Element of the meteor.",
                    "",
                    selected ? ChatColor.GREEN + "Selected" : ChatColor.YELLOW + "Click to select"));
        }

        for (int level = 1; level <= MAX_POWER; level++) {
            boolean selected = level == this.power;

            this.inventory.setItem(FIRST_POWER_SLOT + level - 1, makeItem(MeteorMaterial.TNT, level, this.element.getColor().toString() + ChatColor.BOLD + "Power " + level, selected,
                    ChatColor.GRAY + "Crater radius: " + ChatColor.WHITE + (2 + level) + " blocks",
                    ChatColor.GRAY + "Blast radius: " + ChatColor.WHITE + (2 * (2 + level)) + " blocks",
                    "",
                    selected ? ChatColor.GREEN + "Selected" : ChatColor.YELLOW + "Click to select"));
        }

        this.inventory.setItem(CONFIRM_SLOT, makeItem(MeteorMaterial.EMERALD_BLOCK, 1, ChatColor.GREEN.toString() + ChatColor.BOLD + "Confirm strike", false,
                ChatColor.GRAY + "Element: " + this.element.getColor() + this.element.getDisplayName(),
                ChatColor.GRAY + "Power: " + ChatColor.WHITE + this.power,
                ChatColor.GRAY + "Target: " + ChatColor.WHITE + this.target.getX() + ", " + this.target.getY() + ", " + this.target.getZ(),
                "",
                ChatColor.YELLOW + "Click to launch the meteor"));
    }

    private void drawSky() {
        int step = this.frame % SKY_STEPS;
        int head = step <= 8 ? 8 - step : -1;

        for (int column = 0; column <= 8; column++) {
            ItemStack item;

            if (column == head)
                item = makeItem(this.element.getHead(), 1, this.element.getColor() + "Meteor", false);
            else if (head != -1 && column > head && column <= head + 3)
                item = makeItem(this.element.getPane(column - head - 1), 1, " ", false);
            else if (step == 9)
                item = makeItem(this.element.getPane(0), 1, " ", false);
            else if (step == 10)
                item = makeItem(this.element.getPane(2), 1, " ", false);
            else
                item = makeItem(MeteorMaterial.PANE_BLACK, 1, " ", false);

            this.inventory.setItem(column, item);
        }
    }

    private void drawBorder() {
        for (int index = 0; index < BORDER_SLOTS.length; index++)
            this.inventory.setItem(BORDER_SLOTS[index], makeItem(this.element.getPane(this.frame / 2 + index), 1, " ", false));
    }

    private static ItemStack makeItem(MeteorMaterial material, int amount, String name, boolean glint, String... lore) {
        ItemStack item = material.toItem(amount);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(name);

        if (lore.length > 0) {
            List<String> lines = new ArrayList<>(Arrays.asList(lore));

            meta.setLore(lines);
        }

        if (glint) {
            meta.addEnchant(Compat.getGlint(), 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);

        return item;
    }
}
