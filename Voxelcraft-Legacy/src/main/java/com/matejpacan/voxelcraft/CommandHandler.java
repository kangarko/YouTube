package com.matejpacan.voxelcraft;

public class CommandHandler {

	private final Voxelcraft game;

	public CommandHandler(final Voxelcraft game) {
		this.game = game;
	}

	public void executeCommand(final String input, final GameUI ui) {
		if (input == null || input.isEmpty())
			return;

		final String trimmed = input.trim();
		if (!trimmed.startsWith("/")) {
			ui.addChatMessage("<Player> " + trimmed, 1, 1, 1);
			return;
		}

		final String[] parts = trimmed.substring(1).split("\\s+");
		if (parts.length == 0)
			return;

		final String command = parts[0].toLowerCase();

		switch (command) {
			case "tp" -> this.handleTeleport(parts, ui);
			case "time" -> this.handleTime(parts, ui);
			case "cycle" -> this.handleCycle(parts, ui);
			case "help" -> this.handleHelp(ui);
			case "clear" -> this.handleClear(ui);
			default -> ui.addErrorMessage("Unknown command: /" + command + ". Type /help for available commands.");
		}
	}

	private void handleTeleport(final String[] parts, final GameUI ui) {
		if (parts.length < 4) {
			ui.addErrorMessage("Usage: /tp x y z");
			return;
		}

		try {
			final float x = this.parseCoordinate(parts[1], this.game.getCamera().getPosition().x);
			final float y = this.parseCoordinate(parts[2], this.game.getCamera().getPosition().y);
			final float z = this.parseCoordinate(parts[3], this.game.getCamera().getPosition().z);

			if (y < 0) {
				ui.addErrorMessage("Cannot teleport below Y=0");
				return;
			}
			if (y > Chunk.HEIGHT + 50) {
				ui.addErrorMessage("Cannot teleport above Y=" + (Chunk.HEIGHT + 50));
				return;
			}

			this.game.teleportPlayer(x, y, z);
			ui.addSuccessMessage(String.format("Teleported to %.1f, %.1f, %.1f", x, y, z));
		} catch (final NumberFormatException e) {
			ui.addErrorMessage("Invalid coordinates. Use numbers or ~ for relative position.");
		}
	}

	private float parseCoordinate(final String value, final float current) {
		if (value.startsWith("~")) {
			if (value.length() == 1)
				return current;
			return current + Float.parseFloat(value.substring(1));
		}
		return Float.parseFloat(value);
	}

	private void handleTime(final String[] parts, final GameUI ui) {
		if (parts.length < 2) {
			final long current = this.game.getDayTimeTicks();
			ui.addSystemMessage("Current time: " + current + " ticks (" + this.ticksToTimeString(current) + ")");
			ui.addSystemMessage("Usage: /time set <day|noon|night|0-24000>");
			return;
		}

		final String subCommand = parts[1].toLowerCase();

		if ("query".equals(subCommand)) {
			final long current = this.game.getDayTimeTicks();
			ui.addSuccessMessage("Time: " + current + " ticks (" + this.ticksToTimeString(current) + ")");
			ui.addSystemMessage("World ticks: " + this.game.getWorldTicks());
			return;
		}

		if (!"set".equals(subCommand) && !"add".equals(subCommand)) {
			ui.addErrorMessage("Usage: /time set|add|query <value>");
			return;
		}

		if (parts.length < 3) {
			ui.addErrorMessage("Usage: /time " + subCommand + " <day|noon|night|0-24000>");
			return;
		}

		final String timeValue = parts[2].toLowerCase();
		long ticks;
		String timeName;

		switch (timeValue) {
			case "day", "morning" -> {
				ticks = 8000;
				timeName = "day";
			}
			case "noon", "midday" -> {
				ticks = 12000;
				timeName = "noon";
			}
			case "sunset", "evening" -> {
				ticks = 18000;
				timeName = "sunset";
			}
			case "night" -> {
				ticks = 20000;
				timeName = "night";
			}
			case "midnight" -> {
				ticks = 0;
				timeName = "midnight";
			}
			case "sunrise", "dawn" -> {
				ticks = 6000;
				timeName = "sunrise";
			}
			default -> {
				try {
					ticks = Long.parseLong(timeValue);
					if (ticks < 0 || ticks >= 24000) {
						ui.addErrorMessage("Time value must be between 0 and 23999.");
						return;
					}
					timeName = ticks + " ticks";
				} catch (final NumberFormatException e) {
					ui.addErrorMessage("Invalid time. Use day, noon, night, sunrise, sunset, midnight, or 0-23999.");
					return;
				}
			}
		}

		if ("add".equals(subCommand)) {
			final long current = this.game.getDayTimeTicks();
			ticks = (current + ticks) % 24000;
			this.game.setDayTime(ticks);
			ui.addSuccessMessage("Added time. Now: " + ticks + " (" + this.ticksToTimeString(ticks) + ")");
		} else {
			this.game.setDayTime(ticks);
			ui.addSuccessMessage("Set time to " + timeName + " (" + this.ticksToTimeString(ticks) + ")");
		}
	}

	private String ticksToTimeString(final long ticks) {
		final int hours = (int) (ticks / 1000 % 24);
		final int minutes = (int) (ticks % 1000 * 60 / 1000);
		return String.format("%02d:%02d", hours, minutes);
	}

	private void handleCycle(final String[] parts, final GameUI ui) {
		if (parts.length < 2) {
			final boolean current = this.game.isDayCycleEnabled();
			ui.addSystemMessage("Day cycle is " + (current ? "enabled" : "disabled") + ". Use /cycle on|off");
			return;
		}

		final String value = parts[1].toLowerCase();
		switch (value) {
			case "on", "true", "enable" -> {
				this.game.setDayCycleEnabled(true);
				ui.addSuccessMessage("Day cycle enabled");
			}
			case "off", "false", "disable" -> {
				this.game.setDayCycleEnabled(false);
				ui.addSuccessMessage("Day cycle disabled");
			}
			default -> ui.addErrorMessage("Usage: /cycle on|off");
		}
	}

	private void handleHelp(final GameUI ui) {
		ui.addSystemMessage("=== Available Commands ===");
		ui.addChatMessage("/tp x y z - Teleport to coordinates (use ~ for relative)", 0.8f, 0.8f, 0.8f);
		ui.addChatMessage("/time set|add|query <value> - Manage time", 0.8f, 0.8f, 0.8f);
		ui.addChatMessage("  Values: day, noon, night, sunrise, sunset, midnight, or 0-23999", 0.6f, 0.6f, 0.6f);
		ui.addChatMessage("  0=midnight, 6000=sunrise, 12000=noon, 18000=sunset", 0.6f, 0.6f, 0.6f);
		ui.addChatMessage("/cycle on|off - Toggle day/night cycle", 0.8f, 0.8f, 0.8f);
		ui.addChatMessage("/clear - Clear chat history", 0.8f, 0.8f, 0.8f);
		ui.addChatMessage("/help - Show this help message", 0.8f, 0.8f, 0.8f);
		ui.addSystemMessage("=========================");
	}

	private void handleClear(final GameUI ui) {
		ui.addSystemMessage("Chat cleared!");
	}
}
