package org.karatachi.scala.opengl

import scala.collection.mutable._

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
import org.lwjgl.util.vector._
import org.newdawn.slick.opengl._

import org.karatachi.scala.IOUtils._
import org.karatachi.scala.opengl.GLUtils._
import org.karatachi.scala.opengl.ShaderProgram._

object PMDLoader {
  def load(path: String): Option[PMDModel] = {
    try {
      val file = new File(path)
      using(new RandomAccessFile(file, "r")) { f =>
        val channel = f.getChannel
        val buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        Some(new PMDModel(file, buffer))
      }
    } catch {
      case e: Exception => e.printStackTrace; None
    }
  }
}

class PMDModel(file: File, buffer: ByteBuffer) {
  val MAX_MATRICES = 32

  val VERTEX_ELEMENTS = 12
  val VERTEX_BUFFER_STRIDE = VERTEX_ELEMENTS * 4;

  val basedir = file.getParent

  val header = new PMDHeader(buffer)
  val vertices = Array.fill(buffer.getInt) { new PMDVertex(buffer) }
  val indices = Array.fill(buffer.getInt) { val i = buffer.getShort; if (i < 0) i + 0x10000 else i.toInt }
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

  // テクスチャ読み込み
  materials.foreach { m =>
    val filenames = m.textureFileName.split("\\*|/").map(_.replace('\\', '/'))
    filenames.foreach { f =>
      val file = new File(basedir, f)
      if (file.isFile) {
        val extension = file.getName.split("\\.").last.toUpperCase
        extension match {
          case "SPH" =>
            m.textures(1) = Some(TextureLoader.getTexture("BMP", new FileInputStream(file)))
          case "SPA" =>
            m.textures(1) = Some(TextureLoader.getTexture("BMP", new FileInputStream(file)))
          case ext =>
            m.textures(0) = Some(TextureLoader.getTexture(ext, new FileInputStream(file)))
        }
      }
    }
  }

  // ボーンのインデックスの設定および親子関係の構築
  bones.indices.foreach { i => bones(i).index = i }
  bones.filter(_.parentBoneIndex != -1).foreach { b => bones(b.parentBoneIndex).children ::= b }

  // 表示リストの頂点置換マップ
  val verticesmap = new HashMap[Int, Set[Int]] with MultiMap[Int, Int]

  // 表示リスト
  val displaylist = {
    val displaylist = new ArrayBuffer[PMDDisplayList]

    var vertexbase = 0
    var indexbase = 0
    materials.foreach { m =>
      var rest = ((indexbase / 3) until ((indexbase + m.faceVertCount) / 3)).toArray

      while (rest.length > 0) {
        val matricesbuf = new ArrayBuffer[Int]
        val verticesbuf = new ArrayBuffer[Float]
        val indicesbuf = new ArrayBuffer[Int]
        val map = new ArrayBuffer[Int]

        def translate(orig: Int): Int = {
          var trans = map.indexOf(orig)
          if (trans == -1 && matricesbuf.size < MAX_MATRICES - 6) {
            map += orig
            trans = map.indexOf(orig)
            verticesmap.addBinding(orig, trans + vertexbase)

            val v = vertices(orig)
            if (matricesbuf.indexOf(v.boneNum._1) == -1)
              matricesbuf += v.boneNum._1
            if (matricesbuf.indexOf(v.boneNum._2) == -1)
              matricesbuf += v.boneNum._2

            verticesbuf.append(v.pos.x, v.pos.y, v.pos.z)
            verticesbuf.append(v.normal.x, v.normal.y, v.normal.z)
            verticesbuf.append(v.uv.u, v.uv.v)
            verticesbuf.append(matricesbuf.indexOf(v.boneNum._1))
            verticesbuf.append(matricesbuf.indexOf(v.boneNum._2))
            verticesbuf.append(1.0f - (v.boneWight / 100.0f))
            verticesbuf.append(v.edgeFlag)
          }
          return trans
        }

        rest = rest.filter { i =>
          val trans0 = translate(indices(i * 3 + 0))
          val trans1 = translate(indices(i * 3 + 1))
          val trans2 = translate(indices(i * 3 + 2))

          if (trans0 != -1 && trans1 != -1 && trans2 != -1) {
            indicesbuf.append(trans0 + vertexbase, trans1 + vertexbase, trans2 + vertexbase)
            false
          } else {
            true
          }
        }

        displaylist += PMDDisplayList(m, matricesbuf.toArray, verticesbuf.toArray, indicesbuf.toArray)
        vertexbase += map.length
      }

      indexbase += m.faceVertCount
    }
    displaylist.toArray
  }

  /** 頂点リストの元配列 */
  val vertexBufferRaw = {
    val verticeslength = displaylist.foldLeft(0)(_ + _.vertices.length)
    val buffer = BufferUtils.createFloatBuffer(verticeslength)
    displaylist.foreach(l => buffer.put(l.vertices))
    buffer.flip
    buffer
  }
  /** 連結リストの元配列 */
  val indexBufferRaw = {
    val indiceslength = displaylist.foldLeft(0)(_ + _.indices.length)
    val buffer = BufferUtils.createIntBuffer(indiceslength)
    displaylist.foreach(l => buffer.put(l.indices))
    buffer.flip
    buffer
  }

  /** 頂点リスト */
  private val vertexBuffer = glLoadBufferObject(GL_ARRAY_BUFFER, vertexBufferRaw)
  /** 連結リスト */
  private val indexBuffer = glLoadBufferObject(GL_ARRAY_BUFFER, indexBufferRaw)

  /** スキニング用変換行列リスト */
  private val modelViewMatrix = new Array[Float](16 * bones.length)
  private val normalMatrix = new Array[Float](9 * bones.length)

  private val modelViewMatrixBuffer = BufferUtils.createFloatBuffer(16 * MAX_MATRICES)
  private val normalMatrixBuffer = BufferUtils.createFloatBuffer(9 * MAX_MATRICES)

  /** 使い回し用オブジェクト */
  private val tempMatrix = new Matrix4f
  private val tempMatrixBuffer = BufferUtils.createFloatBuffer(16)

  private var motion = None.asInstanceOf[Option[VMDModel]]

  def attachMotion(path: String): Unit = {
    motion = VMDLoader.load(path, this)
  }

  def render(frame: Float=0.0f): Unit = {
    glEnable(GL_DEPTH_TEST)

    // 全ボーンの変換行列の取得と設定
    loadBoneMatrix(bones(0), frame)

    if (!skins.isEmpty) {
      // Baseスキンの読み込み
      skins(0).skinVertData.foreach { v =>
        verticesmap.get(v.skinVertIndex).foreach(_.foreach { i =>
          val index = i * VERTEX_ELEMENTS
          vertexBufferRaw.put(index+0, v.skinVertPos.x)
          vertexBufferRaw.put(index+1, v.skinVertPos.y)
          vertexBufferRaw.put(index+2, v.skinVertPos.z)
        })
      }

      // スキンの差分を適用
      (1 to 4).foreach(i => motion.foreach(_.applySkin(i, frame)))
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
    ShaderProgram.active.foreach(_.attribute("edgeFlag")(glEnableVertexAttribArray(_)))

    // vertex buffer中のデータの位置を指定
    glVertexPointer(3, GL_FLOAT, VERTEX_BUFFER_STRIDE, 0)
    glNormalPointer(GL_FLOAT, VERTEX_BUFFER_STRIDE, 3 * 4)
    glTexCoordPointer(2, GL_FLOAT, VERTEX_BUFFER_STRIDE, 6 * 4)
    ShaderProgram.active.foreach(_.attribute("boneIndex")(
      glVertexAttribPointer(_, 2, GL_FLOAT, false, VERTEX_BUFFER_STRIDE, 8 * 4)))
    ShaderProgram.active.foreach(_.attribute("boneWeight")(
      glVertexAttribPointer(_, 1, GL_FLOAT, false, VERTEX_BUFFER_STRIDE, 10 * 4)))
    ShaderProgram.active.foreach(_.attribute("edgeFlag")(
      glVertexAttribPointer(_, 1, GL_FLOAT, false, VERTEX_BUFFER_STRIDE, 11 * 4)))

    ShaderProgram.active.foreach(_.uniform("texture0")(glUniform1i(_, 0)))
    ShaderProgram.active.foreach(_.uniform("texture1")(glUniform1i(_, 1)))

    // ディスプレイリスト毎に描画を行う
    var vertexbase = 0
    var indexbase = 0
    displaylist.foreach { l =>
      l.material.material.bind

      modelViewMatrixBuffer.clear
      normalMatrixBuffer.clear
      l.matrices.foreach { m =>
        modelViewMatrixBuffer.put(modelViewMatrix, m*16, 16)
        normalMatrixBuffer.put(normalMatrix, m*9, 9)
      }

      modelViewMatrixBuffer.clear
      normalMatrixBuffer.clear

      ShaderProgram.active.foreach(_.uniform("modelViewMatrix[0]")(
        glUniformMatrix4(_, false,  modelViewMatrixBuffer)))
      ShaderProgram.active.foreach(_.uniform("normalMatrix[0]")(
        glUniformMatrix3(_, false,  normalMatrixBuffer)))

      // テクスチャの設定
      glActiveTexture(GL_TEXTURE0)
      l.material.textures(0) match {
        case Some(texture) =>
          ShaderProgram.active.foreach(_.uniform("texturing")(glUniform1i(_, GL_TRUE)))
          texture.bind
        case None =>
          ShaderProgram.active.foreach(_.uniform("texturing")(glUniform1i(_, GL_FALSE)))
          glBindTexture(GL_TEXTURE_2D, 0)
      }

      // 環境マッピングの設定
      glActiveTexture(GL_TEXTURE1)
      l.material.textures(1) match {
        case Some(texture) =>
          if (l.material.textureFileName.endsWith(".sph"))
            ShaderProgram.active.foreach(_.uniform("sphere")(glUniform1i(_, 1)))
          else
            ShaderProgram.active.foreach(_.uniform("sphere")(glUniform1i(_, 2)))
          texture.bind
        case None =>
          ShaderProgram.active.foreach(_.uniform("sphere")(glUniform1i(_, 0)))
          glBindTexture(GL_TEXTURE_2D, 0)
      }

      // 描画
      val vertexend = vertexbase + l.vertices.length / VERTEX_ELEMENTS - 1
      glDrawRangeElements(GL_TRIANGLES, vertexbase, vertexend,
                          l.indices.length, GL_UNSIGNED_INT, indexbase * 4)

      vertexbase = vertexend + 1
      indexbase += l.indices.length
    }

    // 後始末
    glDisableClientState(GL_VERTEX_ARRAY)
    glDisableClientState(GL_NORMAL_ARRAY)
    glDisableClientState(GL_TEXTURE_COORD_ARRAY)
    ShaderProgram.active.foreach(_.attribute("boneIndex")(glDisableVertexAttribArray(_)))
    ShaderProgram.active.foreach(_.attribute("boneWeight")(glDisableVertexAttribArray(_)))
    ShaderProgram.active.foreach(_.attribute("edgeFlag")(glDisableVertexAttribArray(_)))

    glBindBuffer(GL_ARRAY_BUFFER, 0)
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)

    glActiveTexture(GL_TEXTURE1)
    glBindTexture(GL_TEXTURE_2D, 0)
    glActiveTexture(GL_TEXTURE0)
    glBindTexture(GL_TEXTURE_2D, 0)

    ShaderProgram.active.foreach(_.uniform("texturing")(glUniform1i(_, GL_FALSE)))
    ShaderProgram.active.foreach(_.uniform("sphere")(glUniform1i(_, 0)))
  }

  def loadBoneMatrix(bone: PMDBone, frame: Float): Unit = {
    glMatrix {
      // ボーンアニメーションの読み込み
      motion.foreach(_.applyBoneMatrix(bone, frame))

      // 現在の変換行列をバッファに書き込む
      tempMatrixBuffer.clear
      glGetFloat(GL_MODELVIEW_MATRIX, tempMatrixBuffer)

      tempMatrixBuffer.clear
      tempMatrixBuffer.get(modelViewMatrix, bone.index*16, 16)

      // 法線計算のための逆行列の算出
      tempMatrixBuffer.clear
      tempMatrix.load(tempMatrixBuffer)
      tempMatrix.invert
      normalMatrix(bone.index*9+0) = tempMatrix.m00;
      normalMatrix(bone.index*9+1) = tempMatrix.m10;
      normalMatrix(bone.index*9+2) = tempMatrix.m20;
      normalMatrix(bone.index*9+3) = tempMatrix.m01;
      normalMatrix(bone.index*9+4) = tempMatrix.m11;
      normalMatrix(bone.index*9+5) = tempMatrix.m21;
      normalMatrix(bone.index*9+6) = tempMatrix.m02;
      normalMatrix(bone.index*9+7) = tempMatrix.m12;
      normalMatrix(bone.index*9+8) = tempMatrix.m22;

      bone.children.foreach(loadBoneMatrix(_, frame))
    }
  }
}

case class PMDDisplayList(material: PMDMaterial, matrices: Array[Int],
                          vertices: Array[Float], indices: Array[Int])

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
  val pos = new Vector(buffer)
  val normal = new Vector(buffer)
  val uv = new Coords2d(buffer)
  val boneNum = (buffer.getShort, buffer.getShort)
  val boneWight = buffer.get
  val edgeFlag = buffer.get
}

class PMDMaterial(buffer: ByteBuffer) {
  val material = new Material(buffer)
  val toonIndex = buffer.get
  val edgeFlag = buffer.get
  val faceVertCount = buffer.getInt
  val textureFileName = buffer.getString(20)

  val textures = Array[Option[Texture]](None, None)
}

class PMDBone(buffer: ByteBuffer) {
  val boneName = buffer.getString(20)
  val parentBoneIndex = buffer.getShort
  val tailPosBoneIndex = buffer.getShort
  val boneType = buffer.get
  val ikParentBoneIndex = buffer.getShort
  val boneHeadPos = new Vector(buffer)

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
  val skinVertPos = new Vector(buffer)
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

class PMDFormatException extends Exception {
}
