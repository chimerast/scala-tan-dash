uniform mat4 modelViewMatrix[32];
uniform mat3 normalMatrix[32];

attribute vec2 boneIndex;
attribute float boneWeight;
attribute float edgeFlag;

void main() {
    ivec2 index = ivec2(boneIndex);
    vec4 position0 = modelViewMatrix[index.x] * gl_Vertex;
    vec4 position1 = modelViewMatrix[index.y] * gl_Vertex;
    vec3 normal0 = normalMatrix[index.x] * gl_Normal;
    vec3 normal1 = normalMatrix[index.y] * gl_Normal;
    vec4 position = mix(position0, position1, boneWeight);
    vec3 normal = normalize(mix(normal0, normal1, boneWeight));
    gl_Position = gl_ProjectionMatrix * mix(position0, position1, boneWeight);
    gl_Position.z += 0.02;
    if (edgeFlag == 0.0)
        gl_Position.xy += normal.xy * 0.015;
}
