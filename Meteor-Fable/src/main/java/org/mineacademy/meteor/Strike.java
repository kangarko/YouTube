package org.mineacademy.meteor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public final class Strike {

    private static final int COUNTDOWN_TICKS = 60;
    private static final int FLIGHT_TICKS = 40;
    private static final int IMPACT_TICK = COUNTDOWN_TICKS + FLIGHT_TICKS;
    private static final int REMOVAL_TICKS = 5;
    private static final int FREEZE_TICK = IMPACT_TICK + REMOVAL_TICKS;
    private static final int RESUME_TICK = FREEZE_TICK + 20;
    private static final int FINALE_TICK = 160;
    private static final int REBUILD_TICK = 260;
    private static final int END_TICK = 360;
    private static final int NIGHT_RANGE = 128;
    private static final long NIGHT_TIME = 18000L;
    private static final long DAY_LENGTH = 24000L;
    private static final double METEOR_HEIGHT = 60;
    private static final double METEOR_DISTANCE = 40;
    private static final long IMMUNITY_MILLIS = 15000L;
    private static final int WAVE_LIFETIME = 14;
    private static final double WAVE_STEP = 1.25;
    private static final double GRAVITY = 0.04;
    private static final double DRAG = 0.98;

    private final MeteorPlugin plugin;
    private final World world;
    private final Location center;
    private final UUID casterId;
    private final Element element;
    private final int power;
    private final int radius;
    private final double waveRadius;
    private final Crater crater;
    private final Location meteorStart;
    private final Vector meteorStep;
    private final Vector meteorDirection;
    private final Random random = new Random();
    private final Set<UUID> ownedEntities = new HashSet<>();
    private final List<FallingBlock> meteor = new ArrayList<>();
    private final List<Vector> meteorOffsets = new ArrayList<>();
    private final List<Debris> debris = new ArrayList<>();
    private final List<WaveBlock> wave = new ArrayList<>();
    private final List<LivingEntity> flung = new ArrayList<>();
    private final Set<UUID> nightPlayers = new HashSet<>();
    private final List<Location> ringPoints = new ArrayList<>();
    private final List<Placement> placements = new ArrayList<>();
    private final List<Location> finalePoints = new ArrayList<>();
    private final List<Block> holeBlocks = new ArrayList<>();
    private final List<BlockRecord> rebuildQueue = new ArrayList<>();
    private Location holeCenter;
    private long startTime;
    private int tick;
    private int placementIndex;
    private int rebuildIndex;
    private int debrisIndex;
    private int scanY;
    private double voidAngle;
    private BukkitTask task;
    private boolean finished;

    Strike(MeteorPlugin plugin, Player caster, Block target, Element element, int power) {
        this.plugin = plugin;
        this.world = target.getWorld();
        this.center = target.getLocation().add(0.5, 0.5, 0.5);
        this.casterId = caster.getUniqueId();
        this.element = element;
        this.power = power;
        this.radius = 2 + power;
        this.waveRadius = this.radius * 3 + 4;
        this.crater = new Crater(target, this.radius);

        Vector horizontal = this.center.toVector().subtract(caster.getLocation().toVector()).setY(0);

        if (horizontal.lengthSquared() < 0.01)
            horizontal = caster.getLocation().getDirection().setY(0);

        if (horizontal.lengthSquared() < 0.01)
            horizontal = new Vector(1, 0, 0);

        horizontal.normalize();

        this.meteorStart = this.center.clone().add(horizontal.clone().multiply(METEOR_DISTANCE)).add(0, METEOR_HEIGHT, 0);

        Vector path = this.center.toVector().subtract(this.meteorStart.toVector());

        this.meteorStep = path.clone().multiply(1.0 / FLIGHT_TICKS);
        this.meteorDirection = path.clone().normalize();
    }

    void start() {
        this.startTime = this.world.getTime();

        this.collectNightPlayers();
        this.computeRingPoints();
        this.holdPathChunks();

        this.task = Bukkit.getScheduler().runTaskTimer(this.plugin, this::tick, 0L, 1L);
    }

    void abort() {
        if (this.finished)
            return;

        this.removeEntities();
        Compat.releaseChunks(this.world, this.plugin);
        this.crater.restoreAll();
        this.liftEntities();
        this.extinguish();
        this.resetSky(true);
        this.stop();
    }

    boolean isOwnedEntity(UUID entityId) {
        return this.ownedEntities.contains(entityId);
    }

    void handleDebrisLanding(FallingBlock falling) {
        Location location = falling.getLocation().add(0, 0.5, 0);

        MeteorParticle.BLOCK.spawnBlock(location, falling.getMaterial(), 8, 0.3, 0.06);
        this.element.playRebuildPoint(location);

        if (this.random.nextInt(6) == 0)
            MeteorSound.STONE_BREAK.play(location, 0.8F, 0.7F + this.random.nextFloat() * 0.5F);
    }

    boolean isProtectedEntity(Entity entity) {
        return this.isNear(entity.getLocation(), this.waveRadius + 4);
    }

    boolean isNearCrater(Location location) {
        return this.isNear(location, this.radius + 6);
    }

    boolean isAtCrater(Location location) {
        return this.isNear(location, this.radius + 2);
    }

    private boolean isNear(Location location, double reach) {
        if (!this.world.equals(location.getWorld()))
            return false;

        return location.distanceSquared(this.center) <= reach * reach;
    }

    private void tick() {
        if (this.finished)
            return;

        try {
            this.tickPhase();

        } catch (RuntimeException ex) {
            this.abort();

            throw ex;
        }
    }

    private void tickPhase() {
        if (this.tick % 2 == 0)
            this.extinguish();

        if (this.tick < COUNTDOWN_TICKS)
            this.tickCountdown();
        else if (this.tick < IMPACT_TICK)
            this.tickFlight();
        else if (this.tick < FINALE_TICK)
            this.tickAftermath();
        else if (this.tick < REBUILD_TICK)
            this.tickFinale();
        else if (this.tick < END_TICK)
            this.tickRebuild();
        else {
            this.finish();

            return;
        }

        this.tick++;
    }

    private void tickCountdown() {
        if (this.tick % 20 == 0) {
            int second = 3 - this.tick / 20;

            this.collectNightPlayers();
            this.sendTitle(this.element.getColor().toString() + ChatColor.BOLD + second, ChatColor.GRAY + "Brace for impact", 0, 20, 5);
            this.playForViewers(MeteorSound.PLING, 1F, 0.6F + (3 - second) * 0.3F);
            MeteorSound.BASS.play(this.center, 6F, 0.5F);

            if (this.tick == 0) {
                MeteorSound.THUNDER.play(this.center, 8F, 0.5F);
                MeteorSound.WITHER_SPAWN.play(this.center, 4F, 0.5F);
                this.tellCaster(this.element.getColor() + this.element.getDisplayName() + ChatColor.GRAY + " meteor called down at power " + ChatColor.WHITE + this.power + ChatColor.GRAY + " onto " + ChatColor.WHITE + this.crater.getCenterX() + ", " + this.crater.getCenterY() + ", " + this.crater.getCenterZ() + ChatColor.GRAY + ". Impact in five seconds.");
            }
        }

        this.updateSky(lerp(this.startTime, NIGHT_TIME, (this.tick + 1) / (double) COUNTDOWN_TICKS));
        this.drawMarker();

        if (this.tick % 2 == 0)
            this.drawRing(2);
    }

    private void tickFlight() {
        int step = this.tick - COUNTDOWN_TICKS;

        if (step == 0) {
            this.spawnMeteor();
            this.sendTitle(this.element.getTitle(), ChatColor.GRAY + "Meteor sighted", 0, 40, 10);
            MeteorSound.DRAGON_GROWL.play(this.center, 5F, 0.5F);
            MeteorSound.FIREWORK_LAUNCH.play(this.meteorStart, 10F, 0.5F);
        }

        if (this.tick == IMPACT_TICK - 2)
            this.crater.capture();

        Location position = this.meteorStart.clone().add(this.meteorStep.clone().multiply(step + 1));

        this.moveMeteor(position);
        this.element.playTrail(position, this.meteorDirection, this.random);

        if (step % 5 == 0)
            this.element.playTrailSound(position, step / 5);

        if (this.tick % 20 == 0) {
            this.collectNightPlayers();
            this.updateSky(NIGHT_TIME);
        }

        this.drawRing(1);
    }

    private void tickAftermath() {
        if (this.tick == IMPACT_TICK)
            this.impact();

        if (this.tick < FREEZE_TICK)
            this.removeShell(this.tick - IMPACT_TICK);

        if (this.tick == FREEZE_TICK)
            this.freezeDebris();

        if (this.tick > FREEZE_TICK && this.tick < RESUME_TICK)
            this.holdDebris();

        if (this.tick == RESUME_TICK)
            this.releaseDebris();

        if (this.tick >= RESUME_TICK)
            this.tickWave(this.tick - RESUME_TICK);

        this.expireWaveBlocks();

        if (this.tick % 20 == 0) {
            this.collectNightPlayers();
            this.updateSky(NIGHT_TIME);
        }
    }

    private void tickFinale() {
        int step = this.tick - FINALE_TICK;

        if (step == 0) {
            this.planFinale();
            this.announceFinale();
        }

        this.executePlacements();

        if (this.element == Element.FIRE)
            this.tickFire(step);
        else if (this.element == Element.ICE)
            this.tickIce(step);
        else
            this.tickVoid(step);

        if (this.element != Element.VOID)
            this.consumeDebris(step);

        if (step == REBUILD_TICK - FINALE_TICK - 1)
            this.consumeAllDebris();

        this.expireWaveBlocks();
        this.liftEntities();

        if (this.tick % 20 == 0) {
            this.collectNightPlayers();
            this.updateSky(NIGHT_TIME);
        }
    }

    private void tickRebuild() {
        int step = this.tick - REBUILD_TICK;
        int duration = END_TICK - REBUILD_TICK;

        if (step == 0) {
            this.planRebuild();
            this.sendTitle("", ChatColor.GREEN + "The land heals", 10, 40, 10);
            MeteorSound.BEACON_ACTIVATE.play(this.center, 4F, 1.2F);
            MeteorSound.ORB.play(this.center, 3F, 0.5F);
        }

        int target = (int) ((long) this.rebuildQueue.size() * (step + 1) / duration);
        Location last = null;

        while (this.rebuildIndex < target) {
            BlockRecord record = this.rebuildQueue.get(this.rebuildIndex++);

            record.restore();
            last = new Location(this.world, record.getX() + 0.5, record.getY() + 0.5, record.getZ() + 0.5);

            if (this.rebuildIndex % 2 == 0)
                this.element.playRebuildPoint(last);
        }

        if (last != null) {
            MeteorSound.STONE_PLACE.play(last, 1.2F, 0.6F + 0.8F * step / duration);
            this.scanY = last.getBlockY();
        }

        if (step % 10 == 0)
            MeteorSound.CHIME.play(this.center, 1.5F, 0.5F + (float) step / duration);

        this.drawScanline();
        this.liftEntities();

        if (step >= 60)
            this.updateSky(lerp(NIGHT_TIME, this.getDawnTime(), (step - 59) / 41.0));
    }

    private void finish() {
        this.crater.restoreAll();
        this.removeEntities();
        Compat.releaseChunks(this.world, this.plugin);
        this.liftEntities();
        this.extinguish();
        this.sendTitle("", ChatColor.GREEN + "The land is whole again", 10, 40, 10);
        this.resetSky(false);
        MeteorSound.LEVEL_UP.play(this.center, 3F, 1.2F);
        MeteorSound.BEACON_POWER.play(this.center, 3F, 1F);
        MeteorParticle.TOTEM.spawn(this.center.clone().add(0, 1, 0), 60, 2, 0.3);
        MeteorParticle.ENCHANT.spawn(this.center.clone().add(0, 1, 0), 40, 3, 1);
        this.tellCaster("The land is whole again.");
        this.stop();
    }

    private void stop() {
        this.finished = true;

        this.nightPlayers.clear();
        this.flung.clear();

        if (this.task != null)
            this.task.cancel();

        this.plugin.clearStrike();
    }

    private void drawMarker() {
        double height = this.tick % 30 / 30.0 * 6;

        for (int arm = 0; arm < 3; arm++) {
            double angle = this.tick * 0.35 + arm * Math.PI * 2 / 3;
            Location point = this.center.clone().add(Math.cos(angle) * 1.4, height + 0.5, Math.sin(angle) * 1.4);

            MeteorParticle.DUST.spawnDust(point, arm == 0 ? this.element.getDustAccent() : this.element.getDustColor(), 1.5F, 1, 0);
        }

        if (this.tick % 4 == 0)
            this.element.playRingPoint(this.center.clone().add(0, 6.5, 0), 0, 0);
    }

    private void drawRing(int stride) {
        for (int index = 0; index < this.ringPoints.size(); index += stride)
            this.element.playRingPoint(this.ringPoints.get(index), index, this.tick);
    }

    private void drawScanline() {
        double ringRadius = this.radius + 1.5;

        for (int index = 0; index < 20; index++) {
            double angle = index * Math.PI / 10 + this.tick * 0.1;
            Location point = new Location(this.world, this.center.getX() + Math.cos(angle) * ringRadius, this.scanY + 1.0, this.center.getZ() + Math.sin(angle) * ringRadius);

            MeteorParticle.DUST.spawnDust(point, this.element.getDustAccent(), 1.2F, 1, 0);
        }
    }

    private void computeRingPoints() {
        int count = 36 + this.radius * 6;
        double ringRadius = this.radius + 0.5;

        for (int index = 0; index < count; index++) {
            double angle = Math.PI * 2 * index / count;
            double x = this.center.getX() + Math.cos(angle) * ringRadius;
            double z = this.center.getZ() + Math.sin(angle) * ringRadius;
            int surfaceY = this.findSurfaceY((int) Math.floor(x), (int) Math.floor(z));

            if (surfaceY == Integer.MIN_VALUE)
                surfaceY = this.crater.getCenterY();

            this.ringPoints.add(new Location(this.world, x, surfaceY + 1.15, z));
        }
    }

    private int findSurfaceY(int x, int z) {
        int centerY = this.crater.getCenterY();

        for (int y = centerY + 6; y >= centerY - 6; y--)
            if (this.world.getBlockAt(x, y, z).getType().isSolid())
                return y;

        return Integer.MIN_VALUE;
    }

    private void holdPathChunks() {
        Set<Long> held = new HashSet<>();
        Vector path = this.center.toVector().subtract(this.meteorStart.toVector());
        int samples = (int) Math.ceil(path.length() / 8) + 1;

        for (int sample = 0; sample <= samples; sample++) {
            Vector point = this.meteorStart.toVector().add(path.clone().multiply(sample / (double) samples));
            int chunkX = point.getBlockX() >> 4;
            int chunkZ = point.getBlockZ() >> 4;

            if (held.add(((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL)))
                Compat.holdChunk(this.world, chunkX, chunkZ, this.plugin);
        }
    }

    private void spawnMeteor() {
        double bodyRadius = this.power >= 4 ? 2.0 : 1.5;
        int range = (int) Math.ceil(bodyRadius);
        Vector velocity = this.toServerVelocity(this.meteorStep);

        for (int dx = -range; dx <= range; dx++)
            for (int dy = -range; dy <= range; dy++)
                for (int dz = -range; dz <= range; dz++) {
                    if (dx * dx + dy * dy + dz * dz > bodyRadius * bodyRadius)
                        continue;

                    Vector offset = new Vector(dx, dy, dz);
                    FallingBlock falling = Compat.spawnFallingBlock(this.meteorStart.clone().add(offset), this.element.pickBody(this.random).getMaterial());

                    Compat.setGravity(falling, false);
                    falling.setVelocity(velocity);

                    this.meteor.add(falling);
                    this.meteorOffsets.add(offset);
                    this.ownedEntities.add(falling.getUniqueId());
                }
    }

    private Vector toServerVelocity(Vector clientStep) {
        if (Compat.canControlGravity())
            return clientStep.clone().multiply(1 / DRAG);

        return new Vector(clientStep.getX() / DRAG, (clientStep.getY() + GRAVITY) / DRAG + GRAVITY, clientStep.getZ() / DRAG);
    }

    private void moveMeteor(Location position) {
        Vector velocity = this.toServerVelocity(this.meteorStep);

        for (int index = 0; index < this.meteor.size(); index++) {
            FallingBlock falling = this.meteor.get(index);

            if (!falling.isValid())
                continue;

            falling.teleport(position.clone().add(this.meteorOffsets.get(index)));
            falling.setVelocity(velocity);
        }
    }

    private void impact() {
        for (FallingBlock falling : this.meteor)
            if (falling.isValid())
                falling.remove();

        this.meteor.clear();
        Compat.releaseChunks(this.world, this.plugin);
        this.element.playImpact(this.center);
        MeteorParticle.BLOCK.spawnBlock(this.center.clone().add(0, 1, 0), this.crater.getDominant(), 80, 3, 0.4);
        this.sendTitle(this.element.getColor().toString() + ChatColor.BOLD + "IMPACT", "", 0, 20, 10);
    }

    private void removeShell(int shell) {
        List<BlockRecord> inner = this.crater.getInner();
        int from = inner.size() * shell / REMOVAL_TICKS;
        int to = inner.size() * (shell + 1) / REMOVAL_TICKS;

        for (int index = from; index < to; index++) {
            BlockRecord record = inner.get(index);

            if (record.isAir() || this.crater.isFloor(record))
                continue;

            record.clearContents();
            record.getBlock().setType(Material.AIR, false);

            if (!record.isLiquid())
                this.spawnDebris(record);
        }

        MeteorSound.GRAVEL_BREAK.play(this.center, 4F, 0.5F + shell * 0.1F);
    }

    private void spawnDebris(BlockRecord record) {
        Location location = new Location(this.world, record.getX() + 0.5, record.getY(), record.getZ() + 0.5);
        FallingBlock falling;

        if (record.canFly())
            falling = Compat.spawnFallingBlock(location, record.getState());
        else if (record.canSubstitute())
            falling = Compat.spawnFallingBlock(location, this.crater.getDominant());
        else
            return;

        double dx = location.getX() - this.center.getX();
        double dy = location.getY() + 0.5 - this.center.getY();
        double dz = location.getZ() - this.center.getZ();
        Vector out = new Vector(dx, Math.max(dy, 0) * 0.6 + 0.55, dz);

        if (out.lengthSquared() < 0.01)
            out = new Vector(0, 1, 0);

        out.normalize();

        Vector burst = out.clone().multiply(0.28 + this.power * 0.04).add(new Vector(this.jitter(0.06), this.jitter(0.05), this.jitter(0.06)));
        Vector flight = out.clone().multiply(0.5 + this.power * 0.1 + this.random.nextDouble() * 0.3).add(new Vector(this.jitter(0.1), 0.2 + this.random.nextDouble() * 0.3, this.jitter(0.1)));

        falling.setVelocity(burst);

        this.debris.add(new Debris(falling, flight));
        this.ownedEntities.add(falling.getUniqueId());
    }

    private void freezeDebris() {
        Vector hover = this.toServerVelocity(new Vector(0, 0, 0));

        for (Debris piece : this.debris) {
            if (!piece.entity.isValid())
                continue;

            piece.anchor = piece.entity.getLocation();

            Compat.setGravity(piece.entity, false);
            piece.entity.setVelocity(hover);
        }

        this.sendTitle("", ChatColor.WHITE.toString() + ChatColor.ITALIC + "Time stands still", 5, 15, 10);
        MeteorSound.BEACON_DEACTIVATE.play(this.center, 5F, 0.5F);
        MeteorSound.BASS.play(this.center, 5F, 0.4F);
        MeteorParticle.END_ROD.spawn(this.center.clone().add(0, 1, 0), 80, 4, 0.02);
    }

    private void holdDebris() {
        if (!Compat.canControlGravity()) {
            Vector hover = this.toServerVelocity(new Vector(0, 0, 0));

            for (Debris piece : this.debris) {
                if (!piece.entity.isValid())
                    continue;

                piece.entity.teleport(piece.anchor);
                piece.entity.setVelocity(hover);
            }
        }

        if (this.tick % 2 == 0 && !this.debris.isEmpty())
            for (int sample = 0; sample < 6; sample++) {
                Debris piece = this.debris.get(this.random.nextInt(this.debris.size()));

                if (piece.entity.isValid())
                    MeteorParticle.END_ROD.spawn(piece.entity.getLocation().add(0, 0.5, 0), 1, 0.4, 0);
            }

        if (this.tick % 5 == 0)
            MeteorSound.BASS.play(this.center, 4F, 0.5F - (this.tick - FREEZE_TICK) * 0.01F);
    }

    private void releaseDebris() {
        for (Debris piece : this.debris) {
            if (!piece.entity.isValid())
                continue;

            Compat.setGravity(piece.entity, true);
            piece.entity.setVelocity(piece.velocity);
        }

        MeteorSound.BEACON_ACTIVATE.play(this.center, 5F, 0.9F);
        MeteorSound.EXPLODE.play(this.center, 6F, 0.8F);
        MeteorSound.DRAGON_GROWL.play(this.center, 4F, 0.6F);
        MeteorParticle.GUST_EMITTER.spawn(this.center.clone().add(0, 1, 0), 1, 0, 0);
        MeteorParticle.EXPLOSION_EMITTER.spawn(this.center.clone().add(0, 1, 0), 1, 0, 0);
    }

    private void tickWave(int step) {
        if (step % 2 != 0)
            return;

        int ring = step / 2;
        double ringRadius = this.radius + 1 + ring * WAVE_STEP;

        if (ringRadius > this.waveRadius)
            return;

        double amplitude = 1 - 0.55 * (ringRadius - this.radius) / (this.waveRadius - this.radius);
        int count = Math.max(12, (int) (Math.PI * ringRadius));
        double offset = this.random.nextDouble() * Math.PI * 2;
        int sounds = 0;

        for (int index = 0; index < count; index++) {
            double angle = offset + Math.PI * 2 * index / count;
            int x = (int) Math.floor(this.center.getX() + Math.cos(angle) * ringRadius);
            int z = (int) Math.floor(this.center.getZ() + Math.sin(angle) * ringRadius);
            int surfaceY = this.findSurfaceY(x, z);

            if (surfaceY == Integer.MIN_VALUE)
                continue;

            Block block = this.world.getBlockAt(x, surfaceY, z);
            Location top = new Location(this.world, x + 0.5, surfaceY + 1, z + 0.5);

            MeteorParticle.BLOCK.spawnBlock(top.clone().add(0, 0.2, 0), block, 5, 0.4, 0.08);

            if (index % 2 == 0)
                MeteorParticle.DUST.spawnDust(top, this.element.getDustColor(), 1.5F, 2, 0.3);

            if (index % 6 == 0)
                MeteorParticle.CLOUD.spawn(top, 2, 0.3, 0.02);

            if (sounds < 3 && this.random.nextInt(count / 3 + 1) == 0) {
                MeteorSound.STONE_BREAK.play(top, 2F, 0.5F);
                sounds++;
            }

            BlockRecord probe = new BlockRecord(block);

            if (!probe.canFly())
                continue;

            FallingBlock falling = Compat.spawnFallingBlock(top, probe.getState());

            falling.setVelocity(new Vector(Math.cos(angle) * 0.06, 0.42 * amplitude, Math.sin(angle) * 0.06));

            this.wave.add(new WaveBlock(falling, this.tick));
            this.ownedEntities.add(falling.getUniqueId());
        }

        this.flingWithin(ringRadius);
    }

    private void flingWithin(double reach) {
        double limit = this.radius * 2.0;

        for (Entity entity : this.world.getNearbyEntities(this.center, limit, limit, limit)) {
            if (!(entity instanceof LivingEntity) || entity instanceof ArmorStand)
                continue;

            if (this.flung.contains(entity))
                continue;

            double distance = entity.getLocation().distance(this.center);

            if (distance > limit || distance > reach)
                continue;

            this.fling((LivingEntity) entity);
        }
    }

    private void fling(LivingEntity entity) {
        Vector away = entity.getLocation().toVector().subtract(this.center.toVector()).setY(0);

        if (away.lengthSquared() < 0.09)
            away = this.randomHorizontal();
        else
            away.normalize();

        entity.setFallDistance(0);
        entity.setVelocity(away.multiply(1.0 + this.power * 0.15).setY(0.9 + this.power * 0.12));

        this.flung.add(entity);
        this.plugin.protect(entity.getUniqueId(), IMMUNITY_MILLIS);

        MeteorSound.BREEZE_WIND.play(entity.getLocation(), 2F, 1.2F);
        MeteorParticle.CLOUD.spawn(entity.getLocation(), 20, 0.5, 0.2);
    }

    private void expireWaveBlocks() {
        Iterator<WaveBlock> iterator = this.wave.iterator();

        while (iterator.hasNext()) {
            WaveBlock block = iterator.next();
            int age = this.tick - block.born;

            if (!block.entity.isValid()) {
                iterator.remove();

                continue;
            }

            if (age <= WAVE_LIFETIME && (age <= 4 || !block.entity.isOnGround()))
                continue;

            block.entity.remove();
            iterator.remove();
        }
    }

    private void planFinale() {
        int bottomY = this.crater.getBottomY();
        int centerY = this.crater.getCenterY();

        for (BlockRecord record : this.crater.getShell()) {
            if (record.isAir() || record.isSpecial() || record.getY() > centerY + 1)
                continue;

            int at = FINALE_TICK + Math.max(0, record.getY() - bottomY + 1) * 30 / (centerY + 2 - bottomY) + this.random.nextInt(5);

            this.placements.add(new Placement(record.getBlock(), this.element.pickCrust(this.random), at, false, false));
        }

        if (this.element == Element.FIRE)
            this.planFire(bottomY, centerY);
        else if (this.element == Element.ICE)
            this.planIce(bottomY, centerY);
        else
            this.planVoid();

        this.placements.sort((first, second) -> Integer.compare(first.tick, second.tick));

        for (Placement placement : this.placements)
            this.finalePoints.add(placement.block.getLocation().add(0.5, 0.5, 0.5));
    }

    private void planFire(int bottomY, int centerY) {
        int poolTop = Math.max(bottomY, centerY - Math.max(1, this.radius / 3));

        for (BlockRecord record : this.crater.getInner()) {
            if (record.getY() > centerY)
                continue;

            if (record.getY() <= poolTop) {
                this.placements.add(new Placement(record.getBlock(), MeteorMaterial.LAVA, FINALE_TICK + 30 + (record.getY() - bottomY) * 8 + this.random.nextInt(4), true, false));

                continue;
            }

            boolean onShell = !this.crater.isInside(record.getX(), record.getY() - 1, record.getZ());

            if (onShell && this.random.nextDouble() < 0.18)
                this.placements.add(new Placement(record.getBlock(), MeteorMaterial.FIRE, FINALE_TICK + 40 + this.random.nextInt(40), true, true));
        }
    }

    private void planIce(int bottomY, int centerY) {
        List<BlockRecord> rim = new ArrayList<>();

        for (BlockRecord record : this.crater.getInner()) {
            if (record.getY() <= centerY) {
                this.placements.add(new Placement(record.getBlock(), this.element.pickFill(this.random), FINALE_TICK + 15 + (record.getY() - bottomY) * 6 + this.random.nextInt(4), true, false));

                continue;
            }

            if (record.getY() == centerY + 1)
                rim.add(record);
        }

        Collections.shuffle(rim, this.random);

        Set<Long> spires = new HashSet<>();
        int wanted = 3 + this.power * 2;
        double reachSquared = (this.radius - 1.5) * (this.radius - 1.5);

        for (BlockRecord record : rim) {
            if (spires.size() >= wanted)
                break;

            int dx = record.getX() - this.crater.getCenterX();
            int dz = record.getZ() - this.crater.getCenterZ();

            if (dx * dx + dz * dz > reachSquared)
                continue;

            spires.add(columnKey(record.getX(), record.getZ()));

            int height = 2 + this.random.nextInt(Math.max(1, this.radius - 1));

            for (int level = 0; level < height; level++) {
                int y = centerY + 1 + level;

                if (!this.crater.isInside(record.getX(), y, record.getZ()))
                    break;

                Block block = this.world.getBlockAt(record.getX(), y, record.getZ());

                this.placements.add(new Placement(block, level % 2 == 0 ? MeteorMaterial.PACKED_ICE : MeteorMaterial.ICE, FINALE_TICK + 55 + level * 4 + this.random.nextInt(3), true, false));
            }
        }

        for (BlockRecord record : rim) {
            if (spires.contains(columnKey(record.getX(), record.getZ())) || this.random.nextDouble() > 0.5)
                continue;

            this.placements.add(new Placement(record.getBlock(), MeteorMaterial.SNOW_LAYER, FINALE_TICK + 90 + this.random.nextInt(5), true, true));
        }
    }

    private void planVoid() {
        this.holeCenter = this.center.clone().add(0, 1, 0);

        double maxRadius = this.getHoleRadius();

        for (BlockRecord record : this.crater.getInner()) {
            double distance = new Location(this.world, record.getX() + 0.5, record.getY() + 0.5, record.getZ() + 0.5).distance(this.holeCenter);

            if (distance > maxRadius)
                continue;

            Block block = record.getBlock();

            this.placements.add(new Placement(block, this.element.pickFill(this.random), FINALE_TICK + 5 + (int) (distance / maxRadius * 30) + this.random.nextInt(2), true, false));
            this.holeBlocks.add(block);
        }
    }

    private double getHoleRadius() {
        return Math.min(this.radius - 1, 2.5);
    }

    private void announceFinale() {
        String text = this.element == Element.FIRE ? "The crater burns" : this.element == Element.ICE ? "The crater freezes" : "The void opens";

        this.sendTitle("", this.element.getColor() + text, 10, 40, 10);
    }

    private void executePlacements() {
        int placed = 0;
        Location last = null;

        while (this.placementIndex < this.placements.size() && this.placements.get(this.placementIndex).tick <= this.tick) {
            Placement placement = this.placements.get(this.placementIndex++);
            Block block = placement.block;

            if (placement.onlyAir && !Compat.isAir(block.getType()))
                continue;

            if (placement.needsSolidBelow && !block.getRelative(0, -1, 0).getType().isSolid())
                continue;

            placement.material.place(block);
            placed++;
            last = block.getLocation().add(0.5, 0.5, 0.5);

            if (placed % 2 == 0)
                this.playPlacementParticles(last);
        }

        if (last != null)
            this.playPlacementSound(last);
    }

    private void playPlacementParticles(Location location) {
        if (this.element == Element.FIRE) {
            MeteorParticle.FLAME.spawn(location, 2, 0.3, 0.02);
            MeteorParticle.LAVA.spawn(location, 1, 0.3, 0);

            return;
        }

        if (this.element == Element.ICE) {
            MeteorParticle.SNOWFLAKE.spawn(location, 3, 0.4, 0.02);
            MeteorParticle.ENCHANT.spawn(location, 2, 0.4, 0.3);

            return;
        }

        MeteorParticle.PORTAL.spawn(location, 4, 0.3, 0.3);
    }

    private void playPlacementSound(Location location) {
        if (this.element == Element.FIRE)
            MeteorSound.LAVA_POP.play(location, 1.5F, 0.6F + this.random.nextFloat() * 0.4F);
        else if (this.element == Element.ICE)
            MeteorSound.GLASS_PLACE.play(location, 1.5F, 0.8F + this.random.nextFloat() * 0.6F);
        else
            MeteorSound.STONE_PLACE.play(location, 1.2F, 0.5F);
    }

    private void tickFire(int step) {
        if (step == 0) {
            MeteorSound.FIRE_IGNITE.play(this.center, 4F, 0.5F);
            MeteorSound.GHAST_SHOOT.play(this.center, 4F, 0.4F);
            MeteorSound.LAVA_AMBIENT.play(this.center, 4F, 0.5F);
        }

        for (int sample = 0; sample < 4; sample++) {
            Location point = this.findRandomFinalePoint();

            if (point == null)
                break;

            point.add(0, 0.6, 0);
            MeteorParticle.LAVA.spawn(point, 1, 0.3, 0);
            MeteorParticle.FLAME.spawn(point, 2, 0.3, 0.03);
            MeteorParticle.SMOKE.spawn(point, 2, 0.3, 0.02);
        }

        Location point = this.findRandomFinalePoint();

        if (point == null)
            return;

        if (step % 3 == 0)
            MeteorParticle.CAMPFIRE_SMOKE.spawn(point.clone().add(0, 1.2, 0), 2, 0.5, 0.02);

        if (step % 15 == 0) {
            MeteorParticle.EXPLOSION.spawn(point, 1, 0, 0);
            MeteorParticle.LAVA.spawn(point, 8, 0.3, 0.3);
            MeteorSound.LAVA_POP.play(point, 3F, 0.5F);
        }

        if (step % 20 == 0) {
            MeteorSound.LAVA_AMBIENT.play(this.center, 3F, 0.5F + this.random.nextFloat() * 0.3F);
            MeteorSound.FIRE_AMBIENT.play(this.center, 3F, 0.8F);
        }
    }

    private void tickIce(int step) {
        if (step == 0) {
            MeteorSound.FREEZE.play(this.center, 4F, 0.7F);
            MeteorSound.GLASS_PLACE.play(this.center, 3F, 0.5F);
            MeteorSound.CHIME.play(this.center, 3F, 0.6F);
        }

        for (int sample = 0; sample < 4; sample++) {
            Location point = this.findRandomFinalePoint();

            if (point == null)
                break;

            point.add(0, 1, 0);
            MeteorParticle.SNOWFLAKE.spawn(point, 3, 0.5, 0.01);
            MeteorParticle.WHITE_ASH.spawn(point, 1, 0.5, 0.01);
        }

        if (step % 4 == 0)
            MeteorParticle.ENCHANT.spawn(this.center.clone().add(0, 1, 0), 12, 2.5, 0.5);

        if (step % 10 == 0) {
            Location point = this.findRandomFinalePoint();

            MeteorSound.CHIME.play(point == null ? this.center : point, 2F, 0.6F + this.random.nextFloat() * 0.8F);
        }

        if (step % 25 == 0)
            MeteorSound.FREEZE.play(this.center, 2F, 0.6F);
    }

    private void tickVoid(int step) {
        if (step == 0) {
            MeteorSound.END_PORTAL_SPAWN.play(this.center, 4F, 0.6F);
            MeteorSound.PORTAL_TRIGGER.play(this.center, 3F, 0.5F);
        }

        double holeRadius = this.getHoleRadius() * Math.min(1, step / 35.0);

        this.voidAngle += 0.22;

        for (int index = 0; index < 20; index++) {
            double angle = this.voidAngle + Math.PI * index / 10;
            double diskRadius = holeRadius + 1.8 + 0.5 * Math.sin(step * 0.1 + index);
            Location point = this.holeCenter.clone().add(Math.cos(angle) * diskRadius, 0.15 * Math.sin(angle * 2), Math.sin(angle) * diskRadius);

            MeteorParticle.PORTAL.spawn(point, 2, 0.1, 0.05);

            if (index % 2 == 0)
                MeteorParticle.DUST.spawnDust(point, this.element.getDustColor(), 1.4F, 1, 0);
        }

        MeteorParticle.SQUID_INK.spawn(this.holeCenter, 3, holeRadius * 0.6, 0.02);

        if (step % 3 == 0) {
            Vector direction = this.randomUnit();

            for (int sample = 0; sample < 8; sample++)
                MeteorParticle.END_ROD.spawn(this.holeCenter.clone().add(direction.clone().multiply(7 - sample * 0.8)), 1, 0, 0);
        }

        if (step % 15 == 0)
            MeteorSound.PORTAL_AMBIENT.play(this.center, 2F, 0.5F);

        if (step % 40 == 0)
            MeteorSound.ENDERMAN_SCREAM.play(this.center, 1.2F, 0.3F);

        if (step >= 3)
            this.pullDebris(step, holeRadius);

        if (step == REBUILD_TICK - FINALE_TICK - 2)
            this.collapseHole();
    }

    private void pullDebris(int step, double holeRadius) {
        double speed = Math.min(1.3, 0.12 + step * 0.014);
        int sounds = 0;

        for (Debris piece : this.debris) {
            FallingBlock entity = piece.entity;

            if (!entity.isValid())
                continue;

            if (step == 3) {
                piece.anchor = entity.getLocation();

                Compat.setGravity(entity, false);
            }

            Location location = piece.anchor.clone().add(0, 0.5, 0);
            Vector direction = this.holeCenter.toVector().subtract(location.toVector());
            double distance = direction.length();

            if (distance < holeRadius + 1.0) {
                this.element.playDebrisVanish(location, this.random, sounds++ < 3);
                entity.remove();

                continue;
            }

            Vector clientStep = direction.multiply(Math.min(speed, distance) / distance);

            piece.anchor.add(clientStep);
            entity.teleport(piece.anchor);
            entity.setVelocity(this.toServerVelocity(clientStep));
        }
    }

    private void collapseHole() {
        for (Block block : this.holeBlocks)
            block.setType(Material.AIR, false);

        this.consumeAllDebris();

        MeteorParticle.REVERSE_PORTAL.spawn(this.holeCenter, 150, 0.5, 1.5);
        MeteorParticle.FLASH.spawn(this.holeCenter, 1, 0, 0);
        MeteorParticle.EXPLOSION_EMITTER.spawn(this.holeCenter, 1, 0, 0);
        MeteorSound.SONIC_BOOM.play(this.center, 5F, 0.8F);
        MeteorSound.EXPLODE.play(this.center, 4F, 0.5F);
        MeteorSound.ENDERMAN_TELEPORT.play(this.center, 3F, 0.5F);
    }

    private void consumeDebris(int step) {
        if (step < 5 || this.debris.isEmpty())
            return;

        int target = Math.min(this.debris.size(), (int) ((long) this.debris.size() * (step - 4) / 76));
        int sounds = 0;

        while (this.debrisIndex < target) {
            Debris piece = this.debris.get(this.debrisIndex++);

            if (!piece.entity.isValid())
                continue;

            this.element.playDebrisVanish(piece.entity.getLocation().add(0, 0.5, 0), this.random, sounds++ < 2);
            piece.entity.remove();
        }
    }

    private void consumeAllDebris() {
        int sounds = 0;

        for (Debris piece : this.debris) {
            if (!piece.entity.isValid())
                continue;

            this.element.playDebrisVanish(piece.entity.getLocation().add(0, 0.5, 0), this.random, sounds++ < 3);
            piece.entity.remove();
        }

        this.debrisIndex = this.debris.size();
    }

    private Location findRandomFinalePoint() {
        if (this.finalePoints.isEmpty())
            return null;

        return this.finalePoints.get(this.random.nextInt(this.finalePoints.size())).clone();
    }

    private void planRebuild() {
        for (BlockRecord record : this.crater.sortedBottomUp())
            if (!record.isAir() || !Compat.isAir(record.getBlock().getType()))
                this.rebuildQueue.add(record);

        this.scanY = this.crater.getCenterY() - this.radius;
    }

    private void liftEntities() {
        double reach = this.radius + 2;

        for (Entity entity : this.world.getNearbyEntities(this.center, reach, reach, reach)) {
            if (!(entity instanceof LivingEntity))
                continue;

            Location location = entity.getLocation();
            int x = location.getBlockX();
            int z = location.getBlockZ();
            int y = location.getBlockY();

            if (!this.isOccluding(x, y, z) && !this.isOccluding(x, y + 1, z))
                continue;

            while (y < this.world.getMaxHeight() - 2 && (this.isOccluding(x, y, z) || this.isOccluding(x, y + 1, z)))
                y++;

            location.setY(y);
            entity.teleport(location);
        }
    }

    private void extinguish() {
        double reach = this.waveRadius + 4;

        for (Entity entity : this.world.getNearbyEntities(this.center, reach, reach, reach))
            if (entity instanceof LivingEntity && entity.getFireTicks() > 0)
                entity.setFireTicks(0);

        for (LivingEntity entity : this.flung)
            if (entity.isValid() && entity.getFireTicks() > 0)
                entity.setFireTicks(0);
    }

    private boolean isOccluding(int x, int y, int z) {
        Material type = this.world.getBlockAt(x, y, z).getType();

        return type.isOccluding() || type.name().endsWith("ICE");
    }

    private void removeEntities() {
        for (FallingBlock falling : this.meteor)
            if (falling.isValid())
                falling.remove();

        for (Debris piece : this.debris)
            if (piece.entity.isValid())
                piece.entity.remove();

        for (WaveBlock block : this.wave)
            if (block.entity.isValid())
                block.entity.remove();

        this.meteor.clear();
        this.debris.clear();
        this.wave.clear();
    }

    private void collectNightPlayers() {
        double reachSquared = NIGHT_RANGE * NIGHT_RANGE;

        for (Player online : Bukkit.getOnlinePlayers())
            if (online.getWorld().equals(this.world) && online.getLocation().distanceSquared(this.center) <= reachSquared)
                this.nightPlayers.add(online.getUniqueId());
    }

    private List<Player> getViewers() {
        List<Player> viewers = new ArrayList<>();

        for (UUID viewerId : this.nightPlayers) {
            Player viewer = Bukkit.getPlayer(viewerId);

            if (viewer != null && viewer.getWorld().equals(this.world))
                viewers.add(viewer);
        }

        return viewers;
    }

    private void updateSky(long time) {
        for (Player viewer : this.getViewers())
            viewer.setPlayerTime(time, false);
    }

    private void resetSky(boolean clearTitles) {
        for (UUID viewerId : this.nightPlayers) {
            Player viewer = Bukkit.getPlayer(viewerId);

            if (viewer == null)
                continue;

            viewer.resetPlayerTime();

            if (clearTitles)
                viewer.resetTitle();
        }
    }

    private long getDawnTime() {
        long now = this.world.getTime();

        return now < NIGHT_TIME ? now + DAY_LENGTH : now;
    }

    private void sendTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        for (Player viewer : this.getViewers())
            Compat.sendTitle(viewer, title, subtitle, fadeIn, stay, fadeOut);
    }

    private void playForViewers(MeteorSound sound, float volume, float pitch) {
        for (Player viewer : this.getViewers())
            sound.play(viewer, volume, pitch);
    }

    private void tellCaster(String message) {
        Player caster = Bukkit.getPlayer(this.casterId);

        if (caster != null)
            MeteorPlugin.tell(caster, message);
    }

    private double jitter(double amount) {
        return (this.random.nextDouble() * 2 - 1) * amount;
    }

    private Vector randomUnit() {
        return new Vector(this.jitter(1), this.jitter(1), this.jitter(1)).normalize();
    }

    private Vector randomHorizontal() {
        double angle = this.random.nextDouble() * Math.PI * 2;

        return new Vector(Math.cos(angle), 0, Math.sin(angle));
    }

    private static long lerp(long from, long to, double progress) {
        return from + Math.round((to - from) * Math.min(1, Math.max(0, progress)));
    }

    private static long columnKey(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    private static final class Debris {

        private final FallingBlock entity;
        private final Vector velocity;
        private Location anchor;

        private Debris(FallingBlock entity, Vector velocity) {
            this.entity = entity;
            this.velocity = velocity;
        }
    }

    private static final class WaveBlock {

        private final FallingBlock entity;
        private final int born;

        private WaveBlock(FallingBlock entity, int born) {
            this.entity = entity;
            this.born = born;
        }
    }

    private static final class Placement {

        private final Block block;
        private final MeteorMaterial material;
        private final int tick;
        private final boolean onlyAir;
        private final boolean needsSolidBelow;

        private Placement(Block block, MeteorMaterial material, int tick, boolean onlyAir, boolean needsSolidBelow) {
            this.block = block;
            this.material = material;
            this.tick = tick;
            this.onlyAir = onlyAir;
            this.needsSolidBelow = needsSolidBelow;
        }
    }
}
