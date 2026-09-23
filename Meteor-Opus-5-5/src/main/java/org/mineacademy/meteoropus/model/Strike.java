package org.mineacademy.meteoropus.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.mineacademy.meteoropus.listener.StrikeListener;
import org.mineacademy.meteoropus.remain.BlockLook;
import org.mineacademy.meteoropus.remain.CompParticle;
import org.mineacademy.meteoropus.remain.CompSound;
import org.mineacademy.meteoropus.remain.Remain;

public final class Strike {

    private static final long BLOCK_BUDGET_NANOS = 12_000_000L;
    private static final int COUNTDOWN           = 60;
    private static final int IMPACT              = COUNTDOWN + Meteor.FLIGHT_TICKS;
    private static final int RELEASE             = IMPACT + 20;
    private static final int FINALE              = 160;
    private static final int REBUILD_END         = 350;
    private static final int FINISH              = 360;
    private static final String[] SUBTITLES      = { "§7Target locked", "§7Meteor inbound", "§7Brace for impact" };
    private static final float[] COUNT_PITCHES   = { 0.7F, 0.9F, 1.2F };

    private static Strike active;

    private final Plugin plugin;
    private final Player owner;
    private final Block target;
    private final World world;
    private final Location center;
    private final Location surface;
    private final Element element;
    private final int power;
    private final int radius;
    private final Vector approach;
    private final Audience audience;
    private final DebrisField debris;
    private final Shockwave shockwave;
    private final StrikeListener listener;
    private final Set<Long> heldChunks       = new HashSet<>();
    private final List<Chunk> ticketChunks   = new ArrayList<>();
    private final List<long[]> pendingChunks = new ArrayList<>();
    private final List<Location> ring        = new ArrayList<>();
    private final List<Player> viewers       = new ArrayList<>();
    private final List<Entity> nearby        = new ArrayList<>();
    private Meteor meteor;
    private Crater crater;
    private Finale finale;
    private Rebuild rebuild;
    private BukkitTask task;
    private int tick;
    private long deadline;
    private boolean suppressingDrops;
    private int viewersTick  = -1;
    private int nearbyTick   = -1;
    private int finaleStart  = -1;
    private int rebuildStart = -1;
    private int rebuildEnd   = -1;
    private boolean rebuilt;

    private Strike(final Plugin plugin, final Player owner, final Block target, final Element element, final int power) {
        this.plugin    = plugin;
        this.owner     = owner;
        this.target    = target;
        this.world     = target.getWorld();
        this.center    = target.getLocation().add(0.5, 0.5, 0.5);
        this.surface   = target.getLocation().add(0.5, 1, 0.5);
        this.element   = element;
        this.power     = power;
        this.radius    = 2 + power;
        this.approach  = findApproach(owner, this.center);
        this.audience  = new Audience(this.center);
        this.debris    = new DebrisField(this);
        this.shockwave = new Shockwave(this);
        this.listener  = new StrikeListener(this);
    }

    private void begin() {
        Bukkit.getPluginManager().registerEvents(this.listener, this.plugin);

        this.holdChunks();
        this.audience.refresh();
        this.countdown();

        this.task = Bukkit.getScheduler().runTaskTimer(this.plugin, this::run, 1, 1);
    }

    private void run() {
        try {
            this.tick++;
            this.advance();

        } catch (final Throwable throwable) {
            this.abort();

            throw new IllegalStateException("The meteor strike crashed at tick " + this.tick + " and the world was restored", throwable);
        }
    }

    private void advance() {
        this.deadline = System.nanoTime() + BLOCK_BUDGET_NANOS;

        if (this.tick % 10 == 0)
            this.audience.refresh();

        this.audience.darken(this.getDarkness());

        if (this.tick < COUNTDOWN)
            this.countdown();

        if (this.tick == COUNTDOWN)
            this.launch();

        if (this.tick >= COUNTDOWN && this.tick < IMPACT)
            this.fly(this.tick - COUNTDOWN);

        if (this.tick == IMPACT)
            this.impact();

        if (this.crater != null && !this.crater.isCarved())
            this.carve();

        if (this.tick > IMPACT && this.tick < RELEASE)
            this.freezeTime();

        if (this.tick == RELEASE)
            this.release();

        this.debris.tick(this.tick);
        this.shockwave.tick();

        this.advanceFinale();
        this.advanceRebuild();

        if (this.rebuilt && this.tick >= Math.max(FINISH, this.rebuildEnd + 10))
            this.finish();
    }

    private void countdown() {
        if (this.tick % 20 == 0) {
            final int second = this.tick / 20;

            for (final Player player : this.getViewers())
                Remain.sendTitle(player, this.element.getColor() + "§l" + (3 - second), SUBTITLES[second], 0, 22, 6);

            CompSound.BASS_DRUM.playNear(this.getViewers(), 1F, 0.5F);
            CompSound.PLING.playNear(this.getViewers(), 1F, COUNT_PITCHES[second]);

            if (second == 0)
                CompSound.CURSE.playNear(this.getViewers(), 0.7F, 1F);

            if (second == 2)
                CompSound.WITHER_AMBIENT.playNear(this.getViewers(), 0.6F, 0.5F);
        }

        this.showReticle();
        this.loadPendingChunk();
    }

    private void launch() {
        this.meteor = new Meteor(this, this.approach);

        this.buildRing();

        for (final Player player : this.getViewers())
            Remain.sendTitle(player, "", this.element.getColor() + "§lLOOK UP", 2, 30, 10);

        CompSound.WITHER_SHOOT.playNear(this.getViewers(), 1F, 0.5F);
        CompSound.ROCKET.playNear(this.getViewers(), 1F, 0.6F);
        this.element.getSelectSound().playNear(this.getViewers(), 1F, 0.5F);
    }

    private void fly(final int age) {
        this.meteor.tick(age);
        this.showRing();

        if (age % 10 == 0)
            CompSound.PLING.play(this.getViewers(), this.surface, 2F, age % 20 == 0 ? 1.8F : 1.4F);
    }

    private void impact() {
        this.meteor.remove();
        this.meteor = null;
        this.crater = new Crater(this.target, this.radius);

        final ThreadLocalRandom random = ThreadLocalRandom.current();

        CompParticle.EXPLOSION_EMITTER.spawn(this.surface, 2 + this.power / 2, this.radius * 0.35, 0);
        CompParticle.FLASH.spawn(this.surface.clone().add(0, 1, 0), 3, 0.5, 0);
        CompParticle.dust(this.surface, this.element.getPrimary(), 3F, 40, this.radius * 0.8);
        CompParticle.dust(this.surface, this.element.getSecondary(), 2.5F, 25, this.radius * 0.6);
        CompParticle.LARGE_SMOKE.spawn(this.surface, 40, this.radius * 0.5, 0.15);

        for (int index = 0; index < 70; index++) {
            final Vector direction = randomUnit();

            direction.setY(Math.abs(direction.getY()));

            CompParticle.FIREWORK.shoot(this.surface, direction.getX(), direction.getY(), direction.getZ(), 0.6 + random.nextDouble() * 0.9);

            if (index % 2 == 0)
                this.element.getGlow().shoot(this.surface, direction.getX(), direction.getY(), direction.getZ(), 0.3 + random.nextDouble() * 0.5);
        }

        CompSound.EXPLODE.play(this.getViewers(), this.surface, 8F, 0.5F);
        CompSound.LIGHTNING_IMPACT.play(this.getViewers(), this.surface, 8F, 0.6F);
        CompSound.THUNDER.playNear(this.getViewers(), 1F, 0.7F);
        CompSound.LARGE_BLAST.play(this.getViewers(), this.surface, 6F, 0.6F);

        switch (this.element) {
            case FIRE:
                CompSound.BLAZE_DEATH.play(this.getViewers(), this.surface, 5F, 0.5F);
                break;

            case ICE:
                CompSound.GLASS_BREAK.play(this.getViewers(), this.surface, 6F, 0.5F);
                CompSound.FREEZE.play(this.getViewers(), this.surface, 4F, 0.6F);
                break;

            case VOID:
                CompSound.ENDERMAN_SCREAM.play(this.getViewers(), this.surface, 5F, 0.5F);
                CompSound.TELEPORT.play(this.getViewers(), this.surface, 5F, 0.5F);
                break;

            default:
                throw new IllegalStateException("Report / Unhandled element " + this.element);
        }
    }

    private void carve() {
        this.suppressingDrops = true;

        try {
            int vanished = 0;

            while (!this.crater.isCarved() && System.nanoTime() < this.deadline) {
                final SavedBlock block = this.crater.carveNext();

                if (!block.isSolid())
                    continue;

                if (!this.debris.add(block, this.tick) && vanished++ % 3 == 0)
                    this.showVanished(block);

                block.clear();
            }

        } finally {
            this.suppressingDrops = false;
        }

        if (this.crater.isCarved())
            this.crater.markShell();
    }

    private void showVanished(final SavedBlock block) {
        final BlockLook look = BlockLook.findFlyingLook(block.getState());

        if (look != null)
            CompParticle.block(block.getCenter(), look, 8, 0.4, 0.15);

        else
            CompParticle.CLOUD.spawn(block.getCenter(), 3, 0.4, 0.05);
    }

    private void freezeTime() {
        final int age = this.tick - IMPACT;

        if (age == 3) {
            CompSound.BEACON_DEACTIVATE.playNear(this.getViewers(), 1F, 0.5F);
            CompSound.BELL.playNear(this.getViewers(), 0.5F, 2F);
        }

        if (age % 6 == 4)
            CompSound.HEARTBEAT.playNear(this.getViewers(), 1F, 0.7F);

        if (age % 2 == 0)
            this.debris.showFrozenGlow();

        if (age % 3 == 0)
            for (final Location point : this.buildCircle(this.radius + 1.5, this.surface.getY() + 0.4, 24))
                CompParticle.dust(point, this.element.getShadow(), 2F, 1, 0.2);
    }

    private void release() {
        final ThreadLocalRandom random = ThreadLocalRandom.current();

        this.debris.release(this.tick);
        this.shockwave.start();

        CompSound.SONIC_BOOM.play(this.getViewers(), this.surface, 6F, 0.7F);
        CompSound.EXPLODE.play(this.getViewers(), this.surface, 6F, 0.8F);
        CompSound.TRIDENT_THUNDER.playNear(this.getViewers(), 1F, 0.8F);
        CompSound.LARGE_BLAST.playNear(this.getViewers(), 0.8F, 0.6F);

        for (final Location point : this.buildCircle(this.radius, this.surface.getY() + 0.6, 10))
            CompParticle.SONIC_BOOM.spawn(point, 1, 0, 0);

        CompParticle.EXPLOSION.spawn(this.surface, 4, this.radius * 0.3, 0);

        for (int index = 0; index < 50; index++) {
            final double angle = random.nextDouble(Math.PI * 2);

            this.element.getGlow().shoot(this.surface, Math.cos(angle), 0.15, Math.sin(angle), 0.6 + random.nextDouble() * 0.6);
        }
    }

    private void advanceFinale() {
        if (this.finale == null) {
            if (this.tick < FINALE || this.crater == null || !this.crater.isCarved())
                return;

            this.finale      = this.createFinale();
            this.finaleStart = this.tick;

            this.finale.start();
        }

        if (this.rebuild != null)
            return;

        final int age = this.tick - this.finaleStart;

        if (age < Finale.DURATION) {
            this.finale.tick(age);

            return;
        }

        this.suppressingDrops = true;

        final boolean cleared;

        try {
            cleared = this.finale.clear(this.deadline);

        } finally {
            this.suppressingDrops = false;
        }

        if (!cleared)
            return;

        this.debris.removeAll();
        this.shockwave.remove();

        this.rebuildStart = this.tick;
        this.rebuildEnd   = Math.max(REBUILD_END, this.tick + 60);
        this.rebuild      = new Rebuild(this, this.rebuildStart, this.rebuildEnd);
    }

    private void advanceRebuild() {
        if (this.rebuild == null || this.rebuilt)
            return;

        this.rebuild.tick(this.tick);

        if (!this.rebuild.isDone())
            return;

        this.rebuilt = true;

        CompSound.COMPLETE.playNear(this.getViewers(), 0.8F, 1F);
        CompSound.BEACON_ACTIVATE.play(this.getViewers(), this.surface, 3F, 1.2F);

        for (final Location point : this.buildCircle(this.radius + 0.5, this.surface.getY() + 0.2, 36)) {
            CompParticle.HAPPY_VILLAGER.spawn(point, 2, 0.2, 0);
            CompParticle.dust(point, this.element.getSecondary(), 1.5F, 1, 0.1);
        }

        CompParticle.FIREWORK.spawn(this.surface, 40, this.radius * 0.4, 0.05);
    }

    private Finale createFinale() {
        switch (this.element) {
            case FIRE:
                return new FireFinale(this);

            case ICE:
                return new IceFinale(this);

            case VOID:
                return new VoidFinale(this);

            default:
                throw new IllegalStateException("Report / Unhandled element " + this.element);
        }
    }

    private void finish() {
        this.stop();

        this.suppressingDrops = true;

        try {
            this.crater.reconcile();

        } finally {
            this.suppressingDrops = false;
        }

        this.cleanUp();
    }

    public void abort() {
        this.stop();

        if (this.finale != null)
            this.finale.abort();

        if (this.rebuild != null)
            this.rebuild.abort();

        if (this.meteor != null)
            this.meteor.remove();

        this.debris.removeAll();
        this.shockwave.remove();

        if (this.crater != null) {
            this.suppressingDrops = true;

            try {
                this.crater.restoreAll(this::rescueEntities);

            } finally {
                this.suppressingDrops = false;
            }
        }

        for (final Player player : this.getViewers())
            if (player.isOnline()) {
                player.resetTitle();

                for (final CompSound sound : CompSound.values())
                    Remain.stopSound(player, sound.getKey());
            }

        this.cleanUp();
    }

    private void stop() {
        if (this.task != null) {
            this.task.cancel();

            this.task = null;
        }
    }

    private void cleanUp() {
        MovingBlock.removeAll();

        this.audience.restore();
        this.releaseChunks();

        HandlerList.unregisterAll(this.listener);

        active = null;
    }

    private void holdChunks() {
        final int reach = (int) Math.ceil(this.shockwave.getReach()) + 20;

        for (int chunkX = this.target.getX() - reach >> 4; chunkX <= this.target.getX() + reach >> 4; chunkX++)
            for (int chunkZ = this.target.getZ() - reach >> 4; chunkZ <= this.target.getZ() + reach >> 4; chunkZ++)
                if (this.world.isChunkLoaded(chunkX, chunkZ))
                    this.holdChunk(chunkX, chunkZ);

        final int margin = this.radius + 2;

        for (int chunkX = this.target.getX() - margin >> 4; chunkX <= this.target.getX() + margin >> 4; chunkX++)
            for (int chunkZ = this.target.getZ() - margin >> 4; chunkZ <= this.target.getZ() + margin >> 4; chunkZ++)
                this.pendingChunks.add(new long[] { chunkX, chunkZ });

        final int meteorX = (int) Math.floor(this.center.getX() + this.approach.getX() * 40);
        final int meteorZ = (int) Math.floor(this.center.getZ() + this.approach.getZ() * 40);

        this.pendingChunks.add(new long[] { meteorX >> 4, meteorZ >> 4 });
    }

    private void loadPendingChunk() {
        while (!this.pendingChunks.isEmpty()) {
            final long[] chunk = this.pendingChunks.remove(0);
            final int chunkX   = (int) chunk[0];
            final int chunkZ   = (int) chunk[1];

            if (this.world.isChunkLoaded(chunkX, chunkZ))
                continue;

            if (this.world.loadChunk(chunkX, chunkZ, false))
                this.holdChunk(chunkX, chunkZ);

            return;
        }
    }

    private void holdChunk(final int chunkX, final int chunkZ) {
        if (!this.heldChunks.add(toChunkKey(chunkX, chunkZ)))
            return;

        if (Remain.hasChunkTickets()) {
            final Chunk chunk = this.world.getChunkAt(chunkX, chunkZ);

            Remain.addChunkTicket(chunk, this.plugin);

            this.ticketChunks.add(chunk);
        }
    }

    private void releaseChunks() {
        for (final Chunk chunk : this.ticketChunks)
            Remain.removeChunkTicket(chunk, this.plugin);

        this.ticketChunks.clear();
        this.heldChunks.clear();
    }

    private void showReticle() {
        final double progress = (this.tick % 20 + 1) / 20D;
        final int points      = Math.max(10, (int) (this.radius * progress * Math.PI * 2 / 0.7));

        for (final Location point : this.buildCircle(this.radius * progress, this.surface.getY() + 0.15, points))
            CompParticle.dust(point, this.element.getPrimary(), 1.4F, 1, 0);

        if (this.tick % 2 == 0)
            for (int level = 0; level < 8; level++)
                CompParticle.dust(this.surface.clone().add(0, level * 0.5, 0), level % 2 == 0 ? this.element.getPrimary() : this.element.getSecondary(), 1.5F, 1, 0.05);

        this.element.getGlow().shoot(this.surface.clone().add(0, 0.2, 0), 0, 1, 0, 0.12);

        if (this.tick % 5 == 0)
            CompParticle.END_ROD.shoot(this.surface, 0, 1, 0, 0.35);
    }

    private void buildRing() {
        final int points = Math.max(24, (int) (this.radius * Math.PI * 2 / 0.55));

        for (int index = 0; index < points; index++) {
            final double angle = Math.PI * 2 * index / points;
            final double x     = this.center.getX() + Math.cos(angle) * this.radius;
            final double z     = this.center.getZ() + Math.sin(angle) * this.radius;

            this.ring.add(new Location(this.world, x, this.findGround((int) Math.floor(x), (int) Math.floor(z)) + 0.15, z));
        }
    }

    private void showRing() {
        final int phase = this.tick / 2;

        for (int index = this.tick % 2; index < this.ring.size(); index += 2) {
            final Location point = this.ring.get(index);

            CompParticle.dust(point, (index + phase) % 6 < 3 ? this.element.getPrimary() : this.element.getSecondary(), 1.8F, 1, 0.02);

            if (index % 8 == this.tick % 8)
                this.element.getGlow().shoot(point, 0, 1, 0, 0.06);
        }
    }

    private double findGround(final int x, final int z) {
        for (int y = this.target.getY() + this.radius; y >= this.target.getY() - this.radius; y--)
            if (this.world.getBlockAt(x, y, z).getType().isSolid() && !this.world.getBlockAt(x, y + 1, z).getType().isSolid())
                return y + 1;

        return this.surface.getY();
    }

    private List<Location> buildCircle(final double circleRadius, final double y, final int points) {
        final List<Location> circle = new ArrayList<>(points);

        for (int index = 0; index < points; index++) {
            final double angle = Math.PI * 2 * index / points;

            circle.add(new Location(this.world, this.center.getX() + Math.cos(angle) * circleRadius, y, this.center.getZ() + Math.sin(angle) * circleRadius));
        }

        return circle;
    }

    private double getDarkness() {
        if (this.tick <= COUNTDOWN)
            return smooth(this.tick / (double) COUNTDOWN);

        if (this.rebuildStart < 0)
            return 1;

        return 1 - smooth((this.tick - this.rebuildStart) / (double) Math.max(1, this.rebuildEnd - this.rebuildStart));
    }

    public void rescueEntities(final SavedBlock block) {
        if (this.nearbyTick != this.tick) {
            this.nearbyTick = this.tick;
            this.nearby.clear();

            final double range = this.radius + 3;

            for (final Entity entity : this.world.getNearbyEntities(this.center, range, range, range))
                if (!(entity instanceof FallingBlock) && !(entity instanceof Hanging) && !(entity instanceof Player && ((Player) entity).getGameMode() == GameMode.SPECTATOR))
                    this.nearby.add(entity);
        }

        for (final Entity entity : this.nearby) {
            final Location location = entity.getLocation();

            if (location.getBlockX() != block.getX() || location.getBlockZ() != block.getZ())
                continue;

            final int feet = location.getBlockY();

            if (feet == block.getY() || feet + 1 == block.getY() && entity instanceof LivingEntity)
                this.lift(entity, location, block.getY());
        }
    }

    private void lift(final Entity entity, final Location location, final int solidY) {
        for (int y = solidY + 1; y < solidY + this.radius * 2 + 4; y++)
            if (!this.world.getBlockAt(location.getBlockX(), y, location.getBlockZ()).getType().isSolid() && !this.world.getBlockAt(location.getBlockX(), y + 1, location.getBlockZ()).getType().isSolid()) {
                location.setY(y + 0.05);

                entity.setFallDistance(0);
                entity.teleport(location);

                return;
            }
    }

    public List<Player> getViewers() {
        if (this.viewersTick != this.tick) {
            this.viewersTick = this.tick;
            this.viewers.clear();
            this.viewers.addAll(this.audience.getPlayers());

            if (this.owner.isOnline() && this.owner.getWorld().equals(this.world) && !this.viewers.contains(this.owner))
                this.viewers.add(this.owner);
        }

        return this.viewers;
    }

    public boolean isInCrater(final Block block) {
        return this.crater != null && block.getWorld().equals(this.world) && this.crater.contains(block.getX(), block.getY(), block.getZ());
    }

    public boolean isNear(final Location location, final double margin) {
        if (!location.getWorld().equals(this.world))
            return false;

        final double reach = this.radius + margin;

        return location.distanceSquared(this.center) <= reach * reach;
    }

    public boolean isNear(final Block block, final double margin) {
        if (!block.getWorld().equals(this.world))
            return false;

        final double reach = this.radius + margin;
        final double x     = block.getX() + 0.5 - this.center.getX();
        final double y     = block.getY() + 0.5 - this.center.getY();
        final double z     = block.getZ() + 0.5 - this.center.getZ();

        return x * x + y * y + z * z <= reach * reach;
    }

    public boolean isChunkHeld(final Chunk chunk) {
        return chunk.getWorld().equals(this.world) && this.heldChunks.contains(toChunkKey(chunk.getX(), chunk.getZ()));
    }

    public boolean isSuppressingDrops() {
        return this.suppressingDrops;
    }

    void setSuppressingDrops(final boolean suppressingDrops) {
        this.suppressingDrops = suppressingDrops;
    }

    public double getDamageRange() {
        return this.radius * 2 + 12;
    }

    long getDeadline() {
        return this.deadline;
    }

    Block getTarget() {
        return this.target;
    }

    Location getCenter() {
        return this.center.clone();
    }

    Location getSurface() {
        return this.surface.clone();
    }

    Element getElement() {
        return this.element;
    }

    int getPower() {
        return this.power;
    }

    int getRadius() {
        return this.radius;
    }

    Crater getCrater() {
        return this.crater;
    }

    DebrisField getDebris() {
        return this.debris;
    }

    public static boolean isRunning() {
        return active != null;
    }

    public static void start(final Plugin plugin, final Player owner, final Block target, final Element element, final int power) {
        if (active != null)
            throw new IllegalStateException("Report / A meteor strike is already running");

        final Strike strike = new Strike(plugin, owner, target, element, power);

        active = strike;

        try {
            strike.begin();

        } catch (final RuntimeException ex) {
            strike.abort();

            throw ex;
        }
    }

    public static void abortActive() {
        if (active != null)
            active.abort();
    }

    static Vector randomUnit() {
        final ThreadLocalRandom random = ThreadLocalRandom.current();

        return new Vector(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize();
    }

    private static Vector findApproach(final Player owner, final Location center) {
        final Location eye   = owner.getLocation();
        final Vector outward = new Vector(center.getX() - eye.getX(), 0, center.getZ() - eye.getZ());

        if (outward.lengthSquared() < 0.25) {
            final Vector facing = eye.getDirection().setY(0);

            return facing.lengthSquared() < 0.0001 ? new Vector(1, 0, 0) : facing.normalize();
        }

        return outward.normalize();
    }

    private static double smooth(final double progress) {
        final double clamped = Math.max(0, Math.min(1, progress));

        return clamped * clamped * (3 - 2 * clamped);
    }

    private static long toChunkKey(final int chunkX, final int chunkZ) {
        return (long) chunkX << 32 | chunkZ & 0xFFFFFFFFL;
    }
}
