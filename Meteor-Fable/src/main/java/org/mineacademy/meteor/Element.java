package org.mineacademy.meteor;

import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public enum Element {

    FIRE("Fire", ChatColor.GOLD, ChatColor.RED, Color.fromRGB(255, 140, 0), Color.fromRGB(255, 40, 0),
            MeteorMaterial.BLAZE_POWDER, MeteorMaterial.FIRE_CHARGE,
            new MeteorMaterial[] { MeteorMaterial.PANE_ORANGE, MeteorMaterial.PANE_RED, MeteorMaterial.PANE_YELLOW },
            new MeteorMaterial[] { MeteorMaterial.MAGMA, MeteorMaterial.MAGMA, MeteorMaterial.NETHERRACK, MeteorMaterial.NETHERRACK, MeteorMaterial.OBSIDIAN, MeteorMaterial.GLOWSTONE },
            new MeteorMaterial[] { MeteorMaterial.MAGMA, MeteorMaterial.MAGMA, MeteorMaterial.MAGMA, MeteorMaterial.NETHERRACK, MeteorMaterial.NETHERRACK, MeteorMaterial.NETHERRACK, MeteorMaterial.OBSIDIAN },
            new MeteorMaterial[] { MeteorMaterial.LAVA }),

    ICE("Ice", ChatColor.AQUA, ChatColor.WHITE, Color.fromRGB(140, 220, 255), Color.fromRGB(255, 255, 255),
            MeteorMaterial.PACKED_ICE, MeteorMaterial.BLUE_ICE,
            new MeteorMaterial[] { MeteorMaterial.PANE_LIGHT_BLUE, MeteorMaterial.PANE_WHITE, MeteorMaterial.PANE_CYAN },
            new MeteorMaterial[] { MeteorMaterial.PACKED_ICE, MeteorMaterial.PACKED_ICE, MeteorMaterial.PACKED_ICE, MeteorMaterial.ICE, MeteorMaterial.ICE, MeteorMaterial.SNOW_BLOCK, MeteorMaterial.SEA_LANTERN },
            new MeteorMaterial[] { MeteorMaterial.PACKED_ICE, MeteorMaterial.PACKED_ICE, MeteorMaterial.PACKED_ICE, MeteorMaterial.PACKED_ICE, MeteorMaterial.ICE, MeteorMaterial.ICE, MeteorMaterial.BLUE_ICE },
            new MeteorMaterial[] { MeteorMaterial.PACKED_ICE, MeteorMaterial.PACKED_ICE, MeteorMaterial.PACKED_ICE, MeteorMaterial.ICE, MeteorMaterial.ICE, MeteorMaterial.SNOW_BLOCK }),

    VOID("Void", ChatColor.DARK_PURPLE, ChatColor.LIGHT_PURPLE, Color.fromRGB(110, 20, 160), Color.fromRGB(20, 0, 30),
            MeteorMaterial.ENDER_PEARL, MeteorMaterial.ENDER_EYE,
            new MeteorMaterial[] { MeteorMaterial.PANE_PURPLE, MeteorMaterial.PANE_BLACK, MeteorMaterial.PANE_MAGENTA },
            new MeteorMaterial[] { MeteorMaterial.OBSIDIAN, MeteorMaterial.OBSIDIAN, MeteorMaterial.OBSIDIAN, MeteorMaterial.COAL_BLOCK, MeteorMaterial.COAL_BLOCK, MeteorMaterial.CRYING_OBSIDIAN, MeteorMaterial.PURPUR },
            new MeteorMaterial[] { MeteorMaterial.OBSIDIAN, MeteorMaterial.OBSIDIAN, MeteorMaterial.OBSIDIAN, MeteorMaterial.BLACK_CONCRETE, MeteorMaterial.BLACK_CONCRETE, MeteorMaterial.CRYING_OBSIDIAN, MeteorMaterial.PURPUR },
            new MeteorMaterial[] { MeteorMaterial.COAL_BLOCK, MeteorMaterial.COAL_BLOCK, MeteorMaterial.BLACK_CONCRETE, MeteorMaterial.BLACK_CONCRETE, MeteorMaterial.OBSIDIAN, MeteorMaterial.CRYING_OBSIDIAN });

    private final String displayName;
    private final ChatColor color;
    private final ChatColor accent;
    private final Color dustColor;
    private final Color dustAccent;
    private final MeteorMaterial icon;
    private final MeteorMaterial head;
    private final MeteorMaterial[] panes;
    private final MeteorMaterial[] body;
    private final MeteorMaterial[] crust;
    private final MeteorMaterial[] fill;

    Element(String displayName, ChatColor color, ChatColor accent, Color dustColor, Color dustAccent, MeteorMaterial icon, MeteorMaterial head, MeteorMaterial[] panes, MeteorMaterial[] body, MeteorMaterial[] crust, MeteorMaterial[] fill) {
        this.displayName = displayName;
        this.color = color;
        this.accent = accent;
        this.dustColor = dustColor;
        this.dustAccent = dustAccent;
        this.icon = icon;
        this.head = head;
        this.panes = panes;
        this.body = body;
        this.crust = crust;
        this.fill = fill;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public ChatColor getColor() {
        return this.color;
    }

    public ChatColor getAccent() {
        return this.accent;
    }

    public Color getDustColor() {
        return this.dustColor;
    }

    public Color getDustAccent() {
        return this.dustAccent;
    }

    public MeteorMaterial getIcon() {
        return this.icon;
    }

    public MeteorMaterial getHead() {
        return this.head;
    }

    public MeteorMaterial getPane(int index) {
        return this.panes[Math.floorMod(index, this.panes.length)];
    }

    public MeteorMaterial pickBody(Random random) {
        return this.body[random.nextInt(this.body.length)];
    }

    public MeteorMaterial pickCrust(Random random) {
        return this.crust[random.nextInt(this.crust.length)];
    }

    public MeteorMaterial pickFill(Random random) {
        return this.fill[random.nextInt(this.fill.length)];
    }

    public String getTitle() {
        return this.color.toString() + ChatColor.BOLD + this.displayName.toUpperCase();
    }

    public void playMenuClick(Player player) {
        if (this == FIRE)
            MeteorSound.FIRE_IGNITE.play(player, 1F, 1.2F);
        else if (this == ICE)
            MeteorSound.CHIME.play(player, 1F, 1.5F);
        else
            MeteorSound.ENDERMAN_TELEPORT.play(player, 0.7F, 1.6F);
    }

    public void playTrail(Location center, Vector direction, Random random) {
        Location behind = center.clone().subtract(direction.clone().multiply(1.6));
        Location farBehind = center.clone().subtract(direction.clone().multiply(3.2));

        MeteorParticle.DUST.spawnDust(center, random.nextBoolean() ? this.dustColor : this.dustAccent, 2F, 6, 1.6);

        if (this == FIRE) {
            MeteorParticle.FLAME.spawn(center, 12, 1.1, 0.06);
            MeteorParticle.SMALL_FLAME.spawn(behind, 8, 0.8, 0.1);
            MeteorParticle.LAVA.spawn(center, 2, 0.8, 0);
            MeteorParticle.LARGE_SMOKE.spawn(behind, 6, 1.2, 0.02);
            MeteorParticle.LARGE_SMOKE.spawn(farBehind, 4, 1.4, 0.01);

            return;
        }

        if (this == ICE) {
            MeteorParticle.SNOWFLAKE.spawn(center, 12, 1.2, 0.02);
            MeteorParticle.CLOUD.spawn(behind, 6, 1.2, 0.02);
            MeteorParticle.SNOWBALL.spawn(center, 4, 1, 0.05);
            MeteorParticle.ENCHANT.spawn(farBehind, 10, 1.5, 0.5);
            MeteorParticle.WHITE_ASH.spawn(behind, 6, 1.5, 0.01);

            return;
        }

        MeteorParticle.PORTAL.spawn(center, 24, 1.2, 0.5);
        MeteorParticle.REVERSE_PORTAL.spawn(behind, 12, 1, 0.3);
        MeteorParticle.WITCH.spawn(center, 6, 1, 0.02);
        MeteorParticle.SQUID_INK.spawn(behind, 4, 1.2, 0.05);
        MeteorParticle.DRAGON_BREATH.spawn(farBehind, 4, 1, 0.02);
    }

    public void playTrailSound(Location location, int step) {
        if (this == FIRE) {
            MeteorSound.GHAST_SHOOT.play(location, 6F, 0.5F + step * 0.05F);
            MeteorSound.BLAZE_SHOOT.play(location, 4F, 0.4F);

            return;
        }

        if (this == ICE) {
            MeteorSound.FIREWORK_LAUNCH.play(location, 6F, 0.6F + step * 0.05F);
            MeteorSound.CHIME.play(location, 4F, 0.5F + step * 0.1F);

            return;
        }

        MeteorSound.PORTAL_AMBIENT.play(location, 6F, 0.4F + step * 0.05F);
        MeteorSound.ENDERMAN_TELEPORT.play(location, 4F, 0.3F);
    }

    public void playRingPoint(Location point, int index, int tick) {
        MeteorParticle.DUST.spawnDust(point, (index + tick / 4) % 3 == 0 ? this.dustAccent : this.dustColor, 1.6F, 1, 0);

        if ((index + tick) % 4 != 0)
            return;

        if (this == FIRE)
            MeteorParticle.FLAME.spawn(point, 1, 0.1, 0.02);
        else if (this == ICE)
            MeteorParticle.SNOWFLAKE.spawn(point, 1, 0.1, 0.02);
        else
            MeteorParticle.PORTAL.spawn(point, 2, 0.1, 0.2);
    }

    public void playImpact(Location center) {
        Location above = center.clone().add(0, 1, 0);

        MeteorParticle.FLASH.spawn(above, 1, 0, 0);
        MeteorParticle.EXPLOSION_EMITTER.spawn(above, 2, 1, 0);
        MeteorParticle.EXPLOSION.spawn(above, 20, 4, 0);
        MeteorParticle.LARGE_SMOKE.spawn(above, 120, 0.5, 0.35);
        MeteorParticle.CLOUD.spawn(above, 60, 1, 0.3);
        MeteorSound.EXPLODE.play(center, 8F, 0.6F);
        MeteorSound.THUNDER.play(center, 6F, 0.5F);

        if (this == FIRE) {
            MeteorParticle.FLAME.spawn(above, 200, 0, 0.6);
            MeteorParticle.LAVA.spawn(above, 40, 3, 0);
            MeteorSound.GHAST_SHOOT.play(center, 6F, 0.5F);
            MeteorSound.LAVA_POP.play(center, 4F, 0.6F);

            return;
        }

        if (this == ICE) {
            MeteorParticle.SNOWFLAKE.spawn(above, 200, 0, 0.5);
            MeteorParticle.SNOWBALL.spawn(above, 100, 2, 0.3);
            MeteorParticle.ENCHANT.spawn(above, 100, 3, 1);
            MeteorSound.GLASS_BREAK.play(center, 6F, 0.5F);
            MeteorSound.FREEZE.play(center, 5F, 0.8F);

            return;
        }

        MeteorParticle.PORTAL.spawn(above, 300, 0, 2);
        MeteorParticle.REVERSE_PORTAL.spawn(above, 150, 1, 1);
        MeteorParticle.SQUID_INK.spawn(above, 100, 0.5, 0.5);
        MeteorParticle.DRAGON_BREATH.spawn(above, 80, 0.5, 0.3);
        MeteorSound.ENDERMAN_SCREAM.play(center, 6F, 0.4F);
        MeteorSound.END_PORTAL_SPAWN.play(center, 5F, 0.6F);
    }

    public void playDebrisVanish(Location location, Random random, boolean sound) {
        if (this == FIRE) {
            MeteorParticle.FLAME.spawn(location, 8, 0.4, 0.05);
            MeteorParticle.LAVA.spawn(location, 2, 0.3, 0);
            MeteorParticle.LARGE_SMOKE.spawn(location, 5, 0.4, 0.02);

            if (sound)
                MeteorSound.FIRE_EXTINGUISH.play(location, 1.2F, 0.8F + random.nextFloat() * 0.4F);

            return;
        }

        if (this == ICE) {
            MeteorParticle.SNOWBALL.spawn(location, 12, 0.4, 0.1);
            MeteorParticle.SNOWFLAKE.spawn(location, 6, 0.4, 0.02);

            if (sound)
                MeteorSound.GLASS_BREAK.play(location, 1.2F, 1.2F + random.nextFloat() * 0.4F);

            return;
        }

        MeteorParticle.PORTAL.spawn(location, 15, 0.4, 0.5);
        MeteorParticle.REVERSE_PORTAL.spawn(location, 6, 0.3, 0.2);

        if (sound)
            MeteorSound.ENDERMAN_TELEPORT.play(location, 1F, 1.5F + random.nextFloat() * 0.5F);
    }

    public void playRebuildPoint(Location location) {
        if (this == FIRE)
            MeteorParticle.SMALL_FLAME.spawn(location, 2, 0.3, 0.02);
        else if (this == ICE)
            MeteorParticle.SNOWFLAKE.spawn(location, 2, 0.3, 0.02);
        else
            MeteorParticle.REVERSE_PORTAL.spawn(location, 3, 0.3, 0.1);
    }
}
