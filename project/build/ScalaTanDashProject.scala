import sbt._

class ScalaTanDashProject(info: ProjectInfo) extends DefaultWebstartProject(info) {
  override def fork = Some(new ForkScalaRun {
    val os = System.getProperty("os.name").split(" ")(0).toLowerCase match {
      case "linux" => "linux"
      case "mac" => "macosx"
      case "windows" => "windows"
      case "sunos" => "solaris"
      case x => x
    }

    val newPath = System.getProperty("java.library.path") + ":" + ("lib"/"lwjgl"/"native"/os)

    override def runJVMOptions = super.runJVMOptions ++ Seq("-Djava.library.path=" + newPath,
      "-XX:+UseConcMarkSweepGC", "-XX:+CMSParallelRemarkEnabled", "-XX:+UseParNewGC")

    override def scalaJars = buildScalaInstance.libraryJar :: buildScalaInstance.compilerJar :: Nil
  })

  override def webstartLibraries = super.webstartLibraries.filter(
    !_.projectRelativePath.contains("lwjgl"))

  override def jnlpXML(libraries: Seq[WebstartJarResource]) =
    <jnlp spec="1.0+" codebase="http://scalatan.karatachi.org/" href={artifactBaseName + ".jnlp"}>
      <information>
        <title>Scala-tan Dash</title>
        <vendor>chimerast</vendor>
        <description>Scala-tan Dash</description>
        <offline-allowed />
      </information>
      <security>
        <all-permissions/>
      </security>
      <resources>
        <j2se version="1.5+" java-vm-args="-XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:+UseParNewGC" />
        { defaultElements(libraries) }
        <extension name="LWJGLExtension" href="http://lwjgl.org/jnlp/extension.php"/>
      </resources>
      <application-desc main-class="org.karatachi.scalatan.Bootstrap" />
    </jnlp>
}
