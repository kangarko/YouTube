package org.mineacademy.bungeecordplugin;

import net.md_5.bungee.api.plugin.Plugin;

import java.util.concurrent.TimeUnit;

public final class BungeeCordPlugin extends Plugin {

	private static BungeeCordPlugin instance;

	@Override
	public void onEnable() {
		instance = this;

		getProxy().getPluginManager().registerListener(this, new BungeeListener());
		getProxy().getPluginManager().registerCommand(this, new TablistCommand());

		Settings.load();

		System.out.println("Example Key: " + Settings.getConfig().getString("Example_Section.Example_Key"));

		getProxy().getScheduler().schedule(this, () -> {
			System.out.println("Hey there");

		}, 50, TimeUnit.MILLISECONDS);

		getProxy().registerChannel("BungeeCord");
	}

	@Override
	public void onDisable() {
		getProxy().unregisterChannel("BungeeCord");
	}

	public static BungeeCordPlugin getInstance() {
		return instance;
	}
}
