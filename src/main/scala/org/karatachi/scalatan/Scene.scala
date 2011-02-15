package org.karatachi.scalatan

import org.newdawn.slick._
import org.newdawn.slick.opengl._
import org.karatachi.scala.opengl.GLUtils._

trait Scene {
  val layers: List[Layer]
  var delta: Double = 0.0

  def init

  def update

  def render = {
    layers.foreach(_.render(this))
  }

  def next(): Scene = this
}

class OpeningScene extends Scene {
  override val layers: List[Layer] = List(new DebugLayer)

  var texture: Texture = null

  override def init = {
    glLoadFontGlyphs
    texture = TextureLoader.getTexture("PNG", getClass.getResourceAsStream("/data/yukari.png"))
  }

  override def update = {
  }

  override def render = {
    glOrthoFixed {
      glDrawTexture(0,0,texture)
    }
    super.render
  }
}
