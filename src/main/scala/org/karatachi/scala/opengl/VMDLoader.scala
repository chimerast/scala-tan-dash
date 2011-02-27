package org.karatachi.scala.opengl

import scala.collection.mutable.{Set, HashMap, MultiMap}
import java.io._
import java.nio._
import java.nio.channels._

import org.lwjgl.opengl.GL11._
import org.karatachi.scala.IOUtils._

object VMDLoader {
  def load(path: String): Option[VMDModel] = {
    try {
      val file = new File(path)
      using(new RandomAccessFile(file, "r")) { f =>
        val channel = f.getChannel
        val buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        Some(new VMDModel(file, buffer))
      }
    } catch {
      case e: Exception => e.printStackTrace; None
    }
  }
}

class VMDModel(file: File, buffer: ByteBuffer) {
  val basedir = file.getParent

  val header = new VMDHeader(buffer)
  val bones = Array.fill(buffer.getInt) { new VMDBone(buffer) }
  val skins = Array.fill(buffer.getInt) { new VMDSkin(buffer) }
  val maxFrame = bones.foldLeft(0)((a,b) => math.max(a, b.frameNum))

  var bonemap = {
    val bonemap = new HashMap[String, Set[VMDBone]] with MultiMap[String, VMDBone]
    bones.foreach(bone => bonemap.addBinding(bone.boneName, bone))
    bonemap.map { case (name, bone) => (name, bone.toList.sortWith(_.frameNum < _.frameNum)) }
  }

  bonemap.values.foreach(list => list.foldLeft(list.last) { case (prev, curr) => prev.next = curr; curr })

  def apply(bone: PMDBone, buffer: FloatBuffer, frame: Float): Unit = {
    var f = frame
    while (f >= maxFrame) f -= maxFrame
    val b = bonemap.get(bone.boneName).map(_.takeWhile(_.frameNum <= f).last)
    b.foreach { curr =>
      val next = curr.next
      val frameDist = {
        if (next.frameNum < curr.frameNum)
          next.frameNum + maxFrame - curr.frameNum
        else
          next.frameNum - curr.frameNum
      }
      val t = if (frameDist != 0.0f) (f - curr.frameNum) / frameDist else 0.0f

      if (t < 0.0f || t > 1.0f) println(t)
      val pos = curr.pos.interporate(next.pos, t)
      val quat = curr.quat.slerp(next.quat, t)

      quat.setupMatrix(buffer)
      buffer.position(buffer.position - 16)

      val head = bone.boneHeadPos
      glTranslatef(head.x+pos.x, head.y+pos.y, head.z+pos.z)
      glMultMatrix(buffer)
      glTranslatef(-head.x, -head.y, -head.z)
    }
  }
}

class VMDHeader(buffer: ByteBuffer) {
  val magic = buffer.getString(30)
  val modelName = buffer.getString(20)

  if (magic != "Vocaloid Motion Data 0002")
    throw new PMDFormatException
}

class VMDBone(buffer: ByteBuffer) {
  val boneName = buffer.getString(15)
  val frameNum = buffer.getInt
  val pos = new Vector(buffer)

  val quat = new Quaternion(buffer)
  val params = Array.fill(64) { buffer.get }

  var next = this
}

class VMDSkin(buffer: ByteBuffer) {
  val skinName = buffer.getString(15)
  val frameNum = buffer.getInt
  val weight = buffer.getFloat
}

class VMDFormatException extends Exception {
}
