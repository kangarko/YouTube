package org.mineacademy.meteor;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

final class Compat {

    private static Method setGravity;
    private static Method setCancelDrop;
    private static Method getMinHeight;
    private static Method addChunkTicket;
    private static Method removeChunkTickets;
    private static Method sendTitleTimed;
    private static Method sendPaperTitle;
    private static Constructor<?> paperTitle;
    private static Method getBlockData;
    private static Method createBlockData;
    private static Method spawnFallingBlockData;
    private static Enchantment glint;

    private Compat() {
    }

    static boolean isAir(Material material) {
        return material == Material.AIR || material.name().endsWith("_AIR");
    }

    static boolean canControlGravity() {
        return setGravity != null;
    }

    static void setGravity(Entity entity, boolean gravity) {
        if (setGravity == null)
            return;

        invoke(setGravity, entity, gravity);
    }

    static FallingBlock spawnFallingBlock(Location location, Material material) {
        FallingBlock falling;

        if (spawnFallingBlockData != null)
            falling = (FallingBlock) invoke(spawnFallingBlockData, location.getWorld(), location, invoke(createBlockData, null, material));
        else
            falling = location.getWorld().spawnFallingBlock(location, material, (byte) 0);

        prepare(falling, location);

        return falling;
    }

    static FallingBlock spawnFallingBlock(Location location, BlockState state) {
        FallingBlock falling;

        if (spawnFallingBlockData != null)
            falling = (FallingBlock) invoke(spawnFallingBlockData, location.getWorld(), location, invoke(getBlockData, state));
        else
            falling = location.getWorld().spawnFallingBlock(location, state.getType(), state.getRawData());

        prepare(falling, location);

        return falling;
    }

    static int getMinHeight(World world) {
        if (getMinHeight == null)
            return 0;

        return (Integer) invoke(getMinHeight, world);
    }

    static void holdChunk(World world, int chunkX, int chunkZ, Plugin plugin) {
        if (addChunkTicket != null) {
            invoke(addChunkTicket, world, chunkX, chunkZ, plugin);

            return;
        }

        world.loadChunk(chunkX, chunkZ);
    }

    static void releaseChunks(World world, Plugin plugin) {
        if (removeChunkTickets != null)
            invoke(removeChunkTickets, world, plugin);
    }

    static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (sendTitleTimed != null) {
            invoke(sendTitleTimed, player, title, subtitle, fadeIn, stay, fadeOut);

            return;
        }

        if (paperTitle != null) {
            Object paper;

            try {
                paper = paperTitle.newInstance(title, subtitle, fadeIn, stay, fadeOut);

            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("Report / Cannot create a title", ex);
            }

            invoke(sendPaperTitle, player, paper);

            return;
        }

        player.sendTitle(title, subtitle);
    }

    static Enchantment getGlint() {
        return glint;
    }

    static void load() {
        setGravity = findMethod(Entity.class, "setGravity", boolean.class);
        setCancelDrop = findMethod(FallingBlock.class, "setCancelDrop", boolean.class);
        getMinHeight = findMethod(World.class, "getMinHeight");
        addChunkTicket = findMethod(World.class, "addPluginChunkTicket", int.class, int.class, Plugin.class);
        removeChunkTickets = findMethod(World.class, "removePluginChunkTickets", Plugin.class);
        sendTitleTimed = findMethod(Player.class, "sendTitle", String.class, String.class, int.class, int.class, int.class);
        glint = findEnchantment("UNBREAKING", "DURABILITY");

        loadPaperTitle();
        loadBlockData();
    }

    private static void loadPaperTitle() {
        if (sendTitleTimed != null)
            return;

        Class<?> titleClass;

        try {
            titleClass = Class.forName("org.github.paperspigot.Title");

        } catch (ClassNotFoundException ex) {
            return;
        }

        try {
            paperTitle = titleClass.getConstructor(String.class, String.class, int.class, int.class, int.class);
            sendPaperTitle = Player.class.getMethod("sendTitle", titleClass);

        } catch (NoSuchMethodException ex) {
            paperTitle = null;
            sendPaperTitle = null;
        }
    }

    private static void loadBlockData() {
        Class<?> blockDataClass;

        try {
            blockDataClass = Class.forName("org.bukkit.block.data.BlockData");

        } catch (ClassNotFoundException ex) {
            return;
        }

        getBlockData = findMethod(BlockState.class, "getBlockData");
        createBlockData = findMethod(Bukkit.class, "createBlockData", Material.class);
        spawnFallingBlockData = findMethod(World.class, "spawnFallingBlock", Location.class, blockDataClass);

        if (getBlockData == null || createBlockData == null || spawnFallingBlockData == null)
            throw new IllegalStateException("Report / Block data API of this server is incomplete");
    }

    private static void prepare(FallingBlock falling, Location location) {
        if (spawnFallingBlockData == null)
            falling.teleport(location);

        falling.setDropItem(false);
        falling.setHurtEntities(false);

        if (setCancelDrop != null)
            invoke(setCancelDrop, falling, true);
    }

    private static Method findMethod(Class<?> owner, String name, Class<?>... parameters) {
        try {
            return owner.getMethod(name, parameters);

        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    private static Enchantment findEnchantment(String... candidates) {
        for (String candidate : candidates)
            try {
                return (Enchantment) Enchantment.class.getField(candidate).get(null);

            } catch (NoSuchFieldException ex) {
                continue;

            } catch (IllegalAccessException ex) {
                throw new IllegalStateException("Report / Cannot read enchantment " + candidate, ex);
            }

        throw new IllegalStateException("Report / None of the enchantments " + String.join(", ", candidates) + " exist on this server");
    }

    private static Object invoke(Method method, Object target, Object... arguments) {
        try {
            return method.invoke(target, arguments);

        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalStateException("Report / Cannot call " + method.getName(), ex);
        }
    }
}
