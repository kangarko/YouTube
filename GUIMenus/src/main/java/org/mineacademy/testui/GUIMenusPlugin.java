package org.mineacademy.testui;

import org.bukkit.plugin.java.JavaPlugin;

public class GUIMenusPlugin extends JavaPlugin {

	private static GUIMenusPlugin instance;

	@Override
	public void onEnable() {
		instance = this;

		this.getServer().getPluginManager().registerEvents(new PlayerListener(), this);
		this.getServer().getScheduler().runTaskTimer(this, () -> Menu.tickAll(), 1, 1);
		this.getCommand("testui").setExecutor(new TestUICommand());
	}

	@Override
	public void onDisable() {
		Menu.closeAll();
	}

	public static GUIMenusPlugin getInstance() {
		return instance;
	}
}
