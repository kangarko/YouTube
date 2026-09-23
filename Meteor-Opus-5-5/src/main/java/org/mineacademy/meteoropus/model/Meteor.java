package org.mineacademy.meteoropus.model;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.mineacademy.meteoropus.remain.BlockLook;
import org.mineacademy.meteoropus.remain.CompParticle;
import org.mineacademy.meteoropus.remain.CompSound;

final class Meteor {

    static final int FLIGHT_TICKS = 40;

    private static final double RESYNC_DISTANCE_SQUARED = 0.6 * 0.6;

    private final Strike strike;
    private final World world;
    private final Vector start;
    private final Vector end;
    private final Vector step;
    private final Vector direction;
    private final double radius;
    private final List<Piece> pieces = new ArrayList<>();

    Meteor(final Strike strike, final Vector approach) {
        final Location target = strike.getCenter();
        final double exactX   = target.getX() + approach.getX() * 40;
        final double exactY   = target.getY() + 60;
        final double exactZ   = target.getZ() + approach.getZ() * 40;

        this.strike    = strike;
        this.world     = target.getWorld();
        this.radius    = 1.5 + strike.getPower() * 0.45;
        this.start     = new Vector(Math.floor(exactX) + 0.5, Math.floor(exactY) + 0.5, Math.floor(exactZ) + 0.5);
        this.direction = target.toVector().subtract(this.start).normalize();
        this.end       = target.toVector().subtract(this.direction.clone().multiply(this.radius * 0.6));
        this.step      = this.end.clone().subtract(this.start).multiply(1D / FLIGHT_TICKS);

        final int reach       = (int) Math.ceil(this.radius);
        final double inner    = Math.max(0, this.radius - 1.3);
        final Element element = strike.getElement();

        for (int x = -reach; x <= reach; x++)
            for (int y = -reach; y <= reach; y++)
                for (int z = -reach; z <= reach; z++) {
                    final double distance = Math.sqrt(x * x + y * y + z * z);

                    if (distance <= this.radius && distance >= inner)
                        this.pieces.add(new Piece(new Vector(x, y, z), element.getRandomRock()));
                }
    }

    void tick(final int age) {
        final Vector center = this.getPosition(age);
        final Vector next   = this.getPosition(age + 1).subtract(center);

        for (final Piece piece : this.pieces)
            piece.fly(center, next);

        this.showBody(center.clone().add(new Vector(0, 0.5, 0)).toLocation(this.world));

        if (age % 5 == 0)
            this.playRoar(center.toLocation(this.world), age);
    }

    void remove() {
        for (final Piece piece : this.pieces)
            piece.remove();
    }

    private Vector getPosition(final int age) {
        return this.start.clone().add(this.step.clone().multiply(Math.min(age, FLIGHT_TICKS)));
    }

    private void showBody(final Location head) {
        final Element element = this.strike.getElement();
        final double spread   = this.radius * 0.6;

        element.getGlow().spawn(head, 14, spread, 0.03);
        element.getSmoke().spawn(head, 7, spread * 0.8, 0.04);
        element.getSpark().spawn(head, 3, spread, 0.05);

        CompParticle.dust(head, element.getPrimary(), 2.5F, 10, this.radius);
        CompParticle.dust(head, element.getSecondary(), 1.8F, 6, this.radius * 0.7);

        for (int step = 1; step <= 9; step++) {
            final Location trail = head.clone().subtract(this.direction.clone().multiply(this.radius + step * 1.3));

            element.getSmoke().spawn(trail, 2, 0.35 + step * 0.08, 0.01);

            if (step <= 4) {
                element.getGlow().spawn(trail, 2, 0.3, 0.01);

                CompParticle.dust(trail, element.getRandomColor(), 2F, 1, 0.4);
            }
        }
    }

    private void playRoar(final Location location, final int age) {
        final List<Player> viewers = this.strike.getViewers();
        final float pitch          = 0.5F + age / (float) FLIGHT_TICKS * 0.5F;

        CompSound.WINGS.play(viewers, location, 8F, pitch);

        switch (this.strike.getElement()) {
            case FIRE:
                CompSound.BURN.play(viewers, location, 8F, 0.6F);
                CompSound.BLAZE_SHOOT.play(viewers, location, 5F, pitch);
                break;

            case ICE:
                CompSound.WIND.play(viewers, location, 3F, 0.6F + pitch * 0.4F);
                CompSound.GLASS_BREAK.play(viewers, location, 5F, 0.5F);
                break;

            case VOID:
                CompSound.PORTAL_AMBIENT.play(viewers, location, 8F, pitch);
                CompSound.PORTAL_TRIGGER.play(viewers, location, 3F, 1.6F);
                break;

            default:
                throw new IllegalStateException("Report / Unhandled element " + this.strike.getElement());
        }
    }

    private final class Piece {

        private final Vector offset;
        private final BlockLook look;
        private MovingBlock block;

        private Piece(final Vector offset, final BlockLook look) {
            this.offset = offset;
            this.look   = look;
        }

        private void fly(final Vector center, final Vector motion) {
            final Location expected = center.clone().add(this.offset).toLocation(Meteor.this.world);

            if (this.block == null || !this.block.isAlive()) {
                if (this.block != null)
                    this.block.remove();

                this.block = MovingBlock.spawn(expected, this.look);

            } else if (this.block.getLocation().distanceSquared(expected) > RESYNC_DISTANCE_SQUARED)
                this.block.teleport(expected);

            if (this.block != null)
                this.block.push(motion);
        }

        private void remove() {
            if (this.block != null)
                this.block.remove();
        }
    }
}
