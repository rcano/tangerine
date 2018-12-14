package tangerine

import javafx.beans.binding._
import javafx.beans.value.ObservableValue

sealed trait BindingTypeSelector[R] {
  type BindingType <: Binding[_]
  def bind[T](obss: ObservableValue[T]*)(compute: BindingType => R): BindingType
}

object BindingTypeSelector extends LowPrioImplicits {
  implicit object IntBindingTypeSelector extends BindingTypeSelector[Int] {
    type BindingType = IntegerBinding
    def bind[T](obs: ObservableValue[T]*)(compute: BindingType => Int) = new IntegerBinding {
      bind(obs:_*)
      override def computeValue = compute(this)
    }
  }
  implicit object LongBindingTypeSelector extends BindingTypeSelector[Long] {
    type BindingType = LongBinding
    def bind[T](obs: ObservableValue[T]*)(compute: BindingType => Long) = new LongBinding {
      bind(obs:_*)
      override def computeValue = compute(this)
    }
  }
  implicit object FloatBindingTypeSelector extends BindingTypeSelector[Float] {
    type BindingType = FloatBinding
    def bind[T](obs: ObservableValue[T]*)(compute: BindingType => Float) = new FloatBinding {
      bind(obs:_*)
      override def computeValue = compute(this)
    }
  }
  implicit object DoubleBindingTypeSelector extends BindingTypeSelector[Double] {
    type BindingType = DoubleBinding
    def bind[T](obs: ObservableValue[T]*)(compute: BindingType => Double) = new DoubleBinding {
      bind(obs:_*)
      override def computeValue = compute(this)
    }
  }
}
sealed trait LowPrioImplicits {
  object AnyBindingTypeSelector extends BindingTypeSelector[Any] {
    type BindingType = ObjectBinding[_]
    def bind[T](obs: ObservableValue[T]*)(compute: BindingType => Any) = new ObjectBinding[Any] {
      bind(obs:_*)
      override def computeValue = compute(this)
    }
  }
  implicit def anyBindingTypeSelector[R]: BindingTypeSelector[R] { type BindingType = ObjectBinding[R] } = 
    AnyBindingTypeSelector.asInstanceOf[BindingTypeSelector[R] { type BindingType = ObjectBinding[R] }]
}
