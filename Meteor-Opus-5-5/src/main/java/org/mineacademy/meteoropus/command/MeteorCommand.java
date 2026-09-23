package org.mineacademy.meteoropus.command;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.mineacademy.meteoropus.menu.MeteorMenu;
import org.mineacademy.meteoropus.model.Strike;
import org.mineacademy.meteoropus.remain.CompSound;
import org.mineacademy.meteoropus.remain.Remain;

public final class MeteorCommand implements CommandExecutor {

    private static final double RANGE  = 64;
    private static final String PREFIX = "§8[§6Meteor§8] ";

    private final Plugin plugin;

    public MeteorCommand(final Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(PREFIX + "§cOnly players can call down a meteor.");

            return true;
        }

        final Player player = (Player) sender;

        if (Strike.isRunning())
            return this.refuse(player, "Another meteor is already falling. Wait until its crater has healed.");

        final Block target = this.findTarget(player);

        if (target == null)
            return this.refuse(player, "Look at a solid block within 64 blocks to mark the impact point.");

        new MeteorMenu(this.plugin, player, target).open();

        return true;
    }

    private boolean refuse(final Player player, final String message) {
        player.sendMessage(PREFIX + "§c" + message);

        CompSound.REFUSE.play(player, player.getLocation(), 1F, 1F);

        return true;
    }

    private Block findTarget(final Player player) {
        final Location eye     = player.getEyeLocation();
        final Vector direction = eye.getDirection();
        final World world      = eye.getWorld();
        final int bottom       = Remain.getMinHeight(world);
        final int top          = world.getMaxHeight() - 1;
        final int stepX        = (int) Math.signum(direction.getX());
        final int stepY        = (int) Math.signum(direction.getY());
        final int stepZ        = (int) Math.signum(direction.getZ());
        final double deltaX    = stepX == 0 ? Double.MAX_VALUE : Math.abs(1 / direction.getX());
        final double deltaY    = stepY == 0 ? Double.MAX_VALUE : Math.abs(1 / direction.getY());
        final double deltaZ    = stepZ == 0 ? Double.MAX_VALUE : Math.abs(1 / direction.getZ());
        int x                  = eye.getBlockX();
        int y                  = eye.getBlockY();
        int z                  = eye.getBlockZ();
        double nextX           = stepX == 0 ? Double.MAX_VALUE : (stepX > 0 ? x + 1 - eye.getX() : eye.getX() - x) * deltaX;
        double nextY           = stepY == 0 ? Double.MAX_VALUE : (stepY > 0 ? y + 1 - eye.getY() : eye.getY() - y) * deltaY;
        double nextZ           = stepZ == 0 ? Double.MAX_VALUE : (stepZ > 0 ? z + 1 - eye.getZ() : eye.getZ() - z) * deltaZ;
        double traveled        = 0; 

        while (traveled <= RANGE) {
            if (y < bottom && stepY <= 0 || y > top && stepY >= 0)
                return null;

            if (y >= bottom && y <= top) {
                if (!world.isChunkLoaded(x >> 4, z >> 4))
                    return null;

                final Block block = world.getBlockAt(x, y, z);

                if (block.getType().isSolid())
                    return block;
            }

            if (nextX < nextY && nextX < nextZ) {
                traveled = nextX;
                nextX   += deltaX;
                x       += stepX;

            } else if (nextY < nextZ) {
                traveled = nextY;
                nextY   += deltaY;
                y       += stepY;

            } else {
                traveled = nextZ;
                nextZ   += deltaZ;
                z       += stepZ;
            }
        }

        return null;
    }
}
