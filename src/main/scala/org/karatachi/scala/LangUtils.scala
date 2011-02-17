package org.karatachi.scala

import java.io.Closeable
import java.nio.ByteBuffer

object IOUtils {
  def using[S <% Closeable, T](what: S)(block: S => T): T = {
    try {
      block(what)
    } finally {
      what.close
    }
  }

  implicit def byteBufferWrapper(buffer: ByteBuffer): ByteBufferWrapper =
    new ByteBufferWrapper(buffer)

  class ByteBufferWrapper(buffer: ByteBuffer) {
    val DEFAULT_STRING_ENCODING = "Windows-31J"

    def getString(size: Int, encoding: String = DEFAULT_STRING_ENCODING): String = {
      val buf = new Array[Byte](size)
      buffer.get(buf)
      var end = buf.indexOf(0)
      if (end == -1)
        end = buf.length
      new String(buf, 0, end, encoding)
    }
  }
}
