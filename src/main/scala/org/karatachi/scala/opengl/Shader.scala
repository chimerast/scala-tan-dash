package org.karatachi.scala.opengl

import scala.io._
import java.io._

import org.lwjgl.opengl._
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL20._
import org.karatachi.scala.IOUtils._
import org.karatachi.scala.opengl.GLUtils._

object ShaderProgram {
  var active: Option[ShaderProgram] = None
  var programs = Map[String, ShaderProgram]()

  def load(name: String, path: Array[String]): ShaderProgram = {
    val program = new ShaderProgram(path)
    programs += (name -> program)
    program
  }

  def bind(name: String): Unit = {
    programs.get(name).foreach { p =>
      glUseProgram(p.program)
      active = Some(p)
    }
  }

  def unbind(): Unit = {
    glUseProgram(0)
    active = None
  }
}

class ShaderProgram(path: Array[String]) {
  private val program = glCreateProgram

  path.foreach(attach)
  glLinkProgram(program)
  glValidateProgram(program)
  glPrintProgramLog(program)

  private val uniforms = {
    val maxLength = glGetProgram(program, GL_ACTIVE_UNIFORM_MAX_LENGTH)
    (0 until glGetProgram(program, GL_ACTIVE_UNIFORMS))
      .map(i => (glGetActiveUniform(program, i, maxLength), i)).toMap
  }
  private val attributes = {
    val maxLength = glGetProgram(program, GL_ACTIVE_ATTRIBUTE_MAX_LENGTH)
    (0 until glGetProgram(program, GL_ACTIVE_ATTRIBUTES))
      .map(i => (glGetActiveAttrib(program, i, maxLength), i)).toMap
  }

  def attach(path: String): Unit = {
    using {
      var in = getClass().getResourceAsStream(path)
      if (in == null) {
        val file = new File(path)
        if (file.isFile) {
          in = new FileInputStream(file)
        } else {
          throw new FileNotFoundException(path)
        }
      }
      in
    } { in =>
      val code = Source.fromInputStream(in, "UTF-8").mkString
      path.split("\\.").last match {
        case "vert" =>
          val vertexShader = glLoadShaderObject(GL_VERTEX_SHADER, code)
          glAttachShader(program, vertexShader)
          glDeleteShader(vertexShader)
        case "frag" =>
          val fragmentShader = glLoadShaderObject(GL_FRAGMENT_SHADER, code)
          glAttachShader(program, fragmentShader)
          glDeleteShader(fragmentShader)
      }
    }
  }

  def release(): Unit = {
    glDeleteProgram(program)
  }

  def uniform(name: String)(func: (Int) => Unit): Unit = {
    uniforms.get(name).foreach(func(_))
  }
}
