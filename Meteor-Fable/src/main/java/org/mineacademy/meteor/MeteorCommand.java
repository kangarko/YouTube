package org.mineacademy.meteor;

import java.util.Collections;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;

public final class MeteorCommand implements CommandExecutor, TabCompleter {

    private static final int RANGE = 64;

    private final MeteorPlugin plugin;

    MeteorCommand(MeteorPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            MeteorPlugin.tell(sender, ChatColor.RED + "Only players can call down a meteor.");

            return true;
        }

        Player player = (Player) sender;

        if (this.plugin.hasStrike()) {
            MeteorPlugin.tell(player, ChatColor.RED + "A meteor strike is already in progress. Wait for the sky to clear.");

            return true;
        }

        Block target = findTarget(player);

        if (target == null) {
            MeteorPlugin.tell(player, ChatColor.RED + "Look at a solid block within " + RANGE + " blocks to choose the impact point.");

            return true;
        }

        new MeteorMenu(this.plugin, player, target).open();

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }

    private static Block findTarget(Player player) {
        BlockIterator iterator = new BlockIterator(player, RANGE);

        while (iterator.hasNext()) {
            Block block = iterator.next();

            if (block.getType().isSolid())
                return block;
        }

        return null;
    }
}
