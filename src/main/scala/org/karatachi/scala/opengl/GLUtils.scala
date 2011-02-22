package org.karatachi.scala.opengl

import java.awt.{ GraphicsEnvironment, Font }
import java.nio._

import org.lwjgl._
import org.lwjgl.opengl._
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL15._
import org.lwjgl.opengl.GL20._
import org.newdawn.slick._
import org.newdawn.slick.opengl._
import org.newdawn.slick.font.effects._

object GLUtils {
  def checkSupport(): Unit = {
    val caps = GLContext.getCapabilities

    println("Vendor: " + glGetString(GL_VENDOR))
    println("GPU: " + glGetString(GL_RENDERER))
    println("OpenGL: " + glGetString(GL_VERSION))
    println("GLSL: " + glGetString(GL_SHADING_LANGUAGE_VERSION))
    println("framebuffer_object: " +  caps.GL_EXT_framebuffer_object)
  }

  val defaultFont: UnicodeFont = {
    import java.awt.{ GraphicsEnvironment, Font }

    val candidate = List("HiraKakuPro-W3", "Meiryo", Font.SANS_SERIF)
    val genv = GraphicsEnvironment.getLocalGraphicsEnvironment
    val defaultFontName = candidate.intersect(genv.getAllFonts.map(_.getName))(0)

    val font = new UnicodeFont(new Font(defaultFontName, Font.PLAIN, 12))
    font.getEffects.asInstanceOf[java.util.List[Effect]].add(new ColorEffect())
    font
  }

  def glLoadFontGlyphs(): Unit = {
    defaultFont.clearGlyphs
    defaultFont.addNeheGlyphs
    defaultFont.loadGlyphs
  }

  def glDrawString(x: Float, y: Float, str: String, color: Color): Unit = {
    glOrthogonal {
      glDisable(GL_DEPTH_TEST)
      glDisable(GL_LIGHTING)
      defaultFont.drawString(x, y, str, color)
    }
  }

  def glDrawImage(x: Float, y: Float, w: Float, h: Float, texture: Int): Unit = {
    glOrthogonal {
      glDisable(GL_DEPTH_TEST)
      glDisable(GL_LIGHTING)
      glEnable(GL_TEXTURE_2D)
      glColor3f(1.0f, 1.0f, 1.0f)
      glBindTexture(GL_TEXTURE_2D, texture)
      glRender(GL_QUADS) {
        glTexCoord2f(0.0f, 1.0f)
        glVertex2f(x, y)
        glTexCoord2f(0.0f, 0.0f)
        glVertex2f(x, y + h)
        glTexCoord2f(1.0f, 0.0f)
        glVertex2f(x + w, y + h)
        glTexCoord2f(1.0f, 1.0f)
        glVertex2f(x + w, y)
      }
      glDisable(GL_TEXTURE_2D)
      glBindTexture(GL_TEXTURE_2D, 0)
    }
  }

  def glDrawImage(x: Float, y: Float, texture: Texture): Unit = {
    glOrthogonal {
      glDisable(GL_DEPTH_TEST)
      glDisable(GL_LIGHTING)
      glEnable(GL_TEXTURE_2D)
      glColor3f(1.0f, 1.0f, 1.0f)
      texture.bind
      glRender(GL_QUADS) {
        glTexCoord2f(0.0f, 0.0f)
        glVertex2f(x, y)
        glTexCoord2f(0.0f, 1.0f)
        glVertex2f(x, y + texture.getTextureHeight)
        glTexCoord2f(1.0f, 1.0f)
        glVertex2f(x + texture.getTextureWidth, y + texture.getTextureHeight)
        glTexCoord2f(1.0f, 0.0f)
        glVertex2f(x + texture.getTextureWidth, y)
      }
      glDisable(GL_TEXTURE_2D)
      glBindTexture(GL_TEXTURE_2D, 0)
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
    val id = glGenBuffers
    glBindBuffer(target, id)
    glBufferData(target, buffer, GL_DYNAMIC_DRAW)
    glBindBuffer(target, 0)
    id
  }

  def glLoadBufferObject(target: Int, buffer: IntBuffer): Int = {
    val id = glGenBuffers
    glBindBuffer(target, id)
    glBufferData(target, buffer, GL_DYNAMIC_DRAW)
    glBindBuffer(target, 0)
    id
  }

  def glLoadBufferObject(target: Int, buffer: FloatBuffer): Int = {
    val id = glGenBuffers
    glBindBuffer(target, id)
    glBufferData(target, buffer, GL_DYNAMIC_DRAW)
    glBindBuffer(target, 0)
    id
  }

  def glLoadShaderObject(target: Int, code: String): Int = {
    val id = glCreateShader(target)
    glShaderSource(id, code)
    glCompileShader(id)
    glPrintShaderLog(id)
    id
  }

  def glPrintShaderLog(id: Int): Unit = {
    val size = glGetShader(id, GL_INFO_LOG_LENGTH);
    if (size > 0) {
      println("Info log:")
      println(glGetShaderInfoLog(id, size))
    }
  }

  def glPrintProgramLog(id: Int): Unit = {
    val size = glGetProgram(id, GL_INFO_LOG_LENGTH);
    if (size > 0) {
      println("Info log:")
      println(glGetProgramInfoLog(id, size))
    }
  }

  def glMatrix(block: => Unit): Unit = {
    glPushMatrix
    block
    glPopMatrix
  }

  def glOrthogonal(block: => Unit): Unit = {
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
