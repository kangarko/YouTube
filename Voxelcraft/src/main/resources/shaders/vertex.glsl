#version 410 core

layout (location = 0) in vec3 position;
layout (location = 1) in vec2 texCoord;
layout (location = 2) in vec3 normal;
layout (location = 3) in vec4 color;

out vec2 fragTexCoord;
out vec3 fragNormal;
out vec3 fragPos;
out vec3 fragColor;
out float fragLeafData;
out float visibility;
out vec4 fragPosLightSpace;

uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 modelMatrix;
uniform mat4 lightSpaceMatrix;

const float fogDensity = 0.007;
const float fogGradient = 1.5;

void main() {
    vec4 worldPos = modelMatrix * vec4(position, 1.0);
    vec4 posRelativeToCamera = viewMatrix * worldPos;
    gl_Position = projectionMatrix * posRelativeToCamera;

    fragTexCoord = texCoord;
    fragNormal = mat3(transpose(inverse(modelMatrix))) * normal;
    fragPos = worldPos.xyz;
    fragColor = color.rgb;
    fragLeafData = color.a;
    fragPosLightSpace = lightSpaceMatrix * worldPos;

    float distance = length(posRelativeToCamera.xyz);
    visibility = exp(-pow(distance * fogDensity, fogGradient));
    visibility = clamp(visibility, 0.0, 1.0);
}
