package org.mineacademy.bungeecordplugin;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class TablistCommand extends Command {

	public TablistCommand() {
		super("tablist"); // /tablist
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		sender.sendMessage(ChatColor.GOLD + "Connected BungeeCord players:");

		for (ProxiedPlayer online : ProxyServer.getInstance().getPlayers()) {
			sender.sendMessage("§f - " + online.getName() + " §7- " + online.getServer().getInfo().getName());
		}
	}
}
