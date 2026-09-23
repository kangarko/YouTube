package org.mineacademy.meteor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

final class Crater {

    private final World world;
    private final int centerX;
    private final int centerY;
    private final int centerZ;
    private final int radius;
    private final int floorY;
    private final List<BlockRecord> records = new ArrayList<>();
    private final List<BlockRecord> inner = new ArrayList<>();
    private final List<BlockRecord> shell = new ArrayList<>();
    private Material dominant = Material.STONE;
    private boolean captured;

    Crater(Block target, int radius) {
        this.world = target.getWorld();
        this.centerX = target.getX();
        this.centerY = target.getY();
        this.centerZ = target.getZ();
        this.radius = radius;
        this.floorY = Compat.getMinHeight(this.world);
    }

    int getCenterX() {
        return this.centerX;
    }

    int getCenterY() {
        return this.centerY;
    }

    int getCenterZ() {
        return this.centerZ;
    }

    List<BlockRecord> getInner() {
        return this.inner;
    }

    List<BlockRecord> getShell() {
        return this.shell;
    }

    Material getDominant() {
        return this.dominant;
    }

    int distanceSquared(int x, int y, int z) {
        int dx = x - this.centerX;
        int dy = y - this.centerY;
        int dz = z - this.centerZ;

        return dx * dx + dy * dy + dz * dz;
    }

    int distanceSquared(BlockRecord record) {
        return this.distanceSquared(record.getX(), record.getY(), record.getZ());
    }

    boolean isInside(int x, int y, int z) {
        return this.distanceSquared(x, y, z) <= this.radius * this.radius;
    }

    boolean isFloor(BlockRecord record) {
        return record.getY() <= this.floorY;
    }

    int getBottomY() {
        return Math.max(this.centerY - this.radius, this.floorY + 1);
    }

    void capture() {
        int regionRadius = this.radius + 1;
        int regionSquared = regionRadius * regionRadius;
        int innerSquared = this.radius * this.radius;
        int minY = Math.max(this.floorY, this.centerY - regionRadius);
        int maxY = Math.min(this.world.getMaxHeight() - 1, this.centerY + regionRadius);
        Map<Material, Integer> counts = new HashMap<>();

        for (int y = minY; y <= maxY; y++)
            for (int x = this.centerX - regionRadius; x <= this.centerX + regionRadius; x++)
                for (int z = this.centerZ - regionRadius; z <= this.centerZ + regionRadius; z++) {
                    int distance = this.distanceSquared(x, y, z);

                    if (distance > regionSquared)
                        continue;

                    BlockRecord record = new BlockRecord(this.world.getBlockAt(x, y, z));

                    this.records.add(record);

                    if (distance > innerSquared) {
                        this.shell.add(record);

                        continue;
                    }

                    this.inner.add(record);

                    if (record.isOccluding())
                        counts.merge(record.getType(), 1, Integer::sum);
                }

        this.inner.sort((first, second) -> Integer.compare(this.distanceSquared(first), this.distanceSquared(second)));

        int best = 0;

        for (Map.Entry<Material, Integer> entry : counts.entrySet())
            if (entry.getValue() > best) {
                best = entry.getValue();
                this.dominant = entry.getKey();
            }

        this.captured = true;
    }

    void restoreAll() {
        if (!this.captured)
            return;

        for (BlockRecord record : this.sortedBottomUp())
            record.restore();
    }

    List<BlockRecord> sortedBottomUp() {
        List<BlockRecord> sorted = new ArrayList<>(this.records);

        sorted.sort((first, second) -> {
            if (first.getY() != second.getY())
                return Integer.compare(first.getY(), second.getY());

            return Integer.compare(this.distanceSquared(first), this.distanceSquared(second));
        });

        return sorted;
    }
}
