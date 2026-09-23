package org.mineacademy.meteoropus.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.mineacademy.meteoropus.remain.BlockLook;
import org.mineacademy.meteoropus.remain.CompParticle;
import org.mineacademy.meteoropus.remain.CompSound;

final class Rebuild {

    private static final int FLIGHT_TICKS = 6;
    private static final int MAX_HEADROOM = 6;
    private static final double MAX_STEP  = 2.5;

    private final Strike strike;
    private final List<SavedBlock> quiet = new ArrayList<>();
    private final List<Entry> entries    = new ArrayList<>();
    private final List<Entry> flying     = new ArrayList<>();
    private int quietIndex;
    private int launchIndex;
    private int restoreIndex;

    Rebuild(final Strike strike, final int startTick, final int endTick) {
        this.strike = strike;

        final Location center = strike.getCenter();

        for (final SavedBlock block : strike.getCrater().getBlocks())
            if (block.isModified()) {
                if (block.isSolid())
                    this.entries.add(new Entry(block, Math.atan2(block.getZ() + 0.5 - center.getZ(), block.getX() + 0.5 - center.getX())));
                else
                    this.quiet.add(block);
            }

        this.entries.sort((first, second) -> {
            if (first.block.getY() != second.block.getY())
                return Integer.compare(first.block.getY(), second.block.getY());

            final double firstSweep  = (first.angle + first.block.getY() * 0.9) % (Math.PI * 2);
            final double secondSweep = (second.angle + second.block.getY() * 0.9) % (Math.PI * 2);

            return Double.compare(firstSweep, secondSweep);
        });

        final int window = Math.max(1, endTick - startTick - FLIGHT_TICKS);

        for (int index = 0; index < this.entries.size(); index++)
            this.entries.get(index).restoreTick = startTick + FLIGHT_TICKS + (int) ((long) index * window / Math.max(1, this.entries.size()));
    }

    void tick(final int tick) {
        this.restoreQuietly();
        this.launch(tick);
        this.steer(tick);
        this.land(tick);
    }

    boolean isDone() {
        return this.quietIndex >= this.quiet.size() && this.restoreIndex >= this.entries.size();
    }

    void abort() {
        for (final Entry entry : this.flying)
            entry.ghost.remove();

        this.flying.clear();
    }

    private void restoreQuietly() {
        this.strike.setSuppressingDrops(true);

        try {
            while (this.quietIndex < this.quiet.size() && System.nanoTime() < this.strike.getDeadline())
                this.quiet.get(this.quietIndex++).restore();

        } finally {
            this.strike.setSuppressingDrops(false);
        }
    }

    private void launch(final int tick) {
        while (this.launchIndex < this.entries.size() && this.entries.get(this.launchIndex).restoreTick - FLIGHT_TICKS <= tick) {
            final Entry entry    = this.entries.get(this.launchIndex++);
            final BlockLook look = BlockLook.findFlyingLook(entry.block.getState());
            final int headroom   = this.findHeadroom(entry.block.getBlock());

            if (look == null || headroom < 2)
                continue;

            final MovingBlock ghost = MovingBlock.spawn(entry.block.getBlock().getLocation().add(0.5, headroom, 0.5), look);

            if (ghost == null)
                continue;

            entry.ghost = ghost;

            this.flying.add(entry);
        }
    }

    private void steer(final int tick) {
        for (final Entry entry : this.flying) {
            if (!entry.ghost.isAlive())
                continue;

            final int remaining     = Math.max(1, entry.restoreTick - tick);
            final Location location = entry.ghost.getLocation();
            final Vector step       = entry.block.getBlock().getLocation().add(0.5, 0, 0.5).toVector().subtract(location.toVector()).multiply(1D / remaining);

            if (step.lengthSquared() > MAX_STEP * MAX_STEP)
                step.normalize().multiply(MAX_STEP);

            entry.ghost.push(step);

            if (ThreadLocalRandom.current().nextInt(3) == 0)
                CompParticle.dust(location.add(0, 0.5, 0), this.strike.getElement().getRandomColor(), 1F, 1, 0.3);
        }
    }

    private void land(final int tick) {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        int sounds                     = 0;

        this.strike.setSuppressingDrops(true);

        try {
            while (this.restoreIndex < this.entries.size() && this.entries.get(this.restoreIndex).restoreTick <= tick && System.nanoTime() < this.strike.getDeadline()) {
                final Entry entry = this.entries.get(this.restoreIndex++);

                if (entry.ghost != null) {
                    entry.ghost.remove();

                    this.flying.remove(entry);
                }

                this.strike.rescueEntities(entry.block);

                entry.block.restore();

                this.showRestored(entry.block, random, sounds++ < 2);
            }

        } finally {
            this.strike.setSuppressingDrops(false);
        }
    }

    private void showRestored(final SavedBlock block, final ThreadLocalRandom random, final boolean withSound) {
        final Location center = block.getCenter();
        final BlockLook look  = BlockLook.findFlyingLook(block.getState());

        if (look != null)
            CompParticle.block(center, look, 4, 0.3, 0.05);
        else
            CompParticle.CLOUD.spawn(center, 2, 0.3, 0.02);

        if (random.nextInt(3) == 0)
            CompParticle.dust(center.add(0, 0.6, 0), this.strike.getElement().getRandomColor(), 1.2F, 1, 0.3);

        if (random.nextInt(5) == 0)
            CompParticle.END_ROD.spawn(center, 1, 0.3, 0.02);

        if (!withSound)
            return;

        final float progress = this.restoreIndex / (float) Math.max(1, this.entries.size());

        CompSound.STONE_PLACE.play(this.strike.getViewers(), center, 1F, 0.8F + random.nextFloat() * 0.3F);
        CompSound.CHIME.play(this.strike.getViewers(), center, 0.7F, 0.5F + progress * 1.5F);
    }

    private int findHeadroom(final Block block) {
        int headroom = 0;

        for (int above = 1; above <= MAX_HEADROOM; above++) {
            final Block cell = block.getRelative(0, above, 0);

            if (cell.getType().isSolid() || BlockLook.isLiquid(cell.getType()))
                break;

            headroom++;
        }

        return headroom;
    }

    private static final class Entry {

        private final SavedBlock block;
        private final double angle;
        private int restoreTick;
        private MovingBlock ghost;

        private Entry(final SavedBlock block, final double angle) {
            this.block = block;
            this.angle = angle;
        }
    }
}
