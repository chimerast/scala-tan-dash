uniform sampler2D texture0;
uniform sampler2D texture1;
uniform sampler2D texture2;
uniform bool texturing;
uniform int sphere;
uniform bool toon;

varying vec4 position;
varying vec3 normal;

void main() {
    vec3 P = position.xyz;
    vec3 L = normalize(gl_LightSource[0].position.xyz - P);
    vec3 N = normalize(normal);
    float dotNL = clamp(dot(N, L), 0.1, 1.0);
    vec3 V = normalize(-P);

    if (toon) {
        if (texturing) {
            gl_FragColor = texture2D(texture0, gl_TexCoord[0].st);
        } else {
            gl_FragColor = gl_FrontMaterial.diffuse;
        }
        gl_FragColor.rgb *= texture2D(texture2, vec2(0.0, 1.0-dotNL)).rgb;
    } else {
        gl_FragColor.rgb = gl_FrontLightProduct[0].diffuse.rgb * dotNL;
        gl_FragColor.a = gl_FrontLightProduct[0].diffuse.a;
    }

    gl_FragColor.rgb += gl_FrontLightProduct[0].ambient.rgb;
    gl_FragColor = clamp(gl_FragColor, 0.0, 1.0);


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
