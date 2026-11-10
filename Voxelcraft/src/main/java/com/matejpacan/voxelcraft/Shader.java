package com.matejpacan.voxelcraft;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;

public class Shader {

	private final int programId;
	private int vertexShaderId;
	private int fragmentShaderId;
	private final Map<String, Integer> uniforms;

	public Shader() {
		this.programId = GL20.glCreateProgram();
		if (this.programId == 0)
			throw new RuntimeException("Could not create shader program");
		this.uniforms = new HashMap<>();
	}

	public void createVertexShader(final String shaderCode) {
		this.vertexShaderId = this.createShader(shaderCode, GL20.GL_VERTEX_SHADER);
	}

	public void createFragmentShader(final String shaderCode) {
		this.fragmentShaderId = this.createShader(shaderCode, GL20.GL_FRAGMENT_SHADER);
	}

	private int createShader(final String shaderCode, final int shaderType) {
		final int shaderId = GL20.glCreateShader(shaderType);
		if (shaderId == 0)
			throw new RuntimeException("Error creating shader. Type: " + shaderType);

		GL20.glShaderSource(shaderId, shaderCode);
		GL20.glCompileShader(shaderId);

		if (GL20.glGetShaderi(shaderId, GL20.GL_COMPILE_STATUS) == 0)
			throw new RuntimeException("Error compiling shader: " + GL20.glGetShaderInfoLog(shaderId, 1024));

		GL20.glAttachShader(this.programId, shaderId);
		return shaderId;
	}

	public void link() {
		GL20.glLinkProgram(this.programId);
		if (GL20.glGetProgrami(this.programId, GL20.GL_LINK_STATUS) == 0)
			throw new RuntimeException(
					"Error linking shader program: " + GL20.glGetProgramInfoLog(this.programId, 1024));

		if (this.vertexShaderId != 0) {
			GL20.glDetachShader(this.programId, this.vertexShaderId);
			GL20.glDeleteShader(this.vertexShaderId);
		}
		if (this.fragmentShaderId != 0) {
			GL20.glDetachShader(this.programId, this.fragmentShaderId);
			GL20.glDeleteShader(this.fragmentShaderId);
		}
	}

	public void createUniform(final String uniformName) {
		final int uniformLocation = GL20.glGetUniformLocation(this.programId, uniformName);
		if (uniformLocation < 0)
			throw new RuntimeException("Could not find uniform: " + uniformName);
		this.uniforms.put(uniformName, uniformLocation);
	}

	public void setUniform(final String uniformName, final Matrix4f value) {
		final int location = this.getUniformLocation(uniformName);
		try (MemoryStack stack = MemoryStack.stackPush()) {
			final FloatBuffer fb = stack.mallocFloat(16);
			value.get(fb);
			GL20.glUniformMatrix4fv(location, false, fb);
		}
	}

	public void setUniform(final String uniformName, final int value) {
		GL20.glUniform1i(this.getUniformLocation(uniformName), value);
	}

	public void setUniform(final String uniformName, final float value) {
		GL20.glUniform1f(this.getUniformLocation(uniformName), value);
	}

	public void setUniform(final String uniformName, final Vector2f value) {
		final int location = this.getUniformLocation(uniformName);
		GL20.glUniform2f(location, value.x, value.y);
	}

	public void setUniform(final String uniformName, final Vector3f value) {
		final int location = this.getUniformLocation(uniformName);
		GL20.glUniform3f(location, value.x, value.y, value.z);
	}

	private int getUniformLocation(final String uniformName) {
		final Integer location = this.uniforms.get(uniformName);
		if (location == null)
			throw new RuntimeException("Uniform not found: " + uniformName);
		return location;
	}

	public void bind() {
		GL20.glUseProgram(this.programId);
	}

	public void unbind() {
		GL20.glUseProgram(0);
	}

	public void cleanup() {
		this.unbind();
		if (this.programId != 0)
			GL20.glDeleteProgram(this.programId);
	}
}
