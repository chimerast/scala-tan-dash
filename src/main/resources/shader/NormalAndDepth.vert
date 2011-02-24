attribute vec2 boneIndex;
attribute float boneWeight;

uniform mat4 modelViewMatrix[256];

void main() {
    //vec4 position0 = gl_ProjectionMatrix * modelViewMatrix[int(boneIndex[0])] * gl_Vertex;
    //vec4 position1 = gl_ProjectionMatrix * modelViewMatrix[int(boneIndex[1])] * gl_Vertex;
    vec4 position0 = gl_ProjectionMatrix * gl_ModelViewMatrix * gl_Vertex;
    vec4 position1 = gl_ProjectionMatrix * gl_ModelViewMatrix * gl_Vertex;
    vec4 position = mix(position0, position1, boneWeight);
    gl_Position = position;
    gl_FrontColor.xyz = normalize(gl_NormalMatrix * gl_Normal);
    gl_FrontColor.w = position.z / position.w;
}
