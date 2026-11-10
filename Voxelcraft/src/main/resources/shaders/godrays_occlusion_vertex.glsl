#version 410 core

layout (location = 0) in vec3 position;
layout (location = 1) in vec2 texCoord;
layout (location = 2) in vec3 normal;
layout (location = 3) in vec4 color;

out vec2 fragTexCoord;
out vec3 fragNormal;

uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 modelMatrix;

void main() {
    vec4 worldPos = modelMatrix * vec4(position, 1.0);
    vec4 posRelativeToCamera = viewMatrix * worldPos;
    gl_Position = projectionMatrix * posRelativeToCamera;

    fragTexCoord = texCoord;
    fragNormal = mat3(transpose(inverse(modelMatrix))) * normal;
}
