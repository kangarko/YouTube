package org.mineacademy.meteoropus.remain;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.Set;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

public final class Remain {

    private static final Class<?> BLOCK_DATA = findClass("org.bukkit.block.data.BlockData");

    private static final boolean LEGACY_SOUNDS = findClass("org.bukkit.Particle") == null;

    private static final Method STATE_GET_BLOCK_DATA       = BLOCK_DATA == null ? null : findMethod(BlockState.class, "getBlockData");
    private static final Method BLOCK_GET_BLOCK_DATA       = BLOCK_DATA == null ? null : findMethod(Block.class, "getBlockData");
    private static final Method BLOCK_SET_BLOCK_DATA       = BLOCK_DATA == null ? null : findMethod(Block.class, "setBlockData", BLOCK_DATA, boolean.class);
    private static final Method MATERIAL_CREATE_BLOCK_DATA = BLOCK_DATA == null ? null : findMethod(Material.class, "createBlockData");
    private static final Method WORLD_SPAWN_FALLING_BLOCK  = BLOCK_DATA == null ? null : findMethod(World.class, "spawnFallingBlock", Location.class, BLOCK_DATA);
    private static final Method PLAYER_SEND_BLOCK_CHANGE   = BLOCK_DATA == null ? null : findMethod(Player.class, "sendBlockChange", Location.class, BLOCK_DATA);

    private static final Method ENTITY_SET_GRAVITY      = findMethod(Entity.class, "setGravity", boolean.class);
    private static final Method ENTITY_SET_PERSISTENT   = findMethod(Entity.class, "setPersistent", boolean.class);
    private static final Method ENTITY_SET_FREEZE_TICKS = findMethod(Entity.class, "setFreezeTicks", int.class);
    private static final Method PLAYER_SEND_TITLE       = findMethod(Player.class, "sendTitle", String.class, String.class, int.class, int.class, int.class);
    private static final Method PLAYER_SET_TITLE_TIMES  = findMethod(Player.class, "setTitleTimes", int.class, int.class, int.class);
    private static final Method PLAYER_STOP_SOUND       = findMethod(Player.class, "stopSound", String.class);
    private static final Method META_SET_GLINT          = findMethod(ItemMeta.class, "setEnchantmentGlintOverride", Boolean.class);
    private static final Method CHUNK_ADD_TICKET        = findMethod(Chunk.class, "addPluginChunkTicket", Plugin.class);
    private static final Method CHUNK_REMOVE_TICKET     = findMethod(Chunk.class, "removePluginChunkTicket", Plugin.class);
    private static final Method WORLD_GET_MIN_HEIGHT    = findMethod(World.class, "getMinHeight");

    private static final Enchantment GLOW_ENCHANTMENT = META_SET_GLINT == null ? findEnchantment("UNBREAKING", "DURABILITY") : null;

    private static final Set<Material> AIR_TYPES = findAirTypes();

    private Remain() {
    }

    public static boolean hasBlockData() {
        return BLOCK_DATA != null;
    }

    public static boolean hasLegacySounds() {
        return LEGACY_SOUNDS;
    }

    public static boolean canToggleGravity() {
        return ENTITY_SET_GRAVITY != null;
    }

    public static boolean canSetPersistent() {
        return ENTITY_SET_PERSISTENT != null;
    }

    public static boolean canFreeze() {
        return ENTITY_SET_FREEZE_TICKS != null;
    }

    public static boolean hasChunkTickets() {
        return CHUNK_ADD_TICKET != null && CHUNK_REMOVE_TICKET != null;
    }

    public static boolean isAir(final Material material) {
        return AIR_TYPES.contains(material);
    }

    public static Object getBlockData(final BlockState state) {
        return invoke(STATE_GET_BLOCK_DATA, state);
    }

    public static Object getBlockData(final Block block) {
        return invoke(BLOCK_GET_BLOCK_DATA, block);
    }

    public static Object createBlockData(final Material material) {
        return invoke(MATERIAL_CREATE_BLOCK_DATA, material);
    }

    public static void setBlockData(final Block block, final Object blockData) {
        invoke(BLOCK_SET_BLOCK_DATA, block, blockData, false);
    }

    public static FallingBlock spawnFallingBlock(final Location location, final Object blockData) {
        return (FallingBlock) invoke(WORLD_SPAWN_FALLING_BLOCK, location.getWorld(), location, blockData);
    }

    public static void sendBlockChange(final Player player, final Location location, final Object blockData) {
        invoke(PLAYER_SEND_BLOCK_CHANGE, player, location, blockData);
    }

    public static void setGravity(final Entity entity, final boolean gravity) {
        invoke(ENTITY_SET_GRAVITY, entity, gravity);
    }

    public static void setPersistent(final Entity entity, final boolean persistent) {
        invoke(ENTITY_SET_PERSISTENT, entity, persistent);
    }

    public static void setFreezeTicks(final Entity entity, final int ticks) {
        invoke(ENTITY_SET_FREEZE_TICKS, entity, ticks);
    }

    public static void addChunkTicket(final Chunk chunk, final Plugin plugin) {
        invoke(CHUNK_ADD_TICKET, chunk, plugin);
    }

    public static void removeChunkTicket(final Chunk chunk, final Plugin plugin) {
        invoke(CHUNK_REMOVE_TICKET, chunk, plugin);
    }

    public static int getMinHeight(final World world) {
        if (WORLD_GET_MIN_HEIGHT == null)
            return 0;

        return (Integer) invoke(WORLD_GET_MIN_HEIGHT, world);
    }

    @SuppressWarnings("deprecation")
    public static void sendTitle(final Player player, final String title, final String subtitle, final int fadeIn, final int stay, final int fadeOut) {
        if (PLAYER_SEND_TITLE != null) {
            invoke(PLAYER_SEND_TITLE, player, title, subtitle, fadeIn, stay, fadeOut);

            return;
        }

        if (PLAYER_SET_TITLE_TIMES != null)
            invoke(PLAYER_SET_TITLE_TIMES, player, fadeIn, stay, fadeOut);

        player.sendTitle(title, subtitle);
    }

    public static void stopSound(final Player player, final String sound) {
        if (PLAYER_STOP_SOUND != null)
            invoke(PLAYER_STOP_SOUND, player, sound);
    }

    public static void setGlowing(final ItemMeta meta) {
        if (META_SET_GLINT != null) {
            invoke(META_SET_GLINT, meta, Boolean.TRUE);

            return;
        }

        meta.addEnchant(GLOW_ENCHANTMENT, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    }

    private static Object invoke(final Method method, final Object instance, final Object... arguments) {
        try {
            return method.invoke(instance, arguments);

        } catch (final InvocationTargetException ex) {
            throw new IllegalStateException("Calling " + method + " failed", ex.getCause());

        } catch (final IllegalAccessException ex) {
            throw new IllegalStateException("Cannot access " + method, ex);
        }
    }

    private static Set<Material> findAirTypes() {
        final Set<Material> types = EnumSet.noneOf(Material.class);

        for (final String name : new String[] { "AIR", "CAVE_AIR", "VOID_AIR" }) {
            final Material material = Material.getMaterial(name);

            if (material != null)
                types.add(material);
        }

        return types;
    }

    private static Enchantment findEnchantment(final String... names) {
        for (final String name : names)
            try {
                final Field field = Enchantment.class.getField(name);

                return (Enchantment) field.get(null);

            } catch (final NoSuchFieldException ex) {
            } catch (final IllegalAccessException ex) {
                throw new IllegalStateException("Cannot read enchantment " + name, ex);
            }

        throw new IllegalStateException("None of the glow enchantments exist on this server");
    }

    public static Class<?> findClass(final String name) {
        try {
            return Class.forName(name);

        } catch (final ClassNotFoundException ex) {
            return null;
        }
    }

    public static Method findMethod(final Class<?> owner, final String name, final Class<?>... parameters) {
        try {
            return owner.getMethod(name, parameters);

        } catch (final NoSuchMethodException ex) {
            return null;
        }
    }
}
