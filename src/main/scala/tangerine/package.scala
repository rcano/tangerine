package tangerine

import javafx.beans.binding.{Binding, ObjectBinding}
import javafx.beans.value.ObservableValue
import javafx.scene.text.Font

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
  
  object & {
    def unapply[T](t: T) = Some((t, t))
  }
}
