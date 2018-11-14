package tangerine

import com.sun.javafx.application.ParametersImpl
import java.io.IOException
import java.net.URLClassLoader
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{Files , StandardWatchEventKinds, FileVisitor, FileVisitResult, Path, Paths, WatchEvent}
import javafx.application.Application
import javafx.application.Platform
import scala.collection.JavaConverters._
import scala.util.control.NonFatal

/**
 * Main application used during developement for hot reloading of the application using classloaders magic.
 * Note that this class is meant for a sbt like setup, since it the file monitoring will be done over the directories `target/scala-2.12/classes` and `target/scala-2.12/test-classes`
 */
object DevAppReloader {
  def main(args: Array[String]) = {
    System.setProperty("prism.lcdtext", "false")
    System.setProperty("prism.text", "t2k")
    Application.launch(classOf[DevAppReloader], args:_*)
  }
}
class DevAppReloader extends Application {
  def sceneRoot = null
  val classesDirectories = Array(Paths.get("target/scala-2.12/classes"), Paths.get("target/scala-2.12/test-classes")).filter(Files.exists(_)).map(_.toAbsolutePath)
  override def init(): Unit = {
    super.init()
    //install a monitor on the classes to detect a change
    val fileWatcher = classesDirectories.head.getFileSystem.newWatchService
    classesDirectories foreach { classesDir =>
      Files.walkFileTree(classesDir, new FileVisitor[Path] {
          override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes) = {
            dir.register(fileWatcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY)
            FileVisitResult.CONTINUE
          }
          override def postVisitDirectory(dir: Path, excec: IOException) = {
            FileVisitResult.CONTINUE
          }
          override def visitFile(file: Path, attrs: BasicFileAttributes) = {
            FileVisitResult.CONTINUE
          }
          override def visitFileFailed(file: Path, excec: IOException) = {
            FileVisitResult.TERMINATE
          }
        })
      classesDir.register(fileWatcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY)
    }

    new Thread("ClassesChangesWatcher") {
      override def run(): Unit = {
        println("watching")
        var updateFound = false
        var lastUpdate = System.currentTimeMillis

        while(!isInterrupted()) {
          val now = System.currentTimeMillis
          val wk = fileWatcher.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS)
          if (wk != null) {
            wk.pollEvents.asScala foreach {
              case watchEvent: WatchEvent[Path @unchecked] =>
                val context = wk.watchable.asInstanceOf[Path].resolve(watchEvent.context)

                if (watchEvent.kind == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(context)) {
                  context.register(fileWatcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY)
                }
            }
            wk.reset()
            updateFound = true
            lastUpdate = now
          }
          if (updateFound && (now - lastUpdate) > 1000) { //if there was some time ago, trigger reloading
            updateFound = false
            reloadApp()
          }
        }
        println("watcher dying")
      }
    }.tap(_.setDaemon(true)).start()
  }

  var reloadCounter = 0
  @volatile var recompiling = false
  var lastApplication: Application = _
  def reloadApp(): Unit = {
    if (!recompiling) { // if I'm already recompiling, ignore the request. This might happen if the watcher thread detects many file changing in quick not not so quick intervals
      reloadCounter += 1
      println(Console.CYAN + "RELOADING #" + reloadCounter + Console.RESET)
      recompiling = true

      val classLoadingMxBean = java.lang.management.ManagementFactory.getClassLoadingMXBean()
      println("currently loaded classes " + classLoadingMxBean.getLoadedClassCount)
      Platform runLater new Runnable {
        def run: Unit = {
          //is there was an application, we need to dispose of it first
          if (lastApplication != null) {
            val loadedClassesBefore = classLoadingMxBean.getLoadedClassCount
            try lastApplication.stop()
            catch { case NonFatal(e) => println("stopping application failed"); e.printStackTrace() }
            primaryStage.setScene(null) //remove all references generated by the dynamically loaded class
            val cl = lastApplication.getClass.getClassLoader.asInstanceOf[URLClassLoader]
            System.gc()
            cl.close()
            val classesDifference = classLoadingMxBean.getLoadedClassCount - loadedClassesBefore
            if (classesDifference > 0) println(Console.YELLOW + s"WARNING: classes failed to be unloaded, leaked: $classesDifference" + Console.RESET)
            else println("unloaded classes " + classLoadingMxBean.getUnloadedClassCount)
          }

          println(classesDirectories.mkString("Root urls:[\n", "\n", "\n]"))
          val loader = new URLClassLoader(classesDirectories.map(_.toAbsolutePath.toUri.toURL).toArray) {
            //override default class loader behaviour to prioritize classes in this classloader
            override def loadClass(name: String, resolve: Boolean): Class[_] = {
              var res: Class[_] = findLoadedClass(name)
              val startTime = System.currentTimeMillis
              while (res == null && System.currentTimeMillis - startTime < 5000) {//will retry for an entire second for this class to appear
                try res = findClass(name)
                catch { case e: ClassNotFoundException =>
                    try res = super.loadClass(name, false)
                    catch { case e: ClassNotFoundException => Thread.sleep(50) } //sleep 50ms and retry
                }
              }
              if (res == null) throw new ClassNotFoundException(name)
              if (resolve) resolveClass(res)
              res
            }
          }

          lastApplication = loader.loadClass(getParameters.getRaw.get(0)).newInstance.asInstanceOf[Application]
          ParametersImpl.registerParameters(lastApplication, getParameters)
          try {
            lastApplication.init()
            lastApplication.start(primaryStage)
          } catch { case NonFatal(e) => println("starting application failed"); e.printStackTrace() }
          recompiling = false
        }
      }
    }
  }

  var primaryStage: javafx.stage.Stage = _
  override def start(stage: javafx.stage.Stage): Unit = {
    primaryStage = stage
    Platform.setImplicitExit(true)
    reloadApp()
  }
}