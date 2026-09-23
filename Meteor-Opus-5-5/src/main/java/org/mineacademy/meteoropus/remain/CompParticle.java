package org.mineacademy.meteoropus.remain;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.material.MaterialData;

public enum CompParticle {

    FLAME(Kind.PLAIN, "FLAME", "FLAME"),
    SOUL_FLAME(Kind.PLAIN, "FLAME", "SOUL_FIRE_FLAME", "FLAME"),
    LAVA(Kind.PLAIN, "LAVA_POP", "LAVA"),
    DRIP_LAVA(Kind.PLAIN, "LAVADRIP", "FALLING_LAVA", "DRIPPING_LAVA", "DRIP_LAVA"),
    SMOKE(Kind.PLAIN, "PARTICLE_SMOKE", "SMOKE", "SMOKE_NORMAL"),
    LARGE_SMOKE(Kind.PLAIN, "LARGE_SMOKE", "LARGE_SMOKE", "SMOKE_LARGE"),
    SMOKE_COLUMN(Kind.PLAIN, "LARGE_SMOKE", "CAMPFIRE_SIGNAL_SMOKE", "LARGE_SMOKE", "SMOKE_LARGE"),
    EXPLOSION_EMITTER(Kind.PLAIN, "EXPLOSION_HUGE", "EXPLOSION_EMITTER", "EXPLOSION_HUGE"),
    EXPLOSION(Kind.PLAIN, "EXPLOSION_LARGE", "EXPLOSION", "EXPLOSION_LARGE"),
    POOF(Kind.PLAIN, "EXPLOSION", "POOF", "EXPLOSION_NORMAL"),
    FIREWORK(Kind.PLAIN, "FIREWORKS_SPARK", "FIREWORK", "FIREWORKS_SPARK"),
    END_ROD(Kind.PLAIN, "FIREWORKS_SPARK", "END_ROD", "FIREWORK", "FIREWORKS_SPARK"),
    FLASH(Kind.PLAIN, "FIREWORKS_SPARK", "FLASH", "FIREWORK", "FIREWORKS_SPARK"),
    ELECTRIC_SPARK(Kind.PLAIN, "FIREWORKS_SPARK", "ELECTRIC_SPARK", "FIREWORK", "FIREWORKS_SPARK"),
    CLOUD(Kind.PLAIN, "CLOUD", "CLOUD"),
    GUST(Kind.PLAIN, "CLOUD", "GUST", "CLOUD"),
    SNOWFLAKE(Kind.PLAIN, "SNOW_SHOVEL", "SNOWFLAKE", "SNOW_SHOVEL"),
    SNOWBALL(Kind.PLAIN, "SNOWBALL_BREAK", "ITEM_SNOWBALL", "SNOWBALL"),
    PORTAL(Kind.PLAIN, "PORTAL", "PORTAL"),
    REVERSE_PORTAL(Kind.PLAIN, "PORTAL", "REVERSE_PORTAL", "PORTAL"),
    WITCH(Kind.PLAIN, "WITCH_MAGIC", "WITCH", "SPELL_WITCH"),
    SQUID_INK(Kind.PLAIN, "LARGE_SMOKE", "SQUID_INK", "LARGE_SMOKE", "SMOKE_LARGE"),
    SONIC_BOOM(Kind.PLAIN, "EXPLOSION_LARGE", "SONIC_BOOM", "EXPLOSION", "EXPLOSION_LARGE"),
    ENCHANT(Kind.PLAIN, "FLYING_GLYPH", "ENCHANT", "ENCHANTMENT_TABLE"),
    CRIT_MAGIC(Kind.PLAIN, "MAGIC_CRIT", "ENCHANTED_HIT", "CRIT_MAGIC"),
    HAPPY_VILLAGER(Kind.PLAIN, "HAPPY_VILLAGER", "HAPPY_VILLAGER", "VILLAGER_HAPPY"),
    DUST(Kind.DUST, "COLOURED_DUST", "DUST", "REDSTONE"),
    BLOCK(Kind.BLOCK, "TILE_BREAK", "BLOCK", "BLOCK_CRACK");

    private static final int LEGACY_RADIUS = 192;

    private final Kind kind;
    private final Effect effect;
    private final Object particle;
    private final Class<?> dataType;

    CompParticle(final Kind kind, final String legacyName, final String... modernNames) {
        this.kind = kind;

        if (Api.PARTICLE == null) {
            this.effect   = Effect.valueOf(legacyName);
            this.particle = null;
            this.dataType = null;

            return;
        }

        final Object found = findParticle(kind, modernNames);

        this.effect   = null;
        this.particle = found;
        this.dataType = Api.getDataType(found);
    }

    public void spawn(final Location location, final int count, final double spread, final double speed) {
        this.spawn(location, count, spread, spread, spread, speed);
    }

    public void spawn(final Location location, final int count, final double offsetX, final double offsetY, final double offsetZ, final double speed) {
        this.checkKind(Kind.PLAIN);

        if (this.effect != null) {
            playLegacy(location, this.effect, 0, 0, offsetX, offsetY, offsetZ, speed, count);

            return;
        }

        Api.display(location, this.particle, count, offsetX, offsetY, offsetZ, speed, this.dataType == Void.class ? null : Color.WHITE);
    }

    public void shoot(final Location location, final double directionX, final double directionY, final double directionZ, final double speed) {
        this.spawn(location, 0, directionX, directionY, directionZ, speed);
    }

    private void checkKind(final Kind expected) {
        if (this.kind != expected)
            throw new IllegalStateException("Report / " + this + " is a " + this.kind + " particle, not " + expected);
    }

    public static void dust(final Location location, final Color color, final float size, final int count, final double spread) {
        if (DUST.dataType != null && DUST.dataType != Void.class) {
            Api.display(location, DUST.particle, count, spread, spread, spread, 0, Api.newDustOptions(color, size));

            return;
        }

        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final double red               = Math.max(color.getRed() / 255D, 0.001D);
        final double green             = color.getGreen() / 255D;
        final double blue              = color.getBlue() / 255D;

        for (int index = 0; index < count; index++) {
            final Location point = count == 1 ? location : location.clone().add(random.nextDouble(-spread, spread + 0.001D), random.nextDouble(-spread, spread + 0.001D), random.nextDouble(-spread, spread + 0.001D));

            if (DUST.effect != null)
                playLegacy(point, DUST.effect, 0, 0, red, green, blue, 1, 0);
            else
                Api.display(point, DUST.particle, 0, red, green, blue, 1, null);
        }
    }

    @SuppressWarnings("deprecation")
    public static void block(final Location location, final BlockLook look, final int count, final double spread, final double speed) {
        if (BLOCK.effect != null) {
            playLegacy(location, BLOCK.effect, look.getMaterial().getId(), look.getData(), spread, spread, spread, speed, count);

            return;
        }

        final Object data = BLOCK.dataType == MaterialData.class ? new MaterialData(look.getMaterial(), look.getData()) : look.getBlockData();

        Api.display(location, BLOCK.particle, count, spread, spread, spread, speed, data);
    }

    private static void playLegacy(final Location location, final Effect effect, final int id, final int data, final double offsetX, final double offsetY, final double offsetZ, final double speed, final int count) {
        location.getWorld().spigot().playEffect(location, effect, id, data, (float) offsetX, (float) offsetY, (float) offsetZ, (float) speed, count, LEGACY_RADIUS);
    }

    private static Object findParticle(final Kind kind, final String[] names) {
        for (final String name : names) {
            final Object candidate = Api.findParticle(name);

            if (candidate != null && kind.accepts(Api.getDataType(candidate)))
                return candidate;
        }

        throw new IllegalStateException("None of the particles " + Arrays.toString(names) + " can be spawned on this server");
    }

    private enum Kind {

        PLAIN,
        DUST,
        BLOCK;

        private boolean accepts(final Class<?> dataType) {
            final String name = dataType.getSimpleName();

            switch (this) {
                case PLAIN:
                    return dataType == Void.class || dataType == Color.class;

                case DUST:
                    return dataType == Void.class || name.equals("DustOptions");

                case BLOCK:
                    return dataType == MaterialData.class || name.equals("BlockData");

                default:
                    throw new IllegalStateException("Report / Unhandled particle kind " + this);
            }
        }
    }

    private static final class Api {

        private static final Class<?> PARTICLE           = Remain.findClass("org.bukkit.Particle");
        private static final Method GET_DATA_TYPE        = PARTICLE == null ? null : Remain.findMethod(PARTICLE, "getDataType");
        private static final Method SPAWN_FORCED         = PARTICLE == null ? null : Remain.findMethod(World.class, "spawnParticle", PARTICLE, Location.class, int.class, double.class, double.class, double.class, double.class, Object.class, boolean.class);
        private static final Method SPAWN                = PARTICLE == null ? null : Remain.findMethod(World.class, "spawnParticle", PARTICLE, Location.class, int.class, double.class, double.class, double.class, double.class, Object.class);
        private static final Constructor<?> DUST_OPTIONS = findDustOptions();

        private static Object findParticle(final String name) {
            try {
                return PARTICLE.getField(name).get(null);

            } catch (final NoSuchFieldException ex) {
                return null;

            } catch (final IllegalAccessException ex) {
                throw new IllegalStateException("Cannot read particle " + name, ex);
            }
        }

        private static Class<?> getDataType(final Object particle) {
            return (Class<?>) invoke(GET_DATA_TYPE, particle);
        }

        private static void display(final Location location, final Object particle, final int count, final double offsetX, final double offsetY, final double offsetZ, final double speed, final Object data) {
            if (SPAWN_FORCED != null)
                invoke(SPAWN_FORCED, location.getWorld(), particle, location, count, offsetX, offsetY, offsetZ, speed, data, true);
            else
                invoke(SPAWN, location.getWorld(), particle, location, count, offsetX, offsetY, offsetZ, speed, data);
        }

        private static Object newDustOptions(final Color color, final float size) {
            try {
                return DUST_OPTIONS.newInstance(color, size);

            } catch (final InvocationTargetException ex) {
                throw new IllegalStateException("Creating dust options failed", ex.getCause());

            } catch (final ReflectiveOperationException ex) {
                throw new IllegalStateException("Cannot create dust options", ex);
            }
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

        private static Constructor<?> findDustOptions() {
            final Class<?> type = Remain.findClass("org.bukkit.Particle$DustOptions");

            if (type == null)
                return null;

            try {
                return type.getConstructor(Color.class, float.class);

            } catch (final NoSuchMethodException ex) {
                throw new IllegalStateException("DustOptions lacks its (Color, float) constructor", ex);
            }
        }
    }
}
