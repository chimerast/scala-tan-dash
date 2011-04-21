package org.karatachi.scala.opengl

import java.io._
import org.karatachi.scala.IOUtils._
import org.karatachi.scala.opengl.GLUtils._
import org.lwjgl.opengl.GL20._
import org.lwjgl.opengl._
import scala.collection.mutable._
import scala.io._

object ShaderProgram {
  var rootpath = ""
  var active: Option[ShaderProgram] = None
  var programs = new HashMap[String, ShaderProgram]
  var stack = new Stack[Option[ShaderProgram]]

  def load(name: String): ShaderProgram = {
    val program = new ShaderProgram(Array(name + ".vert", name + ".frag"))
    programs += (name -> program)
    program
  }

  def reload(): Unit = {
    programs = programs.map {
      case (name, program) =>
        program.release
        (name -> new ShaderProgram(program.path))
    }
  }

  def get(name: String): Option[ShaderProgram] = {
    programs.get(name)
  }

  def bind(name: String): Unit = {
    stack.push(active)
    programs.get(name).foreach { p =>
      if (!p.hasError) {
        glUseProgram(p.program)
        active = Some(p)
      }
    }
  }

  def unbind(): Unit = {
    stack.pop match {
      case Some(p) =>
        if (!p.hasError) {
          glUseProgram(p.program)
          active = Some(p)
        }
      case None =>
        glUseProgram(0)
        active = None
    }
  }

  def apply(name: String)(block: => Unit): Unit = {
    bind(name)
    if (active != None)
      block
    unbind
  }
}

class ShaderProgram(val path: Array[String]) {
  private val program = glCreateProgram
  private var hasError = false

  path.foreach(attach)
  glLinkProgram(program)
  glValidateProgram(program)
  hasError = glPrintProgramLog(program)

  private val uniforms = {
    val maxLength = glGetProgram(program, GL_ACTIVE_UNIFORM_MAX_LENGTH)
    (0 until glGetProgram(program, GL_ACTIVE_UNIFORMS)).map { i =>
      val name = glGetActiveUniform(program, i, maxLength)
      (name, glGetUniformLocation(program, name))
    }.toMap
  }

  private val attributes = {
    val maxLength = glGetProgram(program, GL_ACTIVE_ATTRIBUTE_MAX_LENGTH)
    (0 until glGetProgram(program, GL_ACTIVE_ATTRIBUTES)).map { i =>
      val name = glGetActiveAttrib(program, i, maxLength)
      (name, glGetAttribLocation(program, name))
    }.toMap
  }

  def attach(path: String): Unit = {
    using {
      var in = getClass().getResourceAsStream(ShaderProgram.rootpath + "/" + path)
      if (in == null) {
        val file = new File(ShaderProgram.rootpath + "/" + path)
        if (file.isFile) {
          in = new FileInputStream(file)
        } else {
          throw new FileNotFoundException(ShaderProgram.rootpath + "/" + path)
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

  def attribute(name: String)(func: (Int) => Unit): Unit = {
    attributes.get(name).foreach(func(_))
  }
}
