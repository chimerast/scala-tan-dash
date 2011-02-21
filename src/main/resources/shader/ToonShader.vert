varying vec3 normal;

void main() {
  normal = gl_NormalMatrix * gl_Normal;

  gl_Position = ftransform();
  gl_FrontColor = gl_Color;
  gl_TexCoord[0] = gl_TextureMatrix[0] * gl_MultiTexCoord0;
} 
