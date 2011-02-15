package org.karatachi.scala.opengl

import java.awt.{ GraphicsEnvironment, Font }

import org.lwjgl.opengl._
import org.lwjgl.opengl.GL11._
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
    defaultFont.drawString(x, y, str, color)
  }

  def glDrawTexture(x: Float, y: Float, texture: Texture) = {
    Color.white.bind
    texture.bind
    glDraw(GL_QUADS) {
      GL11.glTexCoord2f(0.0f, 0.0f)
      glVertex2f(x, y)
      GL11.glTexCoord2f(1.0f, 0.0f)
      glVertex2f(x + texture.getTextureWidth, y)
      GL11.glTexCoord2f(1.0f, 1.0f)
      glVertex2f(x + texture.getTextureWidth, y + texture.getTextureHeight)
      GL11.glTexCoord2f(0.0f, 1.0f)
      glVertex2f(x, y + texture.getTextureHeight)
    }
  }

  def glDraw(mode: Int)(block: => Unit) = {
    glBegin(mode)
    block
    glEnd
  }

  def glMatrix(block: => Unit) = {
    glPushMatrix
    block
    glPopMatrix
  }

  def glOrthoFixed(block: => Unit) = {
    val displayMode = Display.getDisplayMode
    glMatrixMode(GL_PROJECTION)
    glPushMatrix
    glLoadIdentity
    glOrtho(0.0, displayMode.getWidth, displayMode.getHeight, 0.0, -1.0, 1.0)
    glMatrixMode(GL_MODELVIEW)
    glPushMatrix
    glLoadIdentity
    block
    glMatrixMode(GL_MODELVIEW)
    glPopMatrix
    glMatrixMode(GL_PROJECTION)
    glPopMatrix
  }
}
