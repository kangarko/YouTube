package org.mineacademy.meteoropus.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.mineacademy.meteoropus.remain.CompMaterial;
import org.mineacademy.meteoropus.remain.CompParticle;
import org.mineacademy.meteoropus.remain.CompSound;
import org.mineacademy.meteoropus.remain.Remain;

final class IceFinale extends Finale {

    private static final CompMaterial[] GLACIER = {
            CompMaterial.PACKED_ICE, CompMaterial.PACKED_ICE, CompMaterial.PACKED_ICE, CompMaterial.PACKED_ICE,
            CompMaterial.BLUE_ICE, CompMaterial.BLUE_ICE, CompMaterial.BLUE_ICE, CompMaterial.ICE, CompMaterial.ICE, CompMaterial.LIGHT_BLUE_GLASS
    };

    private static final CompMaterial[] SNOWCAP = { CompMaterial.SNOW_BLOCK, CompMaterial.SNOW_BLOCK, CompMaterial.PACKED_ICE, CompMaterial.WHITE_GLASS };
    private static final CompMaterial[] SPIKE   = { CompMaterial.PACKED_ICE, CompMaterial.BLUE_ICE, CompMaterial.ICE };

    private static final int FREEZE_TICKS = 36;
    private static final int SPIKE_START  = 34;
    private static final int FROST_TICKS  = 120;

    private final List<SavedBlock> fill               = new ArrayList<>();
    private final Map<Long, Integer> tops             = new HashMap<>();
    private final List<List<List<SavedBlock>>> spikes = new ArrayList<>();
    private final List<Player> frosted                = new ArrayList<>();
    private int frozen;
    private int grown;

    IceFinale(final Strike strike) {
        super(strike);
    }

    @Override
    void start() {
        final Location center = this.strike.getCenter();

        for (final SavedBlock block : this.crater.getBlocks())
            if (block.isSolid()) {
                final long column = toColumn(block.getX(), block.getZ());
                final Integer top = this.tops.get(column);

                this.fill.add(block);

                if (top == null || block.getY() > top)
                    this.tops.put(column, block.getY());
            }

        this.fill.sort((first, second) -> {
            final double firstDistance  = Math.pow(first.getX() + 0.5 - center.getX(), 2) + Math.pow(first.getZ() + 0.5 - center.getZ(), 2);
            final double secondDistance = Math.pow(second.getX() + 0.5 - center.getX(), 2) + Math.pow(second.getZ() + 0.5 - center.getZ(), 2);

            return firstDistance != secondDistance ? Double.compare(secondDistance, firstDistance) : Integer.compare(first.getY(), second.getY());
        });

        this.planSpikes();

        final Location surface = this.strike.getSurface();

        CompSound.FREEZE.play(this.strike.getViewers(), surface, 4F, 0.6F);
        CompSound.GLASS_BREAK.play(this.strike.getViewers(), surface, 4F, 0.5F);
        CompSound.AMETHYST.play(this.strike.getViewers(), surface, 3F, 0.5F);
        CompSound.WIND.play(this.strike.getViewers(), surface, 1.5F, 0.6F);

        CompParticle.CLOUD.spawn(surface, 60, this.crater.getRadius() * 0.6, 0.12);
        CompParticle.SNOWFLAKE.spawn(surface, 80, this.crater.getRadius() * 0.7, 0.1);
    }

    @Override
    void tick(final int age) {
        this.freeze(age);

        if (age >= SPIKE_START && age % 2 == 0)
            this.growSpikes();

        this.showBlizzard(age);
        this.chill(age);
    }

    @Override
    protected void beginClear() {
        this.getPlaced().sort((first, second) -> Integer.compare(second.getY(), first.getY()));

        final Location surface = this.strike.getSurface();

        CompSound.GLASS_BREAK.play(this.strike.getViewers(), surface, 5F, 0.5F);
        CompSound.GLASS_BREAK.play(this.strike.getViewers(), surface, 5F, 0.8F);
        CompSound.FREEZE.play(this.strike.getViewers(), surface, 3F, 1.2F);

        this.thaw();
    }

    @Override
    protected void onCleared(final SavedBlock block, final int index) {
        if (index % 3 != 0)
            return;

        final Location location = block.getCenter();

        CompParticle.block(location, CompMaterial.PACKED_ICE.getLook(), 6, 0.35, 0.1);
        CompParticle.SNOWBALL.spawn(location, 2, 0.3, 0.05);

        if (index % 45 == 0)
            CompSound.GLASS_BREAK.play(this.strike.getViewers(), location, 1.5F, 0.6F + ThreadLocalRandom.current().nextFloat() * 0.6F);
    }

    @Override
    void abort() {
        this.thaw();
    }

    private void freeze(final int age) {
        final int target               = share(this.fill.size(), age, FREEZE_TICKS);
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final int before               = this.frozen;

        while (this.frozen < target && System.nanoTime() < this.strike.getDeadline()) {
            final SavedBlock block       = this.fill.get(this.frozen++);
            final Integer top            = this.tops.get(toColumn(block.getX(), block.getZ()));
            final CompMaterial[] palette = top != null && top == block.getY() ? SNOWCAP : GLACIER;

            this.place(block, palette[random.nextInt(palette.length)].getLook());

            if (this.frozen % 5 == 0)
                CompParticle.SNOWFLAKE.spawn(block.getCenter(), 2, 0.4, 0.02);
        }

        if (this.frozen > before && age % 2 == 0) {
            final float pitch = 0.6F + this.frozen / (float) Math.max(1, this.fill.size()) * 1.2F;

            CompSound.GLASS_BREAK.play(this.strike.getViewers(), this.strike.getSurface(), 1.6F, pitch);
            CompSound.SNOW_PLACE.play(this.strike.getViewers(), this.strike.getSurface(), 1.6F, pitch);
        }
    }

    private void planSpikes() {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final List<Long> columns       = new ArrayList<>(this.tops.keySet());
        final int count                = Math.min(columns.size(), 3 + this.strike.getPower());
        final Location center          = this.strike.getCenter();
        final int radius               = this.crater.getRadius();

        columns.sort((first, second) -> Double.compare(
                Math.pow(columnX(first) + 0.5 - center.getX(), 2) + Math.pow(columnZ(first) + 0.5 - center.getZ(), 2),
                Math.pow(columnX(second) + 0.5 - center.getX(), 2) + Math.pow(columnZ(second) + 0.5 - center.getZ(), 2)));

        for (int index = 0; index < count && !columns.isEmpty(); index++) {
            final long column                   = index == 0 ? columns.remove(0) : columns.remove(random.nextInt(columns.size()));
            final int height                    = index == 0 ? radius + 1 : 2 + random.nextInt(Math.max(1, radius - 1));
            final List<List<SavedBlock>> levels = new ArrayList<>();
            final int x                         = columnX(column);
            final int z                         = columnZ(column);
            final int base                      = this.tops.get(column) + 1;

            for (int level = 0; level < height; level++) {
                final List<SavedBlock> ring = new ArrayList<>();

                this.addSpikeBlock(ring, x, base + level, z);

                if (level < height / 2)
                    for (final int[] side : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } })
                        this.addSpikeBlock(ring, x + side[0], base + level, z + side[1]);

                if (ring.isEmpty())
                    break;

                levels.add(ring);
            }

            if (!levels.isEmpty())
                this.spikes.add(levels);
        }
    }

    private void addSpikeBlock(final List<SavedBlock> ring, final int x, final int y, final int z) {
        final SavedBlock block = this.crater.find(x, y, z);

        if (block != null && !block.isSolid())
            ring.add(block);
    }

    private void growSpikes() {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        boolean grew                   = false;

        for (final List<List<SavedBlock>> levels : this.spikes) {
            if (this.grown >= levels.size())
                continue;

            for (final SavedBlock block : levels.get(this.grown)) {
                if (block.isModified())
                    continue;

                this.place(block, SPIKE[random.nextInt(SPIKE.length)].getLook());

                CompParticle.END_ROD.spawn(block.getCenter(), 1, 0.3, 0.02);

                grew = true;
            }
        }

        this.grown++;

        if (grew)
            CompSound.AMETHYST.play(this.strike.getViewers(), this.strike.getSurface(), 2F, 0.5F + this.grown * 0.12F);
    }

    private void showBlizzard(final int age) {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final Location surface         = this.strike.getSurface();
        final double spread            = this.crater.getRadius() * 2;
        final Element element          = Element.ICE;

        for (int index = 0; index < 10; index++) {
            final Location flake = surface.clone().add(random.nextDouble(-spread, spread), 4 + random.nextDouble() * 4, random.nextDouble(-spread, spread));

            CompParticle.SNOWFLAKE.shoot(flake, random.nextDouble(-0.2, 0.2), -1, random.nextDouble(-0.2, 0.2), 0.15);
        }

        for (int index = 0; index < 3; index++) {
            final Location mist = surface.clone().add(random.nextDouble(-spread * 0.5, spread * 0.5), 0.3 + random.nextDouble(), random.nextDouble(-spread * 0.5, spread * 0.5));

            CompParticle.CLOUD.shoot(mist, random.nextDouble(-1, 1), 0, random.nextDouble(-1, 1), 0.04);
            CompParticle.dust(mist, element.getRandomColor(), 1.3F, 1, 0.5);
        }

        if (age % 2 == 0)
            CompParticle.END_ROD.spawn(surface.clone().add(random.nextDouble(-spread * 0.4, spread * 0.4), 0.8, random.nextDouble(-spread * 0.4, spread * 0.4)), 1, 0.2, 0);

        if (age % 30 == 10)
            CompSound.WIND.play(this.strike.getViewers(), surface, 1.2F, 0.6F + random.nextFloat() * 0.2F);

        if (age % 12 == 6)
            CompSound.FREEZE.play(this.strike.getViewers(), surface, 1.5F, 0.5F + random.nextFloat() * 0.3F);
    }

    private void chill(final int age) {
        if (!Remain.canFreeze() || age > Finale.DURATION - 5)
            return;

        final double range = this.crater.getRadius() * 1.5 + 3;

        for (final Player player : this.strike.getViewers())
            if (player.getLocation().distanceSquared(this.strike.getCenter()) <= range * range) {
                Remain.setFreezeTicks(player, FROST_TICKS);

                if (!this.frosted.contains(player))
                    this.frosted.add(player);
            }
    }

    private void thaw() {
        if (!Remain.canFreeze())
            return;

        for (final Player player : this.frosted)
            if (player.isOnline())
                Remain.setFreezeTicks(player, 0);

        this.frosted.clear();
    }

    private static long toColumn(final int x, final int z) {
        return (long) x << 32 | z & 0xFFFFFFFFL;
    }

    private static int columnX(final long column) {
        return (int) (column >> 32);
    }

    private static int columnZ(final long column) {
        return (int) column;
    }
}
