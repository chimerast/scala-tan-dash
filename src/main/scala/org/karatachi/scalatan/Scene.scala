package org.karatachi.scalatan

import org.lwjgl._
import org.lwjgl.opengl._
import org.lwjgl.opengl.GL11._
import org.lwjgl.util.glu.Project._
import org.newdawn.slick.opengl._
import org.karatachi.scala.opengl._
import org.karatachi.scala.opengl.GLUtils._

trait Scene {
  val layers: List[Layer]
  var delta: Float = 0.0f
  var time: Float = 0.0f

  def init

  def update

  def render = {
    layers.foreach(_.render(this))
  }

  def next(): Scene = this
}

class OpeningScene extends Scene {
  override val layers: List[Layer] = List(new DebugLayer)

  var yukari: PMDModel = null
  var ran: PMDModel = null
  var texture: Texture = null

  override def init = {
    glLoadFontGlyphs
    yukari = PMDLoader.load("resources/kask_yukari/kask_yukari.pmd")
    ran = PMDLoader.load("resources/kask_ran/kask_ran.pmd")
    texture = TextureLoader.getTexture("PNG", getClass.getResourceAsStream("/data/yukari.png"))

    FrameBuffer.create("NormalAndDepth", 800, 600)

    ShaderProgram.rootpath = "src/main/resources/shader";
    ShaderProgram.load("ToonShader", Array("ToonShader.vert", "ToonShader.frag"))
    ShaderProgram.load("NormalAndDepth", Array("NormalAndDepth.vert", "NormalAndDepth.frag"))
    ShaderProgram.load("Composition", Array("Composition.vert", "Composition.frag"))
  }

  def draw = {
    glMatrix {
      glTranslatef(1.0f, 0.0f, 0.0f)
      glScalef(0.10f, 0.10f, 0.10f)
      glRotatef(time*60, 0.0f, 1.0f, 0.0f)
      yukari.activeSkins = List(5, 16)
      yukari.skinEffect = math.sin(time*3).toFloat
      yukari.render
    }
    glMatrix {
      glTranslatef(-1.0f, 0.0f, 0.0f)
      glScalef(0.10f, 0.10f, 0.10f)
      glRotatef(time*60, 0.0f, 1.0f, 0.0f)
      ran.activeSkins = List(5, 16)
      ran.skinEffect = math.sin(time*3).toFloat
      ran.render
    }
    glMatrix {
      glRender(GL_QUADS) {
        glColor3f(0.3f, 0.3f, 0.7f)
        glVertex3f(4.0f, 0.0f, -4.0f)
        glVertex3f(4.0f, 0.0f, 4.0f)
        glVertex3f(-4.0f, 0.0f, 4.0f)
        glVertex3f(-4.0f, 0.0f, -4.0f)
      }
    }
  }

  override def update = {
  }

  override def render = {
    FrameBuffer.bind("NormalAndDepth")
    ShaderProgram.bind("NormalAndDepth")
    glDisable(GL_BLEND)
    draw
    glEnable(GL_BLEND)
    ShaderProgram.unbind
    FrameBuffer.unbind

    ShaderProgram.bind("ToonShader")
    draw
    ShaderProgram.unbind

    ShaderProgram.bind("Composition")
    glDrawImage(0, 0, 800, 600, FrameBuffer.texture("NormalAndDepth"))
    ShaderProgram.unbind

    glDrawImage(0, 0, texture)

    super.render
  }
}
