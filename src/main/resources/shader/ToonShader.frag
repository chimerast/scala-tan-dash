uniform sampler2D texture0;
uniform sampler2D texture1;
uniform bool texturing;
uniform int sphere;

varying vec4 position;
varying vec3 normal;

void main() {
    vec3 V = normalize(-position.xyz);
    vec3 N = normalize(normal);
    vec3 L = normalize(gl_LightSource[0].position.xyz - position.xyz);

    float dotNL = dot(L, N);
    if (dotNL > 0.35) {
        gl_FragColor = vec4(1.0, 1.0, 1.0, 1.0) * gl_FrontMaterial.diffuse * gl_LightSource[0].diffuse;
    } else {
        gl_FragColor = vec4(0.9, 0.9, 0.9, 1.0) * gl_FrontMaterial.diffuse * gl_LightSource[0].diffuse;
    }

    gl_FragColor.rgb += gl_FrontMaterial.ambient.rgb * gl_LightSource[0].ambient.rgb;

    gl_FragColor = clamp(gl_FragColor, 0.0, 1.0);

    if (texturing) {
        gl_FragColor *= texture2D(texture0, gl_TexCoord[0].st);
    }

    if (sphere != 0) {
        vec2 st = N.xy * 0.5 + 0.5;
        vec4 px = texture2D(texture1, st);
        if (sphere == 1) {
            gl_FragColor.rgb *= px.rgb;
        } else if (sphere == 2) {
            gl_FragColor.rgb += px.rgb;
        }
    }

    if (dotNL > 0.0) {
        float NdotHV = max(dot(normal, gl_LightSource[0].halfVector.xyz), 0.0);
        gl_FragColor.rgb += gl_FrontMaterial.specular.rgb * gl_LightSource[0].specular.rgb
            * pow(dotNL, gl_FrontMaterial.shininess);
    }
}
