package com.matejpacan.voxelcraft;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glDepthMask;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

import java.nio.FloatBuffer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

public class VolumetricClouds {

	private final int vao;
	private final int vbo;
	private final ShaderProgram cloudShader;
	private float time = 0.0f;

	public VolumetricClouds() {
		final float[] quadVertices = {
				-1.0f, -1.0f, 0.0f, 0.0f,
				1.0f, -1.0f, 1.0f, 0.0f,
				1.0f, 1.0f, 1.0f, 1.0f,
				-1.0f, -1.0f, 0.0f, 0.0f,
				1.0f, 1.0f, 1.0f, 1.0f,
				-1.0f, 1.0f, 0.0f, 1.0f,
		};

		this.vao = glGenVertexArrays();
		this.vbo = glGenBuffers();

		glBindVertexArray(this.vao);
		glBindBuffer(GL_ARRAY_BUFFER, this.vbo);

		final FloatBuffer buffer = BufferUtils.createFloatBuffer(quadVertices.length);
		buffer.put(quadVertices).flip();
		glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

		glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
		glEnableVertexAttribArray(0);
		glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2L * Float.BYTES);
		glEnableVertexAttribArray(1);

		glBindVertexArray(0);

		this.cloudShader = this.createCloudShader();
	}

	private ShaderProgram createCloudShader() {
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

										uniform mat4 uInvProjection;
										uniform mat4 uInvView;
										uniform vec3 uCameraPos;
										uniform vec3 uSunDirection;
										uniform float uTime;
										uniform float uDayTime;

										layout(location = 0) out vec4 FragColor;
										layout(location = 1) out vec4 GBuffer;

										const float CLOUD_MIN = 150.0;
										const float CLOUD_MAX = 350.0;
										const float CLOUD_SCALE = 0.0003;
										const float PI = 3.14159265359;

										float hash(vec3 p) {
										    p = fract(p * vec3(443.897, 441.423, 437.195));
										    p += dot(p, p.yzx + 19.19);
										    return fract((p.x + p.y) * p.z);
										}

										float hash12(vec2 p) {
										    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
										    p3 += dot(p3, p3.yzx + 33.33);
										    return fract((p3.x + p3.y) * p3.z);
										}

										float noise3D(vec3 p) {
										    vec3 i = floor(p);
										    vec3 f = fract(p);
										    f = f * f * (3.0 - 2.0 * f);

										    float n000 = hash(i);
										    float n100 = hash(i + vec3(1,0,0));
										    float n010 = hash(i + vec3(0,1,0));
										    float n110 = hash(i + vec3(1,1,0));
										    float n001 = hash(i + vec3(0,0,1));
										    float n101 = hash(i + vec3(1,0,1));
										    float n011 = hash(i + vec3(0,1,1));
										    float n111 = hash(i + vec3(1,1,1));

										    return mix(
										        mix(mix(n000, n100, f.x), mix(n010, n110, f.x), f.y),
										        mix(mix(n001, n101, f.x), mix(n011, n111, f.x), f.y),
										        f.z
										    );
										}

										float sampleCloudDensity(vec3 pos, float lod) {
										    vec3 wind = vec3(uTime * 6.0, 0.0, uTime * 3.0);
										    vec3 samplePos = pos * CLOUD_SCALE + wind * CLOUD_SCALE;

										    float heightFraction = (pos.y - CLOUD_MIN) / (CLOUD_MAX - CLOUD_MIN);
										    float heightGradient = smoothstep(0.0, 0.2, heightFraction) * smoothstep(1.0, 0.7, heightFraction);

										    float baseNoise = noise3D(samplePos);
										    float detailNoise = noise3D(samplePos * 2.5) * 0.5;

										    float coverage = noise3D(samplePos * 0.4 + vec3(100.0));
										    coverage = smoothstep(0.25, 0.55, coverage);

										    float density = (baseNoise + detailNoise) * 1.8 - 0.5;
										    density = max(0.0, density * coverage);
										    density *= heightGradient;
										    density *= 0.7;

										    return density;
										}

										float henyeyGreenstein(float cosTheta, float g) {
										    float g2 = g * g;
										    return (1.0 - g2) / (4.0 * PI * pow(1.0 + g2 - 2.0 * g * cosTheta, 1.5));
										}

										float lightMarch(vec3 pos, vec3 lightDir) {
										    float stepSize = 50.0;
										    float totalDensity = 0.0;

										    pos += lightDir * stepSize;
										    totalDensity += sampleCloudDensity(pos, 1.0) * stepSize * 0.4;
										    pos += lightDir * stepSize;
										    totalDensity += sampleCloudDensity(pos, 1.0) * stepSize * 0.3;

										    return exp(-totalDensity * 0.5);
										}

										vec2 rayCloudIntersect(vec3 rayOrigin, vec3 rayDir) {
										    float tMin = (CLOUD_MIN - rayOrigin.y) / rayDir.y;
										    float tMax = (CLOUD_MAX - rayOrigin.y) / rayDir.y;
										    if (tMin > tMax) {
										        float temp = tMin;
										        tMin = tMax;
										        tMax = temp;
										    }
										    return vec2(max(0.0, tMin), max(0.0, tMax));
										}

										vec4 raymarchClouds(vec3 rayOrigin, vec3 rayDir, vec3 lightDir, float dayFactor, vec3 sunColor, vec3 ambientTop, float jitter) {
										    vec2 intersection = rayCloudIntersect(rayOrigin, rayDir);
										    float tStart = intersection.x;
										    float tEnd = intersection.y;

										    if (tStart >= tEnd || tEnd <= 0.0)
										        return vec4(0.0);

										    float maxDist = 600.0;
										    tEnd = min(tEnd, tStart + maxDist);

										    const int steps = 8;
										    float stepSize = (tEnd - tStart) / float(steps);
										    vec3 pos = rayOrigin + rayDir * (tStart + stepSize * jitter * 0.5);

										    float cosAngle = dot(rayDir, lightDir);
										    float phase = mix(henyeyGreenstein(cosAngle, 0.3), henyeyGreenstein(cosAngle, -0.3), 0.5);

										    float transmittance = 1.0;
										    vec3 luminance = vec3(0.0);

										    for (int i = 0; i < steps; i++) {
										        float lod = float(i) / float(steps);
										        float density = sampleCloudDensity(pos, lod);

										        if (density > 0.005) {
										            float heightFrac = (pos.y - CLOUD_MIN) / (CLOUD_MAX - CLOUD_MIN);
										            vec3 ambient = ambientTop * (0.4 + 0.6 * heightFrac);

										            float lightEnergy = lightMarch(pos, lightDir);

										            vec3 directLight = sunColor * lightEnergy * phase * dayFactor * 2.5;

										            float silverLining = pow(lightEnergy, 0.5) * pow(max(cosAngle, 0.0), 3.0);
										            directLight += vec3(1.0, 0.98, 0.95) * silverLining * dayFactor * 0.5;

										            vec3 scattering = directLight + ambient * 0.7;

										            float extinction = density * stepSize * 0.6;
										            float sampleTransmittance = exp(-extinction);

										            luminance += transmittance * scattering * (1.0 - sampleTransmittance);
										            transmittance *= sampleTransmittance;

										            if (transmittance < 0.03)
										                break;
										        }

										        pos += rayDir * stepSize;
										    }

										    return vec4(luminance, 1.0 - transmittance);
										}

										void main() {
										    float nightFade = smoothstep(-0.1, 0.2, uSunDirection.y);
										    if (nightFade < 0.01) {
										        FragColor = vec4(0.0);
										        GBuffer = vec4(0.0, 0.0, 0.0, 1.0);
										        return;
										    }

										    vec2 ndc = vTexCoord * 2.0 - 1.0;

										    vec4 clipPos = vec4(ndc, 1.0, 1.0);
										    vec4 viewPos = uInvProjection * clipPos;
										    viewPos /= viewPos.w;

										    vec4 worldPos = uInvView * vec4(viewPos.xyz, 0.0);
										    vec3 rayDir = normalize(worldPos.xyz);

										    if (rayDir.y < 0.02) {
										        FragColor = vec4(0.0);
										        GBuffer = vec4(0.0, 0.0, 0.0, 1.0);
										        return;
										    }

										    float jitter = hash12(gl_FragCoord.xy + fract(uTime) * 100.0);

										    vec3 lightDir = normalize(uSunDirection);
										    float dayFactor = smoothstep(-0.05, 0.35, uSunDirection.y);
										    float sunsetFactor = pow(1.0 - abs(uSunDirection.y), 2.5) * step(0.0, uSunDirection.y);

										    vec3 sunColor = mix(vec3(1.3, 0.6, 0.3), vec3(1.0, 0.98, 0.95), smoothstep(0.0, 0.35, uSunDirection.y));
										    sunColor = mix(sunColor, vec3(1.25, 0.6, 0.35), sunsetFactor * 0.4);

										    vec3 ambientTop = mix(vec3(0.15, 0.17, 0.25), vec3(0.55, 0.62, 0.8), dayFactor);

										    vec4 clouds = raymarchClouds(uCameraPos, rayDir, lightDir, dayFactor, sunColor, ambientTop, jitter);

										    float horizonFade = smoothstep(0.02, 0.12, rayDir.y);
										    clouds.a *= horizonFade * nightFade;

										    clouds.rgb = clouds.rgb / (clouds.rgb + vec3(1.0));
										    clouds.rgb = pow(clouds.rgb, vec3(1.0 / 2.2));

										    FragColor = clouds;
										    GBuffer = vec4(0.0, 0.0, 0.0, 1.0);
										}
										""";

		return new ShaderProgram(vertexSource, fragmentSource);
	}

	public void update(float deltaTime) {
		this.time += deltaTime;
	}

	public void render(Matrix4f projection, Matrix4f view, Vector3f cameraPos, Vector3f sunDirection, float dayTime) {
		final Matrix4f invProjection = new Matrix4f();
		projection.invert(invProjection);

		final Matrix4f invView = new Matrix4f();
		view.invert(invView);

		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glDepthMask(false);

		this.cloudShader.use();
		this.cloudShader.setMatrix4f("uInvProjection", invProjection);
		this.cloudShader.setMatrix4f("uInvView", invView);
		this.cloudShader.setVec3("uCameraPos", cameraPos);
		this.cloudShader.setVec3("uSunDirection", sunDirection);
		this.cloudShader.setFloat("uTime", this.time);
		this.cloudShader.setFloat("uDayTime", dayTime);

		glBindVertexArray(this.vao);
		glDrawArrays(GL_TRIANGLES, 0, 6);
		glBindVertexArray(0);

		glDepthMask(true);
		glUseProgram(0);
	}

	public void cleanup() {
		glDeleteVertexArrays(this.vao);
		glDeleteBuffers(this.vbo);
		this.cloudShader.cleanup();
	}
}
