package org.mineacademy.meteoropus.model;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Color;
import org.mineacademy.meteoropus.remain.BlockLook;
import org.mineacademy.meteoropus.remain.CompMaterial;
import org.mineacademy.meteoropus.remain.CompParticle;
import org.mineacademy.meteoropus.remain.CompSound;

public enum Element {

    FIRE("Fire", "§6", "§e", "The crater melts into a lake of lava.",
            Color.fromRGB(255, 96, 16), Color.fromRGB(255, 204, 64), Color.fromRGB(140, 24, 0),
            CompMaterial.FIRE_CHARGE, CompMaterial.BLAZE_POWDER,
            new CompMaterial[] { CompMaterial.YELLOW_PANE, CompMaterial.ORANGE_PANE, CompMaterial.ORANGE_PANE, CompMaterial.RED_PANE, CompMaterial.RED_PANE, CompMaterial.BROWN_PANE },
            new CompMaterial[] { CompMaterial.MAGMA_BLOCK, CompMaterial.MAGMA_BLOCK, CompMaterial.MAGMA_BLOCK, CompMaterial.NETHERRACK, CompMaterial.NETHERRACK, CompMaterial.OBSIDIAN, CompMaterial.BLACKSTONE, CompMaterial.SHROOMLIGHT },
            CompParticle.FLAME, CompParticle.LARGE_SMOKE, CompParticle.LAVA,
            CompSound.FIRE_CHARGE, CompSound.FIZZ),

    ICE("Ice", "§b", "§f", "The crater freezes into a glacier.",
            Color.fromRGB(110, 200, 255), Color.fromRGB(236, 250, 255), Color.fromRGB(30, 80, 190),
            CompMaterial.BLUE_ICE, CompMaterial.SNOWBALL,
            new CompMaterial[] { CompMaterial.WHITE_PANE, CompMaterial.LIGHT_BLUE_PANE, CompMaterial.LIGHT_BLUE_PANE, CompMaterial.CYAN_PANE, CompMaterial.BLUE_PANE, CompMaterial.BLUE_PANE },
            new CompMaterial[] { CompMaterial.PACKED_ICE, CompMaterial.PACKED_ICE, CompMaterial.BLUE_ICE, CompMaterial.BLUE_ICE, CompMaterial.ICE, CompMaterial.SNOW_BLOCK, CompMaterial.LIGHT_BLUE_GLASS },
            CompParticle.SNOWFLAKE, CompParticle.CLOUD, CompParticle.END_ROD,
            CompSound.AMETHYST, CompSound.GLASS_BREAK),

    VOID("Void", "§5", "§d", "A black hole swallows the debris.",
            Color.fromRGB(150, 40, 255), Color.fromRGB(236, 120, 255), Color.fromRGB(24, 0, 48),
            CompMaterial.ENDER_EYE, CompMaterial.ENDER_PEARL,
            new CompMaterial[] { CompMaterial.MAGENTA_PANE, CompMaterial.MAGENTA_PANE, CompMaterial.PURPLE_PANE, CompMaterial.PURPLE_PANE, CompMaterial.BLUE_PANE, CompMaterial.GRAY_PANE },
            new CompMaterial[] { CompMaterial.OBSIDIAN, CompMaterial.OBSIDIAN, CompMaterial.CRYING_OBSIDIAN, CompMaterial.CRYING_OBSIDIAN, CompMaterial.BLACK_CONCRETE, CompMaterial.COAL_BLOCK, CompMaterial.PURPLE_GLASS },
            CompParticle.PORTAL, CompParticle.SQUID_INK, CompParticle.REVERSE_PORTAL,
            CompSound.TELEPORT, CompSound.TELEPORT);

    private final String name;
    private final String color;
    private final String accent;
    private final String description;
    private final Color primary;
    private final Color secondary;
    private final Color shadow;
    private final CompMaterial icon;
    private final CompMaterial powerIcon;
    private final CompMaterial[] comet;
    private final CompMaterial[] rock;
    private final CompParticle glow;
    private final CompParticle smoke;
    private final CompParticle spark;
    private final CompSound selectSound;
    private final CompSound landSound;

    Element(final String name, final String color, final String accent, final String description,
            final Color primary, final Color secondary, final Color shadow,
            final CompMaterial icon, final CompMaterial powerIcon,
            final CompMaterial[] comet, final CompMaterial[] rock,
            final CompParticle glow, final CompParticle smoke, final CompParticle spark,
            final CompSound selectSound, final CompSound landSound) {

        this.name        = name;
        this.color       = color;
        this.accent      = accent;
        this.description = description;
        this.primary     = primary;
        this.secondary   = secondary;
        this.shadow      = shadow;
        this.icon        = icon;
        this.powerIcon   = powerIcon;
        this.comet       = comet;
        this.rock        = rock;
        this.glow        = glow;
        this.smoke       = smoke;
        this.spark       = spark;
        this.selectSound = selectSound;
        this.landSound   = landSound;
    }

    public String getName() {
        return this.name;
    }

    public String getColor() {
        return this.color;
    }

    public String getAccent() {
        return this.accent;
    }

    public String getDescription() {
        return this.description;
    }

    public Color getPrimary() {
        return this.primary;
    }

    public Color getSecondary() {
        return this.secondary;
    }

    public Color getShadow() {
        return this.shadow;
    }

    public Color getRandomColor() {
        return ThreadLocalRandom.current().nextInt(3) == 0 ? this.secondary : this.primary;
    }

    public CompMaterial getIcon() {
        return this.icon;
    }

    public CompMaterial getPowerIcon() {
        return this.powerIcon;
    }

    public CompMaterial[] getComet() {
        return this.comet;
    }

    public BlockLook getRandomRock() {
        return this.rock[ThreadLocalRandom.current().nextInt(this.rock.length)].getLook();
    }

    public CompParticle getGlow() {
        return this.glow;
    }

    public CompParticle getSmoke() {
        return this.smoke;
    }

    public CompParticle getSpark() {
        return this.spark;
    }

    public CompSound getSelectSound() {
        return this.selectSound;
    }

    public CompSound getLandSound() {
        return this.landSound;
    }
}
