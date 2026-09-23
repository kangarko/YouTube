package org.mineacademy.meteoropus.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.mineacademy.meteoropus.remain.BlockLook;
import org.mineacademy.meteoropus.remain.CompMaterial;
import org.mineacademy.meteoropus.remain.CompParticle;
import org.mineacademy.meteoropus.remain.CompSound;

final class FireFinale extends Finale {

    private static final CompMaterial[] CRUST = {
            CompMaterial.MAGMA_BLOCK, CompMaterial.MAGMA_BLOCK, CompMaterial.MAGMA_BLOCK, CompMaterial.MAGMA_BLOCK, CompMaterial.MAGMA_BLOCK, CompMaterial.MAGMA_BLOCK,
            CompMaterial.NETHERRACK, CompMaterial.NETHERRACK, CompMaterial.OBSIDIAN, CompMaterial.BLACKSTONE, CompMaterial.SHROOMLIGHT, CompMaterial.RED_TERRACOTTA
    };

    private static final int LINING_TICKS  = 30;
    private static final int LAKE_START    = 12;
    private static final int LAYER_TICKS   = 4;
    private static final int BOMB_LIFETIME = 60;

    private final List<SavedBlock> lining            = new ArrayList<>();
    private final List<List<SavedBlock>> layers      = new ArrayList<>();
    private final List<SavedBlock> lava              = new ArrayList<>();
    private final Map<UUID, Player> informed         = new HashMap<>();
    private final List<MovingBlock> bombs            = new ArrayList<>();
    private final Map<MovingBlock, Integer> bombAges = new HashMap<>();
    private List<SavedBlock> surface                 = new ArrayList<>();
    private int lined;
    private int raised;

    FireFinale(final Strike strike) {
        super(strike);
    }

    @Override
    void start() {
        final TreeMap<Integer, List<SavedBlock>> byLayer = new TreeMap<>();
        final int lakeTop                                = this.crater.getCenterY() - 1;

        for (final SavedBlock block : this.crater.getBlocks())
            if (block.isShell())
                this.lining.add(block);

            else if (block.isSolid() && block.getY() <= lakeTop) {
                if (!byLayer.containsKey(block.getY()))
                    byLayer.put(block.getY(), new ArrayList<>());

                byLayer.get(block.getY()).add(block);
            }

        this.lining.sort((first, second) -> Integer.compare(first.getY(), second.getY()));
        this.layers.addAll(byLayer.values());
        this.surface = this.lining;

        final Location center = this.strike.getSurface();

        CompSound.EXPLODE.play(this.strike.getViewers(), center, 4F, 0.5F);
        CompSound.LAVA_AMBIENT.play(this.strike.getViewers(), center, 4F, 0.8F);
        CompSound.BLAZE_SHOOT.play(this.strike.getViewers(), center, 3F, 0.5F);

        CompParticle.FLAME.spawn(center, 60, this.crater.getRadius() * 0.5, 0.08);
        CompParticle.LARGE_SMOKE.spawn(center, 25, this.crater.getRadius() * 0.5, 0.06);
    }

    @Override
    void tick(final int age) {
        this.placeLining(age);

        if (age >= LAKE_START && (age - LAKE_START) % LAYER_TICKS == 0 && this.raised < this.layers.size())
            this.raiseLayer();

        if (age % 10 == 0)
            this.informNewViewers();

        this.tickBombs(age);
        this.showMolten(age);
    }

    @Override
    protected void beginClear() {
        final ThreadLocalRandom random = ThreadLocalRandom.current();

        for (final Player player : this.informed.values())
            if (player.isOnline())
                for (final SavedBlock block : this.lava)
                    BlockLook.fromBlock(block.getBlock()).sendTo(player, block.getBlock().getLocation());

        for (int index = 0; index < Math.min(40, this.lava.size()); index++)
            CompParticle.LARGE_SMOKE.spawn(this.lava.get(random.nextInt(this.lava.size())).getCenter().add(0, 0.6, 0), 3, 0.4, 0.05);

        CompSound.FIZZ.play(this.strike.getViewers(), this.strike.getSurface(), 3F, 0.6F);
        CompSound.FIZZ.play(this.strike.getViewers(), this.strike.getSurface(), 3F, 0.9F);
        CompSound.LAVA_POP.play(this.strike.getViewers(), this.strike.getSurface(), 2F, 0.5F);

        this.lava.clear();
        this.informed.clear();
        this.removeBombs();
    }

    @Override
    protected void onCleared(final SavedBlock block, final int index) {
        if (index % 3 == 0)
            CompParticle.SMOKE.spawn(block.getCenter(), 2, 0.3, 0.02);
    }

    @Override
    void abort() {
        for (final Player player : this.informed.values())
            if (player.isOnline())
                for (final SavedBlock block : this.lava)
                    BlockLook.fromBlock(block.getBlock()).sendTo(player, block.getBlock().getLocation());

        this.lava.clear();
        this.informed.clear();
        this.removeBombs();
    }

    private void placeLining(final int age) {
        final int target               = share(this.lining.size(), age, LINING_TICKS);
        final ThreadLocalRandom random = ThreadLocalRandom.current();

        while (this.lined < target && System.nanoTime() < this.strike.getDeadline()) {
            final SavedBlock block    = this.lining.get(this.lined++);
            final boolean underLiquid = BlockLook.isLiquid(block.getBlock().getRelative(0, 1, 0).getType());

            this.place(block, underLiquid ? CompMaterial.OBSIDIAN.getLook() : CRUST[random.nextInt(CRUST.length)].getLook());

            if (this.lined % 4 == 0)
                CompParticle.FLAME.spawn(block.getCenter().add(0, 0.6, 0), 2, 0.3, 0.02);
        }
    }

    private void raiseLayer() {
        final List<SavedBlock> layer = this.layers.get(this.raised++);
        final BlockLook molten       = CompMaterial.LAVA.getLook();

        for (final Player player : this.strike.getViewers()) {
            final boolean known = this.informed.containsKey(player.getUniqueId());

            for (final SavedBlock block : known ? layer : this.lava)
                molten.sendTo(player, block.getBlock().getLocation());

            if (!known)
                for (final SavedBlock block : layer)
                    molten.sendTo(player, block.getBlock().getLocation());

            this.informed.put(player.getUniqueId(), player);
        }

        this.lava.addAll(layer);
        this.surface = layer;

        final Location center = this.strike.getSurface();

        CompSound.LAVA_POP.play(this.strike.getViewers(), center, 2F, 0.6F + this.raised * 0.1F);
        CompSound.BURN.play(this.strike.getViewers(), center, 2F, 0.8F);
    }

    private void informNewViewers() {
        final BlockLook molten = CompMaterial.LAVA.getLook();

        for (final Player player : this.strike.getViewers())
            if (!this.informed.containsKey(player.getUniqueId())) {
                for (final SavedBlock block : this.lava)
                    molten.sendTo(player, block.getBlock().getLocation());

                this.informed.put(player.getUniqueId(), player);
            }
    }

    private void tickBombs(final int age) {
        final ThreadLocalRandom random = ThreadLocalRandom.current();

        for (final Iterator<MovingBlock> iterator = this.bombs.iterator(); iterator.hasNext();) {
            final MovingBlock bomb = iterator.next();
            final int bombAge      = this.bombAges.get(bomb) + 1;

            this.bombAges.put(bomb, bombAge);

            if (bomb.isAlive() && bombAge < BOMB_LIFETIME) {
                final Location location = bomb.getLocation().add(0, 0.5, 0);

                CompParticle.FLAME.spawn(location, 3, 0.25, 0.02);
                CompParticle.LARGE_SMOKE.spawn(location, 1, 0.2, 0.02);

                continue;
            }

            final Location location = bomb.getLocation().add(0, 0.4, 0);

            CompParticle.LAVA.spawn(location, 6, 0.4, 0);
            CompParticle.LARGE_SMOKE.spawn(location, 3, 0.4, 0.04);
            CompSound.FIZZ.play(this.strike.getViewers(), location, 1.2F, 0.8F + random.nextFloat() * 0.4F);

            bomb.remove();

            this.bombAges.remove(bomb);

            iterator.remove();
        }

        if (age < 25 || age > 85 || age % 7 != 0 || this.surface.isEmpty())
            return;

        for (int count = 0; count < 1 + random.nextInt(2); count++) {
            final SavedBlock origin = this.surface.get(random.nextInt(this.surface.size()));
            final MovingBlock bomb  = MovingBlock.spawn(origin.getBlock().getLocation().add(0.5, 1, 0.5), CompMaterial.MAGMA_BLOCK.getLook());

            if (bomb == null)
                continue;

            bomb.launch(new Vector(random.nextDouble(-0.18, 0.18), 0.55 + random.nextDouble() * 0.25, random.nextDouble(-0.18, 0.18)));

            this.bombs.add(bomb);
            this.bombAges.put(bomb, 0);

            CompSound.BLAZE_SHOOT.play(this.strike.getViewers(), origin.getCenter(), 1.5F, 0.6F + random.nextFloat() * 0.3F);
        }
    }

    private void showMolten(final int age) {
        if (age < 4 || this.surface.isEmpty())
            return;

        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final Element element          = Element.FIRE;

        for (int index = 0; index < 3; index++) {
            final Location top = this.surface.get(random.nextInt(this.surface.size())).getCenter().add(0, 0.6, 0);

            CompParticle.LAVA.spawn(top, 1, 0.3, 0);
            CompParticle.FLAME.shoot(top, random.nextDouble(-0.1, 0.1), 1, random.nextDouble(-0.1, 0.1), 0.12);
            CompParticle.dust(top.add(0, 1 + random.nextDouble() * 2, 0), element.getRandomColor(), 1.4F, 1, 0.8);
        }

        if (!this.lining.isEmpty())
            for (int index = 0; index < 3; index++) {
                final Location wall = this.lining.get(random.nextInt(this.lining.size())).getCenter().add(0, 0.6, 0);

                CompParticle.FLAME.spawn(wall, 1, 0.25, 0.01);

                if (index == 0)
                    CompParticle.DRIP_LAVA.spawn(wall, 1, 0.3, 0);
            }

        if (age % 3 == 0)
            CompParticle.SMOKE_COLUMN.shoot(this.surface.get(random.nextInt(this.surface.size())).getCenter().add(0, 0.8, 0), 0, 1, 0, 0.06);

        if (age % 3 == 1)
            CompSound.LAVA_POP.play(this.strike.getViewers(), this.surface.get(random.nextInt(this.surface.size())).getCenter(), 1.5F, 0.7F + random.nextFloat() * 0.6F);

        if (age % 20 == 0)
            CompSound.LAVA_AMBIENT.play(this.strike.getViewers(), this.strike.getSurface(), 2.5F, 0.7F + random.nextFloat() * 0.3F);

        if (age % 15 == 5)
            CompSound.BURN.play(this.strike.getViewers(), this.strike.getSurface(), 2F, 0.8F);
    }

    private void removeBombs() {
        for (final MovingBlock bomb : this.bombs)
            bomb.remove();

        this.bombs.clear();
        this.bombAges.clear();
    }
}
