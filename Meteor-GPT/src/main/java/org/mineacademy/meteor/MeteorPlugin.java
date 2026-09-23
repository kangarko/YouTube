package org.mineacademy.meteor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public final class MeteorPlugin extends JavaPlugin {

    BukkitVisuals visuals;
    MeteorStrike strike;
    final Map<UUID, MeteorMenu> menus = new HashMap<>();
    private int frame;

    @Override
    public void onEnable() {
        this.visuals = new BukkitVisuals();
        this.getServer().getPluginManager().registerEvents(new MeteorListener(this), this);

        if (this.getCommand("meteor") == null)
            throw new IllegalStateException("The meteor command is missing from plugin.yml");

        this.getCommand("meteor").setExecutor(this);
        this.getServer().getScheduler().runTaskTimer(this, this::tick, 1, 1);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] arguments) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only a player can summon a meteor.");
            return true;
        }

        Player player = (Player) sender;

        if (this.strike != null) {
            player.sendMessage(ChatColor.RED + "A meteor strike is already running. Wait until the world is restored.");

            return true;
        }

        Block target = this.findTarget(player);

        if (target == null) {
            player.sendMessage(ChatColor.RED + "Look at a solid block within 64 blocks to summon a meteor.");

            return true;
        }

        MeteorMenu menu = new MeteorMenu(this, player, target.getLocation());
        player.closeInventory();
        this.menus.put(player.getUniqueId(), menu);
        player.openInventory(menu.inventory);
        this.visuals.sound(player, player.getLocation(), "entity.experience_orb.pickup", "random.orb", 0.8F, 0.8F);

        return true;
    }

    private Block findTarget(Player player) {
        Location eye = player.getEyeLocation();
        Vector step  = eye.getDirection().multiply(0.1);
        int min      = this.visuals.minHeight(player.getWorld());

        for (int sample = 0; sample <= 640; sample++) {
            if (eye.getY() >= min && eye.getY() < player.getWorld().getMaxHeight()) {
                if (!player.getWorld().isChunkLoaded(eye.getBlockX() >> 4, eye.getBlockZ() >> 4))
                    return null;

                Block block = eye.getBlock();

                if (block.getType().isSolid())
                    return block;
            }

            eye.add(step);
        }

        return null;
    }

    void confirm(MeteorMenu menu) {
        if (menu.confirmed)
            return;

        if (this.strike != null) {
            menu.player.sendMessage(ChatColor.RED + "A meteor strike is already running.");

            return;
        }

        if (!menu.target.getBlock().getType().isSolid()) {
            menu.player.sendMessage(ChatColor.RED + "The target block has changed. Close this menu and choose another target.");

            return;
        }

        menu.confirmed = true;
        this.strike    = new MeteorStrike(this, menu);
    }

    private void tick() {
        try {
            this.frame++;

            if (this.frame % 3 == 0)
                for (MeteorMenu menu : new ArrayList<>(this.menus.values()))
                    if (!menu.confirmed)
                        menu.animate(this.frame / 3);

            if (this.strike == null)
                return;

            if (this.strike.tick == 0)
                for (MeteorMenu menu : new ArrayList<>(this.menus.values()))
                    menu.player.closeInventory();

            this.strike.advance();
        } catch (RuntimeException exception) {
            this.getLogger().log(Level.SEVERE, "Meteor stopped after an unexpected response. Restoring the world.", exception);

            if (this.strike != null)
                this.strike.finish(true);
        }
    }

    void complete(MeteorStrike completed) {
        if (this.strike == completed)
            this.strike = null;
    }

    @Override
    public void onDisable() {
        this.getServer().getScheduler().cancelTasks(this);

        for (MeteorMenu menu : new ArrayList<>(this.menus.values()))
            menu.player.closeInventory();

        this.menus.clear();

        if (this.strike != null)
            this.strike.finish(true);
    }
}
