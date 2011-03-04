uniform mat4 modelViewMatrix[32];
uniform mat3 normalMatrix[32];

varying vec4 position;
varying vec3 normal;

attribute vec2 boneIndex;
attribute float boneWeight;
attribute float edgeFlag;

void main() {
    ivec2 index = ivec2(boneIndex);
    vec4 position0 = modelViewMatrix[index.x] * gl_Vertex;
    vec4 position1 = modelViewMatrix[index.y] * gl_Vertex;
    vec3 normal0 = normalMatrix[index.x] * gl_Normal;
    vec3 normal1 = normalMatrix[index.y] * gl_Normal;
    position = mix(position0, position1, boneWeight);
    normal = normalize(mix(normal0, normal1, boneWeight));
    gl_Position = gl_ProjectionMatrix * position;
    gl_TexCoord[0] = gl_MultiTexCoord0;
}
