package org.mineacademy.meteoropus.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.util.Vector;
import org.mineacademy.meteoropus.remain.BlockLook;
import org.mineacademy.meteoropus.remain.Remain;

public final class MovingBlock {

    private static final double GRAVITY               = 0.04D;
    private static final String LEGACY_MARKER         = "MeteorOpus_Block";
    private static final Map<UUID, MovingBlock> ALIVE = new HashMap<>();

    private final FallingBlock entity;
    private final BlockLook look;
    private boolean weightless;
    private boolean landed;

    private MovingBlock(final FallingBlock entity, final BlockLook look) {
        this.entity = entity;
        this.look   = look;
    }

    public void push(final Vector displacement) {
        if (Remain.canToggleGravity()) {
            if (!this.weightless) {
                Remain.setGravity(this.entity, false);

                this.weightless = true;
            }

            this.entity.setVelocity(displacement);

            return;
        }

        this.entity.setVelocity(new Vector(displacement.getX(), displacement.getY() + GRAVITY, displacement.getZ()));
    }

    public void launch(final Vector velocity) {
        if (this.weightless) {
            Remain.setGravity(this.entity, true);

            this.weightless = false;
        }

        this.entity.setVelocity(velocity);
    }

    public void teleport(final Location location) {
        this.entity.teleport(location);
    }

    public Location getLocation() {
        return this.entity.getLocation();
    }

    public BlockLook getLook() {
        return this.look;
    }

    public boolean isAlive() {
        return !this.landed && this.entity.isValid();
    }

    public void markLanded() {
        this.landed = true;
    }

    public void remove() {
        ALIVE.remove(this.entity.getUniqueId());

        this.entity.remove();
    }

    public static MovingBlock spawn(final Location location, final BlockLook look) {
        if (!location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4))
            return null;

        final FallingBlock entity = look.spawnFallingBlock(location);

        entity.setDropItem(false);
        entity.setHurtEntities(false);

        if (Remain.canSetPersistent())
            Remain.setPersistent(entity, false);

        else {
            entity.setCustomName(LEGACY_MARKER);
            entity.setCustomNameVisible(false);
        }

        final MovingBlock block = new MovingBlock(entity, look);

        if (Remain.canToggleGravity()) {
            Remain.setGravity(entity, false);

            block.weightless = true;
        }

        ALIVE.put(entity.getUniqueId(), block);

        return block;
    }

    public static MovingBlock findByEntity(final Entity entity) {
        return ALIVE.get(entity.getUniqueId());
    }

    public static boolean isLeftover(final Entity entity) {
        return entity instanceof FallingBlock && LEGACY_MARKER.equals(entity.getCustomName()) && !ALIVE.containsKey(entity.getUniqueId());
    }

    public static void removeAll() {
        for (final MovingBlock block : new ArrayList<>(ALIVE.values()))
            block.remove();
    }
}
