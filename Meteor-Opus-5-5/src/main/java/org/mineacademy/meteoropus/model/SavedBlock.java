package org.mineacademy.meteoropus.model;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.mineacademy.meteoropus.remain.BlockLook;
import org.mineacademy.meteoropus.remain.Remain;

public final class SavedBlock {

    private final Block block;
    private final int distanceSquared;
    private BlockState state;
    private LegacyTile legacyTile;
    private boolean solid;
    private boolean modified;
    private boolean shell;

    SavedBlock(final Block block, final int distanceSquared) {
        this.block           = block;
        this.distanceSquared = distanceSquared;
    }

    void capture() {
        this.state = this.block.getState();
        this.solid = !Remain.isAir(this.state.getType());

        if (this.solid && !Remain.hasBlockData())
            this.legacyTile = LegacyTile.capture(this.state);
    }

    void clear() {
        this.block.setType(Material.AIR, false);
        this.modified = true;
    }

    void place(final BlockLook look) {
        look.place(this.block);

        this.modified = true;
    }

    void restore() {
        this.state.update(true, false);

        if (this.legacyTile != null)
            this.legacyTile.apply(this.block);

        this.modified = false;
    }

    @SuppressWarnings("deprecation")
    boolean isIntact() {
        if (Remain.hasBlockData())
            return Remain.getBlockData(this.block).equals(Remain.getBlockData(this.state));

        return this.block.getType() == this.state.getType() && this.block.getData() == this.state.getRawData();
    }

    public Block getBlock() {
        return this.block;
    }

    public Location getCenter() {
        return this.block.getLocation().add(0.5, 0.5, 0.5);
    }

    public int getX() {
        return this.block.getX();
    }

    public int getY() {
        return this.block.getY();
    }

    public int getZ() {
        return this.block.getZ();
    }

    public int getDistanceSquared() {
        return this.distanceSquared;
    }

    public BlockState getState() {
        return this.state;
    }

    public boolean isSolid() {
        return this.solid;
    }

    public boolean isModified() {
        return this.modified;
    }

    public boolean isShell() {
        return this.shell;
    }

    void setShell(final boolean shell) {
        this.shell = shell;
    }
}
