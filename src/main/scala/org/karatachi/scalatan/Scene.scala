package org.karatachi.scalatan

import org.lwjgl._
import org.lwjgl.opengl._
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL20._
import org.lwjgl.input._
import org.lwjgl.util.glu.Project._
import org.newdawn.slick._
import org.newdawn.slick.opengl._

import org.karatachi.scala.opengl._
import org.karatachi.scala.opengl.GLUtils._

trait Scene {
  val layers: List[Layer]
  var delta: Float = 0.0f
  var time: Float = 0.0f

  def init(): Unit

  def update(): Unit

  def render(): Unit = {
    layers.foreach(_.render(this))
  }

  def next(): Scene = this
}

class LoadingScene(next: Scene) extends Scene {
  override val layers: List[Layer] = List()

  override def init(): Unit = {
    glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

    glLoadFontGlyphs
  }

  override def update(): Unit = {
  }

  override def render(): Unit = {
    glDrawString(10, 10, "Now Loading...", Color.white)
  }

  override def next(): Scene = {
    next.init
    next
  }
}

class OpeningScene extends Scene {
  override val layers: List[Layer] = List(new DebugLayer)

  var yukari = None.asInstanceOf[Option[PMDModel]]
  var ran = None.asInstanceOf[Option[PMDModel]]
  var miku = None.asInstanceOf[Option[PMDModel]]
  var texture: Texture = null

  var depth = 5.0
  var radian = 0.0

  override def init = {
    glClearColor(0.6f, 0.8f, 1.0f, 0.0f)

    miku = PMDLoader.load("resources/Model/初音ミク.pmd")
    miku.foreach(_.attachMotion("resources/Motion/ごまえミク.vmd"))
    yukari = PMDLoader.load("resources/kask_yukari/kask_yukari.pmd")
    yukari.foreach(_.attachMotion("resources/Motion/ごまえミク.vmd"))
    ran = PMDLoader.load("resources/kask_ran/kask_ran.pmd")
    ran.foreach(_.attachMotion("resources/Motion/ごまえミク.vmd"))
    //miku = PMDLoader.load("resources/ika/ikaopmf1016.pmd")
    //miku = PMDLoader.load("resources/Lat式ミクVer2.3/Lat式ミクVer2.3_Normal.pmd")

    texture = TextureLoader.getTexture("PNG", getClass.getResourceAsStream("/data/yukari.png"))

    //FrameBuffer.create("NormalAndDepth")

    ShaderProgram.rootpath = "src/main/resources/shader";
    ShaderProgram.load("ToonShader")
    ShaderProgram.load("Edge")
    //ShaderProgram.load("NormalAndDepth")
    //ShaderProgram.load("Composition")
  }

  def draw = {
    miku.foreach { m =>
      glMatrix {
        glScalef(0.10f, 0.10f, 0.10f)
        m.render(time * 24.0f)
      }
    }
    yukari.foreach { m =>
      glMatrix {
        glTranslatef(0.7f, 0.0f, 0.0f)
        glScalef(0.10f, 0.10f, 0.10f)
        m.render(time * 24.0f)
      }
    }
    ran.foreach { m =>
      glMatrix {
        glTranslatef(-0.7f, 0.0f, 0.0f)
        glScalef(0.10f, 0.10f, 0.10f)
        m.render(time * 24.0f)
      }
    }
  }

  override def update = {
    if (Keyboard.isKeyDown(Keyboard.KEY_UP))
      depth -= 2.0 * delta
    if (Keyboard.isKeyDown(Keyboard.KEY_DOWN))
      depth += 2.0 * delta
    if (depth < 1.0)
      depth = 1.0
    if (depth > 10.0)
      depth = 10.0

    if (Keyboard.isKeyDown(Keyboard.KEY_LEFT))
      radian -= 1.0 * delta
    if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT))
      radian += 1.0 * delta

    val x = (depth * math.sin(radian)).toFloat
    val z = (depth * math.cos(radian)).toFloat

    glLoadIdentity
    gluLookAt(x, 1.0f, z, 0.0f, 1.2f, 0.0f, 0.0f, 1.0f, 0.0f);
  }

  override def render = {
    ShaderProgram("Edge") { draw }

    FrameBuffer("NormalAndDepth") {
      glDisable(GL_BLEND)
      ShaderProgram("NormalAndDepth") { draw }
      glEnable(GL_BLEND)
    }

    ShaderProgram("ToonShader") { draw }

    glMatrix {
      glRender(GL_QUADS) {
        glColor3f(0.3f, 0.3f, 0.7f)
        glVertex3f(4.0f, 0.0f, -4.0f)
        glVertex3f(-4.0f, 0.0f, -4.0f)
        glVertex3f(-4.0f, 0.0f, 4.0f)
        glVertex3f(4.0f, 0.0f, 4.0f)
      }
    }

    ShaderProgram("Composition") {
      FrameBuffer.texture("NormalAndDepth").foreach(glDrawImage(0, 0, 800, 600, _))
    }

    glDrawImage(0, 0, texture)

    super.render
  }
}
