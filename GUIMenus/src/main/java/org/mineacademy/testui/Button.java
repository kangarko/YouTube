package org.mineacademy.testui;

import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;

public final class Button {

	private static final float WIDTH = 2.6F;
	private static final float HEIGHT = 0.4F;

	private final String label;
	private final Material icon;
	private final Runnable action;

	private double y;
	private BlockDisplay bar;

	public Button(String label, Material icon, Runnable action) {
		this.label = label;
		this.icon = icon;
		this.action = action;
	}

	public void draw(Menu menu, double y) {
		this.y = y;
		this.bar = menu.spawnBlock(Material.GRAY_CONCRETE, y, Menu.DISTANCE + 0.05, WIDTH, HEIGHT);

		menu.spawnText("<white>" + this.label, y, Menu.DISTANCE, 0.9F);

		if (this.icon != null)
			menu.spawnItem(this.icon, -WIDTH / 2 + 0.18, y, Menu.DISTANCE, 0.28F);
	}

	public boolean isWithin(double x, double y) {
		return Math.abs(x) < WIDTH / 2 && Math.abs(y - this.y) < HEIGHT / 2;
	}

	public void setHover(boolean hover) {
		this.bar.setBlock((hover ? Material.LIGHT_BLUE_CONCRETE : Material.GRAY_CONCRETE).createBlockData());
	}

	public void click() {
		this.action.run();
	}
}
