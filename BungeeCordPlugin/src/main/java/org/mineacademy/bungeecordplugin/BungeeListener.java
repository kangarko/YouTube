package org.mineacademy.bungeecordplugin;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BungeeListener implements Listener {

	private final Map<UUID, ServerInfo> originServers = new HashMap<>();

	/*@EventHandler
	public void onPostLogin(PostLoginEvent event) {
		ProxiedPlayer player = event.getPlayer();

		for (ProxiedPlayer online : ProxyServer.getInstance().getPlayers()) {
			online.sendMessage(ChatColor.GREEN + "[+] " + ChatColor.GRAY + player.getName() + " has joined the network!");
		}
	}*/

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

	@EventHandler(priority = 32)
	public void onSwitchEarly(ServerSwitchEvent event) { // on join AND on switch
		ProxiedPlayer player = event.getPlayer();

		if (!originServers.containsKey(player.getUniqueId()))
			ProxyServer.getInstance().broadcast(ChatColor.GREEN + "[+] " + ChatColor.GRAY + player.getName() + " has joined the network!");
	}

	@EventHandler(priority = 64)
	public void onSwitchLate(ServerSwitchEvent event) { // on join AND on switch
		ProxiedPlayer player = event.getPlayer();
		ServerInfo toServer = player.getServer().getInfo();
		ServerInfo fromServer = this.originServers.put(player.getUniqueId(), toServer);

		if (fromServer != null)
			ProxyServer.getInstance().broadcast(ChatColor.LIGHT_PURPLE + "[>] " + ChatColor.GRAY
					+ player.getName() + " has switched from " + fromServer.getName() + " to " + toServer.getName() + "!");
	}

	@EventHandler
	public void onDisconnect(PlayerDisconnectEvent event) {
		ProxiedPlayer player = event.getPlayer();
		ServerInfo fromServer = this.originServers.remove(player.getUniqueId());

		if (fromServer != null)
			ProxyServer.getInstance().broadcast(ChatColor.RED + "[-] " +
					ChatColor.GRAY + player.getName() + " has left the network!");
	}
}
