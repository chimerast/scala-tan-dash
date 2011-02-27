package org.karatachi.scala.opengl

import scala.math._
import java.nio._
import java.nio.channels._

import org.lwjgl._
import org.lwjgl.opengl._
import org.lwjgl.opengl.GL11._

case class Vector(x: Float, y: Float, z: Float) {
  def this(buffer: ByteBuffer) =
    this(buffer.getFloat, buffer.getFloat, -buffer.getFloat)
  def +(rhs: Vector) = Vector(x+rhs.x, y+rhs.y, z+rhs.z)
  def -(rhs: Vector) = Vector(x-rhs.x, y-rhs.y, z-rhs.z)
  def *(rhs: Vector) = Vector(x*rhs.x, y*rhs.y, z*rhs.z)
  def *(rhs: Float) = Vector(x*rhs, y*rhs, z*rhs)
  def unary_- = Vector(-x, -y, -z)
  def dot(rhs: Vector) = x*rhs.x + y*rhs.y + z*rhs.z
  def interporate(rhs: Vector, t: Float) = this*(1.0f-t) + rhs*t
}

case class Quaternion(x: Float, y: Float, z: Float, w: Float) {
  def this(buffer: ByteBuffer) =
    this(buffer.getFloat, buffer.getFloat, -buffer.getFloat, buffer.getFloat)
  def +(rhs: Quaternion) = Quaternion(x+rhs.x, y+rhs.y, z+rhs.z, w+rhs.w)
  def -(rhs: Quaternion) = Quaternion(x-rhs.x, y-rhs.y, z-rhs.z, w-rhs.w)
  def *(rhs: Quaternion) = Quaternion(w*rhs.x + x*rhs.w + y*rhs.z - z*rhs.y,
                                      w*rhs.y - x*rhs.z + y*rhs.w + z*rhs.x,
                                      w*rhs.z + x*rhs.y - y*rhs.x + z*rhs.w,
                                      w*rhs.w - x*rhs.x - y*rhs.y - z*rhs.z)
  def *(rhs: Float) = Quaternion(x*rhs, y*rhs, z*rhs, w*rhs)
  def unary_- = Quaternion(-x, -y, -z, -w)
  def dot(rhs: Quaternion) = x*rhs.x + y*rhs.y + z*rhs.z + w*rhs.w
  def slerp(rhs: Quaternion, t: Float): Quaternion = {
    var lhs = this
    var dot = this.dot(rhs)
    if (dot < 0.0f) { lhs = -this; dot *= -1.0f }
    if (dot >= 1.0f) dot = 1.0f
    val radian = acos(dot)
    if (abs(radian) < 0.0001f) return rhs
    val inverseSin = 1.0f / sin(radian)
    val leftScale = sin((1.0f-t) * radian) * inverseSin
    val rightScale = sin(t * radian) * inverseSin
    lhs * leftScale.toFloat + rhs * rightScale.toFloat
  }
  def setupMatrix(buffer: FloatBuffer): Unit = {
    val xx = x*x; val xy = x*y; val xz = x*z; val xw = x*w;
    val yy = y*y; val yz = y*z; val yw = y*w; val zz = z*z; val zw = z*w;
    buffer.put(1-2*(yy+zz)).put(2*(xy-zw)).put(2*(xz+yw)).put(0)
    buffer.put(2*(xy+zw)).put(1-2*(xx+zz)).put(2*(yz-xw)).put(0)
    buffer.put(2*(xz-yw)).put(2*(yz+xw)).put(1-2*(xx+yy)).put(0)
    buffer.put(0).put(0).put(0).put(1)
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
