package org.karatachi.scalatan

import org.lwjgl._
import org.lwjgl.opengl._
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL20._
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

  var yukari: PMDModel = null
  var ran: PMDModel = null
  var miku: PMDModel = null
  var texture: Texture = null

  override def init = {
    glClearColor(0.6f, 0.8f, 1.0f, 0.0f)

    yukari = PMDLoader.load("resources/kask_yukari/kask_yukari.pmd")
    ran = PMDLoader.load("resources/kask_ran/kask_ran.pmd")
    //miku = PMDLoader.load("resources/Lat式ミクVer2.3/Lat式ミクVer2.3_Normal.pmd")
    miku = PMDLoader.load("resources/Model/初音ミク.pmd")
    //miku = PMDLoader.load("resources/Model/初音ミクmetal.pmd")

    texture = TextureLoader.getTexture("PNG", getClass.getResourceAsStream("/data/yukari.png"))

    FrameBuffer.create("NormalAndDepth", 800, 600)

    ShaderProgram.rootpath = "src/main/resources/shader";
    ShaderProgram.load("ToonShader", Array("ToonShader.vert", "ToonShader.frag"))
    ShaderProgram.load("NormalAndDepth", Array("NormalAndDepth.vert", "NormalAndDepth.frag"))
    ShaderProgram.load("Composition", Array("Composition.vert", "Composition.frag"))
  }

  def draw = {

    glMatrix {
      glTranslatef(0.0f, 0.0f, 0.0f)
      glScalef(0.10f, 0.10f, 0.10f)
      //glRotatef(time*60, 0.0f, 1.0f, 0.0f)
      miku.activeSkins = List(8, 13)
      miku.skinEffect = math.abs(math.sin(time*3).toFloat)
      miku.render
    }

    glMatrix {
      glTranslatef(1.0f, 0.0f, 0.0f)
      glScalef(0.10f, 0.10f, 0.10f)
      //glRotatef(time*60, 0.0f, 1.0f, 0.0f)
      yukari.activeSkins = List(5, 16)
      yukari.skinEffect = math.abs(math.sin(time*3).toFloat)
      yukari.render
    }

    glMatrix {
      glTranslatef(-1.0f, 0.0f, 0.0f)
      glScalef(0.10f, 0.10f, 0.10f)
      //glRotatef(time*60, 0.0f, 1.0f, 0.0f)
      ran.activeSkins = List(5, 16)
      ran.skinEffect = math.abs(math.sin(time*3).toFloat)
      ran.render
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
  }

  override def update = {
  }

  override def render = {
    FrameBuffer.bind("NormalAndDepth")
    ShaderProgram.bind("NormalAndDepth")
    glDisable(GL_BLEND)
    draw
    glEnable(GL_BLEND)
    ShaderProgram.unbind
    FrameBuffer.unbind

    ShaderProgram.bind("ToonShader")
    draw
    ShaderProgram.unbind

    ShaderProgram.bind("Composition")
    //glDrawImage(0, 0, 800, 600, FrameBuffer.texture("NormalAndDepth"))
    ShaderProgram.unbind

    glDrawImage(0, 0, texture)

    super.render
  }
}
