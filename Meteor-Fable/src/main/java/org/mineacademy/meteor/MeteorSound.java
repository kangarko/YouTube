package org.mineacademy.meteor;

import java.lang.reflect.Field;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public enum MeteorSound {

    PLING("BLOCK_NOTE_BLOCK_PLING", "BLOCK_NOTE_PLING", "NOTE_PLING"),
    BASS("BLOCK_NOTE_BLOCK_BASS", "BLOCK_NOTE_BASS", "NOTE_BASS"),
    CHIME("BLOCK_AMETHYST_BLOCK_CHIME", "BLOCK_NOTE_BLOCK_CHIME", "BLOCK_NOTE_CHIME", "NOTE_PLING"),
    CLICK("UI_BUTTON_CLICK", "CLICK"),
    THUNDER("ENTITY_LIGHTNING_BOLT_THUNDER", "ENTITY_LIGHTNING_THUNDER", "AMBIENCE_THUNDER"),
    WITHER_SPAWN("ENTITY_WITHER_SPAWN", "WITHER_SPAWN"),
    DRAGON_GROWL("ENTITY_ENDER_DRAGON_GROWL", "ENTITY_ENDERDRAGON_GROWL", "ENDERDRAGON_GROWL"),
    DRAGON_FLAP("ENTITY_ENDER_DRAGON_FLAP", "ENTITY_ENDERDRAGON_FLAP", "ENDERDRAGON_WINGS"),
    FIREWORK_LAUNCH("ENTITY_FIREWORK_ROCKET_LAUNCH", "ENTITY_FIREWORK_LAUNCH", "FIREWORK_LAUNCH"),
    FIREWORK_BLAST("ENTITY_FIREWORK_ROCKET_LARGE_BLAST", "ENTITY_FIREWORK_LARGE_BLAST", "FIREWORK_LARGE_BLAST"),
    EXPLODE("ENTITY_GENERIC_EXPLODE", "EXPLODE"),
    ANVIL_LAND("BLOCK_ANVIL_LAND", "ANVIL_LAND"),
    STONE_BREAK("BLOCK_STONE_BREAK", "DIG_STONE"),
    STONE_PLACE("BLOCK_STONE_PLACE", "DIG_STONE"),
    GRAVEL_BREAK("BLOCK_GRAVEL_BREAK", "DIG_GRAVEL"),
    BEACON_DEACTIVATE("BLOCK_BEACON_DEACTIVATE", "BLOCK_PORTAL_TRIGGER", "PORTAL_TRIGGER"),
    BEACON_ACTIVATE("BLOCK_BEACON_ACTIVATE", "BLOCK_PORTAL_TRAVEL", "PORTAL_TRAVEL"),
    BEACON_AMBIENT("BLOCK_BEACON_AMBIENT", "BLOCK_PORTAL_AMBIENT", "PORTAL"),
    BEACON_POWER("BLOCK_BEACON_POWER_SELECT", "ENTITY_EXPERIENCE_ORB_PICKUP", "ORB_PICKUP"),
    LEVEL_UP("ENTITY_PLAYER_LEVELUP", "LEVEL_UP"),
    ORB("ENTITY_EXPERIENCE_ORB_PICKUP", "ORB_PICKUP"),
    FIRE_AMBIENT("BLOCK_FIRE_AMBIENT", "FIRE"),
    FIRE_IGNITE("ITEM_FLINTANDSTEEL_USE", "FIRE_IGNITE"),
    FIRE_EXTINGUISH("BLOCK_FIRE_EXTINGUISH", "FIZZ"),
    LAVA_POP("BLOCK_LAVA_POP", "LAVA_POP"),
    LAVA_AMBIENT("BLOCK_LAVA_AMBIENT", "LAVA"),
    BLAZE_SHOOT("ENTITY_BLAZE_SHOOT", "BLAZE_BREATH"),
    GHAST_SHOOT("ENTITY_GHAST_SHOOT", "GHAST_FIREBALL"),
    GLASS_BREAK("BLOCK_GLASS_BREAK", "GLASS"),
    GLASS_PLACE("BLOCK_GLASS_PLACE", "DIG_STONE"),
    SNOW_BREAK("BLOCK_SNOW_BREAK", "DIG_SNOW"),
    FREEZE("ENTITY_PLAYER_HURT_FREEZE", "BLOCK_GLASS_BREAK", "GLASS"),
    ENDERMAN_TELEPORT("ENTITY_ENDERMAN_TELEPORT", "ENTITY_ENDERMEN_TELEPORT", "ENDERMAN_TELEPORT"),
    ENDERMAN_SCREAM("ENTITY_ENDERMAN_SCREAM", "ENTITY_ENDERMEN_SCREAM", "ENDERMAN_SCREAM"),
    PORTAL_TRIGGER("BLOCK_PORTAL_TRIGGER", "PORTAL_TRIGGER"),
    PORTAL_TRAVEL("BLOCK_PORTAL_TRAVEL", "PORTAL_TRAVEL"),
    PORTAL_AMBIENT("BLOCK_PORTAL_AMBIENT", "PORTAL"),
    END_PORTAL_SPAWN("BLOCK_END_PORTAL_SPAWN", "ENTITY_WITHER_SPAWN", "WITHER_SPAWN"),
    SONIC_BOOM("ENTITY_WARDEN_SONIC_BOOM", "ENTITY_ENDER_DRAGON_GROWL", "ENTITY_ENDERDRAGON_GROWL", "ENDERDRAGON_GROWL"),
    BREEZE_WIND("ENTITY_BREEZE_WIND_BURST", "ENTITY_GENERIC_EXPLODE", "EXPLODE");

    private final String[] candidates;
    private Sound sound;

    MeteorSound(String... candidates) {
        this.candidates = candidates;
    }

    public void play(Location location, float volume, float pitch) {
        location.getWorld().playSound(location, this.sound, volume, pitch);
    }

    public void play(Player player, float volume, float pitch) {
        player.playSound(player.getLocation(), this.sound, volume, pitch);
    }

    private void resolve() {
        for (String candidate : this.candidates) {
            Field field;

            try {
                field = Sound.class.getField(candidate);

            } catch (NoSuchFieldException ex) {
                continue;
            }

            try {
                this.sound = (Sound) field.get(null);

            } catch (IllegalAccessException ex) {
                throw new IllegalStateException("Report / Cannot read sound " + candidate, ex);
            }

            return;
        }

        throw new IllegalStateException("Report / None of the sounds " + String.join(", ", this.candidates) + " exist on this server");
    }

    public static void load() {
        for (MeteorSound sound : values())
            sound.resolve();
    }
}
