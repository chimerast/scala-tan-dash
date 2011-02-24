attribute vec2 boneIndex;
attribute float boneWeight;

uniform mat4 modelViewMatrix[256];

varying vec3 normal;

void main() {
    normal = gl_NormalMatrix * gl_Normal;
    //vec4 position0 = gl_ProjectionMatrix * modelViewMatrix[int(boneIndex.x)] * gl_Vertex;
    //vec4 position1 = gl_ProjectionMatrix * modelViewMatrix[int(boneIndex.y)] * gl_Vertex;
    vec4 position0 = gl_ProjectionMatrix * gl_ModelViewMatrix * gl_Vertex;
    vec4 position1 = gl_ProjectionMatrix * gl_ModelViewMatrix * gl_Vertex;
    gl_Position = mix(position0, position1, boneWeight);
    gl_TexCoord[0] = gl_TextureMatrix[0] * gl_MultiTexCoord0;
}
