package org.karatachi.scala.opengl

import java.nio._
import java.util.NoSuchElementException

import org.lwjgl._
import org.lwjgl.opengl._
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL14._
import org.lwjgl.opengl.EXTFramebufferObject._

object FrameBuffer {
  var active: Option[FrameBuffer] = None
  var buffers = Map[String, FrameBuffer]()

  def create(name: String, width: Int = Display.getDisplayMode.getWidth,
             height: Int = Display.getDisplayMode.getWidth): FrameBuffer = {
    val buffer = new FrameBuffer(width, height)
    buffers += (name -> buffer)
    buffer
  }

  def bind(name: String): Unit = {
    buffers.get(name).foreach { b =>
      glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, b.frameBuffer)
      glPushAttrib(GL_VIEWPORT_BIT)
      glViewport(0, 0, b.width, b.height)
      glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
      active = Some(b)
    }
  }

  def unbind(): Unit = {
    glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0);
    glPopAttrib()
    active = None
  }

  def texture(name: String): Int = {
    buffers.get(name) match {
      case Some(b) =>
        b.texture
      case None =>
        throw new NoSuchElementException
    }
  }
}

class FrameBuffer(val width: Int, val height: Int) {
  private val texture = {
    val texture = glGenTextures
    glBindTexture(GL_TEXTURE_2D, texture)
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height,
                 0, GL_RGBA, GL_UNSIGNED_BYTE, null.asInstanceOf[ByteBuffer])
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
    glBindTexture(GL_TEXTURE_2D, 0)
    texture
  }

  private val renderBuffer = {
    val renderBuffer = glGenRenderbuffersEXT
    glBindRenderbufferEXT(GL_RENDERBUFFER_EXT, renderBuffer)
    glRenderbufferStorageEXT(GL_RENDERBUFFER_EXT,
                             GL_DEPTH_COMPONENT24, width, height)
    glBindRenderbufferEXT(GL_RENDERBUFFER_EXT, 0);
    renderBuffer
  }

  private val frameBuffer = {
    val frameBuffer = glGenFramebuffersEXT
    glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, frameBuffer);
    glFramebufferTexture2DEXT(GL_FRAMEBUFFER_EXT, GL_COLOR_ATTACHMENT0_EXT,
                              GL_TEXTURE_2D, texture, 0);
    glFramebufferRenderbufferEXT(GL_FRAMEBUFFER_EXT, GL_DEPTH_ATTACHMENT_EXT,
                                 GL_RENDERBUFFER_EXT, renderBuffer);
    glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0);
    frameBuffer
  }

  def bind(): Unit = {
  }

  def release(): Unit = {
    glDeleteFramebuffersEXT(frameBuffer)
    glDeleteRenderbuffersEXT(renderBuffer)
    glDeleteTextures(texture)
  }
}
