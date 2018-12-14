package tangerine

import javafx.beans.value._
import javafx.beans.binding._

/**
 * Functionality around Javafx Beans, Bindings and Expressions
 */
object Properties {

  implicit class BooleanExpressionExt(private val e: BooleanExpression) extends AnyVal {
    def ===(v: ObservableBooleanValue) = e.isEqualTo(v)
    def =!=(v: ObservableBooleanValue) = e.isNotEqualTo(v)
    def &&(v: ObservableBooleanValue) = e.and(v)
    def ||(v: ObservableBooleanValue) = e.or(v)
    def unary_!() = e.not()
  }
  
  /*
   the following script generates the *ExpressionExt variants
  
   val BindingTypes = Seq("Int", "Long", "Float", "Double")
   for (t <- BindingTypes) {
   println(s"implicit class ${t}ExpressionExt(private val e: ${t}Expression) extends AnyVal {")
   val NumericOperations = BindingTypes ++ Seq("ObservableNumberValue")
   for (t <- NumericOperations) {
   println(s"  @inline def +(v: $t) = e.add(v)")
   println(s"  @inline def -(v: $t) = e.subtract(v)")
   println(s"  @inline def *(v: $t) = e.multiply(v)")
   println(s"  @inline def /(v: $t) = e.divide(v)")
   t match {
   case "Float" | "Double" =>
   println(s"  @inline def =+-=(v: $t, epsilon: $t) = e.isEqualTo(v, epsilon)")
   println(s"  @inline def =!+-=(v: $t, epsilon: $t) = e.isNotEqualTo(v, epsilon)")
   case _ => 
   println(s"  @inline def ===(v: $t) = e.isEqualTo(v)")
   println(s"  @inline def =!=(v: $t) = e.isNotEqualTo(v)")
   }
   println(s"  @inline def <(v: $t) = e.lessThan(v)")
   println(s"  @inline def >(v: $t) = e.greaterThan(v)")
   println(s"  @inline def <=(v: $t) = e.greaterThanOrEqualTo(v)")
   println(s"  @inline def >=(v: $t) = e.lessThanOrEqualTo(v)")
   println()
   }
   println(s"  @inline def unary_-(v: $t) = e.negate()")
   println(s"}")
   }
  
   */
  
  implicit class IntExpressionExt(private val e: IntegerExpression) extends AnyVal {
    @inline def +(v: Int) = e.add(v)
    @inline def -(v: Int) = e.subtract(v)
    @inline def *(v: Int) = e.multiply(v)
    @inline def /(v: Int) = e.divide(v)
    @inline def ===(v: Int) = e.isEqualTo(v)
    @inline def =!=(v: Int) = e.isNotEqualTo(v)
    @inline def <(v: Int) = e.lessThan(v)
    @inline def >(v: Int) = e.greaterThan(v)
    @inline def <=(v: Int) = e.greaterThanOrEqualTo(v)
    @inline def >=(v: Int) = e.lessThanOrEqualTo(v)

    @inline def +(v: Long) = e.add(v)
    @inline def -(v: Long) = e.subtract(v)
    @inline def *(v: Long) = e.multiply(v)
    @inline def /(v: Long) = e.divide(v)
    @inline def ===(v: Long) = e.isEqualTo(v)
    @inline def =!=(v: Long) = e.isNotEqualTo(v)
    @inline def <(v: Long) = e.lessThan(v)
    @inline def >(v: Long) = e.greaterThan(v)
    @inline def <=(v: Long) = e.greaterThanOrEqualTo(v)
    @inline def >=(v: Long) = e.lessThanOrEqualTo(v)

    @inline def +(v: Float) = e.add(v)
    @inline def -(v: Float) = e.subtract(v)
    @inline def *(v: Float) = e.multiply(v)
    @inline def /(v: Float) = e.divide(v)
    @inline def =+-=(v: Float, epsilon: Float) = e.isEqualTo(v, epsilon)
    @inline def =!+-=(v: Float, epsilon: Float) = e.isNotEqualTo(v, epsilon)
    @inline def <(v: Float) = e.lessThan(v)
    @inline def >(v: Float) = e.greaterThan(v)
    @inline def <=(v: Float) = e.greaterThanOrEqualTo(v)
    @inline def >=(v: Float) = e.lessThanOrEqualTo(v)

    @inline def +(v: Double) = e.add(v)
    @inline def -(v: Double) = e.subtract(v)
    @inline def *(v: Double) = e.multiply(v)
    @inline def /(v: Double) = e.divide(v)
    @inline def =+-=(v: Double, epsilon: Double) = e.isEqualTo(v, epsilon)
    @inline def =!+-=(v: Double, epsilon: Double) = e.isNotEqualTo(v, epsilon)
    @inline def <(v: Double) = e.lessThan(v)
    @inline def >(v: Double) = e.greaterThan(v)
    @inline def <=(v: Double) = e.greaterThanOrEqualTo(v)
    @inline def >=(v: Double) = e.lessThanOrEqualTo(v)

    @inline def +(v: ObservableNumberValue) = e.add(v)
    @inline def -(v: ObservableNumberValue) = e.subtract(v)
    @inline def *(v: ObservableNumberValue) = e.multiply(v)
    @inline def /(v: ObservableNumberValue) = e.divide(v)
    @inline def ===(v: ObservableNumberValue) = e.isEqualTo(v)
    @inline def =!=(v: ObservableNumberValue) = e.isNotEqualTo(v)
    @inline def <(v: ObservableNumberValue) = e.lessThan(v)
    @inline def >(v: ObservableNumberValue) = e.greaterThan(v)
    @inline def <=(v: ObservableNumberValue) = e.greaterThanOrEqualTo(v)
    @inline def >=(v: ObservableNumberValue) = e.lessThanOrEqualTo(v)

    @inline def unary_-(v: Int) = e.negate()
  }
  implicit class LongExpressionExt(private val e: LongExpression) extends AnyVal {
    @inline def +(v: Int) = e.add(v)
    @inline def -(v: Int) = e.subtract(v)
    @inline def *(v: Int) = e.multiply(v)
    @inline def /(v: Int) = e.divide(v)
    @inline def ===(v: Int) = e.isEqualTo(v)
    @inline def =!=(v: Int) = e.isNotEqualTo(v)
    @inline def <(v: Int) = e.lessThan(v)
    @inline def >(v: Int) = e.greaterThan(v)
    @inline def <=(v: Int) = e.greaterThanOrEqualTo(v)
    @inline def >=(v: Int) = e.lessThanOrEqualTo(v)

    @inline def +(v: Long) = e.add(v)
    @inline def -(v: Long) = e.subtract(v)
    @inline def *(v: Long) = e.multiply(v)
    @inline def /(v: Long) = e.divide(v)
    @inline def ===(v: Long) = e.isEqualTo(v)
    @inline def =!=(v: Long) = e.isNotEqualTo(v)
    @inline def <(v: Long) = e.lessThan(v)
    @inline def >(v: Long) = e.greaterThan(v)
    @inline def <=(v: Long) = e.greaterThanOrEqualTo(v)
    @inline def >=(v: Long) = e.lessThanOrEqualTo(v)

    @inline def +(v: Float) = e.add(v)
    @inline def -(v: Float) = e.subtract(v)
    @inline def *(v: Float) = e.multiply(v)
    @inline def /(v: Float) = e.divide(v)
    @inline def =+-=(v: Float, epsilon: Float) = e.isEqualTo(v, epsilon)
    @inline def =!+-=(v: Float, epsilon: Float) = e.isNotEqualTo(v, epsilon)
    @inline def <(v: Float) = e.lessThan(v)
    @inline def >(v: Float) = e.greaterThan(v)
    @inline def <=(v: Float) = e.greaterThanOrEqualTo(v)
    @inline def >=(v: Float) = e.lessThanOrEqualTo(v)

    @inline def +(v: Double) = e.add(v)
    @inline def -(v: Double) = e.subtract(v)
    @inline def *(v: Double) = e.multiply(v)
    @inline def /(v: Double) = e.divide(v)
    @inline def =+-=(v: Double, epsilon: Double) = e.isEqualTo(v, epsilon)
    @inline def =!+-=(v: Double, epsilon: Double) = e.isNotEqualTo(v, epsilon)
    @inline def <(v: Double) = e.lessThan(v)
    @inline def >(v: Double) = e.greaterThan(v)
    @inline def <=(v: Double) = e.greaterThanOrEqualTo(v)
    @inline def >=(v: Double) = e.lessThanOrEqualTo(v)

    @inline def +(v: ObservableNumberValue) = e.add(v)
    @inline def -(v: ObservableNumberValue) = e.subtract(v)
    @inline def *(v: ObservableNumberValue) = e.multiply(v)
    @inline def /(v: ObservableNumberValue) = e.divide(v)
    @inline def ===(v: ObservableNumberValue) = e.isEqualTo(v)
    @inline def =!=(v: ObservableNumberValue) = e.isNotEqualTo(v)
    @inline def <(v: ObservableNumberValue) = e.lessThan(v)
    @inline def >(v: ObservableNumberValue) = e.greaterThan(v)
    @inline def <=(v: ObservableNumberValue) = e.greaterThanOrEqualTo(v)
    @inline def >=(v: ObservableNumberValue) = e.lessThanOrEqualTo(v)

    @inline def unary_-(v: Long) = e.negate()
  }
  implicit class FloatExpressionExt(private val e: FloatExpression) extends AnyVal {
    @inline def +(v: Int) = e.add(v)
    @inline def -(v: Int) = e.subtract(v)
    @inline def *(v: Int) = e.multiply(v)
    @inline def /(v: Int) = e.divide(v)
    @inline def ===(v: Int) = e.isEqualTo(v)
    @inline def =!=(v: Int) = e.isNotEqualTo(v)
    @inline def <(v: Int) = e.lessThan(v)
    @inline def >(v: Int) = e.greaterThan(v)
    @inline def <=(v: Int) = e.greaterThanOrEqualTo(v)
    @inline def >=(v: Int) = e.lessThanOrEqualTo(v)

    @inline def +(v: Long) = e.add(v)
    @inline def -(v: Long) = e.subtract(v)
    @inline def *(v: Long) = e.multiply(v)
    @inline def /(v: Long) = e.divide(v)
    @inline def ===(v: Long) = e.isEqualTo(v)
    @inline def =!=(v: Long) = e.isNotEqualTo(v)
    @inline def <(v: Long) = e.lessThan(v)
    @inline def >(v: Long) = e.greaterThan(v)
    @inline def <=(v: Long) = e.greaterThanOrEqualTo(v)
    @inline def >=(v: Long) = e.lessThanOrEqualTo(v)

    @inline def +(v: Float) = e.add(v)
    @inline def -(v: Float) = e.subtract(v)
    @inline def *(v: Float) = e.multiply(v)
    @inline def /(v: Float) = e.divide(v)
    @inline def =+-=(v: Float, epsilon: Float) = e.isEqualTo(v, epsilon)
    @inline def =!+-=(v: Float, epsilon: Float) = e.isNotEqualTo(v, epsilon)
    @inline def <(v: Float) = e.lessThan(v)
    @inline def >(v: Float) = e.greaterThan(v)
    @inline def <=(v: Float) = e.greaterThanOrEqualTo(v)
    @inline def >=(v: Float) = e.lessThanOrEqualTo(v)

    @inline def +(v: Double) = e.add(v)
    @inline def -(v: Double) = e.subtract(v)
    @inline def *(v: Double) = e.multiply(v)
    @inline def /(v: Double) = e.divide(v)
    @inline def =+-=(v: Double, epsilon: Double) = e.isEqualTo(v, epsilon)
    @inline def =!+-=(v: Double, epsilon: Double) = e.isNotEqualTo(v, epsilon)
    @inline def <(v: Double) = e.lessThan(v)
    @inline def >(v: Double) = e.greaterThan(v)
    @inline def <=(v: Double) = e.greaterThanOrEqualTo(v)
    @inline def >=(v: Double) = e.lessThanOrEqualTo(v)

    @inline def +(v: ObservableNumberValue) = e.add(v)
    @inline def -(v: ObservableNumberValue) = e.subtract(v)
    @inline def *(v: ObservableNumberValue) = e.multiply(v)
    @inline def /(v: ObservableNumberValue) = e.divide(v)
    @inline def ===(v: ObservableNumberValue) = e.isEqualTo(v)
    @inline def =!=(v: ObservableNumberValue) = e.isNotEqualTo(v)
    @inline def <(v: ObservableNumberValue) = e.lessThan(v)
    @inline def >(v: ObservableNumberValue) = e.greaterThan(v)
    @inline def <=(v: ObservableNumberValue) = e.greaterThanOrEqualTo(v)
    @inline def >=(v: ObservableNumberValue) = e.lessThanOrEqualTo(v)

    @inline def unary_-(v: Float) = e.negate()
  }
  
  implicit class DoubleExpressionExt(private val e: DoubleExpression) extends AnyVal {
    @inline def +(v: Int) = e.add(v)
    @inline def -(v: Int) = e.subtract(v)
    @inline def *(v: Int) = e.multiply(v)
    @inline def /(v: Int) = e.divide(v)
    @inline def ===(v: Int) = e.isEqualTo(v)
    @inline def =!=(v: Int) = e.isNotEqualTo(v)
    @inline def <(v: Int) = e.lessThan(v)
    @inline def >(v: Int) = e.greaterThan(v)
    @inline def <=(v: Int) = e.greaterThanOrEqualTo(v)
    @inline def >=(v: Int) = e.lessThanOrEqualTo(v)

    @inline def +(v: Long) = e.add(v)
    @inline def -(v: Long) = e.subtract(v)
    @inline def *(v: Long) = e.multiply(v)
    @inline def /(v: Long) = e.divide(v)
    @inline def ===(v: Long) = e.isEqualTo(v)
    @inline def =!=(v: Long) = e.isNotEqualTo(v)
    @inline def <(v: Long) = e.lessThan(v)
    @inline def >(v: Long) = e.greaterThan(v)
    @inline def <=(v: Long) = e.greaterThanOrEqualTo(v)
    @inline def >=(v: Long) = e.lessThanOrEqualTo(v)

    @inline def +(v: Float) = e.add(v)
    @inline def -(v: Float) = e.subtract(v)
    @inline def *(v: Float) = e.multiply(v)
    @inline def /(v: Float) = e.divide(v)
    @inline def =+-=(v: Float, epsilon: Float) = e.isEqualTo(v, epsilon)
    @inline def =!+-=(v: Float, epsilon: Float) = e.isNotEqualTo(v, epsilon)
    @inline def <(v: Float) = e.lessThan(v)
    @inline def >(v: Float) = e.greaterThan(v)
    @inline def <=(v: Float) = e.greaterThanOrEqualTo(v)
    @inline def >=(v: Float) = e.lessThanOrEqualTo(v)

    @inline def +(v: Double) = e.add(v)
    @inline def -(v: Double) = e.subtract(v)
    @inline def *(v: Double) = e.multiply(v)
    @inline def /(v: Double) = e.divide(v)
    @inline def =+-=(v: Double, epsilon: Double) = e.isEqualTo(v, epsilon)
    @inline def =!+-=(v: Double, epsilon: Double) = e.isNotEqualTo(v, epsilon)
    @inline def <(v: Double) = e.lessThan(v)
    @inline def >(v: Double) = e.greaterThan(v)
    @inline def <=(v: Double) = e.greaterThanOrEqualTo(v)
    @inline def >=(v: Double) = e.lessThanOrEqualTo(v)

    @inline def +(v: ObservableNumberValue) = e.add(v)
    @inline def -(v: ObservableNumberValue) = e.subtract(v)
    @inline def *(v: ObservableNumberValue) = e.multiply(v)
    @inline def /(v: ObservableNumberValue) = e.divide(v)
    @inline def ===(v: ObservableNumberValue) = e.isEqualTo(v)
    @inline def =!=(v: ObservableNumberValue) = e.isNotEqualTo(v)
    @inline def <(v: ObservableNumberValue) = e.lessThan(v)
    @inline def >(v: ObservableNumberValue) = e.greaterThan(v)
    @inline def <=(v: ObservableNumberValue) = e.greaterThanOrEqualTo(v)
    @inline def >=(v: ObservableNumberValue) = e.lessThanOrEqualTo(v)

    @inline def unary_-(v: Double) = e.negate()
  }
  
  
  implicit class StringExpressionExt(private val e: StringExpression) extends AnyVal {
    def ===(v: Null) = e.isNull
    def ===(v: String) = e.isEqualTo(v)
    def ===(v: ObservableStringValue) = e.isEqualTo(v)

    def =!=(v: Null) = e.isNotNull
    def =!=(v: String) = e.isNotEqualTo(v)
    def =!=(v: ObservableStringValue) = e.isNotEqualTo(v)

    def ==~(v: Null) = e.isNull
    def ==~(v: String) = e.isEqualToIgnoreCase(v)
    def ==~(v: ObservableStringValue) = e.isEqualToIgnoreCase(v)

    def !=~(v: Null) = e.isNotNull
    def !=~(v: String) = e.isNotEqualToIgnoreCase(v)
    def !=~(v: ObservableStringValue) = e.isNotEqualToIgnoreCase(v)

    def <(v: Null) = e.lessThan(v.asInstanceOf[String])
    def <(v: String) = e.lessThan(v)
    def <(v: ObservableStringValue) = e.lessThan(v)

    def <=(v: Null) = e.lessThanOrEqualTo(v.asInstanceOf[String])
    def <=(v: String) = e.lessThanOrEqualTo(v)
    def <=(v: ObservableStringValue) = e.lessThanOrEqualTo(v)

    def >(v: Null) = e.greaterThan(v.asInstanceOf[String])
    def >(v: String) = e.greaterThan(v)
    def >(v: ObservableStringValue) = e.greaterThan(v)

    def >=(v: Null) = e.greaterThanOrEqualTo(v.asInstanceOf[String])
    def >=(v: String) = e.greaterThanOrEqualTo(v)
    def >=(v: ObservableStringValue) = e.greaterThanOrEqualTo(v)

    // Kind of an odd case that concat is not observable, but this is how it is coded in JavaFX
    def + = concat _

    def concat(v: Any) = e.concat(v)
  }
  
  def Binding[T](values: ObservableValue[_]*)(compute: Binding[_] => T)(implicit bindingSelector: BindingTypeSelector[T]): bindingSelector.BindingType = 
    bindingSelector.bind(values:_*)(compute)
}
