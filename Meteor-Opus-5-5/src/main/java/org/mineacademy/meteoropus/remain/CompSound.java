package org.mineacademy.meteoropus.remain;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public enum CompSound {

    CLICK("random.click", "ui.button.click"),
    MENU_OPEN("random.chestopen", "block.ender_chest.open", "block.enderchest.open"),
    REFUSE("mob.villager.no", "entity.villager.no"),
    CONFIRM("mob.enderdragon.growl", "entity.ender_dragon.growl", "entity.enderdragon.growl"),
    PLING("note.pling", "block.note_block.pling", "block.note.pling"),
    CHIME("note.pling", "block.note_block.chime", "block.note.chime", "block.note.pling"),
    BELL("note.pling", "block.bell.resonate", "block.note_block.chime", "block.note.chime", "block.note.pling"),
    AMETHYST("note.pling", "block.amethyst_block.chime", "block.note_block.chime", "block.note.chime", "block.note.pling"),
    BASS_DRUM("note.bd", "block.note_block.basedrum", "block.note.basedrum"),
    HEARTBEAT("note.bd", "entity.warden.heartbeat", "block.note_block.basedrum", "block.note.basedrum"),
    CURSE("mob.guardian.curse", "entity.elder_guardian.curse"),
    FIRE_CHARGE("mob.ghast.fireball", "item.firecharge.use"),
    GLASS_BREAK("dig.glass", "block.glass.break"),
    TELEPORT("mob.endermen.portal", "entity.enderman.teleport", "entity.endermen.teleport"),
    ENDERMAN_SCREAM("mob.endermen.scream", "entity.enderman.scream", "entity.endermen.scream"),
    WITHER_SHOOT("mob.wither.shoot", "entity.wither.shoot"),
    WITHER_SPAWN("mob.wither.spawn", "entity.wither.spawn"),
    WITHER_AMBIENT("mob.wither.idle", "entity.wither.ambient"),
    ROCKET("fireworks.launch", "entity.firework_rocket.launch", "entity.firework.launch"),
    WINGS("mob.enderdragon.wings", "entity.ender_dragon.flap", "entity.enderdragon.flap"),
    BLAZE_SHOOT("mob.ghast.fireball", "entity.blaze.shoot"),
    BLAZE_DEATH("mob.blaze.death", "entity.blaze.death"),
    BURN("fire.fire", "block.fire.ambient"),
    FIZZ("random.fizz", "block.fire.extinguish"),
    LAVA_POP("liquid.lavapop", "block.lava.pop"),
    LAVA_AMBIENT("liquid.lava", "block.lava.ambient"),
    FREEZE("dig.glass", "entity.player.hurt_freeze", "block.glass.break"),
    SNOW_BREAK("dig.snow", "block.snow.break"),
    SNOW_PLACE("step.snow", "block.powder_snow.place", "block.snow.place"),
    WIND("mob.enderdragon.wings", "item.elytra.flying"),
    PORTAL_AMBIENT("portal.portal", "block.portal.ambient"),
    PORTAL_TRIGGER("portal.trigger", "block.portal.trigger"),
    END_PORTAL("portal.travel", "block.end_portal.spawn", "block.portal.travel"),
    SONIC_CHARGE("portal.trigger", "entity.warden.sonic_charge", "block.beacon.ambient", "block.portal.trigger"),
    DEPLETE("portal.travel", "block.respawn_anchor.deplete", "block.beacon.deactivate", "block.portal.travel"),
    EXPLODE("random.explode", "entity.generic.explode"),
    THUNDER("ambient.weather.thunder", "entity.lightning_bolt.thunder", "entity.lightning.thunder"),
    LIGHTNING_IMPACT("random.explode", "entity.lightning_bolt.impact", "entity.lightning.impact"),
    LARGE_BLAST("fireworks.largeBlast", "entity.firework_rocket.large_blast", "entity.firework.large_blast"),
    TWINKLE("fireworks.twinkle", "entity.firework_rocket.twinkle", "entity.firework.twinkle"),
    SONIC_BOOM("random.explode", "entity.warden.sonic_boom", "entity.generic.explode"),
    TRIDENT_THUNDER("ambient.weather.thunder", "item.trident.thunder", "entity.lightning_bolt.thunder", "entity.lightning.thunder"),
    BEACON_DEACTIVATE("note.bassattack", "block.beacon.deactivate", "block.note.bass"),
    BEACON_ACTIVATE("random.levelup", "block.beacon.activate", "entity.player.levelup"),
    ANVIL("random.anvil_land", "block.anvil.land"),
    STONE_BREAK("dig.stone", "block.stone.break"),
    STONE_PLACE("dig.stone", "block.stone.place"),
    COMPLETE("random.levelup", "ui.toast.challenge_complete", "entity.player.levelup");

    private final String key;

    CompSound(final String legacyKey, final String... modernKeys) {
        this.key = Remain.hasLegacySounds() ? legacyKey : findModernKey(modernKeys);
    }

    public String getKey() {
        return this.key;
    }

    public void play(final Player player, final Location location, final float volume, final float pitch) {
        player.playSound(location, this.key, volume, pitch);
    }

    public void play(final Collection<Player> players, final Location location, final float volume, final float pitch) {
        for (final Player player : players)
            player.playSound(location, this.key, volume, pitch);
    }

    public void playNear(final Collection<Player> players, final float volume, final float pitch) {
        for (final Player player : players)
            player.playSound(player.getLocation(), this.key, volume, pitch);
    }

    private static String findModernKey(final String[] keys) {
        for (final String key : keys)
            try {
                Sound.class.getField(key.toUpperCase(Locale.ROOT).replace('.', '_'));

                return key;

            } catch (final NoSuchFieldException ex) {
            }

        throw new IllegalStateException("None of the sounds " + Arrays.toString(keys) + " exist on this server");
    }
}
