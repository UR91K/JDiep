//fragment.glsl
#version 330 core
out vec4 FragColor;

in vec2 TexCoords;

uniform sampler2D text;
uniform vec4 textColor;
uniform vec4 color;
uniform bool isText;

void main() {
    if (isText) {
        float alpha = texture(text, TexCoords).r;  // Font texture is single channel (red)
        FragColor = textColor * vec4(1.0, 1.0, 1.0, alpha);
    } else {
        FragColor = color;
    }
}