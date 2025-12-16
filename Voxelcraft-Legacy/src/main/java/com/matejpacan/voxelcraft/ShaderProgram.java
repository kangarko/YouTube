package com.matejpacan.voxelcraft;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glDetachShader;
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20.glGetProgrami;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderi;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform2f;
import static org.lwjgl.opengl.GL20.glUniform3f;
import static org.lwjgl.opengl.GL20.glUniform3fv;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glUseProgram;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import lombok.Getter;

/**
 * OpenGL shader program wrapper with uniform caching.
 */
public class ShaderProgram {

	@Getter
	private final int programId;
	private final Map<String, Integer> uniformLocations = new HashMap<>();
	private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

	public ShaderProgram(String vertexSource, String fragmentSource) {
		// Compile vertex shader
		final int vertexShader = this.compileShader(GL_VERTEX_SHADER, vertexSource);

		// Compile fragment shader
		final int fragmentShader = this.compileShader(GL_FRAGMENT_SHADER, fragmentSource);

		// Link program
		this.programId = glCreateProgram();
		if (this.programId == 0)
			throw new RuntimeException("Failed to create shader program");

		glAttachShader(this.programId, vertexShader);
		glAttachShader(this.programId, fragmentShader);
		glLinkProgram(this.programId);

		// Check linking
		if (glGetProgrami(this.programId, GL_LINK_STATUS) == GL_FALSE) {
			final String log = glGetProgramInfoLog(this.programId);
			glDeleteProgram(this.programId);
			glDeleteShader(vertexShader);
			glDeleteShader(fragmentShader);
			throw new RuntimeException("Shader program linking failed:\n" + log);
		}

		// Cleanup individual shaders (they're linked now)
		glDetachShader(this.programId, vertexShader);
		glDetachShader(this.programId, fragmentShader);
		glDeleteShader(vertexShader);
		glDeleteShader(fragmentShader);
	}

	private int compileShader(int type, String source) {
		final int shader = glCreateShader(type);
		if (shader == 0)
			throw new RuntimeException("Failed to create shader of type: " + type);

		glShaderSource(shader, source);
		glCompileShader(shader);

		if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
			final String log = glGetShaderInfoLog(shader);
			glDeleteShader(shader);
			final String typeName = type == GL_VERTEX_SHADER ? "Vertex" : "Fragment";
			throw new RuntimeException(typeName + " shader compilation failed:\n" + log);
		}

		return shader;
	}

	/**
	 * Use this shader program for rendering.
	 */
	public void use() {
		glUseProgram(this.programId);
	}

	private int getUniformLocation(String name) {
		return this.uniformLocations.computeIfAbsent(name, n -> glGetUniformLocation(this.programId, n));
	}

	// Uniform setters

	public void setInt(String name, int value) {
		glUniform1i(this.getUniformLocation(name), value);
	}

	public void setBool(String name, boolean value) {
		glUniform1i(this.getUniformLocation(name), value ? 1 : 0);
	}

	public void setFloat(String name, float value) {
		glUniform1f(this.getUniformLocation(name), value);
	}

	public void setVec3(String name, Vector3f value) {
		glUniform3f(this.getUniformLocation(name), value.x, value.y, value.z);
	}

	public void setVec3(String name, float x, float y, float z) {
		glUniform3f(this.getUniformLocation(name), x, y, z);
	}

	public void setVec3Array(String name, float[] values) {
		if (values == null || values.length == 0)
			return;
		glUniform3fv(this.getUniformLocation(name), values);
	}

	public void setVec2(String name, float x, float y) {
		glUniform2f(this.getUniformLocation(name), x, y);
	}

	public void setMatrix4f(String name, Matrix4f matrix) {
		this.matrixBuffer.clear();
		matrix.get(this.matrixBuffer);
		glUniformMatrix4fv(this.getUniformLocation(name), false, this.matrixBuffer);
	}

	/**
	 * Cleanup OpenGL resources.
	 */
	public void cleanup() {
		if (this.programId != 0)
			glDeleteProgram(this.programId);
	}
}
