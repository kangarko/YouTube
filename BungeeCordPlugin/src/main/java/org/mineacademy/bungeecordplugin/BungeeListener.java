package org.mineacademy.bungeecordplugin;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class BungeeListener implements Listener {

	@EventHandler
	public void onPostLogin(PostLoginEvent event) {
		ProxiedPlayer player = event.getPlayer();

		for (ProxiedPlayer online : ProxyServer.getInstance().getPlayers()) {
			online.sendMessage(ChatColor.GREEN + "[+] " + ChatColor.GRAY + player.getName() + " has joined the network!");
		}
	}
}
