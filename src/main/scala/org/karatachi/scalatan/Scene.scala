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

  var model: PMDModel = null
  var texture: Texture = null

  override def init = {
    glLoadFontGlyphs
    model = PMDLoader.load("resources/kask_yukari/kask_yukari.pmd")
    texture = TextureLoader.getTexture("PNG", getClass.getResourceAsStream("/data/yukari.png"))
  }

  override def update = {
  }

  override def render = {
    glMatrix {
      glDisable(GL_TEXTURE_2D)
      glColor3f(0.0f, 0.0f, 1.0f)
      glTranslatef(0.0f, -1.5f, 0.0f)
      glScalef(0.15f, 0.15f, 0.15f)
      glRotatef(time*20, 0.0f, 1.0f, 0.0f)
      model.render
    }

    glOrthogonal {
      glDrawImage(0, 0, texture)
    }
    super.render
  }
}
