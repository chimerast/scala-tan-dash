package org.karatachi.scala.opengl

import scala.collection.mutable.{Set, HashMap, MultiMap}
import java.io._
import java.nio._
import java.nio.channels._

import org.lwjgl.opengl.GL11._
import org.karatachi.scala.IOUtils._

object VMDLoader {
  def load(path: String, model: PMDModel): Option[VMDModel] = {
    try {
      val file = new File(path)
      using(new RandomAccessFile(file, "r")) { f =>
        val channel = f.getChannel
        val buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        Some(new VMDModel(buffer, model))
      }
    } catch {
      case e: Exception => e.printStackTrace; None
    }
  }
}

class VMDModel(buffer: ByteBuffer, model: PMDModel) {
  val header = new VMDHeader(buffer)
  val bones = Array.fill(buffer.getInt) { new VMDBone(buffer) }
  val skins = Array.fill(buffer.getInt) { new VMDSkin(buffer) }
  val maxFrame = math.max(bones.foldLeft(0)((a,b) => math.max(a, b.frameNum)),
                          skins.foldLeft(0)((a,b) => math.max(a, b.frameNum)))

  var bonemap = {
    val namemap = new HashMap[String, Set[VMDBone]] with MultiMap[String, VMDBone]
    bones.foreach(bone => namemap.addBinding(bone.boneName, bone))
    val ret = namemap.map { case (n, b) => (n, b.toList.sortWith(_.frameNum < _.frameNum)) }
    ret.values.foreach(list => list.foldLeft(list.last) { case (p, c) => p.next = c; c })
    ret
  }

  var skinmap = {
    val namemap = model.skins.indices.map(i => (model.skins(i).skinName -> i)).toMap
    val indexmap = new HashMap[Int, Set[VMDSkin]] with MultiMap[Int, VMDSkin]
    skins.foreach { s =>
      namemap.get(s.skinName).foreach { i =>
        s.index = i
        indexmap.addBinding(model.skins(i).skinType, s)
      }
    }
    val ret = indexmap.map { case (i, b) => (i, b.toList.sortWith(_.frameNum < _.frameNum)) }
    ret.values.foreach(list => list.foldLeft(list.last) { case (p, c) => p.next = c; c })
    ret
  }

  def applyBone(bone: PMDBone, buffer: FloatBuffer, frame: Float): Unit = {
    var f = frame
    while (f >= maxFrame) f -= maxFrame
    bonemap.get(bone.boneName).map(_.takeWhile(_.frameNum <= f).last).foreach { curr =>
      val next = curr.next

      val dist = { var ret = next.frameNum - curr.frameNum; if (ret < 0.0f) ret += maxFrame; ret }
      val t = if (dist != 0.0f) (f - curr.frameNum) / dist else 0.0f

      // 補間
      val pos = curr.pos.interporate(next.pos, t)
      val quat = curr.quat.slerp(next.quat, t)

      // 回転行列の設定
      quat.setupMatrix(buffer)
      buffer.position(buffer.position - 16)

      val head = bone.boneHeadPos
      // 変換
      glTranslatef(head.x+pos.x, head.y+pos.y, head.z+pos.z)
      glMultMatrix(buffer)
      glTranslatef(-head.x, -head.y, -head.z)
    }
  }

  def applySkin(skintype: Int, buffer: ByteBuffer, frame: Float): Unit = {
    var f = frame
    while (f >= maxFrame) f -= maxFrame
    skinmap.get(skintype).map(_.takeWhile(_.frameNum <= f).last).foreach { curr =>
      val next = curr.next

      val dist = { var ret = next.frameNum - curr.frameNum; if (ret < 0.0f) ret += maxFrame; ret }
      val t = if (dist != 0.0f) (f - curr.frameNum) / dist else 0.0f

      val curreffect = curr.weight * (1.0f - t)
      model.skins(curr.index).skinVertData.foreach { v =>
        val index = model.skins(0).skinVertData(v.skinVertIndex).skinVertIndex * model.VERTEX_ELEMENTS
        buffer.putFloat((index + 0) * 4, buffer.getFloat((index + 0) * 4) + v.skinVertPos.x * curreffect)
        buffer.putFloat((index + 1) * 4, buffer.getFloat((index + 1) * 4) + v.skinVertPos.y * curreffect)
        buffer.putFloat((index + 2) * 4, buffer.getFloat((index + 2) * 4) + v.skinVertPos.z * curreffect)
      }
      val nexteffect = next.weight * t
      model.skins(next.index).skinVertData.foreach { v =>
        val index = model.skins(0).skinVertData(v.skinVertIndex).skinVertIndex * model.VERTEX_ELEMENTS
        buffer.putFloat((index + 0) * 4, buffer.getFloat((index + 0) * 4) + v.skinVertPos.x * nexteffect)
        buffer.putFloat((index + 1) * 4, buffer.getFloat((index + 1) * 4) + v.skinVertPos.y * nexteffect)
        buffer.putFloat((index + 2) * 4, buffer.getFloat((index + 2) * 4) + v.skinVertPos.z * nexteffect)
      }
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

  var index = 0
  var next = this
}

class VMDFormatException extends Exception {
}
