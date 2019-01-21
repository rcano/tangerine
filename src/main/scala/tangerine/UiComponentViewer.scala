package tangerine

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import scala.collection.JavaConverters._
import scala.util.control.NoStackTrace

/**
 * Provides an easy way using the DevAppReloader to work on UI Components.
 * This class is marked as abstract so that it is inherited in your actual source. This is required so that
 * DevAppReloader uses a class in your code base as base and the class loader mechanism works.
 * @example 
 * {{{
 * class UiComponentViewer extends tangerine.UiComponentViewer
 * }}}
 *
 */
abstract class UiComponentViewer extends Application {
  override def start(stage: Stage) = {
    val thisClassName = getClass.getCanonicalName
    val uiCompClass = 
      getParameters.getUnnamed.asScala match {
        case Seq(`thisClassName`, className) => className
        case _ => throw new IllegalArgumentException("The viewer is meant to be used with DevAppReloader") with NoStackTrace
      }
    stage setTitle s"UiComponentViewer: $uiCompClass"
    val comp = getClass.getClassLoader.loadClass(uiCompClass).getDeclaredConstructor().newInstance().asInstanceOf[UiComponent]
    stage setScene new Scene(new StackPane(comp.component))
    scala.util.Properties.propOrNone("uicomponentviewer.css.path") foreach (l =>
      stage.getScene.getStylesheets.add(l))
    comp.setupSample()
    
    if (!(stage.getWidth > 0 && stage.getHeight > 0)) stage.sizeToScene()
    stage.show()
  }
}
