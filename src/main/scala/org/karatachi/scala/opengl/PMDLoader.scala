package org.karatachi.scala.opengl

import java.io._
import java.nio._
import java.nio.channels._

import org.lwjgl._
import org.lwjgl.opengl._
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL12._
import org.lwjgl.opengl.GL13._
import org.lwjgl.opengl.GL15._
import org.lwjgl.opengl.GL20._
import org.newdawn.slick.opengl._
import org.karatachi.scala.IOUtils._
import org.karatachi.scala.opengl.GLUtils._
import org.karatachi.scala.opengl.ShaderProgram._

object PMDLoader {
  def load(path: String): PMDModel = {
    val file = new File(path)
    using(new RandomAccessFile(file, "r")) { f =>
      val channel = f.getChannel
      val buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size)
      buffer.order(ByteOrder.LITTLE_ENDIAN)

      new PMDModel(file, buffer)
    }
  }
}

class PMDFormatException extends Exception {
}

class PMDModel(file: File, buffer: ByteBuffer) {
  val basedir = file.getParent

  val header = new PMDHeader(buffer)
  val vertices = Array.fill(buffer.getInt) { new PMDVertex(buffer) }
  val indices = Array.fill(buffer.getInt) { buffer.getShort }
  val materials = Array.fill(buffer.getInt) { new PMDMaterial(buffer) }
  val bones = Array.fill(buffer.getShort) { new PMDBone(buffer) }
  val iks = Array.fill(buffer.getShort) { new PMDIKData(buffer) }
  val skins = Array.fill(buffer.getShort) { new PMDSkinData(buffer) }
  val skinIndex = Array.fill(buffer.get) { buffer.getShort }
  val boneDispName = Array.fill(buffer.get) { buffer.getString(50) }
  val boneDisp = Array.fill(buffer.getInt) { new PMDBoneDisp(buffer) }

  // DirectXとカリングの向きが違うので直す
  (0 until indices.length / 3).foreach { i =>
    val temp = indices(i * 3 + 1)
    indices(i * 3 + 1) = indices(i * 3 + 2)
    indices(i * 3 + 2) = temp
  }

  // テクスチャ
  val textures = materials.map { m =>
    val texture = Array[Option[Texture]](None, None)
    val filenames = m.textureFileName.split("\\*|/").map(_.replace('\\', '/'))
    filenames.foreach { f =>
      val file = new File(basedir, f)
      if (file.isFile) {
        val extension = file.getName.split("\\.").last.toUpperCase
        extension match {
          case "SPH" =>
            texture(1) = Some(TextureLoader.getTexture("BMP", new FileInputStream(file)))
          case "SPA" =>
            texture(1) = Some(TextureLoader.getTexture("BMP", new FileInputStream(file)))
          case ext =>
            texture(0) =Some(TextureLoader.getTexture(ext, new FileInputStream(file)))
        }
      }
    }
    texture
  }

  // ボーンのインデックスの設定および親子関係の構築
  bones.indices.foreach { i => bones(i).index = i }
  bones.filter(_.parentBoneIndex != -1).foreach { b => bones(b.parentBoneIndex).children ::= b }

  val VERTEX_ELEMENTS = 11
  val VERTEX_BUFFER_STRIDE = VERTEX_ELEMENTS * 4;

  /** 頂点リストの元配列 */
  private val vertexBufferRaw = {
    val buffer = BufferUtils.createByteBuffer(vertices.length * VERTEX_BUFFER_STRIDE)
    vertices.foreach { v =>
      buffer.putFloat(v.pos.x)
      buffer.putFloat(v.pos.y)
      buffer.putFloat(v.pos.z)
      buffer.putFloat(v.normal.x)
      buffer.putFloat(v.normal.y)
      buffer.putFloat(v.normal.z)
      buffer.putFloat(v.uv.u)
      buffer.putFloat(v.uv.v)
      buffer.putFloat(v.boneNum(0))
      buffer.putFloat(v.boneNum(1))
      buffer.putFloat(v.boneWight / 100.0f)
    }
    buffer.flip
    buffer
  }

  /** 頂点リスト */
  val vertexBuffer = {
    glLoadBufferObject(GL_ARRAY_BUFFER, vertexBufferRaw)
  }
  /** 連結リスト */
  val indexBuffer = {
    val buffer = BufferUtils.createShortBuffer(indices.length)
    buffer.put(indices)
    buffer.flip
    glLoadBufferObject(GL_ELEMENT_ARRAY_BUFFER, buffer)
  }
  /** スキニング用変換行列リスト */
  val matrixBuffer = BufferUtils.createFloatBuffer(16 * 256)

  var activeSkins = List(1)
  var skinEffect = 0.0f

  def loadMatrix(bone: PMDBone): Unit = {
    glPushMatrix
    matrixBuffer.position(bone.index * 16)
    glGetFloat(GL_MODELVIEW_MATRIX, matrixBuffer)
    bone.children.foreach(loadMatrix)
    glPopMatrix
  }

  def render(): Unit = {
    glEnable(GL_DEPTH_TEST)

    matrixBuffer.clear()
    loadMatrix(bones(0))
    matrixBuffer.position(0)
    matrixBuffer.limit(matrixBuffer.capacity)
    ShaderProgram.active.foreach(_.uniform("modelViewMatrix[0]")(
      glUniformMatrix4(_, false,  matrixBuffer)))

    // baseスキンの読み込み
    skins(0).skinVertData.foreach { v =>
      val index = v.skinVertIndex * VERTEX_ELEMENTS
      vertexBufferRaw.putFloat((index + 0) * 4, v.skinVertPos.x)
      vertexBufferRaw.putFloat((index + 1) * 4, v.skinVertPos.y)
      vertexBufferRaw.putFloat((index + 2) * 4, v.skinVertPos.z)
    }

    // スキンの差分を適用
    activeSkins.foreach { i =>
      skins(i).skinVertData.foreach { v =>
        val index = skins(0).skinVertData(v.skinVertIndex).skinVertIndex * VERTEX_ELEMENTS
        vertexBufferRaw.putFloat((index + 0) * 4,
          vertexBufferRaw.getFloat((index + 0) * 4) + v.skinVertPos.x * skinEffect)
        vertexBufferRaw.putFloat((index + 1) * 4,
          vertexBufferRaw.getFloat((index + 1) * 4) + v.skinVertPos.y * skinEffect)
        vertexBufferRaw.putFloat((index + 2) * 4,
          vertexBufferRaw.getFloat((index + 2) * 4) + v.skinVertPos.z * skinEffect)
      }
    }

    // vertex bufferのバインドとリロード
    glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer)
    glBufferData(GL_ARRAY_BUFFER, vertexBufferRaw, GL_DYNAMIC_DRAW)

    // index bufferのバインド
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer)

    // 頂点データをvertex bufferを使用するように設定
    glEnableClientState(GL_VERTEX_ARRAY)
    glEnableClientState(GL_NORMAL_ARRAY)
    glEnableClientState(GL_TEXTURE_COORD_ARRAY)
    ShaderProgram.active.foreach(_.attribute("boneIndex")(glEnableVertexAttribArray(_)))
    ShaderProgram.active.foreach(_.attribute("boneWeight")(glEnableVertexAttribArray(_)))

    // vertex buffer中のデータの位置を指定
    glVertexPointer(3, GL_FLOAT, VERTEX_BUFFER_STRIDE, 0)
    glNormalPointer(GL_FLOAT, VERTEX_BUFFER_STRIDE, 3 * 4)
    glTexCoordPointer(2, GL_FLOAT, VERTEX_BUFFER_STRIDE, 6 * 4)
    ShaderProgram.active.foreach(_.attribute("boneIndex")(
      glVertexAttribPointer(_, 2, GL_FLOAT, false, VERTEX_BUFFER_STRIDE, 8 * 4)))
    ShaderProgram.active.foreach(_.attribute("boneWeight")(
      glVertexAttribPointer(_, 1, GL_FLOAT, false, VERTEX_BUFFER_STRIDE, 10 * 4)))

    ShaderProgram.active.foreach(_.uniform("texture0")(glUniform1i(_, 0)))
    ShaderProgram.active.foreach(_.uniform("texture1")(glUniform1i(_, 1)))

    // マテリアル毎に描画を行う
    var index = 0
    materials.indices.foreach { i =>
      val m = materials(i)
      m.material.bind

      // テクスチャの設定
      glActiveTexture(GL_TEXTURE0)
      textures(i)(0) match {
        case Some(texture) =>
          ShaderProgram.active.foreach(_.uniform("texturing")(glUniform1i(_, GL_TRUE)))
          texture.bind
        case None =>
          ShaderProgram.active.foreach(_.uniform("texturing")(glUniform1i(_, GL_FALSE)))
          glBindTexture(GL_TEXTURE_2D, 0)
      }

      // 環境マッピングの設定
      glActiveTexture(GL_TEXTURE1)
      textures(i)(1) match {
        case Some(texture) =>
          if (m.textureFileName.endsWith(".sph"))
            ShaderProgram.active.foreach(_.uniform("sphere")(glUniform1i(_, 1)))
          else
            ShaderProgram.active.foreach(_.uniform("sphere")(glUniform1i(_, 2)))
          texture.bind
        case None =>
          ShaderProgram.active.foreach(_.uniform("sphere")(glUniform1i(_, 0)))
          glBindTexture(GL_TEXTURE_2D, 0)
      }

      // 描画
      glDrawRangeElements(GL_TRIANGLES, 0, vertices.length - 1, m.faceVertCount,
        GL_UNSIGNED_SHORT, index << 1)

      index += m.faceVertCount
    }

    // 後始末
    glDisableClientState(GL_VERTEX_ARRAY)
    glDisableClientState(GL_NORMAL_ARRAY)
    glDisableClientState(GL_TEXTURE_COORD_ARRAY)
    ShaderProgram.active.foreach(_.attribute("boneIndex")(glDisableVertexAttribArray(_)))
    ShaderProgram.active.foreach(_.attribute("boneWeight")(glDisableVertexAttribArray(_)))

    glBindBuffer(GL_ARRAY_BUFFER, 0)
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)

    glActiveTexture(GL_TEXTURE1)
    glBindTexture(GL_TEXTURE_2D, 0)
    glActiveTexture(GL_TEXTURE0)
    glBindTexture(GL_TEXTURE_2D, 0)

    ShaderProgram.active.foreach(_.uniform("texturing")(glUniform1i(_, GL_FALSE)))
    ShaderProgram.active.foreach(_.uniform("sphere")(glUniform1i(_, 0)))
  }
}

/*
 * 以下データロード用構造体
 */
class PMDHeader(buffer: ByteBuffer) {
  val magic = buffer.getString(3)
  val version = buffer.getFloat
  val modelName = buffer.getString(20)
  val comment = buffer.getString(256)

  if (magic != "Pmd")
    throw new PMDFormatException
}

class PMDHeaderEg(buffer: ByteBuffer, model: PMDModel) {
  val englishNameCompatibility = buffer.get
  val modelName = buffer.getString(20)
  val comment = buffer.getString(256)
  val boneNameEg = Array.fill(model.bones.length) { buffer.getString(20) }
  val skinNameEg = Array.fill(model.skins.length - 1) { buffer.getString(20) }
  val dispNameEg = Array.fill(model.boneDispName.length) { buffer.getString(50) }
}

class PMDVertex(buffer: ByteBuffer) {
  val pos = new XVector(buffer)
  val normal = new XVector(buffer)
  val uv = new XCoords2d(buffer)
  val boneNum = Array(buffer.getShort, buffer.getShort)
  val boneWight = buffer.get
  val edgeFlag = buffer.get
}

class PMDMaterial(buffer: ByteBuffer) {
  val material = new XMaterial(buffer)
  val toonIndex = buffer.get
  val edgeFlag = buffer.get
  val faceVertCount = buffer.getInt
  val textureFileName = buffer.getString(20)
}

class PMDBone(buffer: ByteBuffer) {
  val boneName = buffer.getString(20)
  val parentBoneIndex = buffer.getShort
  val tailPosBoneIndex = buffer.getShort
  val boneType = buffer.get
  val dummy = buffer.getShort
  val boneHeadPos = new XVector(buffer)

  var index = -1
  var children = List[PMDBone]()
}

class PMDIKData(buffer: ByteBuffer) {
  val ikBoneIndex = buffer.getShort
  val ikTargetBoneIndex = buffer.getShort
  val ikChainLength = buffer.get
  val iteration = buffer.getShort
  val controlWieght = buffer.getFloat
  val ikChildBoneIndex = Array.fill(ikChainLength) { buffer.getShort }
}

class PMDSkinVertData(buffer: ByteBuffer) {
  val skinVertIndex = buffer.getInt
  val skinVertPos = new XVector(buffer)
}

class PMDSkinData(buffer: ByteBuffer) {
  val skinName = buffer.getString(20)
  val skinVertCount = buffer.getInt
  val skinType = buffer.get
  val skinVertData = Array.fill(skinVertCount) { new PMDSkinVertData(buffer) }
}

class PMDBoneDisp(buffer: ByteBuffer) {
  val boneIndex = buffer.getShort
  val boneDispFrameIndex = buffer.get
}

class XVector(buffer: ByteBuffer) {
  val x = buffer.getFloat
  val y = buffer.getFloat
  val z = -buffer.getFloat
}

class XCoords2d(buffer: ByteBuffer) {
  val u = buffer.getFloat
  val v = buffer.getFloat
}

class XColorRGBA(buffer: ByteBuffer) {
  val r = buffer.getFloat
  val g = buffer.getFloat
  val b = buffer.getFloat
  val a = buffer.getFloat
}

class XColorRGB(buffer: ByteBuffer) {
  val r = buffer.getFloat
  val g = buffer.getFloat
  val b = buffer.getFloat
}

class XMaterial(buffer: ByteBuffer) {
  val diffuse = new XColorRGBA(buffer)
  val power = buffer.getFloat
  val specular = new XColorRGB(buffer)
  val ambient = new XColorRGB(buffer)

  private val values = {
    val values = BufferUtils.createFloatBuffer(12)
    values.put(diffuse.r).put(diffuse.g).put(diffuse.b).put(diffuse.a)
    values.put(specular.r).put(specular.g).put(specular.b).put(1.0f)
    values.put(ambient.r).put(ambient.g).put(ambient.b).put(1.0f)
    values.flip
    values
  }

  def bind = {
    values.position(0)
    glMaterial(GL_FRONT, GL_DIFFUSE, values)
    glMaterialf(GL_FRONT, GL_SHININESS, power)
    values.position(4)
    glMaterial(GL_FRONT, GL_SPECULAR, values)
    values.position(8)
    glMaterial(GL_FRONT, GL_AMBIENT, values)
  }
}
