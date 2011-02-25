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
}

case class Quaternion(x: Float, y: Float, z: Float, w: Float) {
  def this(buffer: ByteBuffer) =
    this(buffer.getFloat, buffer.getFloat, buffer.getFloat, buffer.getFloat)
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
