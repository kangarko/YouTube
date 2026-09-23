package org.mineacademy.meteoropus.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.mineacademy.meteoropus.remain.CompMaterial;
import org.mineacademy.meteoropus.remain.CompParticle;
import org.mineacademy.meteoropus.remain.CompSound;

final class VoidFinale extends Finale {

    private static final CompMaterial[] SCAR = {
            CompMaterial.OBSIDIAN, CompMaterial.OBSIDIAN, CompMaterial.OBSIDIAN, CompMaterial.CRYING_OBSIDIAN, CompMaterial.CRYING_OBSIDIAN,
            CompMaterial.BLACK_CONCRETE, CompMaterial.COAL_BLOCK, CompMaterial.PURPLE_TERRACOTTA
    };

    private static final int LINING_TICKS   = 25;
    private static final int CORE_START     = 4;
    private static final int CORE_TICKS     = 20;
    private static final int SWALLOW_START  = 10;
    private static final int SWALLOW_TICKS  = 62;
    private static final int COLLAPSE_START = 86;
    private static final int COLLAPSE_TICKS = 11;
    private static final int DISK_POINTS    = 56;
    private static final double TILT        = 0.32;

    private final List<SavedBlock> lining         = new ArrayList<>();
    private final List<SavedBlock> core           = new ArrayList<>();
    private final List<DebrisField.Piece> victims = new ArrayList<>();
    private final double[] diskRadius             = new double[DISK_POINTS];
    private final double[] diskAngle              = new double[DISK_POINTS];
    private Location heart;
    private double coreRadius;
    private int lined;
    private int formed;
    private int collapsed;
    private int pulled;
    private int swallowed;

    VoidFinale(final Strike strike) {
        super(strike);
    }

    @Override
    void start() {
        final ThreadLocalRandom random = ThreadLocalRandom.current();

        this.heart      = this.strike.getCenter().add(0, Math.max(1.5, this.crater.getRadius() * 0.35), 0);
        this.coreRadius = 1.2 + this.strike.getPower() * 0.3;

        for (final SavedBlock block : this.crater.getBlocks()) {
            if (block.getCenter().distanceSquared(this.heart) <= this.coreRadius * this.coreRadius)
                this.core.add(block);

            else if (block.isShell())
                this.lining.add(block);
        }

        this.lining.sort((first, second) -> Integer.compare(first.getY(), second.getY()));
        this.core.sort((first, second) -> Double.compare(first.getCenter().distanceSquared(this.heart), second.getCenter().distanceSquared(this.heart)));

        for (int index = 0; index < DISK_POINTS; index++) {
            this.diskRadius[index] = this.coreRadius + 0.8 + Math.pow(random.nextDouble(), 0.8) * (3.2 + this.strike.getPower() * 0.4);
            this.diskAngle[index]  = random.nextDouble(Math.PI * 2);
        }

        for (final DebrisField.Piece piece : this.strike.getDebris().getPieces())
            this.victims.add(piece);

        this.victims.sort((first, second) -> Double.compare(first.getBlock().getLocation().distanceSquared(this.heart), second.getBlock().getLocation().distanceSquared(this.heart)));

        CompSound.END_PORTAL.play(this.strike.getViewers(), this.heart, 4F, 0.7F);
        CompSound.CURSE.playNear(this.strike.getViewers(), 0.6F, 0.6F);
        CompSound.SONIC_CHARGE.play(this.strike.getViewers(), this.heart, 3F, 0.5F);

        CompParticle.REVERSE_PORTAL.spawn(this.heart, 120, 0.3, 1.2);
        CompParticle.FLASH.spawn(this.heart, 1, 0, 0);
    }

    @Override
    void tick(final int age) {
        this.scar(age);

        if (age >= CORE_START && age < COLLAPSE_START)
            this.form(age - CORE_START);

        if (age >= CORE_START)
            this.showDisk(age);

        if (age >= SWALLOW_START)
            this.swallow(age - SWALLOW_START);

        if (age >= 20 && age <= 80 && age % 10 == 0)
            this.tug();

        if (age >= COLLAPSE_START)
            this.collapse(age - COLLAPSE_START);

        this.playAmbience(age);
    }

    @Override
    protected void onCleared(final SavedBlock block, final int index) {
        if (index % 3 == 0)
            CompParticle.PORTAL.spawn(block.getCenter(), 6, 0.3, 0.5);
    }

    @Override
    void abort() {
        this.victims.clear();
    }

    private void scar(final int age) {
        final int target               = share(this.lining.size(), age, LINING_TICKS);
        final ThreadLocalRandom random = ThreadLocalRandom.current();

        while (this.lined < target && System.nanoTime() < this.strike.getDeadline()) {
            final SavedBlock block = this.lining.get(this.lined++);

            this.place(block, SCAR[random.nextInt(SCAR.length)].getLook());

            if (this.lined % 4 == 0)
                CompParticle.PORTAL.spawn(block.getCenter(), 3, 0.3, 0.4);
        }
    }

    private void form(final int age) {
        final int target = share(this.core.size(), age, CORE_TICKS);

        while (this.formed < target && System.nanoTime() < this.strike.getDeadline()) {
            final SavedBlock block = this.core.get(this.formed++);

            this.place(block, CompMaterial.BLACK_CONCRETE.getLook());
        }
    }

    private void showDisk(final int age) {
        final Element element = Element.VOID;
        final double sine     = Math.sin(TILT);
        final double cosine   = Math.cos(TILT);
        final double spin     = age * 0.01;

        for (int index = 0; index < DISK_POINTS; index++) {
            final double radius = this.diskRadius[index];

            this.diskAngle[index] += 0.22 * (this.coreRadius + 1) / radius;

            final double angle = this.diskAngle[index];
            final double flatX = Math.cos(angle) * radius;
            final double flatZ = Math.sin(angle) * radius;
            final double x     = flatX * Math.cos(spin) - flatZ * cosine * Math.sin(spin);
            final double y     = flatZ * sine;
            final double z     = flatX * Math.sin(spin) + flatZ * cosine * Math.cos(spin);
            final boolean hot  = radius < this.coreRadius + 2;
            final Color color  = hot ? (index % 4 == 0 ? Color.WHITE : element.getSecondary()) : index % 5 == 0 ? element.getShadow() : element.getPrimary();

            CompParticle.dust(this.heart.clone().add(x, y, z), color, hot ? 1.2F : 1.7F, 1, 0.05);
        }

        final ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int index = 0; index < 10; index++) {
            final Vector reach = Strike.randomUnit().multiply(3 + random.nextDouble() * 6);

            CompParticle.PORTAL.shoot(this.heart, reach.getX(), reach.getY(), reach.getZ(), 1);
        }

        for (int index = 0; index < 3; index++)
            CompParticle.END_ROD.spawn(this.heart.clone().add(Strike.randomUnit().multiply(this.coreRadius + 0.4)), 1, 0.05, 0);

        if (age % 2 == 0)
            CompParticle.SQUID_INK.spawn(this.heart, 2, this.coreRadius * 0.6, 0.02);
    }

    private void swallow(final int age) {
        final int target = Math.min(this.victims.size(), share(this.victims.size(), age, SWALLOW_TICKS));

        while (this.pulled < target)
            this.victims.get(this.pulled++).setSwallowing(true);

        int sounds = 0;

        for (int index = 0; index < this.pulled; index++) {
            final DebrisField.Piece piece = this.victims.get(index);
            final MovingBlock block       = piece.getBlock();

            if (!block.isAlive())
                continue;

            final Location location = block.getLocation().add(0, 0.5, 0);
            final Vector toward     = this.heart.toVector().subtract(location.toVector());
            final double distance   = toward.length();

            if (distance <= this.coreRadius + 0.8) {
                CompParticle.PORTAL.spawn(location, 12, 0.2, 1);
                CompParticle.dust(location, Element.VOID.getSecondary(), 1.5F, 3, 0.3);

                if (sounds++ < 2)
                    CompSound.TELEPORT.play(this.strike.getViewers(), location, 1F, 0.5F + this.swallowed / (float) Math.max(1, this.victims.size()) * 1.3F);

                block.remove();

                this.swallowed++;

                continue;
            }

            final Vector radial  = toward.clone().multiply(1 / distance);
            final Vector tangent = new Vector(radial.getZ(), 0, -radial.getX());
            final double speed   = Math.min(0.2 + 2.2 / distance, 1.2);

            if (tangent.lengthSquared() > 0.0001)
                tangent.normalize().multiply(speed * 0.6);

            block.push(radial.multiply(speed).add(tangent));
        }
    }

    private void tug() {
        final World world  = this.heart.getWorld();
        final double range = this.crater.getRadius() * 2.5;
        final double calm  = this.crater.getRadius() + 2;

        for (final Entity entity : world.getNearbyEntities(this.heart, range, range, range)) {
            if (!(entity instanceof LivingEntity) || !Shockwave.isFlingable(entity))
                continue;

            final Vector toward   = new Vector(this.heart.getX() - entity.getLocation().getX(), 0, this.heart.getZ() - entity.getLocation().getZ());
            final double distance = toward.length();

            if (distance <= calm || distance > range)
                continue;

            entity.setVelocity(toward.multiply(0.28 / distance).setY(0.08));

            Protection.add(entity);

            if (entity instanceof Player)
                CompSound.PORTAL_TRIGGER.play((Player) entity, entity.getLocation(), 0.25F, 1.8F);
        }
    }

    private void collapse(final int age) {
        if (age == 0) {
            CompSound.DEPLETE.play(this.strike.getViewers(), this.heart, 4F, 0.6F);
            CompSound.BEACON_DEACTIVATE.play(this.strike.getViewers(), this.heart, 4F, 0.5F);
        }

        final int target = share(this.core.size(), age, COLLAPSE_TICKS);

        while (this.collapsed < Math.min(target, this.formed)) {
            final SavedBlock block = this.core.get(this.formed - 1 - this.collapsed++);

            this.unplace(block);
        }

        CompParticle.REVERSE_PORTAL.spawn(this.heart, 30, this.coreRadius * 0.5, 0.3);

        if (age == COLLAPSE_TICKS - 1) {
            CompParticle.FLASH.spawn(this.heart, 2, 0.2, 0);
            CompParticle.EXPLOSION.spawn(this.heart, 4, 0.6, 0);
            CompParticle.PORTAL.spawn(this.heart, 150, 0.4, 2.5);

            CompSound.EXPLODE.play(this.strike.getViewers(), this.heart, 5F, 1.4F);
            CompSound.LARGE_BLAST.play(this.strike.getViewers(), this.heart, 4F, 0.5F);
        }
    }

    private void playAmbience(final int age) {
        if (age % 20 == 5)
            CompSound.PORTAL_AMBIENT.play(this.strike.getViewers(), this.heart, 4F, 0.5F);

        if (age == 30)
            CompSound.WITHER_AMBIENT.play(this.strike.getViewers(), this.heart, 3F, 0.5F);

        if (age == 50)
            CompSound.SONIC_CHARGE.play(this.strike.getViewers(), this.heart, 3F, 0.7F);

        if (age % 6 == 0)
            CompSound.HEARTBEAT.play(this.strike.getViewers(), this.heart, 2F, 0.5F);
    }
}
