package tangerine

import javafx.beans.value.ObservableValue
import javafx.scene.{Node, Parent}
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
    @inline def map[U](f: T => U)(implicit bindingSelector: BindingTypeSelector[U]): bindingSelector.BindingType = bindingSelector.bind(property, f)
    @inline def zip[T2](t2: ObservableValue[T2]) = Properties.Binding(property, t2)(_ => (property.getValue, t2.getValue))
  }
  
  implicit final class ChainingOps[T](private val self: T) extends AnyVal {
    @inline def pipe[R](f: T => R): R = f(self)
    @inline def tap(f: T => Any): T = {
      f(self)
      self
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
}
