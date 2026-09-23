package org.mineacademy.testui;

import net.kyori.adventure.title.Title;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TestUICommand implements CommandExecutor {

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
		if (!(sender instanceof Player))
			return false;

		final Player player = (Player) sender;
		final Menu menu = Menu.getMenu(player);

		if (menu != null) {
			menu.close();

			return true;
		}

		final Menu mainMenu = new Menu("MAIN MENU");
		final Menu shopMenu = new Menu("SHOP");

		mainMenu.registerButton("Play", () -> {
			mainMenu.close();
			player.showTitle(Title.title(Util.toComponent("<aqua>Game starting"), Util.toComponent("<gray>Good luck!")));
		});
		mainMenu.registerButton("Shop", () -> shopMenu.displayTo(player));
		mainMenu.registerButton("Close", () -> mainMenu.close());

		shopMenu.registerButton("Diamond Sword", Material.DIAMOND_SWORD, () -> Util.give(player, Material.DIAMOND_SWORD));
		shopMenu.registerButton("Golden Apple", Material.GOLDEN_APPLE, () -> Util.give(player, Material.GOLDEN_APPLE));
		shopMenu.registerButton("Back", () -> mainMenu.displayTo(player));

		mainMenu.displayTo(player);
		return true;
	}

}
