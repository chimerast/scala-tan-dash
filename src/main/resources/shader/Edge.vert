uniform mat4 modelViewMatrix[200];
uniform mat3 normalMatrix[200];

attribute vec2 boneIndex;
attribute float boneWeight;

void main() {
    ivec2 index = ivec2(boneIndex);
    vec4 position0 = gl_ProjectionMatrix * modelViewMatrix[index.x] * gl_Vertex;
    vec4 position1 = gl_ProjectionMatrix * modelViewMatrix[index.y] * gl_Vertex;
    vec4 normal0 = gl_ProjectionMatrix * modelViewMatrix[index.x] * vec4(gl_Normal, 0.0);
    vec4 normal1 = gl_ProjectionMatrix * modelViewMatrix[index.y] * vec4(gl_Normal, 0.0);
    vec2 normal = normalize(mix(normal0, normal1, boneWeight).xy) * 0.01;
    gl_Position = mix(position0, position1, boneWeight) + vec4(normal.x, normal.y, 0.02, 0.0);
}
