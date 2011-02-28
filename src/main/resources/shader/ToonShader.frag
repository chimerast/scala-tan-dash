uniform sampler2D texture0;
uniform sampler2D texture1;
uniform bool texturing;
uniform int sphere;

varying vec4 position;
varying vec3 normal;

void main() {
    vec3 P = position.xyz;
    vec3 L = normalize(gl_LightSource[0].position.xyz - P);
    vec3 N = normalize(normal);
    float dotNL = dot(N, L);
    vec3 V = normalize(-P);

    if (dotNL > 0.05)
        gl_FragColor = vec4(1.0, 1.0, 1.0, 1.0) * gl_FrontLightProduct[0].diffuse;
    else
        gl_FragColor = vec4(0.8, 0.8, 0.8, 1.0) * gl_FrontLightProduct[0].diffuse;

    gl_FragColor.rgb += gl_FrontLightProduct[0].ambient.rgb;
    gl_FragColor = clamp(gl_FragColor, 0.0, 1.0);

    if (texturing)
        gl_FragColor *= texture2D(texture0, gl_TexCoord[0].st);

    if (sphere != 0) {
        vec4 tex = texture2D(texture1, N.xy * 0.5 + 0.5);
        if (sphere == 1) {
            gl_FragColor.rgb *= tex.rgb;
        } else if (sphere == 2) {
            gl_FragColor.rgb += tex.rgb;
        }
    }

    if (dotNL > 0.0) {
        vec3 H = normalize(L + V);
        float powNH = pow(max(dot(H, N), 0.0), gl_FrontMaterial.shininess);
        gl_FragColor.rgb += gl_FrontLightProduct[0].specular.rgb * powNH;
    }
}
