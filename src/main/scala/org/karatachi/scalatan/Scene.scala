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

  def render(): Unit = layers.foreach(_.render(this))

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
    glDrawString(10, 10, "Now Loading" + "." * time.toInt, Color.white)
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

  override def init(): Unit = {
    glClearColor(0.6f, 0.8f, 1.0f, 0.0f)

    miku = PMDLoader.load("resources/Model/ちびまりさ/ちびまりさ.pmd")
    miku.foreach(_.attachMotion("resources/Motion/恋愛サーキュレーション/恋愛サーキュレーション-ちび.vmd"))
    // miku = PMDLoader.load("resources/Model/VOCALOID/初音ミク.pmd")
    // miku.foreach(_.attachMotion("resources/Motion/ごまえミク.vmd"))
    // yukari = PMDLoader.load("resources/Model/kask_yukari/kask_yukari.pmd")
    // yukari.foreach(_.attachMotion("resources/Motion/ごまえミク.vmd"))
    // ran = PMDLoader.load("resources/Model/kask_ran/kask_ran.pmd")
    // ran.foreach(_.attachMotion("resources/Motion/ごまえミク.vmd"))

    texture = TextureLoader.getTexture("PNG", getClass.getResourceAsStream("/data/yukari.png"))
  }

  def draw(): Unit = {
    miku.foreach { m =>
      glMatrix {
        glScalef(0.17f, 0.17f, 0.17f)
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

  override def update(): Unit = {
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

    var lightPosition = Array(3.0f, 1.0f, 5.0f, 1.0f)
    var lightDiffuse = Array(1.0f, 1.0f, 1.0f, 1,0f)
    var lightAmbient = Array(0.0f, 0.0f, 0.0f, 1,0f)
    var lightSpecular = Array(0.6f, 0.6f, 0.6f, 1,0f)

    glEnable(GL_LIGHT0)
    glLightfv(GL_LIGHT0, GL_POSITION, lightPosition)
    glLightfv(GL_LIGHT0, GL_DIFFUSE, lightDiffuse)
    glLightfv(GL_LIGHT0, GL_AMBIENT, lightAmbient)
    glLightfv(GL_LIGHT0, GL_SPECULAR, lightSpecular)
  }

  override def render(): Unit = {
    ShaderProgram("ToonShader") {
      draw
    }

    glMatrix {
      glRender(GL_QUADS) {
        glColor3f(0.3f, 0.3f, 0.7f)
        glVertex3f(4.0f, 0.0f, -4.0f)
        glVertex3f(-4.0f, 0.0f, -4.0f)
        glVertex3f(-4.0f, 0.0f, 4.0f)
        glVertex3f(4.0f, 0.0f, 4.0f)
      }
    }

    glDrawImage(0, 0, texture)

    super.render
  }
}
