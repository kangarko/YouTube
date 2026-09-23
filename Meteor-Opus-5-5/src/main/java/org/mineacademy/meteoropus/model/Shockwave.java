package org.mineacademy.meteoropus.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.mineacademy.meteoropus.remain.BlockLook;
import org.mineacademy.meteoropus.remain.CompParticle;
import org.mineacademy.meteoropus.remain.CompSound;

final class Shockwave {

    private static final double SPEED     = 1.15;
    private static final int POP_LIFETIME = 40;
    private static final int SCAN_UP      = 5;
    private static final int SCAN_DOWN    = 10;
    private static final int CLIMB_TICKS  = 8;

    private final Strike strike;
    private final World world;
    private final int centerX;
    private final int centerY;
    private final int centerZ;
    private final int radius;
    private final double reach;
    private final List<int[]> columns             = new ArrayList<>();
    private final Map<Long, Integer> surfaces     = new HashMap<>();
    private final Map<Long, Boolean> loadedChunks = new HashMap<>();
    private final List<MovingBlock> pops          = new ArrayList<>();
    private final List<LivingEntity> targets      = new ArrayList<>();
    private final List<Throw> climbing            = new ArrayList<>();
    private int nextColumn;
    private int age = -1;

    Shockwave(final Strike strike) {
        final Block target = strike.getTarget();

        this.strike  = strike;
        this.world   = target.getWorld();
        this.centerX = target.getX();
        this.centerY = target.getY();
        this.centerZ = target.getZ();
        this.radius  = strike.getRadius();
        this.reach   = this.radius * 3 + 8;

        final int bound = (int) Math.ceil(this.reach);

        for (int x = -bound; x <= bound; x++)
            for (int z = -bound; z <= bound; z++) {
                final int distanceSquared = x * x + z * z;

                if (distanceSquared > this.radius * this.radius && distanceSquared <= this.reach * this.reach)
                    this.columns.add(new int[] { x, z, distanceSquared });
            }

        this.columns.sort((first, second) -> Integer.compare(first[2], second[2]));
    }

    double getReach() {
        return this.reach;
    }

    void start() {
        final double range = this.radius * 2;

        this.age = 0;

        for (final Entity entity : this.world.getNearbyEntities(this.strike.getCenter(), range + 1, range + 1, range + 1))
            if (entity instanceof LivingEntity && isFlingable(entity) && entity.getLocation().distanceSquared(this.strike.getCenter()) <= range * range)
                this.targets.add((LivingEntity) entity);
    }

    void tick() {
        if (this.age < 0)
            return;

        final double front = this.radius + this.age * SPEED;

        this.age++;
        this.sweepPops();

        if (front > this.reach + 2) {
            this.throwClimbers();

            return;
        }

        this.raiseColumns(front);
        this.fling(front);
        this.throwClimbers();
        this.showFront(front);
    }

    boolean isDone() {
        return this.age >= 0 && this.radius + this.age * SPEED > this.reach + 2 && this.pops.isEmpty() && this.climbing.isEmpty();
    }

    void remove() {
        for (final MovingBlock pop : this.pops)
            pop.remove();

        this.pops.clear();
    }

    private void raiseColumns(final double front) {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final double frontSquared      = front * front;
        final double lift              = 0.2 + this.strike.getPower() * 0.03;
        final double doubleRadius      = this.radius * 2;

        while (this.nextColumn < this.columns.size() && this.columns.get(this.nextColumn)[2] <= frontSquared) {
            final int[] column    = this.columns.get(this.nextColumn++);
            final double distance = Math.sqrt(column[2]);
            final boolean dense   = distance <= doubleRadius || (column[0] + column[1] & 1) == 0;
            final int x           = this.centerX + column[0];
            final int z           = this.centerZ + column[1];
            final Integer surface = this.findSurface(x, z);

            if (surface == null || !dense)
                continue;

            final Block ground   = this.world.getBlockAt(x, surface, z);
            final BlockLook look = BlockLook.findFlyingLook(ground.getState());
            final double falloff = 1 - (distance - this.radius) / (this.reach - this.radius) * 0.65;

            if (look == null)
                continue;

            final MovingBlock pop = MovingBlock.spawn(new Location(this.world, x + 0.5, surface + 0.5, z + 0.5), look);

            if (pop == null)
                continue;

            pop.launch(new Vector(0, lift * falloff * (0.9 + random.nextDouble() * 0.2), 0));

            this.pops.add(pop);

            if (random.nextInt(3) == 0)
                CompParticle.block(new Location(this.world, x + 0.5, surface + 1.05, z + 0.5), look, 3, 0.3, 0.05);
        }
    }

    private void fling(final double front) {
        final Location center = this.strike.getCenter();
        final double range    = this.radius * 2;
        final double strength = 0.75 + this.strike.getPower() * 0.1;

        for (final Iterator<LivingEntity> iterator = this.targets.iterator(); iterator.hasNext();) {
            final LivingEntity entity = iterator.next();

            if (!entity.isValid() || !entity.getWorld().equals(this.world)) {
                iterator.remove();

                continue;
            }

            final Location location = entity.getLocation();
            final Vector away       = new Vector(location.getX() - center.getX(), 0, location.getZ() - center.getZ());
            final double distance   = away.length();

            if (distance > front)
                continue;

            if (distance < 0.1)
                away.setX(ThreadLocalRandom.current().nextDouble(-1, 1)).setZ(ThreadLocalRandom.current().nextDouble(-1, 1));

            final double closeness = 1 - Math.min(1, distance / range);
            final Vector outward   = away.normalize().multiply((0.8 + 1.2 * closeness) * strength);
            final double depth     = this.centerY + 1 - location.getY();

            if (entity.getVehicle() != null)
                entity.leaveVehicle();

            if (distance < this.radius + 0.5 && depth > 0) {
                entity.setVelocity(new Vector(0, Math.min(2, Math.sqrt(0.2 * (depth + 2.5))), 0));

                this.climbing.add(new Throw(entity, outward.setY(0.4), this.age + CLIMB_TICKS));

            } else
                entity.setVelocity(outward.setY((0.7 + 0.6 * closeness) * strength));

            Protection.add(entity);

            CompParticle.POOF.spawn(location, 6, 0.4, 0.05);
            CompParticle.CLOUD.spawn(location, 4, 0.3, 0.08);

            if (entity instanceof Player)
                CompSound.WINGS.play((Player) entity, location, 1F, 1.4F);

            iterator.remove();
        }
    }

    private void throwClimbers() {
        for (final Iterator<Throw> iterator = this.climbing.iterator(); iterator.hasNext();) {
            final Throw pending = iterator.next();

            if (pending.dueAge > this.age)
                continue;

            if (pending.entity.isValid())
                pending.entity.setVelocity(pending.velocity);

            iterator.remove();
        }
    }

    private void showFront(final double front) {
        final Element element = this.strike.getElement();
        final int points      = Math.max(16, (int) (front * Math.PI * 2 / 2.2));
        final double offset   = ThreadLocalRandom.current().nextDouble(Math.PI * 2);

        for (int index = 0; index < points; index++) {
            final double angle = offset + Math.PI * 2 * index / points;
            final int x        = (int) Math.floor(this.centerX + 0.5 + Math.cos(angle) * front);
            final int z        = (int) Math.floor(this.centerZ + 0.5 + Math.sin(angle) * front);
            final Integer y    = this.findSurface(x, z);

            if (y == null)
                continue;

            final Location location = new Location(this.world, this.centerX + 0.5 + Math.cos(angle) * front, y + 1.15, this.centerZ + 0.5 + Math.sin(angle) * front);

            CompParticle.dust(location, index % 3 == 0 ? element.getSecondary() : element.getPrimary(), 2F, 1, 0.15);

            if (index % 4 == 0)
                CompParticle.GUST.shoot(location, Math.cos(angle), 0.1, Math.sin(angle), 0.15);
        }

        if (this.age % 3 == 1)
            for (final Player player : this.strike.getViewers()) {
                final Location position = player.getLocation();
                final Vector toward     = new Vector(position.getX() - this.centerX - 0.5, 0, position.getZ() - this.centerZ - 0.5);

                if (toward.lengthSquared() < 0.01)
                    continue;

                final Location rumble = new Location(this.world, this.centerX + 0.5, this.centerY + 1, this.centerZ + 0.5).add(toward.normalize().multiply(front));

                CompSound.EXPLODE.play(player, rumble, 1.4F, 0.5F);
                CompSound.ANVIL.play(player, rumble, 0.5F, 0.5F);
            }
    }

    private void sweepPops() {
        for (final Iterator<MovingBlock> iterator = this.pops.iterator(); iterator.hasNext();) {
            final MovingBlock pop = iterator.next();

            if (!pop.isAlive() || this.age > POP_LIFETIME + (this.reach - this.radius) / SPEED) {
                pop.remove();

                iterator.remove();
            }
        }
    }

    private Integer findSurface(final int x, final int z) {
        final long key = toKey(x, z);

        if (this.surfaces.containsKey(key))
            return this.surfaces.get(key);

        Integer surface = null;

        if (this.isChunkLoaded(x >> 4, z >> 4))
            for (int y = this.centerY + SCAN_UP; y >= this.centerY - SCAN_DOWN; y--) {
                final Material type  = this.world.getBlockAt(x, y, z).getType();
                final Material above = this.world.getBlockAt(x, y + 1, z).getType();

                if (type.isSolid() && !above.isSolid() && !BlockLook.isLiquid(above)) {
                    surface = y;

                    break;
                }
            }

        this.surfaces.put(key, surface);

        return surface;
    }

    private boolean isChunkLoaded(final int chunkX, final int chunkZ) {
        final long key = toKey(chunkX, chunkZ);
        Boolean loaded = this.loadedChunks.get(key);

        if (loaded == null) {
            loaded = this.world.isChunkLoaded(chunkX, chunkZ);

            this.loadedChunks.put(key, loaded);
        }

        return loaded;
    }

    private static final class Throw {

        private final LivingEntity entity;
        private final Vector velocity;
        private final int dueAge;

        private Throw(final LivingEntity entity, final Vector velocity, final int dueAge) {
            this.entity   = entity;
            this.velocity = velocity;
            this.dueAge   = dueAge;
        }
    }

    static boolean isFlingable(final Entity entity) {
        if (entity instanceof ArmorStand || entity.hasMetadata("NPC"))
            return false;

        return !(entity instanceof Player) || ((Player) entity).getGameMode() != GameMode.SPECTATOR;
    }

    private static long toKey(final int x, final int z) {
        return (long) x << 32 | z & 0xFFFFFFFFL;
    }
}
