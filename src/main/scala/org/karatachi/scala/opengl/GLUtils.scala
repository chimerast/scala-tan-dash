package org.karatachi.scala.opengl

import java.awt.{ GraphicsEnvironment, Font }
import java.nio._

import org.lwjgl._
import org.lwjgl.opengl._
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.ARBBufferObject._
import org.lwjgl.opengl.ARBVertexBufferObject._
import org.newdawn.slick._
import org.newdawn.slick.opengl._
import org.newdawn.slick.font.effects._

object GLUtils {
  val defaultFont: UnicodeFont = {
    import java.awt.{ GraphicsEnvironment, Font }

    val candidate = List("HiraKakuPro-W3", "Meiryo", Font.SANS_SERIF)
    val genv = GraphicsEnvironment.getLocalGraphicsEnvironment
    val defaultFontName = candidate.intersect(genv.getAllFonts.map(_.getName))(0)

    val font = new UnicodeFont(new Font(defaultFontName, Font.PLAIN, 12))
    font.getEffects.asInstanceOf[java.util.List[Effect]].add(new ColorEffect())
    font
  }

  def glLoadFontGlyphs = {
    defaultFont.clearGlyphs
    defaultFont.addNeheGlyphs
    defaultFont.loadGlyphs
  }

  def glDrawString(x: Float, y: Float, str: String, color: Color) = {
    glDisable(GL_DEPTH_TEST)
    glDisable(GL_LIGHTING)
    defaultFont.drawString(x, y, str, color)
  }

  def glDrawImage(x: Float, y: Float, texture: Texture) = {
    glDisable(GL_DEPTH_TEST)
    glDisable(GL_LIGHTING)
    glEnable(GL_TEXTURE_2D)
    glColor3f(1.0f, 1.0f, 1.0f)
    texture.bind
    glRender(GL_QUADS) {
      glTexCoord2f(0.0f, 0.0f)
      glVertex2f(x, y)
      glTexCoord2f(1.0f, 0.0f)
      glVertex2f(x + texture.getTextureWidth, y)
      glTexCoord2f(1.0f, 1.0f)
      glVertex2f(x + texture.getTextureWidth, y + texture.getTextureHeight)
      glTexCoord2f(0.0f, 1.0f)
      glVertex2f(x, y + texture.getTextureHeight)
    }
  }

  def glLightfv(light: Int, pname: Int, params: Array[Float]): Unit = {
    val buffer = BufferUtils.createFloatBuffer(params.length)
    buffer.put(params)
    buffer.flip
    glLight(light, pname, buffer)
  }

  def glRender(mode: Int)(block: => Unit): Unit = {
    glBegin(mode)
    block
    glEnd
  }

  def glList(mode: Int)(block: => Unit): Int = {
    val list = glGenLists(1)
    glNewList(list, GL_COMPILE)
    block
    glEndList
    list
  }

  def glLoadBufferObject(target: Int, buffer: ShortBuffer): Int = {
    val id = glGenBuffersARB
    glBindBufferARB(target, id)
    glBufferDataARB(target, buffer, GL_STATIC_DRAW_ARB)
    glBindBufferARB(target, 0)
    id
  }

  def glLoadBufferObject(target: Int, buffer: IntBuffer): Int = {
    val id = glGenBuffersARB
    glBindBufferARB(target, id)
    glBufferDataARB(target, buffer, GL_STATIC_DRAW_ARB)
    glBindBufferARB(target, 0)
    id
  }

  def glLoadBufferObject(target: Int, buffer: FloatBuffer): Int = {
    val id = glGenBuffersARB
    glBindBufferARB(target, id)
    glBufferDataARB(target, buffer, GL_STATIC_DRAW_ARB)
    glBindBufferARB(target, 0)
    id
  }

  def glMatrix(block: => Unit) = {
    glPushMatrix
    block
    glPopMatrix
  }

  def glOrthogonal(block: => Unit) = {
    val displayMode = Display.getDisplayMode
    glMatrixMode(GL_PROJECTION)
    glPushMatrix
    glLoadIdentity
    glOrtho(0.0, displayMode.getWidth, displayMode.getHeight, 0.0, -1.0, 1.0)
    glMatrixMode(GL_MODELVIEW)
    glPushMatrix
    glLoadIdentity
    block
    glMatrixMode(GL_PROJECTION)
    glPopMatrix
    glMatrixMode(GL_MODELVIEW)
    glPopMatrix
  }
}
