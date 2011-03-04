package org.karatachi.scala.opengl

import scala.collection.mutable.{Set, HashMap, MultiMap}
import java.io._
import java.nio._
import java.nio.channels._

import org.lwjgl._
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
    // ボーン名毎にフレーム順に並んだマップを作成
    val namemap = new HashMap[String, Set[VMDBone]] with MultiMap[String, VMDBone]
    bones.foreach(bone => namemap.addBinding(bone.boneName, bone))
    val ret = namemap.mapValues(_.toList.sortWith(_.frameNum < _.frameNum))
    ret.values.foreach(list => list.foldLeft(list.last) { case (p, c) => p.next = c; c })
    ret
  }

  var skinmap = {
    // スキンタイプ毎にフレーム順に並んだマップを作成
    val namemap = model.skins.map(_.skinName).zipWithIndex.toMap
    val typemap = new HashMap[Int, Set[VMDSkin]] with MultiMap[Int, VMDSkin]
    skins.foreach { s =>
      namemap.get(s.skinName).foreach { i =>
        s.index = i
        typemap.addBinding(model.skins(i).skinType, s)
      }
    }
    val ret = typemap.mapValues(_.toList.sortWith(_.frameNum < _.frameNum))
    ret.values.foreach(list => list.foldLeft(list.last) { case (p, c) => p.next = c; c })
    ret
  }

  /** 使い回し用オブジェクト */
  private val tempBuffer = BufferUtils.createFloatBuffer(16)

  /** ボーンにアニメーションを適用する */
  def applyBoneMatrix(bone: PMDBone, frame: Float): Unit = {
    var f = frame; while (f >= maxFrame) f -= maxFrame
    bonemap.get(bone.boneName).map(_.takeWhile(_.frameNum <= f).last).foreach { curr =>
      val next = curr.next

      // 変化値の計算
      val dist = { var ret = next.frameNum - curr.frameNum; if (ret < 0.0f) ret += maxFrame; ret }
      val t = if (dist != 0.0f) (f - curr.frameNum) / dist else 0.0f

      // 補間
      val pos = curr.pos.interpolate(next.pos, t)
      val quat = curr.quat.slerp(next.quat, t)

      // 回転行列の設定
      tempBuffer.clear
      quat.setupMatrix(tempBuffer)
      tempBuffer.flip

      // 変換
      val head = bone.boneHeadPos
      glTranslatef(head.x+pos.x, head.y+pos.y, head.z+pos.z)
      glMultMatrix(tempBuffer)
      glTranslatef(-head.x, -head.y, -head.z)
    }
  }

  /** スキンにアニメーションを適用する */
  def applySkin(skintype: Int, frame: Float): Unit = {
    var f = frame; while (f >= maxFrame) f -= maxFrame
    skinmap.get(skintype).map(_.takeWhile(_.frameNum <= f).last).foreach { curr =>
      val next = curr.next

      // 変化値の計算
      val dist = { var ret = next.frameNum - curr.frameNum; if (ret < 0.0f) ret += maxFrame; ret }
      val t = if (dist != 0.0f) (f - curr.frameNum) / dist else 0.0f

      // 補間しつつ適用
      val curreffect = curr.weight * (1.0f - t)
      model.skins(curr.index).skinVertData.foreach { v =>
        val skinIndex = model.skins(0).skinVertData(v.skinVertIndex).skinVertIndex
        model.verticesmap.get(skinIndex).foreach(_.foreach { i =>
          val index = i * model.VERTEX_ELEMENTS
          model.vertexBufferRaw.put(
            index+0, model.vertexBufferRaw.get(index+0) + v.skinVertPos.x * curreffect)
          model.vertexBufferRaw.put(
            index+1, model.vertexBufferRaw.get(index+1) + v.skinVertPos.y * curreffect)
          model.vertexBufferRaw.put(
            index+2, model.vertexBufferRaw.get(index+2) + v.skinVertPos.z * curreffect)
        })
      }
      val nexteffect = next.weight * t
      model.skins(next.index).skinVertData.foreach { v =>
        val skinIndex = model.skins(0).skinVertData(v.skinVertIndex).skinVertIndex
        model.verticesmap.get(skinIndex).foreach(_.foreach { i =>
          val index = i * model.VERTEX_ELEMENTS
          model.vertexBufferRaw.put(
            index+0, model.vertexBufferRaw.get(index+0) + v.skinVertPos.x * nexteffect)
          model.vertexBufferRaw.put(
            index+1, model.vertexBufferRaw.get(index+1) + v.skinVertPos.y * nexteffect)
          model.vertexBufferRaw.put(
            index+2, model.vertexBufferRaw.get(index+2) + v.skinVertPos.z * nexteffect)
        })
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
