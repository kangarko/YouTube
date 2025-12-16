package com.matejpacan.voxelcraft;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;

public class TextRenderer {

	private static final int BITMAP_W = 512;
	private static final int BITMAP_H = 512;
	private static final int FIRST_CHAR = 32;
	private static final int CHAR_COUNT = 96;
	private static final float HORIZONTAL_SCALE = 1.0f;

	private int fontTexture;
	private STBTTBakedChar.Buffer charData;
	private int vao;
	private int vbo;
	private ShaderProgram textShader;
	private final float fontSize;
	private boolean fontLoaded = false;

	private int screenWidth = 1;
	private int screenHeight = 1;

	public TextRenderer(final float fontSize) {
		this.fontSize = fontSize;

		try {
			this.initFont();
		} catch (final Exception e) {
			System.err.println("Failed to initialize font: " + e.getMessage());
			this.fontLoaded = false;
		}
	}

	private void initFont() {
		this.charData = STBTTBakedChar.malloc(TextRenderer.CHAR_COUNT);

		final ByteBuffer ttfBuffer = this.loadFont();
		if (ttfBuffer == null || ttfBuffer.remaining() < 100) {
			System.err.println("Font buffer is invalid or too small");
			this.fontLoaded = false;
			return;
		}

		final ByteBuffer bitmap = BufferUtils.createByteBuffer(TextRenderer.BITMAP_W * TextRenderer.BITMAP_H);

		final int result = STBTruetype.stbtt_BakeFontBitmap(ttfBuffer, this.fontSize, bitmap, TextRenderer.BITMAP_W,
				TextRenderer.BITMAP_H, TextRenderer.FIRST_CHAR,
				this.charData);
		if (result <= 0) {
			System.err.println("stbtt_BakeFontBitmap failed with result: " + result);
			this.fontLoaded = false;
			return;
		}

		this.fontTexture = GL11.glGenTextures();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.fontTexture);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RED, TextRenderer.BITMAP_W, TextRenderer.BITMAP_H, 0,
				GL11.GL_RED, GL11.GL_UNSIGNED_BYTE, bitmap);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

		this.vao = GL30.glGenVertexArrays();
		this.vbo = GL15.glGenBuffers();

		GL30.glBindVertexArray(this.vao);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vbo);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, 6 * 4 * 4096 * Float.BYTES, GL15.GL_DYNAMIC_DRAW);

		GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 4 * Float.BYTES, 0);
		GL20.glEnableVertexAttribArray(0);
		GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 4 * Float.BYTES, 2L * Float.BYTES);
		GL20.glEnableVertexAttribArray(1);

		GL30.glBindVertexArray(0);

		this.textShader = this.createTextShader();
		this.fontLoaded = true;
		System.out.println("Font loaded successfully!");
	}

	private ByteBuffer loadFont() {
		final String[] fontPaths = {
				"/System/Library/Fonts/Supplemental/Arial.ttf",
				"C:/Windows/Fonts/arial.ttf",
				"/usr/share/fonts/truetype/msttcorefonts/Arial.ttf",
				"/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
		};

		for (final String path : fontPaths)
			try {
				final java.io.File file = new java.io.File(path);
				if (file.exists() && file.canRead()) {
					final byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
					if (bytes.length > 100) {
						final ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
						buffer.put(bytes).flip();
						System.out.println("Loaded font: " + path);
						return buffer;
					}
				}
			} catch (final Exception e) {
				System.err.println("Failed to load font from " + path + ": " + e.getMessage());
			}

		throw new RuntimeException("No Arial font found on system");
	}

	private ShaderProgram createTextShader() {
		final String vertexSource = """
									#version 410 core
									layout(location = 0) in vec2 aPosition;
									layout(location = 1) in vec2 aTexCoord;

									uniform mat4 uProjection;

									out vec2 vTexCoord;

									void main() {
									    vTexCoord = aTexCoord;
									    gl_Position = uProjection * vec4(aPosition, 0.0, 1.0);
									}
									""";

		final String fragmentSource = """
										#version 410 core
										in vec2 vTexCoord;

										uniform sampler2D uFontTexture;
										uniform vec4 uColor;

										out vec4 FragColor;

										void main() {
										    float alpha = texture(uFontTexture, vTexCoord).r;
										    if (alpha < 0.1)
										        discard;
										    FragColor = vec4(uColor.rgb, uColor.a * alpha);
										}
										""";

		return new ShaderProgram(vertexSource, fragmentSource);
	}

	public void setScreenSize(final int width, final int height) {
		this.screenWidth = Math.max(1, width);
		this.screenHeight = Math.max(1, height);
	}

	public void drawText(final String text, final float x, final float y, final float r, final float g, final float b,
			final float a) {
		this.drawText(text, x, y, r, g, b, a, 1.0f);
	}

	public void drawText(final String text, final float x, final float y, final float r, final float g, final float b,
			final float a, final float scale) {
		if (!this.fontLoaded || text == null || text.isEmpty())
			return;

		final List<Float> vertices = new ArrayList<>();
		final float hScale = scale * TextRenderer.HORIZONTAL_SCALE;

		try (MemoryStack stack = MemoryStack.stackPush()) {
			final FloatBuffer xpos = stack.floats(0);
			final FloatBuffer ypos = stack.floats(this.fontSize);
			final STBTTAlignedQuad q = STBTTAlignedQuad.malloc(stack);

			for (int i = 0; i < text.length(); i++) {
				final char c = text.charAt(i);
				if (c < TextRenderer.FIRST_CHAR || c >= TextRenderer.FIRST_CHAR + TextRenderer.CHAR_COUNT)
					continue;

				STBTruetype.stbtt_GetBakedQuad(this.charData, TextRenderer.BITMAP_W, TextRenderer.BITMAP_H,
						c - TextRenderer.FIRST_CHAR, xpos, ypos, q, true);

				final float x0 = x + q.x0() * hScale;
				final float y0 = y + q.y0() * scale;
				final float x1 = x + q.x1() * hScale;
				final float y1 = y + q.y1() * scale;

				vertices.add(x0);
				vertices.add(y0);
				vertices.add(q.s0());
				vertices.add(q.t0());
				vertices.add(x1);
				vertices.add(y0);
				vertices.add(q.s1());
				vertices.add(q.t0());
				vertices.add(x1);
				vertices.add(y1);
				vertices.add(q.s1());
				vertices.add(q.t1());

				vertices.add(x0);
				vertices.add(y0);
				vertices.add(q.s0());
				vertices.add(q.t0());
				vertices.add(x1);
				vertices.add(y1);
				vertices.add(q.s1());
				vertices.add(q.t1());
				vertices.add(x0);
				vertices.add(y1);
				vertices.add(q.s0());
				vertices.add(q.t1());
			}
		}

		if (vertices.isEmpty())
			return;

		final float[] vertexArray = new float[vertices.size()];
		for (int i = 0; i < vertices.size(); i++)
			vertexArray[i] = vertices.get(i);

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vbo);
		final FloatBuffer buffer = BufferUtils.createFloatBuffer(vertexArray.length);
		buffer.put(vertexArray).flip();
		GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, buffer);

		final Matrix4f ortho = new Matrix4f().ortho(0, this.screenWidth, this.screenHeight, 0, -1, 1);

		this.textShader.use();
		this.textShader.setMatrix4f("uProjection", ortho);
		this.textShader.setInt("uFontTexture", 0);
		GL20.glUniform4f(GL20.glGetUniformLocation(this.textShader.getProgramId(), "uColor"), r, g, b, a);

		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.fontTexture);

		GL30.glBindVertexArray(this.vao);
		GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, vertexArray.length / 4);
		GL30.glBindVertexArray(0);
	}

	public void drawTextWithShadow(final String text, final float x, final float y, final float r, final float g,
			final float b, final float a) {
		this.drawTextWithShadow(text, x, y, r, g, b, a, 1.0f);
	}

	public void drawTextWithShadow(final String text, final float x, final float y, final float r, final float g,
			final float b, final float a, final float scale) {
		final float shadowOffset = 2 * scale;
		this.drawText(text, x + shadowOffset, y + shadowOffset, 0.1f, 0.1f, 0.1f, a * 0.7f, scale);
		this.drawText(text, x, y, r, g, b, a, scale);
	}

	public float getTextWidth(final String text) {
		if (!this.fontLoaded || text == null || text.isEmpty())
			return text != null ? text.length() * this.fontSize * 0.4f : 0;

		float width = 0;
		try (MemoryStack stack = MemoryStack.stackPush()) {
			final FloatBuffer xpos = stack.floats(0);
			final FloatBuffer ypos = stack.floats(this.fontSize);
			final STBTTAlignedQuad q = STBTTAlignedQuad.malloc(stack);

			for (int i = 0; i < text.length(); i++) {
				final char c = text.charAt(i);
				if (c < TextRenderer.FIRST_CHAR || c >= TextRenderer.FIRST_CHAR + TextRenderer.CHAR_COUNT)
					continue;
				STBTruetype.stbtt_GetBakedQuad(this.charData, TextRenderer.BITMAP_W, TextRenderer.BITMAP_H,
						c - TextRenderer.FIRST_CHAR, xpos, ypos, q, true);
			}
			width = xpos.get(0) * TextRenderer.HORIZONTAL_SCALE;
		}
		return width;
	}

	public float getFontSize() {
		return this.fontSize;
	}

	public boolean isFontLoaded() {
		return this.fontLoaded;
	}

	public void cleanup() {
		if (this.fontLoaded) {
			GL11.glDeleteTextures(this.fontTexture);
			GL15.glDeleteBuffers(this.vbo);
			GL30.glDeleteVertexArrays(this.vao);
			if (this.textShader != null)
				this.textShader.cleanup();
			if (this.charData != null)
				this.charData.free();
		}
	}
}
