package tangerine

import javafx.beans.binding.ObjectBinding
import javafx.beans.property.{ReadOnlyObjectPropertyBase, SimpleIntegerProperty, SimpleObjectProperty}
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.event.Event
import javafx.geometry.Point2D
import javafx.scene.Node
import javafx.scene.input.{MouseButton, MouseEvent}
import javafx.scene.paint.{Color, Paint}
import javafx.scene.shape.Path
import javafx.scene.text.{Text, TextFlow}
import scala.collection.JavaConverters._
import scala.collection.immutable.TreeMap
import tangerine.{EventMatchers => Evts}
import Properties.Binding

class TextSelectionSupport() {
  
  def this(n: Node) = {
    this()
    rootNode set n
  }
  
  val rootNode = new SimpleObjectProperty[Node](this, "rootNode")
  val selectionBackground = new SimpleObjectProperty[Paint](this, "selectionBackground", Color.DODGERBLUE)
  private val textFlowsSeq = FXCollections.observableArrayList[TextFlow]()
  
  private var lastRegistration: tangerine.JfxUtils.StructureListenerRegistration = null
  rootNode.addListener { (_, oldv, newv) => 
    if (oldv != null) {
      textFlowsSeq.clear()
      lastRegistration.cancel()
    }

    registerTextFlowsIn(newv)
  
    lastRegistration = tangerine.JfxUtils.registerGraphStructureListener(newv) { evt =>
      evt.getAddedSubList.iterator.asScala foreach registerTextFlowsIn
      evt.getRemoved.iterator.asScala foreach unregisterTextFlowsIn
    }
    
    sm.subscribe(newv, MouseEvent.ANY)
  }
  
  val selection = new ReadOnlyObjectPropertyBase[Seq[Node Either String]] { self =>
    val changeListener = new ChangeListener[Number] with ListChangeListener[TextFlow] {
      override def changed(prop: ObservableValue[_ <: Number], oldv: Number, newv: Number) = self.fireValueChangedEvent()
      override def onChanged(evt: ListChangeListener.Change[_ <: TextFlow]) = {
        while (evt.next) {
          evt.getAddedSubList.asScala foreach { tf => 
            getTextFlowHelper(tf).selectionStart.addListener(this)
            getTextFlowHelper(tf).selectionEnd.addListener(this)
          }
          evt.getRemoved.asScala foreach { tf => 
            val helper = getTextFlowHelper(tf)
            helper.selectionStart.removeListener(this)
            helper.selectionEnd.removeListener(this)
            //check the removed nodes to see if any of these were part of the selection, to fire an update
            if (helper.selectionStart.get != -1) self.fireValueChangedEvent()
          }
        }
      }
    }
    textFlowsSeq.addListener(changeListener)
    
    override def getName = "selection"
    override def getBean = TextSelectionSupport.this
    override def get = for {
      tf <- textFlowsSeq.asScala
      helper = getTextFlowHelper(tf)
      startOffset = helper.selectionStart.get
      endOffset = helper.selectionEnd.get
      if helper.selectionStart.get != -1
      (offset, node) <- helper.layout.range(helper.layout.to(startOffset).lastKey, endOffset)
    } yield node match {
      case Left(node) => Left(node)
      case Right(text) => Right(text.getText.substring((startOffset - offset).max(0), (endOffset - offset).min(text.getText.length)))
    }
  }
  
  private def registerTextFlowsIn(rootNode: Node) = {
    tangerine.JfxUtils.traverseBreadthFirst(rootNode).foreach { 
      case tf: TextFlow => textFlowsSeq add tf
      case other =>
    }
  }
  private def unregisterTextFlowsIn(rootNode: Node) = 
    tangerine.JfxUtils.traverseBreadthFirst(rootNode).foreach { 
      case tf: TextFlow => textFlowsSeq remove tf
      case other =>
    }
  
  private def textFlowAt(sceneX: Double, sceneY: Double): Option[TextFlow] = textFlowsSeq.asScala.find { n =>
    val p = n.sceneToLocal(sceneX, sceneY)
    n.contains(p)
  }
  
  private object sm extends StateMachine[Event] {
    def initState = idle(None)
    def idle(lastTriggerEvent: Option[MouseEvent]): Transition = transition {
      case Evts.MouseEvent(_, _, MouseEvent.MOUSE_PRESSED, evt) if evt.getButton == MouseButton.PRIMARY => 
        //must clean selections first
        if (!evt.isShiftDown) for {
          tf <- textFlowsSeq.asScala
          p <- tf.getChildren.asScala.headOption.collect { case p: Path => p }
        } tf.getChildren.remove(p)
        
        textFlowAt(evt.getSceneX, evt.getSceneY) match {
          case None => current
          case Some(tf) =>
//            tf.getStyleClass.add("debug-region")
            val sourceEvent = lastTriggerEvent.filter(_ => evt.isShiftDown).getOrElse(evt)
            evt.getClickCount match {
              case 2 | 3 => updateTextFlow(tf, sourceEvent, evt)
              case _ =>
            }
            selecting(sourceEvent, tf)
        }
    }
    
    def selecting(triggerEvent: MouseEvent, lastNodeHovered: TextFlow): Transition = transition {
      case Evts.MouseEvent(_, _, MouseEvent.MOUSE_RELEASED, evt) => 
//        lastNodeHovered.getStyleClass.remove("debug-region")
        idle(Some(triggerEvent))

      case Evts.MouseEvent(_, _, MouseEvent.MOUSE_DRAGGED, evt) =>
        val (startPoint, endPoint) = (triggerEvent.getSceneY - evt.getSceneY) match {
          case 0 => if (triggerEvent.getSceneX < evt.getSceneX) (triggerEvent, evt) else (evt, triggerEvent)
          case d if d < 0 => (triggerEvent, evt)
          case other => (evt, triggerEvent)
        }
        
        textFlowAt(evt.getSceneX, evt.getSceneY) match {
          case Some(otherTextFlow) if otherTextFlow != lastNodeHovered =>
            updateTextFlow(lastNodeHovered, startPoint, endPoint)
            updateTextFlow(otherTextFlow, startPoint, endPoint)
//            lastNodeHovered.getStyleClass.remove("debug-region")
//            otherTextFlow.getStyleClass.add("debug-region")
            selecting(triggerEvent, otherTextFlow)
            
          case _ =>
            updateTextFlow(lastNodeHovered, startPoint, endPoint)
            current
        }
        
    }
    
    def updateTextFlow(textFlow: TextFlow, from: MouseEvent, to: MouseEvent): Unit = {
      val bounds = textFlow.getBoundsInLocal
      var fromInLocal = textFlow.sceneToLocal(from.getSceneX, from.getSceneY)
      //if from is negative, drag the start x to the begginning, so we select the entirety of the text
      if (fromInLocal.getY <= 0) fromInLocal = fromInLocal.subtract(fromInLocal.getX, 0)
      //same for end point
      var toInLocal = textFlow.sceneToLocal(to.getSceneX, to.getSceneY)
      if (toInLocal.getY >= bounds.getHeight) toInLocal = new Point2D(bounds.getWidth, toInLocal.getY)
      
      if ((fromInLocal.getY <= 0 && toInLocal.getY <= 0) || (fromInLocal.getY >= bounds.getHeight && toInLocal.getY >= bounds.getHeight)) {
        // if both from and to points do not fall within the vertical space, remove all selection
        getTextFlowHelper(textFlow).clearSelection()
      } else {
        
        from.getClickCount match {
          case 2 => //word selection mode, we need to detect if we are moving backwards (to < from) or forward (to > from)
            val startHitTest = textFlow hitTest fromInLocal
            val endHitTest = textFlow hitTest toInLocal
            //we need to detect the word at startHit and endHit and make sure they are inside the selection
            val helper = getTextFlowHelper(textFlow)
            
            val startOffset = {
              helper.layout.to(startHitTest.getCharIndex).last match {
                case (offset, Right(text)) => findPrev(text.getText, startHitTest.getCharIndex - offset, !_.isLetterOrDigit).map(1.+).getOrElse(0) + offset //increment by 1 the index found, because we don't want to include the empty space
                case (offset, Left(node)) => startHitTest.getCharIndex
              }
            }
            val endOffset = {
              helper.layout.to(endHitTest.getCharIndex).last match {
                case (offset, Right(text)) => findAfter(text.getText, endHitTest.getCharIndex - offset, !_.isLetterOrDigit).getOrElse(text.getText.length) + offset
                case (offset, Left(node)) => endHitTest.getCharIndex
              }
            }
            helper.select(startOffset, endOffset)
            
          case 3 => //line selection mode, we simply map the mouse x coordinate to the bounds of the flow
            fromInLocal = fromInLocal.subtract(fromInLocal.getX, 0)
            toInLocal = toInLocal.add(bounds.getWidth, 0)
            val startHitTest = textFlow hitTest fromInLocal
            val endHitTest = textFlow hitTest toInLocal
            getTextFlowHelper(textFlow).select(startHitTest.getCharIndex, endHitTest.getCharIndex)
            
          case _ => //normal selection mode
            val startHitTest = textFlow hitTest fromInLocal
            val endHitTest = textFlow hitTest toInLocal
            getTextFlowHelper(textFlow).select(startHitTest.getCharIndex, endHitTest.getCharIndex)
            
        }
        
      }
    }
  }
  
  private def findPrev(text: String, from: Int, predicate: Char => Boolean): Option[Int] = {
    var i = from + 1
    var res = -1
    while({i -= 1; res == -1 && i >= 0}) {
      if (predicate(text.charAt(i))) res = i
    }
    if (res == -1) None else Some(res)
  }
  private def findAfter(text: String, from: Int, predicate: Char => Boolean): Option[Int] = { 
    var i = from - 1
    var res = -1
    while({i += 1; res == -1 && i < text.length}) {
      if (predicate(text.charAt(i))) res = i
    }
    if (res == -1) None else Some(res)
  }
  
  private def getTextFlowHelper(tf: TextFlow) = tf.getProperties.asScala.getOrElseUpdate(TextSelectionSupport.this, new TextFlowHelper(tf)).asInstanceOf[TextFlowHelper]
  //helper class to track a TextFlow layout by listening to changes and computing indexes for its Texts
  private class TextFlowHelper(tf: TextFlow) {
    val selectionStart = new SimpleIntegerProperty(-1)
    val selectionEnd = new SimpleIntegerProperty(-1)
    
    private var textLayoutBinding: ObjectBinding[TreeMap[Int, Node Either Text]] = createBinding
    private def createBinding = {
      clearSelection() //clear selection if we have to recompute the binding
      val nodes = tf.getChildren.asScala.collect {
        case t: Text => Right(t)
        case n: Node => Left(n)
      }
      Binding(nodes.collect { case Right(t) => t}.map(_.textProperty):_*) { _ => 
        clearSelection() //texts inside change, so clear the entire selection.
        //ignore unmanaged nodes
        nodes.filter(_.merge.isManaged).foldLeft((0 -> TreeMap.empty[Int, Node Either Text])) {
          case ((idx, acc), n@Right(text)) => (idx + text.getText.length, acc.updated(idx, n))
          case ((idx, acc), n@Left(node)) => (idx + 1, acc.updated(idx, n))
        }._2
      }
    }
    //if the children change, simply recompute the binding. The previous binding will get GCd transparently, so no need to cleanup
    tf.getChildren.addListener({ evt => textLayoutBinding = createBinding }: ListChangeListener[Node])
    def layout = textLayoutBinding.get
    
    
    def select(from: Int, to: Int): Unit = {
      val shape = tf.rangeShape(from, to)
      getOrCreateSelectionShape(tf).getElements.setAll(shape:_*)
      selectionStart set from
      selectionEnd set to
    }
    def clearSelection(): Unit = {
      getOrCreateSelectionShape(tf).getElements.clear()
      selectionStart.set(-1)
      selectionEnd.set(-1)
    }
    
    private def getOrCreateSelectionShape(tf: TextFlow): Path = {
      tf.getChildren.asScala.headOption match {
        case Some(p: Path) => p
        case _ =>
          val p = new Path()
          p.setManaged(false)
          p.setFill(selectionBackground.get)
          p.setStroke(Color.TRANSPARENT)
          tf.getChildren.add(0, p)
          p
      }
    }
  }
}
