package org.mineacademy.meteor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class MeteorPlugin extends JavaPlugin {

    private static final String PREFIX = ChatColor.GOLD + "Meteor " + ChatColor.DARK_GRAY + "» " + ChatColor.GRAY;

    private final Map<UUID, MeteorMenu> menus = new HashMap<>();
    private final Map<UUID, Long> immunities = new HashMap<>();
    private Strike strike;
    private BukkitTask menuTask;

    @Override
    public void onEnable() {
        MeteorMaterial.load();
        MeteorSound.load();
        MeteorParticle.load();
        Compat.load();

        PluginCommand command = this.getCommand("meteor");
        MeteorCommand executor = new MeteorCommand(this);

        command.setExecutor(executor);
        command.setTabCompleter(executor);

        this.getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        this.getServer().getPluginManager().registerEvents(new StrikeListener(this), this);
        this.getLogger().info("Ready on Bukkit " + Bukkit.getBukkitVersion() + " using " + (MeteorParticle.isModern() ? "modern" : "legacy") + " particles.");
    }

    @Override
    public void onDisable() {
        for (MeteorMenu menu : new ArrayList<>(this.menus.values()))
            if (menu.getPlayer().isOnline())
                menu.getPlayer().closeInventory();

        this.menus.clear();
        this.stopMenuTask();

        if (this.strike != null) {
            this.strike.abort();
            this.strike = null;
        }
    }

    public Strike getStrike() {
        return this.strike;
    }

    public boolean hasStrike() {
        return this.strike != null;
    }

    void startStrike(Player caster, Block target, Element element, int power) {
        this.strike = new Strike(this, caster, target, element, power);

        this.strike.start();
    }

    void clearStrike() {
        this.strike = null;
    }

    void protect(UUID entityId, long millis) {
        this.pruneImmunities();
        this.immunities.put(entityId, System.currentTimeMillis() + millis);
    }

    boolean isImmune(UUID entityId) {
        Long until = this.immunities.get(entityId);

        if (until == null)
            return false;

        if (until > System.currentTimeMillis())
            return true;

        this.pruneImmunities();

        return false;
    }

    private void pruneImmunities() {
        Iterator<Map.Entry<UUID, Long>> iterator = this.immunities.entrySet().iterator();

        while (iterator.hasNext())
            if (iterator.next().getValue() <= System.currentTimeMillis())
                iterator.remove();
    }

    void registerMenu(MeteorMenu menu) {
        this.menus.put(menu.getPlayer().getUniqueId(), menu);

        if (this.menuTask != null)
            return;

        this.menuTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (MeteorMenu open : new ArrayList<>(this.menus.values()))
                open.animate();
        }, 2L, 2L);
    }

    void unregisterMenu(UUID playerId) {
        this.menus.remove(playerId);

        if (this.menus.isEmpty())
            this.stopMenuTask();
    }

    private void stopMenuTask() {
        if (this.menuTask == null)
            return;

        this.menuTask.cancel();
        this.menuTask = null;
    }

    static void tell(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + message);
    }
}
