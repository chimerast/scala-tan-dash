uniform sampler2D texture;

vec2 shift(vec2 tex, vec2 shift) {
  const float wh = 1.0 / 3000.0;
  return tex + (shift * wh);
}

void main() {
  vec2 tex = gl_TexCoord[0].st;
  vec4 normalAndDepth = vec4(0.0);
  normalAndDepth += texture2D(texture, shift(tex, vec2(-1.0,  1.0)));
  normalAndDepth += texture2D(texture, shift(tex, vec2( 0.0,  1.0)));
  normalAndDepth += texture2D(texture, shift(tex, vec2( 1.0,  1.0)));
  normalAndDepth += texture2D(texture, shift(tex, vec2(-1.0,  0.0)));
  normalAndDepth += texture2D(texture, shift(tex, vec2( 1.0,  0.0)));
  normalAndDepth += texture2D(texture, shift(tex, vec2(-1.0, -1.0)));
  normalAndDepth += texture2D(texture, shift(tex, vec2( 0.0, -1.0)));
  normalAndDepth += texture2D(texture, shift(tex, vec2( 1.0, -1.0)));
  normalAndDepth += texture2D(texture, tex) * -8.0;

  if (normalAndDepth.w >= 0.03 || length(normalAndDepth.xyz) >= 0.6) {
    gl_FragColor = vec4(0.0, 0.0, 0.0, 0.5);
  } else {
    discard;
  }
}
