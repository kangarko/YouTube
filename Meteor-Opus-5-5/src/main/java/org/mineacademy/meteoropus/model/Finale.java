package org.mineacademy.meteoropus.model;

import java.util.ArrayList;
import java.util.List;

import org.mineacademy.meteoropus.remain.BlockLook;

abstract class Finale {

    static final int DURATION = 100;

    protected final Strike strike;
    protected final Crater crater;
    private final List<SavedBlock> placed = new ArrayList<>();
    private int cleared;
    private boolean clearing;

    protected Finale(final Strike strike) {
        this.strike = strike;
        this.crater = strike.getCrater();
    }

    abstract void start();

    abstract void tick(int age);

    boolean clear(final long deadline) {
        if (!this.clearing) {
            this.clearing = true;

            this.beginClear();
        }

        while (this.cleared < this.placed.size() && System.nanoTime() < deadline) {
            final SavedBlock block = this.placed.get(this.cleared++);

            block.clear();

            this.onCleared(block, this.cleared);
        }

        return this.cleared >= this.placed.size();
    }

    void abort() {
    }

    protected void beginClear() {
    }

    protected void onCleared(final SavedBlock block, final int index) {
    }

    protected final void place(final SavedBlock block, final BlockLook look) {
        this.strike.rescueEntities(block);

        block.place(look);

        this.placed.add(block);
    }

    protected final void unplace(final SavedBlock block) {
        if (this.placed.remove(block))
            block.clear();
    }

    protected final List<SavedBlock> getPlaced() {
        return this.placed;
    }

    protected static int share(final int total, final int age, final int duration) {
        if (age >= duration - 1)
            return total;

        return (int) Math.ceil(total * (age + 1) / (double) duration);
    }
}
