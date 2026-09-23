package org.mineacademy.testui;

import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Menu {

	public static final double DISTANCE = 2.5;

	private static final double EDGE = 2.5;
	private static final double ROW = 0.5;
	private static final float PANEL_WIDTH = 3.2F;
	private static final float CURSOR_SCALE = 0.6F;
	private static final float TEXT_CENTER = 0.11F;
	private static final long CLICK_DELAY = 250;

	private static final Map<UUID, Menu> menus = new HashMap<>();

	private final String title;
	private final List<Button> buttons = new ArrayList<>();
	private final List<Display> displays = new ArrayList<>();

	private Player player;
	private Location start;
	private Location eye;
	private Pig tripod;
	private TextDisplay cursor;
	private Button hovered;
	private long lastClick;

	public Menu(String title) {
		this.title = title;
	}

	public Menu registerButton(String label, Runnable action) {
		return this.registerButton(label, null, action);
	}

	public Menu registerButton(String label, Material icon, Runnable action) {
		this.buttons.add(new Button(label, icon, action));

		return this;
	}

	public void displayTo(Player player) {
		final Menu previous = getMenu(player);

		this.player = player;
		this.lastClick = System.currentTimeMillis();

		if (previous == null)
			this.mount();
		else
			this.takeOver(previous);

		this.draw();

		menus.put(player.getUniqueId(), this);
	}

	private void mount() {
		this.start = this.player.getLocation();
		this.eye = this.player.getEyeLocation();

		this.eye.setPitch(0F);

		this.player.leaveVehicle();
		this.player.setRotation(this.eye.getYaw(), 0F);
		this.player.setInvisible(true);

		this.tripod = this.eye.getWorld().spawn(this.eye, Pig.class, pig -> {
			pig.setInvisible(true);
			pig.setAI(false);
			pig.setGravity(false);
			pig.setSilent(true);
			pig.setInvulnerable(true);
			pig.setPersistent(false);
		});

		this.tripod.teleport(this.eye.clone().subtract(0, this.tripod.getEyeHeight(), 0));
		this.tripod.addPassenger(this.player);
		Util.setCamera(this.player, this.tripod);

		final GUIMenusPlugin plugin = GUIMenusPlugin.getInstance();

		plugin.getServer().getScheduler().runTaskLater(plugin, () -> this.player.sendActionBar(Component.empty()), 2);
	}

	private void takeOver(Menu previous) {
		this.start = previous.start;
		this.eye = previous.eye;
		this.tripod = previous.tripod;

		previous.removeDisplays();
	}

	private void draw() {
		final float height = (float) ((this.buttons.size() + 1) * ROW + 0.3);
		final double top = this.getRowY(0);

		this.spawnBlock(Material.BLACK_CONCRETE, 0, DISTANCE + 0.12, PANEL_WIDTH, height);
		this.spawnBlock(Material.LIGHT_BLUE_CONCRETE, top - 0.2, DISTANCE + 0.05, 1.4F, 0.035F);
		this.spawnText("<gradient:#6ee7ff:#3fa9f5><bold>" + this.title, top, DISTANCE, 1.4F);

		for (int index = 0; index < this.buttons.size(); index++)
			this.buttons.get(index).draw(this, this.getRowY(index + 1));

		this.cursor = this.spawnText(cursorText(false), 0, DISTANCE - 0.05, CURSOR_SCALE);

		this.cursor.setTeleportDuration(1);

		Util.playSound(this.player, Sound.UI_TOAST_IN, 1.4F);
		this.tick();
	}

	private double getRowY(int row) {
		return ROW * (this.buttons.size() / 2D - row);
	}

	private void tick() {
		final double x = Util.project(this.player.getYaw() - this.eye.getYaw(), DISTANCE, EDGE);
		final double y = Util.project(-this.player.getPitch(), DISTANCE, EDGE);

		this.cursor.teleport(Util.toWorld(this.eye, x, y - TEXT_CENTER * CURSOR_SCALE, DISTANCE - 0.05));

		final Button button = this.findButton(x, y);

		if (button == this.hovered)
			return;

		if (this.hovered != null)
			this.hovered.setHover(false);

		if (button != null) {
			button.setHover(true);

			Util.playSound(this.player, Sound.BLOCK_NOTE_BLOCK_HAT, 1.6F);
		}

		this.cursor.text(Util.toComponent(cursorText(button != null)));

		this.hovered = button;
	}

	private Button findButton(double x, double y) {
		for (final Button button : this.buttons)
			if (button.isWithin(x, y))
				return button;

		return null;
	}

	public void click() {
		final long now = System.currentTimeMillis();

		if (this.hovered == null || now - this.lastClick < CLICK_DELAY)
			return;

		this.lastClick = now;

		Util.playSound(this.player, Sound.UI_BUTTON_CLICK, 1.2F);
		this.hovered.click();
	}

	public void close() {
		menus.remove(this.player.getUniqueId());

		this.removeDisplays();
		this.player.leaveVehicle();
		Util.setCamera(this.player, this.player);
		this.tripod.remove();
		this.player.setInvisible(false);
		Util.playSound(this.player, Sound.UI_TOAST_OUT, 1F);
		this.player.teleport(this.start);
	}

	public TextDisplay spawnText(String text, double y, double distance, float scale) {
		final TextDisplay display = this.eye.getWorld().spawn(Util.toWorld(this.eye, 0, y - TEXT_CENTER * scale, distance), TextDisplay.class, spawned -> {
			spawned.text(Util.toComponent(text));
			spawned.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
			spawned.setShadowed(true);
		});

		this.setup(display, new Matrix4f().scale(scale));

		return display;
	}

	public BlockDisplay spawnBlock(Material material, double y, double distance, float width, float height) {
		final BlockDisplay display = this.eye.getWorld().spawn(Util.toWorld(this.eye, 0, y, distance), BlockDisplay.class, spawned -> spawned.setBlock(material.createBlockData()));

		this.setup(display, new Matrix4f().translate(-width / 2, -height / 2, -0.02F).scale(width, height, 0.04F));

		return display;
	}

	public void spawnItem(Material material, double x, double y, double distance, float scale) {
		final ItemDisplay display = this.eye.getWorld().spawn(Util.toWorld(this.eye, x, y, distance), ItemDisplay.class, spawned -> {
			spawned.setItemStack(new ItemStack(material));
			spawned.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GUI);
		});

		this.setup(display, new Matrix4f().scale(scale));
	}

	private void setup(Display display, Matrix4f matrix) {
		display.setBillboard(Display.Billboard.CENTER);
		display.setBrightness(new Display.Brightness(15, 15));
		display.setVisibleByDefault(false);
		display.setPersistent(false);
		display.setTransformationMatrix(matrix);

		this.player.showEntity(GUIMenusPlugin.getInstance(), display);
		this.displays.add(display);
	}

	private void removeDisplays() {
		for (final Display display : this.displays)
			display.remove();

		this.displays.clear();

		this.hovered = null;
	}

	public static Menu getMenu(Player player) {
		return menus.get(player.getUniqueId());
	}

	public static void tickAll() {
		for (final Menu menu : menus.values())
			menu.tick();
	}

	public static void closeAll() {
		for (final Menu menu : new ArrayList<>(menus.values()))
			menu.close();
	}

	private static String cursorText(boolean hovering) {
		return hovering ? "<aqua>●HOVER" : "<white>●NORMAL";
	}
}
