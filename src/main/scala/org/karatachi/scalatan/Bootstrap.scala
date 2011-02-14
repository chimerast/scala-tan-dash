package org.karatachi.scalatan

import org.lwjgl._
import opengl.{Display,GL11,DisplayMode}
import GL11._
import input._

object Bootstrap {
  def main(args: Array[String]) {
    var fullscreen = false
    for (arg <- args) {
      arg match {
        case "-fullscreen" =>
          fullscreen = true
      }
    }

    try {
      init(fullscreen)
      run
    } catch {
      case e => Sys.alert(GAME_TITLE, e.getMessage)
    } finally {
      cleanup();
    }
  }

  val GAME_TITLE = "Scala-tan Dash"
  val FRAMERATE = 60
  val WIDTH = 640
  val HEIGHT = 480

  var terminated = false

  var angle = 0.0f

  def init(fullscreen: Boolean) {
    Display.setTitle(GAME_TITLE)
    Display.setFullscreen(fullscreen)
    Display.setVSyncEnabled(true)
    Display.setDisplayMode(new DisplayMode(WIDTH, HEIGHT))
    Display.create

    glOrtho(0.0, WIDTH, HEIGHT, 0.0, -1.0, 1.0)
  }

  def run() {
    while (!terminated) {
      Display.update

      logic
      render

      Display.sync(FRAMERATE)
    }
  }

  def cleanup() {
    Display.destroy
  }

  def logic() {
    import Keyboard._

    if (isKeyDown(KEY_ESCAPE))
      terminated = true
    if (Display.isCloseRequested)
      terminated = true

    angle += 2.0f % 360;
  }

  def render() {
    glClear(GL_COLOR_BUFFER_BIT)

    glPushMatrix
    glTranslatef(WIDTH / 2, HEIGHT / 2, 0.0f)

    glRotatef(angle, 0, 0, 1.0f)
    glBegin(GL_QUADS)
    glVertex2i(-50, -50)
    glVertex2i(50, -50)
    glVertex2i(50, 50)
    glVertex2i(-50, 50)
    glEnd

    glPopMatrix
  }
}
