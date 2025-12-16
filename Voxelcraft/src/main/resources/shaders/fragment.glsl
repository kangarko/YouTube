#version 410 core

in vec2 fragTexCoord;
in vec3 fragNormal;
in vec3 fragPos;
in vec3 fragColor;
in float fragLeafData;
in float visibility;
in vec4 fragPosLightSpace;

out vec4 outColor;

uniform sampler2D textureSampler;
uniform sampler2DShadow shadowMap;

const vec3 lightDir = normalize(vec3(0.97, 0.26, 0.0));
const vec3 lightColor = vec3(1.0, 0.9, 0.8);
const vec3 ambientColor = vec3(0.5, 0.45, 0.5);
const vec3 skyColor = vec3(0.6, 0.45, 0.35);
const float seaLevel = 64.0;
const vec3 underwaterTint = vec3(0.2, 0.4, 0.6);

float calculateShadow(vec4 posLightSpace, vec3 normal) {
    vec3 projCoords = posLightSpace.xyz / posLightSpace.w;
    projCoords = projCoords * 0.5 + 0.5;

    if (projCoords.z > 1.0 || projCoords.x < 0.0 || projCoords.x > 1.0 ||
        projCoords.y < 0.0 || projCoords.y > 1.0)
        return 0.0;

    float bias = max(0.005 * (1.0 - dot(normal, lightDir)), 0.002);
    projCoords.z -= bias;

    float shadow = 0.0;
    vec2 texelSize = 1.0 / textureSize(shadowMap, 0);

    for (int x = -2; x <= 2; x++) {
        for (int y = -2; y <= 2; y++) {
            vec3 sampleCoords = vec3(projCoords.xy + vec2(x, y) * texelSize, projCoords.z);
            shadow += texture(shadowMap, sampleCoords);
        }
    }
    shadow /= 25.0;

    return 1.0 - shadow;
}

void main() {
    vec4 texColor = texture(textureSampler, fragTexCoord);

    if (texColor.a < 0.1)
        discard;

    vec3 tintedColor = texColor.rgb * fragColor;

    vec3 norm = normalize(fragNormal);
    float diff = max(dot(norm, lightDir), 0.0);

    float topFactor = max(0.0, norm.y);
    float sideFactor = 1.0 - abs(norm.y);
    float bottomFactor = max(0.0, -norm.y);

    float brightness = 1.0 * topFactor + 0.85 * sideFactor + 0.65 * bottomFactor;

    float shadow = calculateShadow(fragPosLightSpace, norm);

    vec3 ambient = ambientColor * tintedColor * 0.6;
    vec3 diffuse = diff * lightColor * tintedColor * 0.7 * (1.0 - shadow * 0.7);

    vec3 finalColor = (ambient + diffuse) * brightness;

    float alpha = texColor.a;
    bool isLeaf = fragLeafData > 0.0;
    bool isInternalLeaf = fragLeafData > 0.0 && fragLeafData < 1.0;

    if (isInternalLeaf) {
        float darkenFactor = 0.55;
        finalColor *= darkenFactor;
        alpha = 0.75;
    }
    else if (isLeaf)
        alpha = 0.92;

    float underwaterDepth = seaLevel - fragPos.y;
    if (underwaterDepth > 0.0) {
        float depthFactor = clamp(underwaterDepth / 20.0, 0.0, 1.0);
        float lightAttenuation = 1.0 - depthFactor * 0.7;
        finalColor *= lightAttenuation;
        finalColor = mix(finalColor, finalColor * underwaterTint, depthFactor * 0.6);
    }

    vec3 fogColor = skyColor * 0.9;
    float fogStrength = texColor.a < 1.0 ? visibility * 0.7 + 0.3 : visibility;
    if (isLeaf) {
        float distanceDarken = 0.4 + 0.6 * visibility;
        finalColor *= distanceDarken;
    }
    finalColor = mix(fogColor, finalColor, fogStrength);

    outColor = vec4(finalColor, alpha);
}
