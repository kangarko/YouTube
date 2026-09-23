package org.mineacademy.meteoropus.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.mineacademy.meteoropus.remain.BlockLook;
import org.mineacademy.meteoropus.remain.CompParticle;
import org.mineacademy.meteoropus.remain.CompSound;
import org.mineacademy.meteoropus.remain.Remain;

final class DebrisField {

    private static final int MAX_PIECES               = 1200;
    private static final int BURST_TICKS              = 3;
    private static final int LIFETIME                 = 150;
    private static final int LANDING_EFFECTS_PER_TICK = 6;
    private static final int LANDING_SOUNDS_PER_TICK  = 2;

    private final Strike strike;
    private final List<Piece> pieces = new ArrayList<>();
    private boolean released;
    private int releaseTick;

    DebrisField(final Strike strike) {
        this.strike = strike;
    }

    boolean add(final SavedBlock block, final int tick) {
        if (this.pieces.size() >= MAX_PIECES)
            return false;

        final BlockLook look = BlockLook.findFlyingLook(block.getState());

        if (look == null)
            return false;

        final Location spawn     = block.getBlock().getLocation().add(0.5, 0, 0.5);
        final MovingBlock moving = MovingBlock.spawn(spawn, look);

        if (moving == null)
            return false;

        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final int radius               = this.strike.getRadius();
        final Vector offset            = block.getCenter().toVector().subtract(this.strike.getCenter().toVector());
        final double closeness         = 1.15 - Math.min(1, offset.length() / radius) * 0.5;
        final Vector direction         = offset.clone().add(new Vector(0, radius * 0.55, 0)).add(Strike.randomUnit().multiply(radius * 0.3)).normalize();
        final double speed             = (0.55 + this.strike.getPower() * 0.07) * closeness * (0.85 + random.nextDouble() * 0.3);

        this.pieces.add(new Piece(moving, direction.multiply(speed), tick));

        return true;
    }

    void tick(final int tick) {
        if (!this.released) {
            this.hold(tick);

            return;
        }

        switch (this.strike.getElement()) {
            case FIRE:
            case ICE:
                this.fall(tick);
                break;

            case VOID:
                this.drift(tick);
                break;

            default:
                throw new IllegalStateException("Report / Unhandled element " + this.strike.getElement());
        }
    }

    void release(final int tick) {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final boolean weightless       = this.strike.getElement() == Element.VOID;

        this.released    = true;
        this.releaseTick = tick;

        for (final Piece piece : this.pieces)
            if (weightless)
                piece.drift = piece.velocity.clone().multiply(0.6);

            else
                piece.block.launch(piece.velocity.clone().multiply(1.1).add(new Vector(0, 0.2 + random.nextDouble() * 0.25, 0)));
    }

    void showFrozenGlow() {
        final Element element = this.strike.getElement();
        final int count       = Math.min(28, this.pieces.size());

        for (int index = 0; index < count; index++) {
            final Location location = this.pieces.get(ThreadLocalRandom.current().nextInt(this.pieces.size())).block.getLocation().add(0, 0.5, 0);

            CompParticle.dust(location, element.getRandomColor(), 1.6F, 1, 0.45);

            if (index % 4 == 0)
                CompParticle.END_ROD.spawn(location, 1, 0.4, 0);
        }
    }

    List<Piece> getPieces() {
        return this.pieces;
    }

    int size() {
        return this.pieces.size();
    }

    void removeAll() {
        for (final Piece piece : this.pieces)
            piece.block.remove();

        this.pieces.clear();
    }

    private void hold(final int tick) {
        for (final Piece piece : this.pieces) {
            final int age = tick - piece.born;

            if (age < BURST_TICKS)
                piece.block.push(piece.velocity.clone().multiply(0.85));

            else if (!piece.frozen || !Remain.canToggleGravity()) {
                piece.block.push(new Vector());

                piece.frozen = true;
            }
        }
    }

    private void fall(final int tick) {
        final Element element          = this.strike.getElement();
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        int effects                    = 0;
        int sounds                     = 0;

        for (final Iterator<Piece> iterator = this.pieces.iterator(); iterator.hasNext();) {
            final Piece piece = iterator.next();

            if (piece.block.isAlive()) {
                if (tick - this.releaseTick > LIFETIME) {
                    piece.block.remove();

                    iterator.remove();

                } else if (random.nextInt(20) == 0)
                    element.getGlow().spawn(piece.block.getLocation().add(0, 0.5, 0), 1, 0.2, 0.01);

                continue;
            }

            if (effects++ < LANDING_EFFECTS_PER_TICK)
                this.showLanding(piece, sounds++ < LANDING_SOUNDS_PER_TICK);

            piece.block.remove();

            iterator.remove();
        }
    }

    private void drift(final int tick) {
        final Location center = this.strike.getCenter();
        final double wobble   = Math.sin(tick * 0.2) * 0.015;
        int effects           = 0;

        for (final Iterator<Piece> iterator = this.pieces.iterator(); iterator.hasNext();) {
            final Piece piece = iterator.next();

            if (!piece.block.isAlive()) {
                if (!piece.swallowing && effects++ < LANDING_EFFECTS_PER_TICK)
                    this.showLanding(piece, false);

                piece.block.remove();

                iterator.remove();

                continue;
            }

            if (piece.swallowing)
                continue;

            final Vector away    = piece.block.getLocation().toVector().subtract(center.toVector());
            final Vector tangent = new Vector(-away.getZ(), 0, away.getX());

            if (tangent.lengthSquared() > 0.0001)
                tangent.normalize().multiply(0.05);

            piece.drift.multiply(0.92);
            piece.block.push(piece.drift.clone().add(tangent).add(new Vector(0, wobble, 0)));
        }
    }

    private void showLanding(final Piece piece, final boolean withSound) {
        final Location location = piece.block.getLocation().add(0, 0.3, 0);
        final Element element   = this.strike.getElement();

        CompParticle.block(location, piece.block.getLook(), 10, 0.35, 0.1);
        CompParticle.POOF.spawn(location, 1, 0.2, 0.02);

        switch (element) {
            case FIRE:
                CompParticle.LAVA.spawn(location, 1, 0.2, 0);
                CompParticle.LARGE_SMOKE.spawn(location, 2, 0.25, 0.03);
                break;

            case ICE:
                CompParticle.SNOWBALL.spawn(location, 5, 0.3, 0.05);
                break;

            case VOID:
                CompParticle.PORTAL.spawn(location, 8, 0.3, 0.4);
                break;

            default:
                throw new IllegalStateException("Report / Unhandled element " + element);
        }

        if (!withSound)
            return;

        final float pitch = 0.6F + ThreadLocalRandom.current().nextFloat() * 0.4F;

        CompSound.STONE_BREAK.play(this.strike.getViewers(), location, 1.2F, pitch);
        element.getLandSound().play(this.strike.getViewers(), location, 0.8F, pitch + 0.2F);
    }

    static final class Piece {

        private final MovingBlock block;
        private final Vector velocity;
        private final int born;
        private boolean frozen;
        private Vector drift;
        private boolean swallowing;

        private Piece(final MovingBlock block, final Vector velocity, final int born) {
            this.block    = block;
            this.velocity = velocity;
            this.born     = born;
        }

        MovingBlock getBlock() {
            return this.block;
        }

        void setSwallowing(final boolean swallowing) {
            this.swallowing = swallowing;
        }
    }
}
