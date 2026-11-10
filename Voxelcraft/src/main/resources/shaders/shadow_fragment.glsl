#version 410 core

in vec2 fragTexCoord;

uniform sampler2D textureSampler;

void main() {
    vec4 texColor = texture(textureSampler, fragTexCoord);
    if (texColor.a < 0.5)
        discard;
}
