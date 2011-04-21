package org.karatachi.scala.opengl

import java.nio._
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl._
import org.lwjgl._
import scala.math._

case class Vector(x: Float = 0.0f, y: Float = 0.0f, z: Float = 0.0f) {
  def this(buffer: ByteBuffer) = this(buffer.getFloat, buffer.getFloat, -buffer.getFloat)
  def +(rhs: Vector) = Vector(x + rhs.x, y + rhs.y, z + rhs.z)
  def -(rhs: Vector) = Vector(x - rhs.x, y - rhs.y, z - rhs.z)
  def *(rhs: Vector) = Vector(x * rhs.x, y * rhs.y, z * rhs.z)
  def *(rhs: Float) = Vector(x * rhs, y * rhs, z * rhs)
  def unary_- = Vector(-x, -y, -z)
  def dot(rhs: Vector) = x * rhs.x + y * rhs.y + z * rhs.z
  def cross(rhs: Vector) = Vector(y * rhs.z - z * rhs.y, z * rhs.x - x * rhs.z, x * rhs.y - y * rhs.x)
  def lerp(rhs: Vector, t: Float) = this * (1.0f - t) + rhs * t
  def lerp(rhs: Vector, tx: Float, ty: Float, tz: Float) = Vector(
    this.x * (1.0f - tx) + rhs.x * tx,
    this.y * (1.0f - ty) + rhs.y * ty,
    this.z * (1.0f - tz) + rhs.z * tz)
  def length = math.sqrt(x * x + y * y + z * z).toFloat
  def normalize: Vector = {
    this * (1.0f / length)
  }
}

case class Quaternion(x: Float = 0.0f, y: Float = 0.0f, z: Float = 0.0f, w: Float = 1.0f) {
  def this(buffer: ByteBuffer) = this(buffer.getFloat, buffer.getFloat, -buffer.getFloat, buffer.getFloat)
  def +(rhs: Quaternion) = Quaternion(x + rhs.x, y + rhs.y, z + rhs.z, w + rhs.w)
  def -(rhs: Quaternion) = Quaternion(x - rhs.x, y - rhs.y, z - rhs.z, w - rhs.w)
  def *(rhs: Quaternion) = Quaternion(
    w * rhs.x + x * rhs.w + y * rhs.z - z * rhs.y,
    w * rhs.y - x * rhs.z + y * rhs.w + z * rhs.x,
    w * rhs.z + x * rhs.y - y * rhs.x + z * rhs.w,
    w * rhs.w - x * rhs.x - y * rhs.y - z * rhs.z)
  def *(rhs: Float) = Quaternion(x * rhs, y * rhs, z * rhs, w * rhs)
  def unary_- = Quaternion(-x, -y, -z, -w)
  def dot(rhs: Quaternion) = x * rhs.x + y * rhs.y + z * rhs.z + w * rhs.w
  def length = math.sqrt(x * x + y * y + z * z + w * w).toFloat
  def normalize: Quaternion = {
    this * (1.0f / length)
  }
  def slerp(rhs: Quaternion, t: Float): Quaternion = {
    var lhs = this
    var dot = this.dot(rhs)
    if (dot < 0.0f) { lhs = -this; dot *= -1.0f }
    if (dot >= 1.0f) dot = 1.0f
    val radian = acos(dot)
    if (abs(radian) < 0.0001f) return rhs
    val inverseSin = 1.0f / sin(radian)
    val leftScale = sin((1.0f - t) * radian) * inverseSin
    val rightScale = sin(t * radian) * inverseSin
    lhs * leftScale.toFloat + rhs * rightScale.toFloat
  }
  def setupMatrix(buffer: FloatBuffer): Unit = {
    val xx = 2 * x * x; val xy = 2 * x * y; val xz = 2 * x * z; val xw = 2 * x * w;
    val yy = 2 * y * y; val yz = 2 * y * z; val yw = 2 * y * w; val zz = 2 * z * z; val zw = 2 * z * w;
    buffer.put(1 - yy - zz).put(xy - zw).put(xz + yw).put(0)
    buffer.put(xy + zw).put(1 - xx - zz).put(yz - xw).put(0)
    buffer.put(xz - yw).put(yz + xw).put(1 - xx - yy).put(0)
    buffer.put(0).put(0).put(0).put(1)
  }
}

object Quaternion {
  def fromAxisAngle(v: Vector, r: Float): Quaternion = {
    val sin = math.sin(r * 0.5f).toFloat
    val cos = math.cos(r * 0.5f).toFloat
    val n = v.normalize
    Quaternion(n.x * sin, n.y * sin, n.z * sin, cos)
  }
}

case class Coords2d(u: Float, v: Float) {
  def this(buffer: ByteBuffer) =
    this(buffer.getFloat, buffer.getFloat)
}

case class ColorRGBA(r: Float, g: Float, b: Float, a: Float) {
  def this(buffer: ByteBuffer) =
    this(buffer.getFloat, buffer.getFloat, buffer.getFloat, buffer.getFloat)
}

case class ColorRGB(r: Float, g: Float, b: Float) {
  def this(buffer: ByteBuffer) =
    this(buffer.getFloat, buffer.getFloat, buffer.getFloat)
}

class Material(buffer: ByteBuffer) {
  val diffuse = new ColorRGBA(buffer)
  val power = buffer.getFloat
  val specular = new ColorRGB(buffer)
  val ambient = new ColorRGB(buffer)

  private val values = {
    val values = BufferUtils.createFloatBuffer(12)
    values.put(diffuse.r).put(diffuse.g).put(diffuse.b).put(diffuse.a)
    values.put(specular.r).put(specular.g).put(specular.b).put(1.0f)
    values.put(ambient.r).put(ambient.g).put(ambient.b).put(1.0f)
    values.flip
    values
  }

  def bind = {
    values.position(0)
    glMaterial(GL_FRONT, GL_DIFFUSE, values)
    glMaterialf(GL_FRONT, GL_SHININESS, power)
    values.position(4)
    glMaterial(GL_FRONT, GL_SPECULAR, values)
    values.position(8)
    glMaterial(GL_FRONT, GL_AMBIENT, values)
  }
}
