package tangerine

import javafx.application.Platform
import javafx.beans.binding.ObjectBinding
import javafx.beans.property.{ReadOnlyObjectPropertyBase, SimpleIntegerProperty, SimpleObjectProperty}
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.event.Event
import javafx.event.EventHandler
import javafx.geometry.Bounds
import javafx.geometry.Point2D
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.input.{MouseButton, MouseEvent}
import javafx.scene.layout.Region
import javafx.scene.paint.{Color, Paint}
import javafx.scene.shape.Path
import javafx.scene.text.{Text, TextFlow}
import scala.collection.immutable.TreeMap
import scala.jdk.CollectionConverters._
import scala.util.chaining._
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
  
  private object textflowLocations {
    val direct = collection.mutable.SortedMap.empty[Point2D, TextFlow]((a, b) => a.getY - b.getY match {
        case 0 => (a.getX - b.getX).toInt
        case o => o.toInt
      })
    val inverse = collection.mutable.Map.empty[TextFlow, Point2D]

    def update(tf: TextFlow, location: Point2D): Unit = {
      inverse.remove(tf) foreach direct.-
      direct(location) = tf
      inverse(tf) = location
    }
  }
  
  
  private var lastRegistration: tangerine.JfxUtils.StructureListenerRegistration = null
  private var lastSmSubscription: EventHandler[MouseEvent] = null
  rootNode.addListener { (_, oldv, newv) => 
    if (oldv != null) {
      textFlowsSeq.clear()
      lastRegistration.cancel()
      oldv.removeEventHandler(MouseEvent.ANY, lastSmSubscription)
    }

    registerTextFlowsIn(newv)
  
    lastRegistration = tangerine.JfxUtils.registerGraphStructureListener(newv) { evt =>
      evt.getAddedSubList.iterator.asScala foreach registerTextFlowsIn
      evt.getRemoved.iterator.asScala foreach unregisterTextFlowsIn
    }
    
    lastSmSubscription = sm.subscribe(newv, MouseEvent.ANY)
  }
  
  val selection = new ReadOnlyObjectPropertyBase[collection.Seq[Node Either String]] { self =>
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
      case tf: TextFlow => 
        installTextFlowHelper(tf)
        layoutTracker track tf
        textFlowsSeq add tf
      case other =>
    }
  }
  private def unregisterTextFlowsIn(rootNode: Node) = 
    tangerine.JfxUtils.traverseBreadthFirst(rootNode).foreach { 
      case tf: TextFlow => 
        layoutTracker remove tf
        textFlowsSeq remove tf
      case other =>
    }

  private def textFlowAt(sceneX: Double, sceneY: Double): Option[TextFlow] = {
    val pointInRoot = rootNode.get.sceneToLocal(sceneX, sceneY)
    textflowLocations.direct.maxBefore(pointInRoot).map(_._2).filter(n => n.getBoundsInLocal.contains(n.sceneToLocal(sceneX, sceneY)))
  }

  private def textFlowsBetween(sceneP1: Point2D, sceneP2: Point2D): Iterable[TextFlow] = {
    if (textflowLocations.direct.isEmpty) Seq.empty
    else {
      val rootP1 = rootNode.get.sceneToLocal(sceneP1)
      val rootP2 = rootNode.get.sceneToLocal(sceneP2)
      val min = textflowLocations.direct.maxBefore(rootP1).get._1
      textflowLocations.direct.range(min, rootP2).values
    }
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
            val sourceEvent = lastTriggerEvent.filter(_ => evt.isShiftDown).getOrElse(evt)
            evt.getClickCount match {
              case 2 | 3 => updateTextFlow(tf, new Point2D(sourceEvent.getSceneX, sourceEvent.getSceneY),
                                           new Point2D(evt.getSceneX, evt.getSceneY), sourceEvent.getClickCount)
              case _ =>
            }
            
            selecting(sourceEvent, Seq.empty)
        }
    }
    
    def selecting(triggerEvent: MouseEvent, previousTargets: Iterable[TextFlow]): Transition = transition {
      case Evts.MouseEvent(_, _, MouseEvent.MOUSE_RELEASED, evt) => 
        idle(Some(triggerEvent))

      case Evts.MouseEvent(_, _, MouseEvent.MOUSE_DRAGGED, evt) =>
        //startPoint needs to be adapted to originalTextFlow, since after scrolling it might be off screen, nevertheless
        //the click corresponds to that event (localX and localY)
        val triggerPoint = triggerEvent.getSource.asInstanceOf[Node].localToScene(triggerEvent.getX, triggerEvent.getY)
        val currentPoint = new Point2D(evt.getSceneX, evt.getSceneY)
        val (startPoint, endPoint) = (triggerPoint.getY - currentPoint.getY) match {
          case 0 => if (triggerPoint.getX < currentPoint.getX) (triggerPoint, currentPoint) else (currentPoint, triggerPoint)
          case d if d < 0 => (triggerPoint, currentPoint)
          case other => (currentPoint, triggerPoint)
        }

        val alreadyProcessed = collection.mutable.HashSet.empty[TextFlow]

        val tf = textFlowAt(evt.getSceneX, evt.getSceneY)

        tf foreach (tf => updateTextFlow(tf, startPoint, endPoint, triggerEvent.getClickCount))

        val toProcess = textFlowsBetween(startPoint, endPoint)
        textFlowsBetween(startPoint, endPoint) foreach { tf => 
          alreadyProcessed += tf
          updateTextFlow(tf, startPoint, endPoint, triggerEvent.getClickCount)
        }

        for (pv <- previousTargets if !alreadyProcessed(pv)) updateTextFlow(pv, startPoint, endPoint, triggerEvent.getClickCount)
        
        selecting(triggerEvent, toProcess)
        
    }
    
    def updateTextFlow(textFlow: TextFlow, from: Point2D, to: Point2D, clickCount: Int): Unit = {
      val bounds = textFlow.getBoundsInLocal
      var fromInLocal = textFlow.sceneToLocal(from.getX, from.getY)
      //if from is negative, drag the start x to the begginning, so we select the entirety of the text
      if (fromInLocal.getY <= 0) fromInLocal = fromInLocal.subtract(fromInLocal.getX, 0)
      //same for end point
      var toInLocal = textFlow.sceneToLocal(to.getX, to.getY)
      if (toInLocal.getY >= bounds.getHeight) toInLocal = new Point2D(bounds.getWidth, toInLocal.getY)
      
      if ((fromInLocal.getY <= 0 && toInLocal.getY <= 0) || (fromInLocal.getY >= bounds.getHeight && toInLocal.getY >= bounds.getHeight)) {
        // if both from and to points do not fall within the vertical space, remove all selection
        getTextFlowHelper(textFlow).clearSelection()
      } else {
        
        clickCount match {
          case 2 => //word selection mode, we need to detect if we are moving backwards (to < from) or forward (to > from)
            val startHitTest = textFlow hitTest fromInLocal
            val endHitTest = textFlow hitTest toInLocal
            //we need to detect the word at startHit and endHit and make sure they are inside the selection
            val helper = getTextFlowHelper(textFlow)
            
            val startOffset = {
              helper.layout.rangeTo(startHitTest.getCharIndex).last match {
                case (offset, Right(text)) => findPrev(text.getText, startHitTest.getCharIndex - offset, !_.isLetterOrDigit).map(1.+).getOrElse(0) + offset //increment by 1 the index found, because we don't want to include the empty space
                case (offset, Left(node)) => startHitTest.getCharIndex
              }
            }
            val endOffset = {
              helper.layout.rangeTo(endHitTest.getCharIndex).last match {
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
  
  
  
  
  
  private def installTextFlowHelper(tf: TextFlow) = tf.getProperties.put(TextSelectionSupport.this, new TextFlowHelper(tf)).asInstanceOf[TextFlowHelper]
  private def getTextFlowHelper(tf: TextFlow) = tf.getProperties.get(TextSelectionSupport.this).asInstanceOf[TextFlowHelper]
  //helper class to track a TextFlow layout by listening to changes and computing indexes for its Texts
  private class TextFlowHelper(tf: TextFlow) {
    val selectionStart = new SimpleIntegerProperty(-1)
    val selectionEnd = new SimpleIntegerProperty(-1)
    
    private var textLayoutBinding: ObjectBinding[TreeMap[Int, Node Either Text]] = createBinding
    private def createBinding = {
      clearSelection() //clear selection if we have to recompute the binding
      val nodes = tf.getChildren.asScala.toSeq.collect {
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
  
  
  
  
  private object layoutTracker {
    val trackedParents = collection.mutable.HashSet.empty[Parent]

    def track(tf: TextFlow): Unit = {
      val rn = rootNode.get
      Iterator.iterate(tf.getParent)(_.getParent).takeWhile(p => p != null && p != rn && !trackedParents(p)).
        foreach { p =>
          trackedParents += p
          p.boundsInParentProperty.addListener(boundsListener(p))
        }
    }
    def remove(tf: TextFlow): Unit = {
      val rn = rootNode.get
      Iterator.iterate(tf.getParent)(_.getParent).takeWhile(p =>  p != null &&  rn != p).
        foreach { p =>
          trackedParents -= p
          p.boundsInParentProperty.removeListener(boundsListener(p))
        }
    }

    var toUpdate = collection.mutable.HashSet.empty[Parent]
    def boundsListener(node: Parent): ChangeListener[Bounds] = new ChangeListener[Bounds]() {
      def changed(obs: ObservableValue[_ <: Bounds], oldv: Bounds, newv: Bounds) = {
        val rn = rootNode.get

        Iterator.iterate(node)(_.getParent).takeWhile(p => p != null && p != rn).find(p => !toUpdate(p)) match {
          case Some(changed) =>
            //gotta remove parents that are a descendant of contained we just detected.
            toUpdate.flatMap(p => Iterator.iterate(p)(_.getParent).takeWhile(p => p != rn && p != null).find(p => p == changed).map(_ => p)).
              foreach(toUpdate.-=)

            toUpdate += changed
//            println(s"${Console.GREEN} detected $changed ${Console.RESET}")

            //we schedule recalculation of layouts to happen after this javafx pulse so that we ensure all layout processing has finished
            if (toUpdate.sizeIs == 1) Platform.runLater(recalculateLayouts _)

          case _ =>
        }
      }
      override def hashCode = layoutTracker.hashCode
      override def equals(that: Any) = this.getClass == that.getClass
    }

    def recalculateLayouts(): Unit = {
      for { 
        node <- toUpdate
        tf <- JfxUtils.traverseDepthFirst(node).collect { case tf: TextFlow => tf }
      } {
        val location = tf.localToScene(0, 0).pipe(rootNode.get.sceneToLocal)
        textflowLocations(tf) = location
        getTextFlowHelper(tf)
      }
      toUpdate.clear()
    }
  }
}
