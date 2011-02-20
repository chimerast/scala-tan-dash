package org.karatachi.scala.opengl

import java.io._
import java.nio._
import java.nio.channels._

import org.lwjgl._
import org.lwjgl.opengl._
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL12._
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
  val textures = materials.map { m =>
    val file = new File(basedir, m.textureFileName)
    if (file.isFile) {
      val extension = file.getName.split("\\.").last.toUpperCase
      Some(TextureLoader.getTexture(extension, new FileInputStream(file)))
    } else {
      None
    }
  }

  skins.indices.foreach { i => println(i + ":" + skins(i).skinName + ":" + skins(i).skinType) }

  val VERTEX_ELEMENTS = 8
  val VERTEX_BUFFER_STRIDE = VERTEX_ELEMENTS * 4;

  private val vertexBufferRaw = {
    val buffer = BufferUtils.createFloatBuffer(vertices.length * VERTEX_ELEMENTS)
    vertices.foreach(v => {
      buffer.put(v.pos.x)
      buffer.put(v.pos.y)
      buffer.put(v.pos.z)
      buffer.put(v.normal.x)
      buffer.put(v.normal.y)
      buffer.put(v.normal.z)
      buffer.put(v.uv.u)
      buffer.put(v.uv.v)
    })
    buffer.flip
    buffer
  }
  val vertexBuffer = {
    glLoadBufferObject(GL_ARRAY_BUFFER, vertexBufferRaw)
  }
  val indexBuffer = {
    val buffer = BufferUtils.createShortBuffer(indices.length)
    buffer.put(indices)
    buffer.flip
    glLoadBufferObject(GL_ELEMENT_ARRAY_BUFFER, buffer)
  }

  var activeSkins = List(1)
  var skinEffect = 0.0f

  def render = {
    glEnable(GL_DEPTH_TEST)

    glEnableClientState(GL_VERTEX_ARRAY)
    glEnableClientState(GL_NORMAL_ARRAY)
    glEnableClientState(GL_TEXTURE_COORD_ARRAY)

    glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer)
    skins(0).skinVertData.foreach { v =>
      val index = v.skinVertIndex * VERTEX_ELEMENTS
      vertexBufferRaw.put(index + 0, v.skinVertPos.x)
      vertexBufferRaw.put(index + 1, v.skinVertPos.y)
      vertexBufferRaw.put(index + 2, v.skinVertPos.z)
    }
    activeSkins.foreach { i =>
      skins(i).skinVertData.foreach { v =>
        val index = skins(0).skinVertData(v.skinVertIndex).skinVertIndex * VERTEX_ELEMENTS
        vertexBufferRaw.put(index + 0, vertexBufferRaw.get(index + 0) + v.skinVertPos.x * skinEffect)
        vertexBufferRaw.put(index + 1, vertexBufferRaw.get(index + 1) + v.skinVertPos.y * skinEffect)
        vertexBufferRaw.put(index + 2, vertexBufferRaw.get(index + 2) + v.skinVertPos.z * skinEffect)
      }
    }
    glBufferData(GL_ARRAY_BUFFER, vertexBufferRaw, GL_DYNAMIC_DRAW)

    glVertexPointer(3, GL_FLOAT, VERTEX_BUFFER_STRIDE, 0)
    glNormalPointer(GL_FLOAT, VERTEX_BUFFER_STRIDE, 3 * 4)
    glTexCoordPointer(2, GL_FLOAT, VERTEX_BUFFER_STRIDE, 6 * 4)

    var index = 0
    materials.indices.foreach { i =>
      val m = materials(i)
      glColor3f(m.material.diffuse.r, m.material.diffuse.g, m.material.diffuse.b)
      textures(i) match {
        case Some(a) =>
          ShaderProgram.active.foreach(_.uniform("texturing")(glUniform1i(_, 1)))
          a.bind
        case None =>
          ShaderProgram.active.foreach(_.uniform("texturing")(glUniform1i(_, 0)))
          glBindTexture(GL_TEXTURE_2D, 0)
      }
      glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer)
      glDrawRangeElements(GL_TRIANGLES, 0, vertices.length - 1, m.faceVertCount,
        GL_UNSIGNED_SHORT, index << 1)
      index += m.faceVertCount
    }

    glDisableClientState(GL_VERTEX_ARRAY)
    glDisableClientState(GL_NORMAL_ARRAY)
    glDisableClientState(GL_TEXTURE_COORD_ARRAY)

    glBindBuffer(GL_ARRAY_BUFFER, 0)
  }
}

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
  val z = buffer.getFloat
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
}
