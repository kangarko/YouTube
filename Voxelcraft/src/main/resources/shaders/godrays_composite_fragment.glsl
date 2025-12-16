#version 410 core

in vec2 fragTexCoord;

out vec4 outColor;

uniform sampler2D sceneTexture;
uniform sampler2D occlusionTexture;
uniform vec2 sunScreenPos;
uniform float density;
uniform float weight;
uniform float decay;
uniform float exposure;
uniform int numSamples;

float computeSunVisibility(vec2 sunPos) {
    if (sunPos.x < -0.2 || sunPos.x > 1.2 || sunPos.y < -0.2 || sunPos.y > 1.2)
        return 0.0;

    float visibility = 0.0;
    float sampleRadius = 0.08;
    int sampleCount = 0;

    for (int x = -8; x <= 8; x++) {
        for (int y = -8; y <= 8; y++) {
            vec2 offset = vec2(x, y) * sampleRadius / 8.0;
            vec2 samplePos = sunPos + offset;
            if (samplePos.x >= 0.0 && samplePos.x <= 1.0 && samplePos.y >= 0.0 && samplePos.y <= 1.0) {
                visibility += texture(occlusionTexture, samplePos).r;
                sampleCount++;
            }
        }
    }

    if (sampleCount == 0)
        return 0.0;

    return visibility / float(sampleCount);
}

vec3 computeGodRays(vec2 uv, vec2 sunPos, float sunVisibility) {
    if (sunVisibility < 0.01)
        return vec3(0.0);

    vec2 toSun = sunPos - uv;
    vec2 deltaUV = toSun * density / float(numSamples);
    vec2 currentUV = uv;
    float illuminationDecay = 1.0;
    vec3 accumulator = vec3(0.0);

    for (int i = 0; i < numSamples; i++) {
        currentUV += deltaUV;

        if (currentUV.x < 0.0 || currentUV.x > 1.0 || currentUV.y < 0.0 || currentUV.y > 1.0)
            break;

        vec3 sampleColor = texture(occlusionTexture, currentUV).rgb;
        sampleColor *= illuminationDecay * weight;
        accumulator += sampleColor;
        illuminationDecay *= decay;
    }

    return accumulator * exposure * sunVisibility;
}

void main() {
    vec3 sceneColor = texture(sceneTexture, fragTexCoord).rgb;

    float sunVisibility = computeSunVisibility(sunScreenPos);

    vec3 godRaysColor = vec3(0.0);
    if (sunVisibility > 0.01)
        godRaysColor = computeGodRays(fragTexCoord, sunScreenPos, sunVisibility);

    vec3 rayTint = vec3(1.0, 0.9, 0.7);
    godRaysColor *= rayTint;

    float rayIntensity = length(godRaysColor);
    if (rayIntensity > 0.5)
        godRaysColor *= 0.5 / rayIntensity;

    vec3 finalColor = sceneColor + godRaysColor;

    outColor = vec4(finalColor, 1.0);
}
