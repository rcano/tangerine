name := "tangerine"
version := "1.0.0"

ThisBuild / scalaVersion := "2.13.0"

fork := true

lazy val jfxVersion = "12.0.1"
lazy val jfxClassifier = settingKey[String]("jfxClassifier")
jfxClassifier := {
  if (scala.util.Properties.isWin) "win"
  else if (scala.util.Properties.isLinux) "linux"
  else if (scala.util.Properties.isMac) "mac"
  else throw new IllegalStateException(s"Unknown OS: ${scala.util.Properties.osName}")
}

libraryDependencies ++= Seq(
  "com.lihaoyi" %% "sourcecode" % "0.1.7",

  "com.github.pathikrit" %% "better-files" % "3.8.0",
  //"com.beachape" %% "enumeratum" % "1.5.13",

  "org.openjfx" % "javafx-graphics" % jfxVersion % "provided" classifier jfxClassifier.value,
  "org.openjfx" % "javafx-controls" % jfxVersion % "provided" classifier jfxClassifier.value,
  "org.openjfx" % "javafx-base" % jfxVersion % "provided" classifier jfxClassifier.value,
  
  "org.controlsfx" % "controlsfx" % "11.0.0" % "provided",
)
addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")

lazy val moduleJars = taskKey[Seq[(Attributed[File], java.lang.module.ModuleDescriptor)]]("moduleJars")
moduleJars := {
  val attributedJars = (Compile/dependencyClasspathAsJars).value.filterNot(_.metadata(moduleID.key).organization == "org.scala-lang")
  val modules = attributedJars.flatMap { aj =>
    try {
      val module = java.lang.module.ModuleFinder.of(aj.data.toPath).findAll().iterator.next.descriptor
      Some(aj -> module)
    } catch { case _: java.lang.module.FindException => None }
  }
  modules
}

javaOptions ++= {
  val modules = moduleJars.value
  Seq(
    "--add-modules=" + modules.map(_._2.name).mkString(","),
    "--module-path=" + modules.map(_._1.data.getAbsolutePath).mkString(java.io.File.pathSeparator)
  )
}

(reStart/javaOptions) += "--add-opens=javafx.graphics/com.sun.javafx.application=ALL-UNNAMED"
reStart/mainClass := Some("expenser.ui.DevAppReloader")
javacOptions := javaOptions.value
