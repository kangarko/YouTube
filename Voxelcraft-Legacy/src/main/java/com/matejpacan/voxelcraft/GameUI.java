package com.matejpacan.voxelcraft;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glBufferSubData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniform4f;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import lombok.Getter;
import lombok.Setter;

public class GameUI {

	public enum GameState {
		PLAYING,
		PAUSED,
		CHAT,
		INVENTORY
	}

	@Getter
	@Setter
	private GameState state = GameState.PLAYING;

	@Getter
	private final TextRenderer textRenderer;
	private final ShaderProgram uiShader;
	private final int quadVao;
	private final int quadVbo;

	private int screenWidth;
	private int screenHeight;
	private float uiScale = 2.0f;

	private double mouseX;
	private double mouseY;
	private int hoveredButton = -1;

	@Getter
	private final StringBuilder chatInput = new StringBuilder();
	private int cursorIndex = 0;
	private final List<ChatMessage> chatHistory = new ArrayList<>();
	private static final int MAX_CHAT_HISTORY = 100;
	private static final int VISIBLE_CHAT_LINES = 10;

	private final List<String> inputHistory = new ArrayList<>();
	private static final int MAX_INPUT_HISTORY = 50;
	private int historyIndex = -1;
	private String savedInput = "";

	private final List<String> commandSuggestions = new ArrayList<>();
	private int selectedSuggestion = -1;

	private int pauseSelection = 0;
	private static final String[] PAUSE_OPTIONS = { "Back to Game", "Exit Game" };

	private float[] buttonBounds;

	private long lastBlinkTime = 0;
	private boolean cursorVisible = true;
	private boolean ignoreNextChar = false;

	private float fps = 0;
	private int frameCount = 0;
	private double fpsTimer = 0;

	public GameUI() {
		this.textRenderer = new TextRenderer(24);
		this.uiShader = this.createUIShader();

		this.quadVao = glGenVertexArrays();
		this.quadVbo = glGenBuffers();

		glBindVertexArray(this.quadVao);
		glBindBuffer(GL_ARRAY_BUFFER, this.quadVbo);
		glBufferData(GL_ARRAY_BUFFER, 6 * 2 * Float.BYTES, GL_DYNAMIC_DRAW);
		glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0);
		glEnableVertexAttribArray(0);
		glBindVertexArray(0);

		this.buttonBounds = new float[PAUSE_OPTIONS.length * 4];
	}

	private ShaderProgram createUIShader() {
		final String vertexSource = """
									#version 410 core
									layout(location = 0) in vec2 aPosition;

									uniform mat4 uProjection;

									void main() {
									    gl_Position = uProjection * vec4(aPosition, 0.0, 1.0);
									}
									""";

		final String fragmentSource = """
										#version 410 core
										uniform vec4 uColor;
										out vec4 FragColor;

										void main() {
										    FragColor = uColor;
										}
										""";

		return new ShaderProgram(vertexSource, fragmentSource);
	}

	public void setScreenSize(int width, int height) {
		this.screenWidth = width;
		this.screenHeight = height;
		this.textRenderer.setScreenSize(width, height);
	}

	public void setUiScale(float scale) {
		this.uiScale = scale;
	}

	public void updateMousePosition(double x, double y, float windowToFramebufferRatio) {
		this.mouseX = x * windowToFramebufferRatio;
		this.mouseY = y * windowToFramebufferRatio;

		if (this.state == GameState.PAUSED)
			this.updateHoveredButton();
	}

	private void updateHoveredButton() {
		this.hoveredButton = -1;
		for (int i = 0; i < PAUSE_OPTIONS.length; i++) {
			final int idx = i * 4;
			if (this.mouseX >= this.buttonBounds[idx] && this.mouseX <= this.buttonBounds[idx + 2] &&
					this.mouseY >= this.buttonBounds[idx + 1] && this.mouseY <= this.buttonBounds[idx + 3]) {
				this.hoveredButton = i;
				this.pauseSelection = i;
				break;
			}
		}
	}

	public int handleMouseClick(double x, double y, float windowToFramebufferRatio) {
		if (this.state != GameState.PAUSED)
			return -1;

		final double mx = x * windowToFramebufferRatio;
		final double my = y * windowToFramebufferRatio;

		for (int i = 0; i < PAUSE_OPTIONS.length; i++) {
			final int idx = i * 4;
			if (mx >= this.buttonBounds[idx] && mx <= this.buttonBounds[idx + 2] &&
					my >= this.buttonBounds[idx + 1] && my <= this.buttonBounds[idx + 3])
				return i;
		}
		return -1;
	}

	public void update(double deltaTime) {
		this.frameCount++;
		this.fpsTimer += deltaTime;
		if (this.fpsTimer >= 1.0) {
			this.fps = (float) (this.frameCount / this.fpsTimer);
			this.frameCount = 0;
			this.fpsTimer = 0;
		}

		final long currentTime = System.currentTimeMillis();
		if (currentTime - this.lastBlinkTime > 500) {
			this.cursorVisible = !this.cursorVisible;
			this.lastBlinkTime = currentTime;
		}
	}

	private boolean playerFlying = true;
	private boolean playerSneaking = false;
	private Biome currentBiome = null;

	public void setPlayerFlying(boolean flying) {
		this.playerFlying = flying;
	}

	public void setPlayerSneaking(boolean sneaking) {
		this.playerSneaking = sneaking;
	}

	public void setCurrentBiome(Biome biome) {
		this.currentBiome = biome;
	}

	public void render(Vector3f playerPos, float yaw, float pitch) {
		if (this.screenWidth <= 0 || this.screenHeight <= 0)
			return;

		glDisable(GL_DEPTH_TEST);
		glDisable(GL_CULL_FACE);
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

		switch (this.state) {
			case PLAYING -> this.renderHUD(playerPos, yaw, pitch);
			case PAUSED -> {
				this.renderHUD(playerPos, yaw, pitch);
				this.renderPauseMenu();
			}
			case CHAT -> {
				this.renderHUD(playerPos, yaw, pitch);
				this.renderChat();
			}
			case INVENTORY -> this.renderHUD(playerPos, yaw, pitch);
		}

		glEnable(GL_CULL_FACE);
		glEnable(GL_DEPTH_TEST);
	}

	private void renderHUD(Vector3f playerPos, float yaw, float pitch) {
		final float padding = 20 * this.uiScale;
		final float lineHeight = 28 * this.uiScale;
		float y = padding;

		final String fpsText = String.format("FPS: %.0f", this.fps);
		this.textRenderer.drawTextWithShadow(fpsText, padding, y, 1, 1, 1, 1, this.uiScale);
		y += lineHeight;

		final String posText = String.format("XYZ: %.1f / %.1f / %.1f", playerPos.x, playerPos.y, playerPos.z);
		this.textRenderer.drawTextWithShadow(posText, padding, y, 1, 1, 1, 1, this.uiScale);
		y += lineHeight;

		final String facingText = String.format("Facing: %s (%.1f / %.1f)", this.getDirection(yaw), yaw, pitch);
		this.textRenderer.drawTextWithShadow(facingText, padding, y, 1, 1, 1, 1, this.uiScale);
		y += lineHeight;

		final String biomeText = "Biome: " + (this.currentBiome != null ? this.formatBiomeName(this.currentBiome.name()) : "Unknown");
		this.textRenderer.drawTextWithShadow(biomeText, padding, y, 0.7f, 0.9f, 0.7f, 1, this.uiScale);
		y += lineHeight;

		String modeText;
		float r, g, b;
		if (this.playerFlying) {
			modeText = "Flying";
			r = 0.5f;
			g = 0.8f;
			b = 1.0f;
		} else if (this.playerSneaking) {
			modeText = "Sneaking";
			r = 1.0f;
			g = 0.7f;
			b = 0.3f;
		} else {
			modeText = "Walking";
			r = 0.3f;
			g = 1.0f;
			b = 0.3f;
		}
		this.textRenderer.drawTextWithShadow(modeText, padding, y, r, g, b, 1, this.uiScale);

		if (this.state == GameState.PLAYING)
			this.renderCrosshair();

		if (this.state == GameState.PLAYING)
			this.renderRecentChat();
	}

	private void renderCrosshair() {
		final float size = 12 * this.uiScale;
		final float thickness = 3 * this.uiScale;
		final float cx = this.screenWidth / 2f;
		final float cy = this.screenHeight / 2f;

		this.drawRect(cx - size, cy - thickness / 2, cx + size, cy + thickness / 2, 1, 1, 1, 0.8f);
		this.drawRect(cx - thickness / 2, cy - size, cx + thickness / 2, cy + size, 1, 1, 1, 0.8f);
	}

	private String getDirection(float yaw) {
		yaw = (yaw % 360 + 360) % 360;
		if (yaw >= 315 || yaw < 45)
			return "South";
		if (yaw >= 45 && yaw < 135)
			return "West";
		if (yaw >= 135 && yaw < 225)
			return "North";
		return "East";
	}

	private String formatBiomeName(String name) {
		final String[] words = name.toLowerCase().split("_");
		final StringBuilder result = new StringBuilder();
		for (final String word : words) {
			if (!result.isEmpty())
				result.append(" ");
			result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}
		return result.toString();
	}

	private void renderPauseMenu() {
		this.drawRect(0, 0, this.screenWidth, this.screenHeight, 0, 0, 0, 0.6f);

		final String title = "Game Menu";
		final float titleWidth = this.textRenderer.getTextWidth(title) * this.uiScale;
		final float titleX = (this.screenWidth - titleWidth) / 2;
		final float titleY = this.screenHeight / 2f - 140 * this.uiScale;
		this.textRenderer.drawTextWithShadow(title, titleX, titleY, 1, 1, 1, 1, this.uiScale * 1.5f);

		final float buttonWidth = 300 * this.uiScale;
		final float buttonHeight = 50 * this.uiScale;
		final float buttonSpacing = 15 * this.uiScale;
		final float startY = this.screenHeight / 2f - 30 * this.uiScale;

		for (int i = 0; i < PAUSE_OPTIONS.length; i++) {
			final float buttonX = (this.screenWidth - buttonWidth) / 2;
			final float buttonY = startY + i * (buttonHeight + buttonSpacing);

			this.buttonBounds[i * 4] = buttonX;
			this.buttonBounds[i * 4 + 1] = buttonY;
			this.buttonBounds[i * 4 + 2] = buttonX + buttonWidth;
			this.buttonBounds[i * 4 + 3] = buttonY + buttonHeight;

			final boolean selected = i == this.pauseSelection;
			final boolean hovered = i == this.hoveredButton;

			if (selected || hovered)
				this.drawRect(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, 0.3f, 0.6f, 0.3f, 0.95f);
			else
				this.drawRect(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, 0.25f, 0.25f, 0.25f, 0.95f);

			this.drawRect(buttonX, buttonY, buttonX + buttonWidth, buttonY + 3 * this.uiScale, 0.5f, 0.5f, 0.5f, 1);
			this.drawRect(buttonX, buttonY + buttonHeight - 3 * this.uiScale, buttonX + buttonWidth, buttonY + buttonHeight, 0.1f,
					0.1f, 0.1f, 1);
			this.drawRect(buttonX, buttonY, buttonX + 3 * this.uiScale, buttonY + buttonHeight, 0.4f, 0.4f, 0.4f, 1);
			this.drawRect(buttonX + buttonWidth - 3 * this.uiScale, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, 0.15f,
					0.15f, 0.15f, 1);

			final String text = PAUSE_OPTIONS[i];
			final float textWidth = this.textRenderer.getTextWidth(text) * this.uiScale;
			final float textX = buttonX + (buttonWidth - textWidth) / 2;
			final float textY = buttonY + (buttonHeight - this.textRenderer.getFontSize() * this.uiScale) / 2;

			if (selected || hovered)
				this.textRenderer.drawTextWithShadow(text, textX, textY, 1, 1, 0.6f, 1, this.uiScale);
			else
				this.textRenderer.drawTextWithShadow(text, textX, textY, 0.9f, 0.9f, 0.9f, 1, this.uiScale);
		}

		final String hint = "Click a button or use arrow keys + Enter";
		final float hintWidth = this.textRenderer.getTextWidth(hint) * this.uiScale;
		this.textRenderer.drawText(hint, (this.screenWidth - hintWidth) / 2, this.screenHeight - 60 * this.uiScale, 0.7f, 0.7f, 0.7f, 1,
				this.uiScale);
	}

	private void renderChat() {
		final float chatHeight = 250 * this.uiScale;
		final float chatWidth = this.screenWidth * 0.6f;
		final float chatX = 20 * this.uiScale;
		final float inputHeight = 40 * this.uiScale;
		final float gap = 15 * this.uiScale;
		final float inputY = this.screenHeight - 100 * this.uiScale;
		final float chatY = inputY - chatHeight - gap;
		final float suggestionHeight = 26 * this.uiScale;
		final float suggestionsHeight = this.commandSuggestions.size() * suggestionHeight;
		final float backgroundTop = Math.min(chatY, inputY - suggestionsHeight);
		final float backgroundBottom = inputY + inputHeight;

		this.drawRect(chatX, backgroundTop, chatX + chatWidth, backgroundBottom, 0, 0, 0, 0.6f);

		final float lineHeight = 26 * this.uiScale;
		final int startIndex = Math.max(0, this.chatHistory.size() - VISIBLE_CHAT_LINES);
		float messageY = chatY + 15 * this.uiScale;

		for (int i = startIndex; i < this.chatHistory.size(); i++) {
			final ChatMessage msg = this.chatHistory.get(i);
			this.textRenderer.drawText(msg.message, chatX + 15 * this.uiScale, messageY, msg.r, msg.g, msg.b, 1, this.uiScale);
			messageY += lineHeight;
		}

		final String inputText = this.chatInput.toString();
		final StringBuilder displayBuilder = new StringBuilder("> ");
		displayBuilder.append(inputText);
		if (this.cursorVisible) {
			final int cursorPos = Math.min(Math.max(this.cursorIndex, 0), inputText.length());
			displayBuilder.insert(cursorPos + 2, "_");
		}
		this.textRenderer.drawText(displayBuilder.toString(), chatX + 15 * this.uiScale, inputY + 8 * this.uiScale, 1, 1, 1, 1,
				this.uiScale);

		if (!this.commandSuggestions.isEmpty()) {
			float suggestionY = inputY - suggestionsHeight;
			for (int i = 0; i < this.commandSuggestions.size(); i++) {
				final String suggestion = this.commandSuggestions.get(i);
				final boolean selected = i == this.selectedSuggestion;

				if (selected)
					this.drawRect(chatX, suggestionY, chatX + chatWidth, suggestionY + suggestionHeight, 0.2f, 0.3f, 0.5f,
							0.9f);
				else
					this.drawRect(chatX, suggestionY, chatX + chatWidth, suggestionY + suggestionHeight, 0.1f, 0.1f, 0.1f,
							0.9f);

				this.textRenderer.drawText(suggestion, chatX + 15 * this.uiScale, suggestionY + 4 * this.uiScale, 0.8f, 0.8f, 0.8f, 1,
						this.uiScale);
				suggestionY += suggestionHeight;
			}
		}
	}

	private void renderRecentChat() {
		if (this.chatHistory.isEmpty())
			return;

		final long currentTime = System.currentTimeMillis();
		final float chatX = 20 * this.uiScale;
		final float chatY = this.screenHeight - 160 * this.uiScale;
		final float lineHeight = 26 * this.uiScale;

		int visibleCount = 0;
		for (int i = this.chatHistory.size() - 1; i >= 0 && visibleCount < 5; i--) {
			final ChatMessage msg = this.chatHistory.get(i);
			final long age = currentTime - msg.timestamp;
			if (age > 10000)
				break;

			final float alpha = age < 8000 ? 1.0f : 1.0f - (age - 8000) / 2000f;
			if (alpha <= 0)
				continue;

			final float y = chatY - visibleCount * lineHeight;
			this.textRenderer.drawTextWithShadow(msg.message, chatX, y, msg.r, msg.g, msg.b, alpha, this.uiScale);
			visibleCount++;
		}
	}

	private void drawRect(float x1, float y1, float x2, float y2, float r, float g, float b, float a) {
		final float[] vertices = {
				x1, y1,
				x2, y1,
				x2, y2,
				x1, y1,
				x2, y2,
				x1, y2
		};

		glBindBuffer(GL_ARRAY_BUFFER, this.quadVbo);
		final FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
		buffer.put(vertices).flip();
		glBufferSubData(GL_ARRAY_BUFFER, 0, buffer);

		final Matrix4f ortho = new Matrix4f().ortho(0, this.screenWidth, this.screenHeight, 0, -1, 1);
		this.uiShader.use();
		this.uiShader.setMatrix4f("uProjection", ortho);
		glUniform4f(glGetUniformLocation(this.uiShader.getProgramId(), "uColor"), r, g, b, a);

		glBindVertexArray(this.quadVao);
		glDrawArrays(GL_TRIANGLES, 0, 6);
		glBindVertexArray(0);
	}

	public void openChat() {
		this.state = GameState.CHAT;
		this.chatInput.setLength(0);
		this.cursorIndex = 0;
		this.commandSuggestions.clear();
		this.selectedSuggestion = -1;
		this.historyIndex = -1;
		this.savedInput = "";
		this.ignoreNextChar = true;
	}

	public void closeChat() {
		this.state = GameState.PLAYING;
		this.chatInput.setLength(0);
		this.cursorIndex = 0;
		this.commandSuggestions.clear();
		this.selectedSuggestion = -1;
		this.historyIndex = -1;
	}

	public boolean shouldIgnoreNextChar() {
		return this.ignoreNextChar;
	}

	public void clearIgnoreNextChar() {
		this.ignoreNextChar = false;
	}

	public void saveInputToHistory(String input) {
		if (input == null || input.trim().isEmpty() || (!this.inputHistory.isEmpty() && this.inputHistory.get(this.inputHistory.size() - 1).equals(input)))
			return;
		this.inputHistory.add(input);
		while (this.inputHistory.size() > MAX_INPUT_HISTORY)
			this.inputHistory.remove(0);
	}

	public void navigateHistoryUp() {
		if (this.inputHistory.isEmpty())
			return;

		if (this.historyIndex == -1) {
			this.savedInput = this.chatInput.toString();
			this.historyIndex = this.inputHistory.size() - 1;
		} else if (this.historyIndex > 0)
			this.historyIndex--;
		else
			return;

		this.chatInput.setLength(0);
		this.chatInput.append(this.inputHistory.get(this.historyIndex));
		this.cursorIndex = this.chatInput.length();
		this.updateCommandSuggestions();
	}

	public void navigateHistoryDown() {
		if (this.historyIndex == -1)
			return;

		if (this.historyIndex < this.inputHistory.size() - 1) {
			this.historyIndex++;
			this.chatInput.setLength(0);
			this.chatInput.append(this.inputHistory.get(this.historyIndex));
		} else {
			this.historyIndex = -1;
			this.chatInput.setLength(0);
			this.chatInput.append(this.savedInput);
		}
		this.cursorIndex = this.chatInput.length();
		this.updateCommandSuggestions();
	}

	public void openPauseMenu() {
		this.state = GameState.PAUSED;
		this.pauseSelection = 0;
		this.hoveredButton = -1;
	}

	public void closePauseMenu() {
		this.state = GameState.PLAYING;
	}

	public void handlePauseInput(int key) {
		if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_UP || key == org.lwjgl.glfw.GLFW.GLFW_KEY_W)
			this.pauseSelection = Math.max(0, this.pauseSelection - 1);
		else if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN || key == org.lwjgl.glfw.GLFW.GLFW_KEY_S)
			this.pauseSelection = Math.min(PAUSE_OPTIONS.length - 1, this.pauseSelection + 1);
	}

	public int getPauseSelection() {
		return this.pauseSelection;
	}

	public void addChatCharacter(char c) {
		if (Character.isISOControl(c) && c != ' ')
			return;
		this.chatInput.insert(this.cursorIndex, c);
		this.cursorIndex++;
		this.updateCommandSuggestions();
	}

	public void removeChatCharacter() {
		if (this.cursorIndex == 0)
			return;
		this.chatInput.deleteCharAt(this.cursorIndex - 1);
		this.cursorIndex--;
		this.updateCommandSuggestions();
	}

	public void removePreviousWord() {
		if (this.cursorIndex == 0)
			return;
		int start = this.cursorIndex;
		while (start > 0 && Character.isWhitespace(this.chatInput.charAt(start - 1)))
			start--;
		while (start > 0 && !Character.isWhitespace(this.chatInput.charAt(start - 1)))
			start--;
		this.chatInput.delete(start, this.cursorIndex);
		this.cursorIndex = start;
		this.updateCommandSuggestions();
	}

	public void moveCursorLeft() {
		if (this.cursorIndex > 0)
			this.cursorIndex--;
	}

	public void moveCursorRight() {
		if (this.cursorIndex < this.chatInput.length())
			this.cursorIndex++;
	}

	public void moveCursorWordLeft() {
		if (this.cursorIndex == 0)
			return;
		int idx = this.cursorIndex;
		while (idx > 0 && Character.isWhitespace(this.chatInput.charAt(idx - 1)))
			idx--;
		while (idx > 0 && !Character.isWhitespace(this.chatInput.charAt(idx - 1)))
			idx--;
		this.cursorIndex = idx;
	}

	public void moveCursorWordRight() {
		if (this.cursorIndex >= this.chatInput.length()) {
			this.cursorIndex = this.chatInput.length();
			return;
		}
		int idx = this.cursorIndex;
		while (idx < this.chatInput.length() && Character.isWhitespace(this.chatInput.charAt(idx)))
			idx++;
		while (idx < this.chatInput.length() && !Character.isWhitespace(this.chatInput.charAt(idx)))
			idx++;
		this.cursorIndex = idx;
	}

	public String getChatInputText() {
		return this.chatInput.toString();
	}

	public void clearChatInput() {
		this.chatInput.setLength(0);
		this.cursorIndex = 0;
		this.commandSuggestions.clear();
		this.selectedSuggestion = -1;
	}

	private void updateCommandSuggestions() {
		this.commandSuggestions.clear();
		this.selectedSuggestion = -1;

		final String input = this.chatInput.toString().trim();
		if (!input.startsWith("/"))
			return;

		final String[] allCommands = {
				"/tp <x> <y> <z>",
				"/time set day",
				"/time set noon",
				"/time set night",
				"/time set sunrise",
				"/time set midnight",
				"/time set <0-23999>",
				"/time add <ticks>",
				"/time query",
				"/cycle on",
				"/cycle off",
				"/help",
				"/clear"
		};

		final String lowerInput = input.toLowerCase();
		for (final String cmd : allCommands)
			if (cmd.toLowerCase().startsWith(lowerInput) || lowerInput.length() < 2)
				this.commandSuggestions.add(cmd);

		if (!this.commandSuggestions.isEmpty())
			this.selectedSuggestion = 0;
	}

	public boolean dismissAutocomplete() {
		if (this.commandSuggestions.isEmpty())
			return false;
		this.commandSuggestions.clear();
		this.selectedSuggestion = -1;
		return true;
	}

	public void tabComplete() {
		if (this.commandSuggestions.isEmpty())
			return;

		if (this.selectedSuggestion >= 0 && this.selectedSuggestion < this.commandSuggestions.size()) {
			final String suggestion = this.commandSuggestions.get(this.selectedSuggestion);
			int spaceIndex = suggestion.indexOf(' ', this.cursorIndex);
			if (spaceIndex == -1)
				spaceIndex = suggestion.indexOf('<');
			if (spaceIndex == -1)
				spaceIndex = suggestion.length();

			final String completion = suggestion.substring(0, spaceIndex);
			this.chatInput.setLength(0);
			this.chatInput.append(completion);
			if (!completion.contains("<"))
				this.chatInput.append(" ");
			this.cursorIndex = this.chatInput.length();
			this.updateCommandSuggestions();
		}
	}

	public void cycleSuggestion(boolean forward) {
		if (this.commandSuggestions.isEmpty())
			return;
		if (this.selectedSuggestion == -1) {
			this.selectedSuggestion = 0;
			return;
		}

		if (forward)
			this.selectedSuggestion = (this.selectedSuggestion + 1) % this.commandSuggestions.size();
		else
			this.selectedSuggestion = (this.selectedSuggestion - 1 + this.commandSuggestions.size()) % this.commandSuggestions.size();
	}

	public boolean hasCommandSuggestions() {
		return !this.commandSuggestions.isEmpty();
	}

	public void selectNextSuggestion() {
		this.cycleSuggestion(true);
	}

	public void selectPreviousSuggestion() {
		this.cycleSuggestion(false);
	}

	public void addChatMessage(String message) {
		this.addChatMessage(message, 1, 1, 1);
	}

	public void addChatMessage(String message, float r, float g, float b) {
		this.chatHistory.add(new ChatMessage(message, r, g, b, System.currentTimeMillis()));
		while (this.chatHistory.size() > MAX_CHAT_HISTORY)
			this.chatHistory.remove(0);
	}

	public void addSystemMessage(String message) {
		this.addChatMessage(message, 1, 1, 0.5f);
	}

	public void addErrorMessage(String message) {
		this.addChatMessage(message, 1, 0.3f, 0.3f);
	}

	public void addSuccessMessage(String message) {
		this.addChatMessage(message, 0.3f, 1, 0.3f);
	}

	private static class ChatMessage {
		final String message;
		final float r, g, b;
		final long timestamp;

		ChatMessage(String message, float r, float g, float b, long timestamp) {
			this.message = message;
			this.r = r;
			this.g = g;
			this.b = b;
			this.timestamp = timestamp;
		}
	}

	public void cleanup() {
		this.textRenderer.cleanup();
		this.uiShader.cleanup();
		glDeleteBuffers(this.quadVbo);
		glDeleteVertexArrays(this.quadVao);
	}
}
