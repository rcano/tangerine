package tangerine

import javafx.beans.binding.BooleanBinding
import javafx.beans.value.{ChangeListener, ObservableValue, ObservableBooleanValue}
import javafx.collections.ListChangeListener
import javafx.geometry.Bounds
import javafx.scene.{Node, Scene, Parent}
import javafx.scene.control._
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
 
  /**
   * Find the front-most node that that has the smallest bounds around the given point
   */
  def pickNode(node: Node, sceneX: Double, sceneY: Double): Option[Node] = {
    //taken from http://fxexperience.com/2016/01/node-picking-in-javafx/
    val p = node.sceneToLocal(sceneX, sceneY)
    
    if (node contains p) {
      node match {
        case HasChildren(children@_*) =>
          // we iterate through all children in reverse order, and stop when we find a match.
          // We do this as we know the elements at the end of the list have a higher
          // z-order, and are therefore the better match, compared to children that
          // might also intersect (but that would be underneath the element).
          val bestMatchingChild = children.reverseIterator.find { child =>
            val p = child.sceneToLocal(sceneX, sceneY)
            child.isVisible && !child.isMouseTransparent && child.contains(p)
          }
          
          bestMatchingChild.flatMap(pickNode(_, sceneX, sceneY))
          
        case _ => Some(node)
      }
    } else None
  }
  
  /**
   * Produces an iterator that traverses, in breadth first style, the graph with `node` as root.
   * Children for a node are obtained using the [[HasChildren]] extractor
   */
  def traverseBreadthFirst(node: Node): Iterator[Node] = {
    val children = HasChildren.unapplySeq(node).getOrElse(Seq.empty)
    Iterator(node) ++ children.iterator ++ children.iterator.flatMap(n => traverseBreadthFirst(n).drop(1))
  }
  
  /**
   * Produces an iterator that traverses, in depth first style, the graph with `node` as root.
   */
  def traverseDepthFirst(node: Node): Iterator[Node] = 
    Iterator(node) ++ HasChildren.unapplySeq(node).getOrElse(Seq.empty).flatMap(traverseDepthFirst)
  
  /**
   * Register a listener for any structural change in a given [[Node]]. This works by recursively adding a listener to children so that
   * any structure change gets notified. It also self register to the nodes added, and deregisters from those removed.
   * Children node are obtained using [[HasChildren]] extractor.
   * @param node Root node of the graph
   * @param listener function to be called on each change
   * @return the Registered synthethized listener. You'll need this listener if you wish to unregister from the underlying graph.
   */
  def registerGraphStructureListener(node: Node)(listener: ListChangeListener.Change[_ <: Node] => Unit): StructureListenerRegistration = {
    object registration extends StructureListenerRegistration {
      val childrenChangeListener: ListChangeListener[Node] = evt => while (evt.next) {
        evt.getAddedSubList forEach register
        evt.getRemoved forEach unregister
        listener(evt)
      }
      val nodeChangeListener: ChangeListener[Node] = (_, prev, newv) => {
        unregister(prev)
        register(newv)
      }
      val tabsChangeListener: ListChangeListener[Tab] = evt => while(evt.next) {
        evt.getAddedSubList forEach (t => register(t.getContent))
        evt.getRemoved forEach (t => unregister(t.getContent))
      }
      def register(n: Node): Unit = traverseDepthFirst(n) foreach {
        case s: ScrollPane => s.contentProperty.addListener(nodeChangeListener)
        case t: TabPane => t.getTabs.addListener(tabsChangeListener)
        case l: Labeled => l.graphicProperty.addListener(nodeChangeListener)
        case a: Accordion => a.getPanes.addListener(childrenChangeListener)
        case t: ToolBar => t.getItems.addListener(childrenChangeListener)
        case b: ButtonBar => b.getButtons.addListener(childrenChangeListener)
        case p: Parent => p.getChildrenUnmodifiable.addListener(childrenChangeListener)
        case _ =>
      }
      def unregister(n: Node): Unit = traverseDepthFirst(n) match {
        case s: ScrollPane => s.contentProperty.removeListener(nodeChangeListener)
        case t: TabPane => t.getTabs.removeListener(tabsChangeListener)
        case l: Labeled => l.graphicProperty.removeListener(nodeChangeListener)
        case a: Accordion => a.getPanes.removeListener(childrenChangeListener)
        case t: ToolBar => t.getItems.removeListener(childrenChangeListener)
        case b: ButtonBar => b.getButtons.removeListener(childrenChangeListener)
        case p: Parent => p.getChildrenUnmodifiable.removeListener(childrenChangeListener)
        case p: Parent => p.getChildrenUnmodifiable.removeListener(childrenChangeListener)
        case _ =>
      }
      override def cancel = unregister(node)
    }
    registration.register(node)
    registration
  }
  
  trait StructureListenerRegistration {
    def cancel(): Unit
  }
}
