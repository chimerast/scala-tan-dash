package org.karatachi.scalatan

import org.lwjgl._
import org.lwjgl.opengl._
import org.lwjgl.opengl.GL11._
import org.lwjgl.input._
import org.karatachi.scala.opengl.GLUtils._

object ScalaTanDash {
  val GAME_TITLE = "Scala-tan Dash"
  val FRAMERATE = 60
  val WIDTH = 800
  val HEIGHT = 600

  var terminated = false
  var scene: Scene = new OpeningScene

  def init(fullscreen: Boolean) {
    Display.setTitle(GAME_TITLE)
    Display.setFullscreen(fullscreen)
    Display.setVSyncEnabled(true)
    Display.setDisplayMode(new DisplayMode(WIDTH, HEIGHT))
    Display.create

    glClearColor(1.0f, 1.0f, 1.0f, 0.0f)

    glViewport(0, 0, WIDTH, HEIGHT)

    glEnable(GL_TEXTURE_2D)
    glEnable(GL_BLEND)
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
  }

  def run() {
    scene.init
    var prev = System.nanoTime
    while (!terminated) {
      val curr = System.nanoTime

      glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT)

      scene.delta = (curr - prev) / 1000000000.0
      scene.update
      scene.render
      scene = scene.next

      Display.sync(FRAMERATE)
      Display.update

      prev = curr

      if (Keyboard.isKeyDown(Keyboard.KEY_ESCAPE))
        terminated = true
      if (Display.isCloseRequested)
        terminated = true
    }
  }

  def cleanup() {
    Display.destroy
  }
}
