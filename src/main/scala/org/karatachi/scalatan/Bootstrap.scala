package org.karatachi.scalatan

import org.lwjgl._

object Bootstrap {
  def main(args: Array[String]): Unit = {
    println(System.getProperty("java.library.path"))

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
