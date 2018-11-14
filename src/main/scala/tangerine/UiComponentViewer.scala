package tangerine

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import scala.collection.JavaConverters._
import scala.util.control.NoStackTrace

class UiComponentViewer extends Application {
  override def start(stage: Stage) = {
    val thisClassName = getClass.getCanonicalName
    val uiCompClass = 
      getParameters.getUnnamed.asScala match {
        case Seq(`thisClassName`, className) => className
        case _ => throw new IllegalArgumentException("The viewer is meant to be used with DevAppReloader") with NoStackTrace
      }
    stage setTitle s"UiComponentViewer: $uiCompClass"
    val comp = getClass.getClassLoader.loadClass(uiCompClass).getDeclaredConstructor().newInstance().asInstanceOf[UiComponent]
    stage setScene new Scene(new StackPane(comp.component).tap(_.getStylesheets.addAll("/expenser.css")))
    comp.setupSample()
    
    stage.sizeToScene()
    stage.show()
  }
}
