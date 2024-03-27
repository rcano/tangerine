package tangerine

import javafx.beans.binding._
import javafx.beans.value.ObservableValue

sealed trait BindingTypeSelector[R] {
  type BindingType <: Binding[?]
  def bind(obss: ObservableValue[?]*)(compute: BindingType => R): BindingType
}

object BindingTypeSelector extends LowPrioImplicits {
  implicit object IntBindingTypeSelector extends BindingTypeSelector[Int] {
    type BindingType = IntegerBinding
    def bind(obss: ObservableValue[?]*)(compute: BindingType => Int) = new IntegerBinding {
      this.bind(obss*)
      override def computeValue = compute(this)
    }
  }
  implicit object LongBindingTypeSelector extends BindingTypeSelector[Long] {
    type BindingType = LongBinding
    def bind(obss: ObservableValue[?]*)(compute: BindingType => Long) = new LongBinding {
      this.bind(obss*)
      override def computeValue = compute(this)
    }
  }
  implicit object FloatBindingTypeSelector extends BindingTypeSelector[Float] {
    type BindingType = FloatBinding
    def bind(obss: ObservableValue[?]*)(compute: BindingType => Float) = new FloatBinding {
      this.bind(obss*)
      override def computeValue = compute(this)
    }
  }
  implicit object DoubleBindingTypeSelector extends BindingTypeSelector[Double] {
    type BindingType = DoubleBinding
    def bind(obss: ObservableValue[?]*)(compute: BindingType => Double) = new DoubleBinding {
      this.bind(obss*)
      override def computeValue = compute(this)
    }
  }
}
sealed trait LowPrioImplicits {
  object AnyBindingTypeSelector extends BindingTypeSelector[Any] {
    type BindingType = ObjectBinding[?]
    def bind(obss: ObservableValue[?]*)(compute: BindingType => Any) = new ObjectBinding[Any] {
      this.bind(obss*)
      override def computeValue = compute(this)
    }
  }
  implicit def anyBindingTypeSelector[R]: BindingTypeSelector[R] { type BindingType = ObjectBinding[R] } = 
    AnyBindingTypeSelector.asInstanceOf[BindingTypeSelector[R] { type BindingType = ObjectBinding[R] }]
}
