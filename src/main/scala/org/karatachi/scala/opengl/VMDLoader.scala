package org.karatachi.scala.opengl

import java.io._
import java.nio._
import java.nio.channels._

import org.karatachi.scala.IOUtils._

object VMDLoader {
  def load(path: String): VMDModel = {
    val file = new File(path)
    using(new RandomAccessFile(file, "r")) { f =>
      val channel = f.getChannel
      val buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size)
      buffer.order(ByteOrder.LITTLE_ENDIAN)

      new VMDModel(file, buffer)
    }
  }
}

class VMDModel(file: File, buffer: ByteBuffer) {
  val basedir = file.getParent

  val header = new VMDHeader(buffer)
  val motions = Array.fill(buffer.getInt) { new VMDMotion(buffer) }

  println(header.modelName)
  motions.foreach(m => println(m.frameNum + ":" + m.boneName))
}

class VMDHeader(buffer: ByteBuffer) {
  val magic = buffer.getString(30)
  val modelName = buffer.getString(20)

  if (magic != "Vocaloid Motion Data 0002")
    throw new PMDFormatException
}

class VMDMotion(buffer: ByteBuffer) {
  val boneName = buffer.getString(15)
  val frameNum = buffer.getInt
  val pos = new Vector(buffer)
  val quart = new Quarternion(buffer)
  val params = Array.fill(64) { buffer.get }

  for (i <- 0 until 4) {
    for (j <- 0 until 16) {
      print("%3d".format(params(i*16+j)))
    }
    println
  }
}

class VMDFormatException extends Exception {
}
