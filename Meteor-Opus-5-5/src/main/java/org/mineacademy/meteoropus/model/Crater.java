package org.mineacademy.meteoropus.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.mineacademy.meteoropus.remain.Remain;

public final class Crater {

    private static final int[][] SIDES   = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };
    private static final int LEAF_MARGIN = 5;

    private final World world;
    private final int centerX;
    private final int centerY;
    private final int centerZ;
    private final int radius;
    private final List<SavedBlock> blocks          = new ArrayList<>();
    private final Map<Long, SavedBlock> byPosition = new HashMap<>();
    private final List<SavedBlock> leaves          = new ArrayList<>();
    private int carved;

    Crater(final Block target, final int radius) {
        this.world   = target.getWorld();
        this.centerX = target.getX();
        this.centerY = target.getY();
        this.centerZ = target.getZ();
        this.radius  = radius;

        final int minY = Remain.getMinHeight(this.world);
        final int maxY = this.world.getMaxHeight() - 1;

        final int reach = radius + LEAF_MARGIN;

        for (int x = -reach; x <= reach; x++)
            for (int y = -reach; y <= reach; y++)
                for (int z = -reach; z <= reach; z++) {
                    final int distanceSquared = x * x + y * y + z * z;
                    final int blockY          = this.centerY + y;

                    if (distanceSquared > reach * reach || blockY < minY || blockY > maxY)
                        continue;

                    final Block block = this.world.getBlockAt(this.centerX + x, blockY, this.centerZ + z);

                    if (distanceSquared <= radius * radius) {
                        final SavedBlock saved = new SavedBlock(block, distanceSquared);

                        saved.capture();

                        this.blocks.add(saved);
                        this.byPosition.put(toKey(saved.getX(), saved.getY(), saved.getZ()), saved);

                    } else if (block.getType().name().contains("LEAVES")) {
                        final SavedBlock saved = new SavedBlock(block, distanceSquared);

                        saved.capture();

                        this.leaves.add(saved);
                    }
                }

        Collections.shuffle(this.blocks, ThreadLocalRandom.current());

        this.blocks.sort((first, second) -> Integer.compare(first.getDistanceSquared(), second.getDistanceSquared()));
    }

    SavedBlock carveNext() {
        return this.blocks.get(this.carved++);
    }

    boolean isCarved() {
        return this.carved >= this.blocks.size();
    }

    void markShell() {
        for (final SavedBlock block : this.blocks)
            for (final int[] side : SIDES) {
                final int x = block.getX() + side[0];
                final int y = block.getY() + side[1];
                final int z = block.getZ() + side[2];

                if (!this.contains(x, y, z) && this.world.getBlockAt(x, y, z).getType().isSolid()) {
                    block.setShell(true);

                    break;
                }
            }
    }

    void restoreAll(final Consumer<SavedBlock> beforeRestore) {
        final List<SavedBlock> ordered = new ArrayList<>(this.blocks);

        ordered.sort((first, second) -> Integer.compare(first.getY(), second.getY()));

        for (final SavedBlock block : ordered)
            if (block.isModified()) {
                beforeRestore.accept(block);
                block.restore();
            }

        this.reconcile();
    }

    void reconcile() {
        for (final SavedBlock block : this.blocks)
            if (block.isModified() || !block.isIntact())
                block.restore();

        for (final SavedBlock leaf : this.leaves)
            if (leaf.getBlock().getType() == leaf.getState().getType() && !leaf.isIntact())
                leaf.restore();
    }

    public SavedBlock find(final int x, final int y, final int z) {
        return this.byPosition.get(toKey(x, y, z));
    }

    public boolean contains(final int x, final int y, final int z) {
        return this.byPosition.containsKey(toKey(x, y, z));
    }

    public List<SavedBlock> getBlocks() {
        return this.blocks;
    }

    public int getRadius() {
        return this.radius;
    }

    public int getCenterY() {
        return this.centerY;
    }

    private static long toKey(final int x, final int y, final int z) {
        return ((long) x & 0x3FFFFFFL) << 38 | ((long) z & 0x3FFFFFFL) << 12 | (long) y & 0xFFFL;
    }
}
