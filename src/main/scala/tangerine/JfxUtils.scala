package tangerine

import javafx.beans.binding.BooleanBinding
import javafx.beans.value.{ChangeListener, ObservableValue, ObservableBooleanValue}
import javafx.event.Event
import javafx.geometry.Bounds
import javafx.scene.{Node, Scene}
import javafx.scene.text.{Font, Text}
import javafx.stage.Window

object JfxUtils {
  //copied from com.sun.javafx.scene.control.skin.Utils
  
  //single text instance, it's not a problem because you shouldn't use this method concurrently, as per JavaFX normal rules
  private val helper = new Text()
  private val DEFAULT_WRAPPING_WIDTH = helper.getWrappingWidth()
  private val DEFAULT_LINE_SPACING = helper.getLineSpacing()
  private val DEFAULT_TEXT = helper.getText()
  
  def computeTextBounds(text: String, font: Font, maxWidth: Double = 0, lineSpacing: Double = 0): Bounds = {
    helper.setText(text)
    helper.setFont(font)
    helper.setWrappingWidth(maxWidth)
    helper.setLineSpacing(lineSpacing)
    
    val res = helper.getLayoutBounds()
    
    // RESTORE STATE
    helper.setWrappingWidth(DEFAULT_WRAPPING_WIDTH)
    helper.setLineSpacing(DEFAULT_LINE_SPACING)
    helper.setText(DEFAULT_TEXT)
    
    res
  }
  
  /**
   * Creates a [[ChangeListener]] that gets executed once and then unregisters itself.
   */
  def onceChangeListener[T](f: (T, T) => Any): ChangeListener[_ >: T] = new ChangeListener[T] {
    def changed(obs: ObservableValue[_ <: T], oldv: T, newv: T) = {
      f(oldv, newv)
      obs.removeListener(this)
    }
  }
  
  
  /* unique object key used to map the showing property to Nodes*/
  private[this] object showingPropertyKey
  
  /**
   * Provides a showing property for a Node and cache it inside the Node properties.
   * The showing property is calculated based on the node being attached to a Window (transitively via a stage) and this window being visible.
   * @param node the Node for the showing property.  
   * @return an ObservableBooleanValue represting whether this node is showing or not on a window.
   */
  def showingProperty(node: Node): ObservableBooleanValue = {
    var property = node.getProperties.get(showingPropertyKey).asInstanceOf[ObservableBooleanValue]
    if (property == null) {
      property = new BooleanBinding {
        var scene: Scene = _
        var window: Window = _
        bind(node.sceneProperty)
        onInvalidating()
      
        override def computeValue(): Boolean = window != null && window.isShowing
        
        override def onInvalidating(): Unit = {
          //check if transitioning from no-scene to scene
          if (scene == null && node.getScene != null) {
            scene = node.getScene
            bind(node.getScene.windowProperty)
            if (node.getScene.getWindow != null) {
              if (window != null) unbind(window.showingProperty)
              window = node.getScene.getWindow
              bind(node.getScene.getWindow.showingProperty)
            }
          } 
          //check if transitioning from scene to no-scene
          if (node.getScene == null && scene != null) {
            unbind(scene.windowProperty)
            if (window != null) {
              unbind(window.showingProperty)
              window = null
            }
            scene = null
          }
          
          if (node.getScene != null) {
            //check if transitioning from window to no-window
            if (window != null && node.getScene.getWindow == null) {
              unbind(window.showingProperty)
              window = null
            } 
            // check if transitioning from no-window to window
            if (window == null && node.getScene.getWindow != null) {
              bind(window.showingProperty)
            }
          }
        }
      }
      node.getProperties.put(showingPropertyKey, property)
    }
    property
  }
  
  object JfxEvent {
    def unapply(evt: Event): ExtractedEvent = new ExtractedEvent(evt)
    
    class ExtractedEvent(val event: Event) extends AnyVal {
      def isEmpty = false
      def get = this
      def _1 = event.getSource
      def _2 = event.getTarget
      def _3 = event.getEventType
      def _4 = event
    }
  }
}
