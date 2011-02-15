package org.karatachi.scalatan

import org.lwjgl._
import org.lwjgl.opengl._
import org.lwjgl.opengl.GL11._
import org.lwjgl.input._
import org.newdawn.slick._
import org.newdawn.slick.opengl._
import org.newdawn.slick.font.effects._
import org.karatachi.scala.opengl.GLUtils._

object Bootstrap {
  def main(args: Array[String]) {
    var fullscreen = false
    for (arg <- args) {
      arg match {
        case "-fullscreen" =>
          fullscreen = true
      }
    }

    try {
      ScalaTanDash.init(fullscreen)
      ScalaTanDash.run
    } catch {
      case e => {
        Sys.alert("Error", e.getMessage)
        e.printStackTrace
      }
    } finally {
      ScalaTanDash.cleanup
    }
  }
}
