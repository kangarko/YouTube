package org.mineacademy.meteoropus.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

final class Audience {

    private static final double RANGE_SQUARED = 128 * 128;
    private static final long DAY             = 24000;
    private static final long MIDNIGHT        = 18000;

    private final Location center;
    private final Map<UUID, Viewer> viewers = new LinkedHashMap<>();
    private final List<Player> players      = new ArrayList<>();

    Audience(final Location center) {
        this.center = center;
    }

    void refresh() {
        final World world = this.center.getWorld();

        for (final Iterator<Viewer> iterator = this.viewers.values().iterator(); iterator.hasNext();) {
            final Viewer viewer = iterator.next();

            if (!this.isInRange(viewer.player)) {
                viewer.restore();

                iterator.remove();
            }
        }

        for (final Player player : world.getPlayers())
            if (!this.viewers.containsKey(player.getUniqueId()) && this.isInRange(player))
                this.viewers.put(player.getUniqueId(), new Viewer(player));

        this.players.clear();

        for (final Viewer viewer : this.viewers.values())
            this.players.add(viewer.player);
    }

    void darken(final double darkness) {
        final long worldTime = this.center.getWorld().getFullTime();

        for (final Viewer viewer : this.viewers.values())
            viewer.darken(worldTime, darkness);
    }

    void restore() {
        for (final Viewer viewer : this.viewers.values())
            viewer.restore();

        this.viewers.clear();
        this.players.clear();
    }

    List<Player> getPlayers() {
        return this.players;
    }

    private boolean isInRange(final Player player) {
        return player.isOnline() && player.getWorld().equals(this.center.getWorld()) && player.getLocation().distanceSquared(this.center) <= RANGE_SQUARED;
    }

    private static final class Viewer {

        private final Player player;
        private final long offset;
        private final boolean relative;

        private Viewer(final Player player) {
            this.player   = player;
            this.offset   = player.getPlayerTimeOffset();
            this.relative = player.isPlayerTimeRelative();
        }

        private void darken(final long worldTime, final double darkness) {
            final long natural = this.relative ? worldTime + this.offset : this.offset;
            long delta         = Math.floorMod(MIDNIGHT - Math.floorMod(natural, DAY), DAY);

            if (delta > DAY / 2)
                delta -= DAY;

            this.player.setPlayerTime(Math.max(0, natural + Math.round(delta * darkness)), false);
        }

        private void restore() {
            if (this.player.isOnline())
                this.player.setPlayerTime(this.offset, this.relative);
        }
    }
}
