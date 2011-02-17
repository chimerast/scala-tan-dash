package org.karatachi.scalatan

import org.lwjgl._
import org.lwjgl.opengl._
import org.lwjgl.opengl.GL11._
import org.newdawn.slick._
import org.karatachi.scala.opengl.GLUtils._

trait Layer {
  def render(scene: Scene)
}

class DebugLayer extends Layer {
  override def render(scene: Scene) = {
    glOrthogonal {
      glDrawString(10.0f, 10.0f, "FPS: %.1f".format(1.0 / scene.delta), Color.black);
    }
  }
}
