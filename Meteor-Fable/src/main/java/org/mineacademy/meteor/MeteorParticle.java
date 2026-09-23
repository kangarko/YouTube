package org.mineacademy.meteor;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.material.MaterialData;

public enum MeteorParticle {

    FLAME(new String[] { "FLAME" }, "FLAME"),
    SMALL_FLAME(new String[] { "SMALL_FLAME", "FLAME" }, "FLAME"),
    SOUL_FIRE_FLAME(new String[] { "SOUL_FIRE_FLAME", "FLAME" }, "FLAME"),
    LAVA(new String[] { "LAVA" }, "LAVA_POP"),
    SMOKE(new String[] { "SMOKE", "SMOKE_NORMAL" }, "PARTICLE_SMOKE"),
    LARGE_SMOKE(new String[] { "LARGE_SMOKE", "SMOKE_LARGE" }, "LARGE_SMOKE"),
    CAMPFIRE_SMOKE(new String[] { "CAMPFIRE_COSY_SMOKE", "LARGE_SMOKE", "SMOKE_LARGE" }, "LARGE_SMOKE"),
    EXPLOSION(new String[] { "EXPLOSION", "EXPLOSION_LARGE" }, "EXPLOSION_LARGE"),
    EXPLOSION_EMITTER(new String[] { "EXPLOSION_EMITTER", "EXPLOSION_HUGE" }, "EXPLOSION_HUGE"),
    POOF(new String[] { "POOF", "EXPLOSION_NORMAL" }, "EXPLOSION"),
    CLOUD(new String[] { "CLOUD" }, "CLOUD"),
    FIREWORK(new String[] { "FIREWORK", "FIREWORKS_SPARK" }, "FIREWORKS_SPARK"),
    CRIT(new String[] { "CRIT" }, "CRIT"),
    ENCHANTED_HIT(new String[] { "ENCHANTED_HIT", "CRIT_MAGIC" }, "MAGIC_CRIT"),
    PORTAL(new String[] { "PORTAL" }, "PORTAL"),
    REVERSE_PORTAL(new String[] { "REVERSE_PORTAL", "PORTAL" }, "PORTAL"),
    WITCH(new String[] { "WITCH", "SPELL_WITCH" }, "WITCH_MAGIC"),
    ENCHANT(new String[] { "ENCHANT", "ENCHANTMENT_TABLE" }, "FLYING_GLYPH"),
    END_ROD(new String[] { "END_ROD", "FIREWORKS_SPARK" }, "FIREWORKS_SPARK"),
    DUST(new String[] { "DUST", "REDSTONE" }, "COLOURED_DUST"),
    SNOWFLAKE(new String[] { "SNOWFLAKE", "SNOW_SHOVEL" }, "SNOW_SHOVEL"),
    SNOWBALL(new String[] { "ITEM_SNOWBALL", "SNOWBALL" }, "SNOWBALL_BREAK"),
    DRAGON_BREATH(new String[] { "DRAGON_BREATH", "WITCH", "SPELL_WITCH" }, "WITCH_MAGIC"),
    WHITE_ASH(new String[] { "WHITE_ASH", "CLOUD" }, "CLOUD"),
    SQUID_INK(new String[] { "SQUID_INK", "LARGE_SMOKE", "SMOKE_LARGE" }, "LARGE_SMOKE"),
    BLOCK(new String[] { "BLOCK", "BLOCK_CRACK" }, "TILE_BREAK"),
    GLOW(new String[] { "GLOW", "END_ROD", "FIREWORKS_SPARK" }, "FIREWORKS_SPARK"),
    ELECTRIC_SPARK(new String[] { "ELECTRIC_SPARK", "CRIT" }, "CRIT"),
    SONIC_BOOM(new String[] { "SONIC_BOOM", "EXPLOSION", "EXPLOSION_LARGE" }, "EXPLOSION_LARGE"),
    FLASH(new String[] { "FLASH", "EXPLOSION_EMITTER", "EXPLOSION_HUGE" }, "EXPLOSION_HUGE"),
    GUST_EMITTER(new String[] { "GUST_EMITTER_LARGE", "EXPLOSION_EMITTER", "EXPLOSION_HUGE" }, "EXPLOSION_HUGE"),
    DUST_PLUME(new String[] { "DUST_PLUME", "CLOUD" }, "CLOUD"),
    SOUL(new String[] { "SOUL", "PORTAL" }, "PORTAL"),
    TOTEM(new String[] { "TOTEM_OF_UNDYING", "TOTEM", "FIREWORKS_SPARK" }, "FIREWORKS_SPARK");

    private static final int LEGACY_RADIUS = 160;

    private static boolean modern;
    private static Class<?> particleClass;
    private static Method spawnMethod;
    private static boolean spawnHasForce;
    private static Method getDataType;
    private static Class<?> dustOptionsClass;
    private static Constructor<?> dustOptions;
    private static Class<?> blockDataClass;
    private static Method createBlockData;
    private static Method blockGetBlockData;

    private final String[] modernNames;
    private final String legacyName;
    private Object particle;
    private Class<?> dataType;
    private Effect effect;

    MeteorParticle(String[] modernNames, String legacyName) {
        this.modernNames = modernNames;
        this.legacyName = legacyName;
    }

    public void spawn(Location location, int count, double spreadX, double spreadY, double spreadZ, double speed) {
        if (modern) {
            this.spawnModern(location, count, spreadX, spreadY, spreadZ, speed, null);

            return;
        }

        this.spawnLegacy(location, 0, 0, (float) spreadX, (float) spreadY, (float) spreadZ, (float) speed, count);
    }

    public void spawn(Location location, int count, double spread, double speed) {
        this.spawn(location, count, spread, spread, spread, speed);
    }

    public void spawnDust(Location location, Color color, float size, int count, double spread) {
        if (this != DUST)
            throw new IllegalArgumentException("Report / Only DUST accepts a color, not " + this);

        if (modern && this.dataType == dustOptionsClass) {
            Object options;

            try {
                options = dustOptions.newInstance(color, size);

            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("Report / Cannot create dust options", ex);
            }

            this.spawnModern(location, count, spread, spread, spread, 0, options);

            return;
        }

        float red = Math.max(0.001F, color.getRed() / 255F);
        float green = color.getGreen() / 255F;
        float blue = color.getBlue() / 255F;

        for (int i = 0; i < count; i++) {
            Location point = spread == 0 ? location : location.clone().add(spreadOffset(spread), spreadOffset(spread), spreadOffset(spread));

            if (modern)
                this.spawnModern(point, 0, red, green, blue, 1, null);
            else
                this.spawnLegacy(point, 0, 0, red, green, blue, 1, 0);
        }
    }

    public void spawnBlock(Location location, Block block, int count, double spread, double speed) {
        if (!modern) {
            this.spawnLegacy(location, block.getType().getId(), block.getData(), (float) spread, (float) spread, (float) spread, (float) speed, count);

            return;
        }

        if (this.dataType != blockDataClass) {
            this.spawnBlock(location, block.getType(), count, spread, speed);

            return;
        }

        this.spawnBlockData(location, block, count, spread, speed);
    }

    public void spawnBlock(Location location, Material material, int count, double spread, double speed) {
        if (this != BLOCK)
            throw new IllegalArgumentException("Report / Only BLOCK accepts a material, not " + this);

        if (!material.isBlock())
            return;

        if (!modern) {
            this.spawnLegacy(location, material.getId(), 0, (float) spread, (float) spread, (float) spread, (float) speed, count);

            return;
        }

        Object blockData;

        if (this.dataType == blockDataClass)
            try {
                blockData = createBlockData.invoke(null, material);

            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("Report / Cannot create block data for " + material, ex);
            }
        else
            blockData = new MaterialData(material, (byte) 0);

        this.spawnModern(location, count, spread, spread, spread, speed, blockData);
    }

    private void spawnBlockData(Location location, Block block, int count, double spread, double speed) {
        Object blockData;

        try {
            blockData = blockGetBlockData.invoke(block);

        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Report / Cannot read block data at " + location, ex);
        }

        this.spawnModern(location, count, spread, spread, spread, speed, blockData);
    }

    private void spawnModern(Location location, int count, double spreadX, double spreadY, double spreadZ, double speed, Object data) {
        if (data == null && this.dataType == Color.class)
            data = Color.WHITE;

        try {
            if (spawnHasForce)
                spawnMethod.invoke(location.getWorld(), this.particle, location, count, spreadX, spreadY, spreadZ, speed, data, true);
            else
                spawnMethod.invoke(location.getWorld(), this.particle, location, count, spreadX, spreadY, spreadZ, speed, data);

        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalStateException("Report / Cannot spawn particle " + this, ex);
        }
    }

    private void spawnLegacy(Location location, int id, int data, float spreadX, float spreadY, float spreadZ, float speed, int count) {
        World world = location.getWorld();

        world.spigot().playEffect(location, this.effect, id, data, spreadX, spreadY, spreadZ, speed, count, LEGACY_RADIUS);
    }

    private void resolve() {
        if (!modern) {
            this.effect = Effect.valueOf(this.legacyName);

            return;
        }

        for (String candidate : this.modernNames) {
            Object found;

            try {
                found = particleClass.getField(candidate).get(null);

            } catch (NoSuchFieldException ex) {
                continue;

            } catch (IllegalAccessException ex) {
                throw new IllegalStateException("Report / Cannot read particle " + candidate, ex);
            }

            Class<?> type;

            try {
                type = (Class<?>) getDataType.invoke(found);

            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new IllegalStateException("Report / Cannot read data type of particle " + candidate, ex);
            }

            if (!this.accepts(type))
                continue;

            this.particle = found;
            this.dataType = type;

            return;
        }

        throw new IllegalStateException("Report / None of the particles " + String.join(", ", this.modernNames) + " exist on this server without unsupported data");
    }

    private boolean accepts(Class<?> type) {
        if (type == Void.class || type == Color.class)
            return true;

        if (this == DUST)
            return type == dustOptionsClass;

        if (this == BLOCK)
            return type == blockDataClass || type == MaterialData.class;

        return false;
    }

    private static double spreadOffset(double spread) {
        return (Math.random() * 2 - 1) * spread;
    }

    public static boolean isModern() {
        return modern;
    }

    public static void load() {
        try {
            particleClass = Class.forName("org.bukkit.Particle");
            modern = true;

        } catch (ClassNotFoundException ex) {
            modern = false;
        }

        if (modern)
            loadModern();

        for (MeteorParticle particle : values())
            particle.resolve();
    }

    private static void loadModern() {
        try {
            getDataType = particleClass.getMethod("getDataType");

            try {
                spawnMethod = World.class.getMethod("spawnParticle", particleClass, Location.class, int.class, double.class, double.class, double.class, double.class, Object.class, boolean.class);
                spawnHasForce = true;

            } catch (NoSuchMethodException ex) {
                spawnMethod = World.class.getMethod("spawnParticle", particleClass, Location.class, int.class, double.class, double.class, double.class, double.class, Object.class);
                spawnHasForce = false;
            }

            try {
                dustOptionsClass = Class.forName("org.bukkit.Particle$DustOptions");
                dustOptions = dustOptionsClass.getConstructor(Color.class, float.class);

            } catch (ClassNotFoundException ex) {
                dustOptionsClass = null;
                dustOptions = null;
            }

            try {
                blockDataClass = Class.forName("org.bukkit.block.data.BlockData");
                createBlockData = Bukkit.class.getMethod("createBlockData", Material.class);
                blockGetBlockData = Block.class.getMethod("getBlockData");

            } catch (ClassNotFoundException ex) {
                blockDataClass = null;
                createBlockData = null;
                blockGetBlockData = null;
            }

        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException("Report / Particle API of this server is unsupported", ex);
        }
    }
}
