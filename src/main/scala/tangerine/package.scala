package tangerine

import javafx.beans.value.ObservableValue
import javafx.scene.{Node, Parent}
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.collections.transformation.TransformationList
import javafx.geometry.Insets
import javafx.scene.control._
import javafx.scene.text.Font
import scala.collection.JavaConverters._

object `package` {

  implicit class EmUnits(private val unit: Double) extends AnyVal {
    @inline def em = Font.getDefault.getSize * unit
  }
  
  implicit class ObservableValueExt[T](private val property: ObservableValue[T]) extends AnyVal {
    import language.existentials
    @inline def foreach(f: T => Unit): Unit = property.addListener((_: t forSome {type t >: T}, _, v) => f(v))
    @inline def map[U](f: T => U)(implicit bindingSelector: BindingTypeSelector[U]): bindingSelector.BindingType = bindingSelector.bind(property)(_ => f(property.getValue))
    @inline def zip[T2](t2: ObservableValue[T2]) = Properties.Binding(property, t2)(_ => (property.getValue, t2.getValue))
  }
  
  implicit final class ChainingOps[T](private val self: T) extends AnyVal {
    @inline def pipe[R](f: T => R): R = f(self)
    @inline def tap(f: T => Any): T = {
      f(self)
      self
    }
  }
  
  implicit class ObservableListExt[T](private val list: ObservableList[T]) extends AnyVal {
    /**
     * Creates an observable list which is equivalent to the passed list transformed with the function `f` and kept in sync regarding changes.
     * Notice that the f function is applied lazily (this implies non eagerness and memoization)
     */
    def mapLazy[U](f: T => U): ObservableList[U] = new TransformationList[U, T](list) {
      private var cache = scala.collection.immutable.IntMap.empty[U]
      private val reverseIndex = scala.collection.mutable.HashMap.empty[T, Int]
      override def get(i: Int) = {
        cache.get(i) match {
          case Some(e) => e
          case _ => 
            val t = list.get(i)
            reverseIndex(t) = i
            f(t) tap (u => cache = cache.updated(i, u))
        }
      }
      override def size() = list.size
      override def getSourceIndex(i: Int) = i
      override def getViewIndex(i: Int) = i
      override protected def sourceChanged(evt: ListChangeListener.Change[_ <: T]) = {
        while (evt.next) {
          if (evt.wasPermutated || evt.wasUpdated) { for (i <- evt.getFrom until evt.getTo) cache -= i }
          if (evt.wasRemoved) evt.getRemoved.forEach { e => reverseIndex.remove(e) foreach (cache -= _) }
        }
      }
    }
    /**
     * Creates an observable list which is equivalent to the passed list transformed with the function `f` and kept in sync regarding changes.
     * Notice that the `f` function is applied every time an element is requested, because the transformation is not stored. For a behaviour
     * that does store see [[mapLazy]]
     */
    def mapNoCache[U](f: T => U): ObservableList[U] = new TransformationList[U, T](list) {
      override def get(i: Int) = list.get(i) pipe f
      override def size() = list.size
      override def getSourceIndex(i: Int) = i
      override def getViewIndex(i: Int) = i
      override protected def sourceChanged(evt: ListChangeListener.Change[_ <: T]) = {
        
      }
    }
  }
  
  object HasChildren {
    def unapplySeq(n: Node): Option[Seq[Node]] = n match {
      case s: ScrollPane => Some(Seq(s.getContent))
      case t: TabPane => Some(t.getTabs.asScala.map(_.getContent))
      case l: Labeled => Some(Seq(l.getGraphic))
      case a: Accordion => Some(a.getPanes.asScala)
      case t: ToolBar => Some(t.getItems.asScala)
      case b: ButtonBar => Some(b.getButtons.asScala)
      case p: Parent => Some(p.getChildrenUnmodifiable.asScala)
      case _ => None
    }
  }
  
  object & {
    def unapply[T](t: T) = Some((t, t))
  }
  
  def Margin(top: Double = 0, right: Double = 0, bot: Double = 0, left: Double = 0) = new Insets(top, right, bot, left)
  
  
  implicit class ColorExt(val c: javafx.scene.paint.Color) extends AnyVal {
    def colorToWeb = "#%02X%02X%02X".format((c.getRed * 255).toInt, (c.getGreen * 255).toInt, (c.getBlue * 255).toInt)
    def toArgb = (c.getOpacity * 255).toInt << 24 | (c.getRed * 255).toInt << 16 | (c.getGreen * 255).toInt << 8 | (c.getBlue * 255).toInt
  }
}
