uniform sampler2D texture;
uniform bool texturing;

varying vec3 normal;

void main() {
    vec3 n = normalize(normal);
    vec4 color;
    float intensity;

    intensity = dot(normalize(vec3(gl_LightSource[0].position)), n);

    if (intensity > 0.90)
        color = vec4(1.0, 1.0, 1.0, 1.0) * gl_Color + vec4(0.2, 0.2, 0.2, 0.2);
    else if (intensity > 0.05)
        color = vec4(1.0, 1.0, 1.0, 1.0) * gl_Color;
    else
        color = vec4(0.9, 0.9, 0.9, 1.0) * gl_Color;

    if (!texturing) {
        gl_FragColor = color;
    } else {
        gl_FragColor = color * texture2DProj(texture, gl_TexCoord[0]);
    }
}
