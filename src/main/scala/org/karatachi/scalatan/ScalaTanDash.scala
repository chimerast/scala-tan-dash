package org.karatachi.scalatan

import org.lwjgl._
import org.lwjgl.opengl._
import org.lwjgl.opengl.GL11._
import org.lwjgl.util.glu.Project._
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

    println("Vendor :" + glGetString(GL_VENDOR))
    println("GPU :" + glGetString(GL_RENDERER))
    println("OpenGL ver. " + glGetString(GL_VERSION))

    glClearColor(0.0f, 0.0f, 0.0f, 0.0f)

    glViewport(0, 0, WIDTH, HEIGHT)

    glMatrixMode(GL_PROJECTION)
    glLoadIdentity
    gluPerspective(30.0f, WIDTH.toFloat / HEIGHT.toFloat, 1.0f, 100.0f)

    glMatrixMode(GL_MODELVIEW)
    glLoadIdentity
    gluLookAt(5.0f, 2.0f, 5.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);

    glEnable(GL_BLEND)
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

    var lightPosition = Array(5.0f, 5.0f, 5.0f, 0.0f)
    var lightDiffuse = Array(1.0f, 1.0f, 1.0f, 1,0f)
    var lightAmbient = Array(0.8f, 0.8f, 0.8f, 1,0f)
    var lightSpecular = Array(0.0f, 0.0f, 0.0f, 1,0f)

    glEnable(GL_LIGHT0)
    glLightfv(GL_LIGHT0, GL_POSITION, lightPosition)
    glLightfv(GL_LIGHT0, GL_DIFFUSE, lightDiffuse)
    glLightfv(GL_LIGHT0, GL_AMBIENT, lightAmbient)
    glLightfv(GL_LIGHT0, GL_SPECULAR, lightSpecular)
  }

  def run() {
    scene.init
    var prev = System.nanoTime
    while (!terminated) {
      val curr = System.nanoTime

      glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT)

      scene.delta = (curr - prev) / 1000000000.0f
      scene.time += scene.delta
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
