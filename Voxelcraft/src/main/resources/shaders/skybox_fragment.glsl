#version 410 core

in vec3 fragPos;

out vec4 outColor;

uniform vec3 sunDirection;

const vec3 zenithColor = vec3(0.15, 0.25, 0.45);
const vec3 horizonColor = vec3(0.95, 0.55, 0.35);
const vec3 groundColor = vec3(0.12, 0.10, 0.15);

const vec3 sunCoreColor = vec3(1.0, 0.98, 0.9);
const vec3 sunGlowColor = vec3(1.0, 0.7, 0.3);
const vec3 sunHaloColor = vec3(1.0, 0.5, 0.2);

void main() {
    vec3 dir = normalize(fragPos);

    float horizonFactor = 1.0 - abs(dir.y);
    horizonFactor = pow(horizonFactor, 0.8);

    float sunInfluence = max(0.0, dot(dir, sunDirection));
    sunInfluence = pow(sunInfluence, 2.0);

    vec3 skyGradient;
    if (dir.y >= 0.0) {
        float t = pow(1.0 - dir.y, 1.5);
        skyGradient = mix(zenithColor, horizonColor, t);

        vec3 warmTint = vec3(1.0, 0.85, 0.7);
        skyGradient = mix(skyGradient, skyGradient * warmTint, sunInfluence * 0.4);
    } else {
        float t = pow(-dir.y, 0.5);
        skyGradient = mix(horizonColor, groundColor, t);
    }

    float sunDot = dot(dir, sunDirection);

    float sunCore = smoothstep(0.9995, 0.9999, sunDot);

    float sunGlow = smoothstep(0.98, 0.9998, sunDot);
    sunGlow = pow(sunGlow, 1.5);

    float sunHalo = smoothstep(0.9, 0.995, sunDot);
    sunHalo = pow(sunHalo, 2.0);

    float sunAtmosphere = smoothstep(0.5, 0.99, sunDot);
    sunAtmosphere = pow(sunAtmosphere, 4.0);

    vec3 finalColor = skyGradient;

    finalColor = mix(finalColor, sunHaloColor * 0.15 + finalColor, sunAtmosphere);
    finalColor = mix(finalColor, sunHaloColor * 0.4, sunHalo * 0.5);
    finalColor = mix(finalColor, sunGlowColor, sunGlow * 0.7);
    finalColor = mix(finalColor, sunCoreColor * 1.2, sunCore);

    float scattering = pow(max(0.0, sunDot), 8.0) * horizonFactor;
    vec3 scatterColor = vec3(1.0, 0.6, 0.3);
    finalColor += scatterColor * scattering * 0.15;

    float cloudNoise = fract(sin(dot(dir.xz, vec2(12.9898, 78.233))) * 43758.5453);
    float cloudFactor = smoothstep(0.6, 0.8, cloudNoise) * horizonFactor * 0.05;
    vec3 cloudColor = mix(vec3(0.9, 0.7, 0.5), vec3(1.0, 0.95, 0.9), sunInfluence);
    finalColor = mix(finalColor, cloudColor, cloudFactor);

    finalColor = pow(finalColor, vec3(1.0 / 2.2));

    outColor = vec4(finalColor, 1.0);
}
