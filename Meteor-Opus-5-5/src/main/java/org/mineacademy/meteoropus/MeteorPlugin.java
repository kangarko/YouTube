package org.mineacademy.meteoropus;

import org.bukkit.plugin.java.JavaPlugin;
import org.mineacademy.meteoropus.command.MeteorCommand;
import org.mineacademy.meteoropus.listener.MeteorListener;
import org.mineacademy.meteoropus.menu.MeteorMenu;
import org.mineacademy.meteoropus.model.Element;
import org.mineacademy.meteoropus.model.Protection;
import org.mineacademy.meteoropus.model.Strike;
import org.mineacademy.meteoropus.remain.CompMaterial;
import org.mineacademy.meteoropus.remain.CompParticle;
import org.mineacademy.meteoropus.remain.CompSound;

public final class MeteorPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        this.checkCompatibility();

        this.getCommand("meteor").setExecutor(new MeteorCommand(this));
        this.getServer().getPluginManager().registerEvents(new MeteorListener(), this);
        this.getServer().getScheduler().runTaskTimer(this, Protection::tick, 1, 1);
    }

    @Override
    public void onDisable() {
        Strike.abortActive();
        MeteorMenu.closeAll();
        Protection.releaseAll();
    }

    private void checkCompatibility() {
        for (final CompMaterial material : CompMaterial.values())
            if (material.getMaterial().isBlock())
                material.getLook();

        this.getLogger().info("Ready with " + CompMaterial.values().length + " materials, " + CompSound.values().length + " sounds, " + CompParticle.values().length + " particles and " + Element.values().length + " elements.");
    }
}
