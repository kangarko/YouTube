package com.matejpacan.voxelcraft;

import static org.lwjgl.opengl.GL20.glUseProgram;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class GodRays {

	private final ShaderProgram godRayShader;

	public GodRays() {
		this.godRayShader = this.createGodRayShader();
	}

	private ShaderProgram createGodRayShader() {
		final String vertexSource = """
									#version 410 core

									layout(location = 0) in vec2 aPosition;
									layout(location = 1) in vec2 aTexCoord;

									out vec2 vTexCoord;

									void main() {
									    vTexCoord = aTexCoord;
									    gl_Position = vec4(aPosition, 0.0, 1.0);
									}
									""";

		final String fragmentSource = """
										#version 410 core

										in vec2 vTexCoord;

										uniform sampler2D uSceneTexture;
										uniform sampler2D uDepthTexture;
										uniform vec2 uSunScreenPos;
										uniform float uSunOnScreen;
										uniform float uSunHeight;
										uniform vec3 uSunColor;
										uniform float uIntensity;
										uniform float uTime;

										out vec4 FragColor;

										const int NUM_SAMPLES = 64;
										const float DENSITY = 0.97;
										const float DECAY = 0.98;
										const float WEIGHT = 0.012;
										const float EXPOSURE = 1.0;

										float hash12(vec2 p) {
										    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
										    p3 += dot(p3, p3.yzx + 33.33);
										    return fract((p3.x + p3.y) * p3.z);
										}

										void main() {
										    vec3 sceneColor = texture(uSceneTexture, vTexCoord).rgb;
										    float currentDepth = texture(uDepthTexture, vTexCoord).r;

										    float sunVisibility = uSunOnScreen * smoothstep(-0.05, 0.2, uSunHeight);
										    if (sunVisibility < 0.001) {
										        FragColor = vec4(sceneColor, 1.0);
										        return;
										    }

										    vec2 toSun = uSunScreenPos - vTexCoord;
										    float distToSun = length(toSun);

										    float falloff = smoothstep(1.5, 0.0, distToSun);

										    vec2 deltaTexCoord = toSun * DENSITY / float(NUM_SAMPLES);

										    float jitter = hash12(gl_FragCoord.xy + fract(uTime) * 100.0);
										    vec2 sampleCoord = vTexCoord + deltaTexCoord * jitter;

										    float illuminationDecay = 1.0;
										    float godRayAccum = 0.0;
										    float occlusionAccum = 0.0;

										    for (int i = 0; i < NUM_SAMPLES; i++) {
										        sampleCoord += deltaTexCoord;

										        if (sampleCoord.x < 0.0 || sampleCoord.x > 1.0 || sampleCoord.y < 0.0 || sampleCoord.y > 1.0)
										            break;

										        float sampleDepth = texture(uDepthTexture, sampleCoord).r;

										        float isSky = step(0.9999, sampleDepth);

										        occlusionAccum += (1.0 - isSky) * 0.15;
										        float occlusion = exp(-occlusionAccum * 2.0);

										        float lightContribution = isSky * occlusion;

										        godRayAccum += lightContribution * illuminationDecay * WEIGHT;
										        illuminationDecay *= DECAY;

										        if (illuminationDecay < 0.01)
										            break;
										    }

										    godRayAccum *= EXPOSURE * falloff;

										    float sunsetFactor = smoothstep(0.3, 0.0, uSunHeight) * smoothstep(-0.1, 0.05, uSunHeight);
										    vec3 rayColor = mix(uSunColor, vec3(1.0, 0.5, 0.2), sunsetFactor * 0.6);

										    vec3 finalRays = vec3(godRayAccum) * rayColor * uIntensity * sunVisibility;

										    vec3 finalColor = sceneColor + finalRays;

										    FragColor = vec4(finalColor, 1.0);
										}
										""";

		return new ShaderProgram(vertexSource, fragmentSource);
	}

	public Vector4f computeSunScreenPos(Matrix4f projection, Matrix4f view, Vector3f sunDirection, Vector3f cameraPos) {
		final Vector4f sunWorldPos = new Vector4f(
				cameraPos.x + sunDirection.x * 1000.0f,
				cameraPos.y + sunDirection.y * 1000.0f,
				cameraPos.z + sunDirection.z * 1000.0f,
				1.0f);

		final Matrix4f viewProj = new Matrix4f();
		projection.mul(view, viewProj);

		final Vector4f sunClipPos = new Vector4f();
		viewProj.transform(sunWorldPos, sunClipPos);

		float sunOnScreen = 0.0f;
		float sunScreenX = 0.5f;
		float sunScreenY = 0.5f;

		if (sunClipPos.w > 0) {
			sunScreenX = sunClipPos.x / sunClipPos.w * 0.5f + 0.5f;
			sunScreenY = sunClipPos.y / sunClipPos.w * 0.5f + 0.5f;

			final float edgeDist = Math.max(
					Math.max(-sunScreenX, sunScreenX - 1.0f),
					Math.max(-sunScreenY, sunScreenY - 1.0f));

			if (edgeDist < 1.5f)
				sunOnScreen = 1.0f - Math.max(0.0f, Math.min(1.0f, edgeDist / 1.5f));
		}

		return new Vector4f(sunScreenX, sunScreenY, sunOnScreen, 0);
	}

	public void render(PostProcess postProcess, Matrix4f projection, Matrix4f view,
			Vector3f cameraPos, Vector3f sunDirection, Vector3f sunColor, float intensity, float time) {

		final Vector4f sunScreenData = this.computeSunScreenPos(projection, view, sunDirection, cameraPos);

		this.godRayShader.use();
		this.godRayShader.setInt("uSceneTexture", 0);
		this.godRayShader.setInt("uDepthTexture", 1);
		this.godRayShader.setVec2("uSunScreenPos", sunScreenData.x, sunScreenData.y);
		this.godRayShader.setFloat("uSunOnScreen", sunScreenData.z);
		this.godRayShader.setFloat("uSunHeight", sunDirection.y);
		this.godRayShader.setVec3("uSunColor", sunColor);
		this.godRayShader.setFloat("uIntensity", intensity);
		this.godRayShader.setFloat("uTime", time);

		postProcess.bindSceneTexture(0);
		postProcess.bindDepthTexture(1);
		postProcess.renderQuad();

		glUseProgram(0);
	}

	public void cleanup() {
		this.godRayShader.cleanup();
	}
}
