package org.mineacademy.bungeecordplugin;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
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

	@EventHandler
	public void onPluginMessage(PluginMessageEvent event) {
		if (!event.getTag().equals("BungeeCord"))
			return;

		ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
		String argument = in.readUTF();

		if ("ListServerAmounts".equals(argument)) {
			ByteArrayDataOutput out = ByteStreams.newDataOutput();

			out.writeUTF("ServerAmounts");
			out.writeInt(ProxyServer.getInstance().getServers().size());

			for (ServerInfo serverInfo : ProxyServer.getInstance().getServers().values())
				serverInfo.sendData("BungeeCord", out.toByteArray());
		}
	}
}
