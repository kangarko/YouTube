#version 410 core

in vec2 fragTexCoord;
in vec3 fragNormal;

out vec4 outColor;

uniform sampler2D textureSampler;
uniform int isLeaves;

void main() {
    vec4 texColor = texture(textureSampler, fragTexCoord);

    if (texColor.a < 0.1)
        discard;

    if (isLeaves == 1) {
        float occlusion = texColor.a * 0.6;
        outColor = vec4(vec3(1.0 - occlusion), 1.0);
    } else {
        outColor = vec4(0.0, 0.0, 0.0, 1.0);
    }
}
