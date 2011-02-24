attribute vec2 boneindex;
attribute float boneweight;

void main() {
    vec4 position = ftransform();

    gl_Position = position;
    gl_FrontColor.xyz = normalize(gl_NormalMatrix * gl_Normal);
    gl_FrontColor.w = position.z / position.w;
}
