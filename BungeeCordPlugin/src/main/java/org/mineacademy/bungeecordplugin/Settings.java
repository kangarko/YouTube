package org.mineacademy.bungeecordplugin;

import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

public final class Settings {

	private static ConfigurationProvider provider = ConfigurationProvider.getProvider(YamlConfiguration.class);
	private static File file = new File(BungeeCordPlugin.getInstance().getDataFolder(), "config.yml");
	private static Configuration config;

	public static Configuration getConfig() {
		return config;
	}

	public static void save() {
		try {
			provider.save(config, file);

		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public static void load() {

		try {

			if (!file.exists()) {
				Files.createDirectories(BungeeCordPlugin.getInstance().getDataFolder().toPath());

				try (InputStream in = BungeeCordPlugin.getInstance().getResourceAsStream("config.yml")) {
					Files.copy(in, file.toPath());
				}
			}

			config = provider.load(file);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
}
