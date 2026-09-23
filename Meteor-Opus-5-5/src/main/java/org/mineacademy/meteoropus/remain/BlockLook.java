package org.mineacademy.meteoropus.remain;

import java.util.EnumMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;

public final class BlockLook {

    private static final Map<Material, CompMaterial> STAND_INS = new EnumMap<>(Material.class);
    private static final Map<Material, Boolean> SHAPELESS      = new EnumMap<>(Material.class);

    private final Material material;
    private final byte data;
    private final Object blockData;

    private BlockLook(final Material material, final byte data, final Object blockData) {
        this.material  = material;
        this.data      = data;
        this.blockData = blockData;
    }

    public Material getMaterial() {
        return this.material;
    }

    public byte getData() {
        return this.data;
    }

    public Object getBlockData() {
        return this.blockData;
    }

    public FallingBlock spawnFallingBlock(final Location location) {
        if (this.blockData != null)
            return Remain.spawnFallingBlock(location, this.blockData);

        return this.spawnLegacyFallingBlock(location);
    }

    public void place(final Block block) {
        if (this.blockData != null) {
            Remain.setBlockData(block, this.blockData);

            return;
        }

        this.placeLegacy(block);
    }

    public void sendTo(final Player player, final Location location) {
        if (this.blockData != null) {
            Remain.sendBlockChange(player, location, this.blockData);

            return;
        }

        this.sendLegacy(player, location);
    }

    @SuppressWarnings("deprecation")
    private FallingBlock spawnLegacyFallingBlock(final Location location) {
        return location.getWorld().spawnFallingBlock(location, this.material, this.data);
    }

    @SuppressWarnings("deprecation")
    private void placeLegacy(final Block block) {
        block.setTypeIdAndData(this.material.getId(), this.data, false);
    }

    @SuppressWarnings("deprecation")
    private void sendLegacy(final Player player, final Location location) {
        player.sendBlockChange(location, this.material, this.data);
    }

    public static BlockLook fromMaterial(final Material material, final byte data) {
        if (Remain.hasBlockData())
            return new BlockLook(material, (byte) 0, Remain.createBlockData(material));

        return new BlockLook(material, data, null);
    }

    @SuppressWarnings("deprecation")
    public static BlockLook fromState(final BlockState state) {
        if (Remain.hasBlockData())
            return new BlockLook(state.getType(), (byte) 0, Remain.getBlockData(state));

        return new BlockLook(state.getType(), state.getRawData(), null);
    }

    @SuppressWarnings("deprecation")
    public static BlockLook fromBlock(final Block block) {
        if (Remain.hasBlockData())
            return new BlockLook(block.getType(), (byte) 0, Remain.getBlockData(block));

        return new BlockLook(block.getType(), block.getData(), null);
    }

    public static BlockLook findFlyingLook(final BlockState state) {
        final Material type = state.getType();

        if (isShapeless(type))
            return null;

        final CompMaterial standIn = findStandIn(type);

        return standIn != null ? standIn.getLook() : fromState(state);
    }

    public static boolean isShapeless(final Material type) {
        Boolean shapeless = SHAPELESS.get(type);

        if (shapeless == null) {
            final String name = type.name();

            shapeless = Remain.isAir(type) || name.endsWith("WATER") || name.endsWith("LAVA") || name.equals("BUBBLE_COLUMN") || name.equals("BARRIER") || name.equals("LIGHT")
                    || name.equals("STRUCTURE_VOID") || name.equals("MOVING_PISTON") || name.equals("PISTON_MOVING_PIECE") || name.contains("PORTAL") && !name.contains("FRAME") || name.equals("END_GATEWAY") || !type.isBlock();

            SHAPELESS.put(type, shapeless);
        }

        return shapeless;
    }

    public static boolean isLiquid(final Material type) {
        final String name = type.name();

        return name.endsWith("WATER") || name.endsWith("LAVA");
    }

    private static CompMaterial findStandIn(final Material type) {
        if (STAND_INS.containsKey(type))
            return STAND_INS.get(type);

        final String name = type.name();
        final CompMaterial standIn;

        if (name.equals("ENDER_CHEST"))
            standIn = CompMaterial.OBSIDIAN;

        else if (name.contains("CHEST") || name.contains("SIGN"))
            standIn = CompMaterial.OAK_PLANKS;

        else if (name.contains("BANNER") || name.endsWith("BED") || name.equals("BED_BLOCK"))
            standIn = CompMaterial.WHITE_WOOL;

        else if (name.contains("SKULL") || name.endsWith("_HEAD") && !name.startsWith("PISTON"))
            standIn = CompMaterial.BONE_BLOCK;

        else if (name.contains("SHULKER_BOX"))
            standIn = CompMaterial.PURPUR_BLOCK;

        else if (name.equals("CONDUIT") || name.equals("DECORATED_POT"))
            standIn = CompMaterial.TERRACOTTA;

        else
            standIn = null;

        STAND_INS.put(type, standIn);

        return standIn;
    }
}
