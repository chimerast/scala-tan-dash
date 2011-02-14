import sbt._
import java.io.File

class ScalaTanDashProject(info: ProjectInfo) extends DefaultProject(info) {
  override def fork = Some(new ForkScalaRun {
    val os = System.getProperty("os.name").split(" ")(0).toLowerCase match {
      case "linux" => "linux"
      case "mac" => "macosx"
      case "windows" => "windows"
      case "sunos" => "solaris"
      case x => x
    }

    val newPath = System.getProperty("java.library.path") + ":" + ("lib"/"lwjgl"/"native"/os)

    override def runJVMOptions = super.runJVMOptions ++ Seq("-Djava.library.path=" + newPath)

    override def scalaJars = Seq(
      new File("project/boot/scala-2.8.1/lib/scala-compiler.jar"),
      new File("project/boot/scala-2.8.1/lib/scala-library.jar"))
  })
}
