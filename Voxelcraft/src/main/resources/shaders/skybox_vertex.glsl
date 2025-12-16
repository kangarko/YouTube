#version 410 core

layout (location = 0) in vec3 position;

out vec3 fragPos;

uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;

void main() {
    fragPos = position;
    vec4 pos = projectionMatrix * viewMatrix * vec4(position, 1.0);
    gl_Position = pos.xyww;
}
