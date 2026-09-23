package org.mineacademy.meteoropus.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;
import org.mineacademy.meteoropus.remain.Remain;

public final class Protection {

    private static final int AIRBORNE_GRACE         = 6;
    private static final int LANDED_GRACE           = 40;
    private static final int MAX_AGE                = 20 * 30;
    private static final Map<Entity, int[]> ENTRIES = new HashMap<>();

    private Protection() {
    }

    public static void add(final Entity entity) {
        ENTRIES.put(entity, new int[] { 0, 0 });

        entity.setFallDistance(0);
    }

    public static boolean isProtected(final Entity entity) {
        return !ENTRIES.isEmpty() && ENTRIES.containsKey(entity);
    }

    public static void tick() {
        if (ENTRIES.isEmpty())
            return;

        for (final Iterator<Map.Entry<Entity, int[]>> iterator = ENTRIES.entrySet().iterator(); iterator.hasNext();) {
            final Map.Entry<Entity, int[]> entry = iterator.next();
            final Entity entity                  = entry.getKey();
            final int[] state                    = entry.getValue();

            state[0]++;

            if (!entity.isValid() || state[0] > MAX_AGE) {
                iterator.remove();

                continue;
            }

            if (state[0] > AIRBORNE_GRACE && entity.isOnGround()) {
                entity.setFallDistance(0);

                if (++state[1] > LANDED_GRACE)
                    iterator.remove();
            }
        }
    }

    public static void releaseAll() {
        for (final Entity entity : ENTRIES.keySet())
            if (entity.isValid() && !entity.isOnGround())
                ground(entity);

        ENTRIES.clear();
    }

    private static void ground(final Entity entity) {
        final Location location = entity.getLocation();
        final World world       = location.getWorld();
        final int minY          = Remain.getMinHeight(world);

        for (int y = Math.min(location.getBlockY(), world.getMaxHeight() - 1); y > minY; y--)
            if (world.getBlockAt(location.getBlockX(), y - 1, location.getBlockZ()).getType().isSolid()) {
                location.setY(y);

                entity.setVelocity(new Vector());
                entity.teleport(location);
                entity.setFallDistance(0);

                return;
            }
    }
}
