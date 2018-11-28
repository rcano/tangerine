package tangerine

import javafx.beans.binding._
import javafx.beans.value.ObservableValue

sealed trait BindingTypeSelector[R] {
  type BindingType <: Binding[_]
  def bind[T](obs: ObservableValue[T], f: T => R): BindingType
}

object BindingTypeSelector extends LowPrioImplicits {
  implicit object IntBindingTypeSelector extends BindingTypeSelector[Int] {
    type BindingType = IntegerBinding
    def bind[T](obs: ObservableValue[T], f: T => Int) = new IntegerBinding {
      bind(obs)
      override def computeValue = f(obs.getValue)
    }
  }
  implicit object LongBindingTypeSelector extends BindingTypeSelector[Long] {
    type BindingType = LongBinding
    def bind[T](obs: ObservableValue[T], f: T => Long) = new LongBinding {
      bind(obs)
      override def computeValue = f(obs.getValue)
    }
  }
  implicit object FloatBindingTypeSelector extends BindingTypeSelector[Float] {
    type BindingType = FloatBinding
    def bind[T](obs: ObservableValue[T], f: T => Float) = new FloatBinding {
      bind(obs)
      override def computeValue = f(obs.getValue)
    }
  }
  implicit object DoubleBindingTypeSelector extends BindingTypeSelector[Double] {
    type BindingType = DoubleBinding
    def bind[T](obs: ObservableValue[T], f: T => Double) = new DoubleBinding {
      bind(obs)
      override def computeValue = f(obs.getValue)
    }
  }
}
sealed trait LowPrioImplicits {
  object AnyBindingTypeSelector extends BindingTypeSelector[Any] {
    type BindingType = ObjectBinding[_]
    def bind[T](obs: ObservableValue[T], f: T => Any) = new ObjectBinding[Any] {
      bind(obs)
      override def computeValue = f(obs.getValue)
    }
  }
  implicit def anyBindingTypeSelector[R]: BindingTypeSelector[R] = AnyBindingTypeSelector.asInstanceOf[BindingTypeSelector[R]]
}
