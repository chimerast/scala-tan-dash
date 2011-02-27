uniform mat4 modelViewMatrix[200];
uniform mat3 normalMatrix[200];

attribute vec2 boneIndex;
attribute float boneWeight;

void main() {
    vec4 position0 = gl_ProjectionMatrix * modelViewMatrix[int(boneIndex.x)] * gl_Vertex;
    vec4 position1 = gl_ProjectionMatrix * modelViewMatrix[int(boneIndex.y)] * gl_Vertex;
    vec3 normal0 = normalMatrix[int(boneIndex.x)] * gl_Normal;
    vec3 normal1 = normalMatrix[int(boneIndex.y)] * gl_Normal;
    vec4 position = mix(position0, position1, boneWeight);
    gl_Position = position;
    gl_FrontColor.xyz = normalize(mix(normal0, normal1, boneWeight));
    gl_FrontColor.w = position.z / position.w;
}
