#version 410 core

in vec3 fragPos;

out vec4 outColor;

uniform vec3 sunDirection;

void main() {
    vec3 dir = normalize(fragPos);
    float sunDot = dot(dir, sunDirection);

    float sunCore = smoothstep(0.995, 0.9999, sunDot);
    float sunGlow = smoothstep(0.9, 0.996, sunDot);
    sunGlow = pow(sunGlow, 2.0) * 0.8;
    float sunHalo = smoothstep(0.5, 0.92, sunDot);
    sunHalo = pow(sunHalo, 3.0) * 0.4;

    float brightness = sunCore + sunGlow + sunHalo;

    outColor = vec4(vec3(brightness), 1.0);
}
