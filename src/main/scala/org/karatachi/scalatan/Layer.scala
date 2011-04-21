package org.karatachi.scalatan

import org.karatachi.scala.opengl.GLUtils._
import org.newdawn.slick._

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
