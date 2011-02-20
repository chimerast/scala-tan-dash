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
  var program: ShaderProgram = null

  override def init = {
    glLoadFontGlyphs
    model = PMDLoader.load("resources/kask_yukari/kask_yukari.pmd")
    //model = PMDLoader.load("resources/kask_ran/kask_ran.pmd")
    texture = TextureLoader.getTexture("PNG", getClass.getResourceAsStream("/data/yukari.png"))
    program = ShaderLoader.load(Array("/shader/screen.vert", "/shader/screen.frag"))
  }

  override def update = {
  }

  override def render = {
    program.bind
    glMatrix {
      glDisable(GL_TEXTURE_2D)
      glColor3f(0.0f, 0.0f, 1.0f)
      glTranslatef(0.0f, -1.5f, 0.0f)
      glScalef(0.15f, 0.15f, 0.15f)
      glRotatef(time*60, 0.0f, 1.0f, 0.0f)
      model.activeSkins = List(5, 16)
      model.skinEffect = math.sin(time).toFloat
      model.render
    }
    program.release

    glOrthogonal {
      glDrawImage(0, 0, texture)
    }
    super.render
  }
}
