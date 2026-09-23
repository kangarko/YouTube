package org.mineacademy.meteoropus.menu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.mineacademy.meteoropus.model.Element;
import org.mineacademy.meteoropus.model.Strike;
import org.mineacademy.meteoropus.remain.CompMaterial;
import org.mineacademy.meteoropus.remain.CompSound;
import org.mineacademy.meteoropus.remain.Remain;

public final class MeteorMenu implements InventoryHolder {

    private static final int SIZE                   = 45;
    private static final int[] BORDER               = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 17, 26, 35, 44, 43, 42, 41, 40, 39, 38, 37, 36, 27, 18, 9 };
    private static final int[] ELEMENT_SLOTS        = { 11, 13, 15 };
    private static final int[] POWER_SLOTS          = { 20, 21, 22, 23, 24 };
    private static final int CONFIRM_SLOT           = 31;
    private static final int PULSE_FRAMES           = 5;
    private static final String TITLE               = "§8Meteor Strike";
    private static final Map<UUID, MeteorMenu> OPEN = new HashMap<>();

    private final Plugin plugin;
    private final Player player;
    private final Block target;
    private final Inventory inventory;
    private final ItemStack[] frameCache             = new ItemStack[SIZE];
    private final Map<CompMaterial, ItemStack> panes = new EnumMap<>(CompMaterial.class);
    private Element element                          = Element.FIRE;
    private int power                                = 3;
    private int frame;
    private boolean confirmed;
    private boolean resyncQueued;
    private BukkitTask animation;

    public MeteorMenu(final Plugin plugin, final Player player, final Block target) {
        this.plugin    = plugin;
        this.player    = player;
        this.target    = target;
        this.inventory = Bukkit.createInventory(this, SIZE, TITLE);
    }

    public void open() {
        this.drawAll();

        final MeteorMenu previous = OPEN.put(this.player.getUniqueId(), this);

        if (previous != null)
            previous.stopAnimation();

        this.player.openInventory(this.inventory);
        this.animation = Bukkit.getScheduler().runTaskTimer(this.plugin, this::animate, 2, 2);

        CompSound.MENU_OPEN.play(this.player, this.player.getLocation(), 0.8F, 1F);
        CompSound.AMETHYST.play(this.player, this.player.getLocation(), 0.6F, 1.4F);
    }

    public void onClick(final int slot) {
        if (this.confirmed)
            return;

        final int elementIndex = indexOf(ELEMENT_SLOTS, slot);
        final int powerIndex   = indexOf(POWER_SLOTS, slot);

        if (elementIndex != -1) {
            this.element = Element.values()[elementIndex];

            this.element.getSelectSound().play(this.player, this.player.getLocation(), 1F, 1F);
            this.drawAll();

        } else if (powerIndex != -1) {
            this.power = powerIndex + 1;

            CompSound.PLING.play(this.player, this.player.getLocation(), 1F, 0.5F + this.power * 0.25F);
            this.drawAll();

        } else if (slot == CONFIRM_SLOT)
            this.confirm();
    }

    public void onClose() {
        this.stopAnimation();

        if (OPEN.get(this.player.getUniqueId()) == this)
            OPEN.remove(this.player.getUniqueId());
    }

    public void resync() {
        if (this.resyncQueued)
            return;

        this.resyncQueued = true;

        Bukkit.getScheduler().runTask(this.plugin, () -> {
            this.resyncQueued = false;

            if (this.player.isOnline())
                this.player.updateInventory();
        });
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    private void confirm() {
        this.confirmed = true;
        this.stopAnimation();

        CompSound.CLICK.play(this.player, this.player.getLocation(), 1F, 1F);

        Bukkit.getScheduler().runTask(this.plugin, () -> {
            if (this.player.isOnline())
                this.player.closeInventory();

            this.onClose();

            if (!this.player.isOnline())
                return;

            if (Strike.isRunning()) {
                this.player.sendMessage("§8[§6Meteor§8] §cAnother meteor is already falling. Wait until its crater has healed.");

                CompSound.REFUSE.play(this.player, this.player.getLocation(), 1F, 1F);

                return;
            }

            if (!this.target.getWorld().isChunkLoaded(this.target.getX() >> 4, this.target.getZ() >> 4)) {
                this.player.sendMessage("§8[§6Meteor§8] §cThe target is no longer loaded. Look at it again and retry.");

                CompSound.REFUSE.play(this.player, this.player.getLocation(), 1F, 1F);

                return;
            }

            CompSound.CONFIRM.play(this.player, this.player.getLocation(), 0.8F, 0.8F);

            Strike.start(this.plugin, this.player, this.target, this.element, this.power);
        });
    }

    private void animate() {
        if (!this.player.isOnline()) {
            this.onClose();

            return;
        }

        this.frame++;
        this.drawBorder();

        if (this.frame % PULSE_FRAMES == 0)
            this.drawConfirm();
    }

    private void drawAll() {
        for (int slot = 0; slot < SIZE; slot++)
            this.set(slot, this.findPane(CompMaterial.BLACK_PANE));

        this.drawBorder();

        for (int index = 0; index < ELEMENT_SLOTS.length; index++)
            this.set(ELEMENT_SLOTS[index], this.createElementItem(Element.values()[index]));

        for (int index = 0; index < POWER_SLOTS.length; index++)
            this.set(POWER_SLOTS[index], this.createPowerItem(index + 1));

        this.drawConfirm();
    }

    private void drawBorder() {
        final CompMaterial[] comet = this.element.getComet();

        for (int index = 0; index < BORDER.length; index++) {
            final int behind        = Math.floorMod(this.frame - index, BORDER.length / 2);
            final CompMaterial pane = behind < comet.length ? comet[behind] : CompMaterial.BLACK_PANE;

            this.set(BORDER[index], this.findPane(pane));
        }
    }

    private void drawConfirm() {
        final boolean lit = this.frame / PULSE_FRAMES % 2 == 0;
        final String name = (lit ? "§a§l» " : "§2§l» ") + "Launch meteor" + (lit ? " §a§l«" : " §2§l«");

        this.set(CONFIRM_SLOT, this.createItem(CompMaterial.NETHER_STAR, 1, name, lit,
                "",
                "§7Element: " + this.element.getColor() + this.element.getName(),
                "§7Power: §f" + this.power + " §8(crater radius " + (2 + this.power) + ")",
                "§7Target: §f" + this.target.getX() + ", " + this.target.getY() + ", " + this.target.getZ(),
                "",
                "§eClick to call it down."));
    }

    private ItemStack createElementItem(final Element option) {
        final boolean selected = option == this.element;

        return this.createItem(option.getIcon(), 1, (selected ? option.getColor() + "§l" : "§7") + option.getName(), selected,
                "§7" + option.getDescription(),
                "",
                selected ? option.getAccent() + "Selected" : "§8Click to select.");
    }

    private ItemStack createPowerItem(final int level) {
        final boolean selected = level == this.power;
        String bar             = "";

        for (int step = 1; step <= 5; step++)
            bar += (step <= level ? this.element.getColor() : "§8") + "■";

        return this.createItem(this.element.getPowerIcon(), level, (selected ? this.element.getColor() + "§l" : "§7") + "Power " + level, selected,
                bar,
                "§7Crater radius: §f" + (2 + level) + " blocks",
                "",
                selected ? this.element.getAccent() + "Selected" : "§8Click to select.");
    }

    private ItemStack findPane(final CompMaterial material) {
        ItemStack pane = this.panes.get(material);

        if (pane == null) {
            pane = this.createItem(material, 1, " ", false);

            this.panes.put(material, pane);
        }

        return pane;
    }

    private ItemStack createItem(final CompMaterial material, final int amount, final String name, final boolean glowing, final String... lore) {
        final ItemStack item = material.toItem(amount);
        final ItemMeta meta  = item.getItemMeta();

        meta.setDisplayName(name);

        if (lore.length > 0)
            meta.setLore(new ArrayList<>(Arrays.asList(lore)));

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        if (glowing)
            Remain.setGlowing(meta);

        item.setItemMeta(meta);

        return item;
    }

    private void set(final int slot, final ItemStack item) {
        if (item.equals(this.frameCache[slot]))
            return;

        this.frameCache[slot] = item;
        this.inventory.setItem(slot, item);
    }

    private void stopAnimation() {
        if (this.animation != null) {
            this.animation.cancel();

            this.animation = null;
        }
    }

    public static MeteorMenu findOpen(final Player player) {
        return OPEN.get(player.getUniqueId());
    }

    public static void closeAll() {
        for (final MeteorMenu menu : new ArrayList<>(OPEN.values())) {
            menu.stopAnimation();

            if (menu.player.isOnline())
                menu.player.closeInventory();
        }

        OPEN.clear();
    }

    private static int indexOf(final int[] slots, final int slot) {
        for (int index = 0; index < slots.length; index++)
            if (slots[index] == slot)
                return index;

        return -1;
    }
}
