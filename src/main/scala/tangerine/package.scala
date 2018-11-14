package tangerine

import javafx.beans.binding.{Binding, ObjectBinding}
import javafx.geometry.VPos
import javafx.beans.value.ObservableValue
import javafx.scene.layout._
import javafx.scene.text.Font

object `package` {

  implicit class EmUnits(private val unit: Double) extends AnyVal {
    @inline def em = Font.getDefault.getSize * unit
  }
  
  implicit class ObservableValueExt[T](private val property: ObservableValue[T]) extends AnyVal {
    import language.existentials
    def foreach(f: T => Unit): Unit = property.addListener((_: t forSome {type t >: T}, _, v) => f(v))
    def map[U](f: T => U): Binding[U] = new ObjectBinding[U] {
      bind(property)
      override def computeValue = f(property.getValue)
    }
  }
  
  implicit final class ChainingOps[T](private val self: T) extends AnyVal {
    @inline def pipe[R](f: T => R): R = f(self)
    @inline def tap(f: T => Any): T = {
      f(self)
      self
    }
  }
}
