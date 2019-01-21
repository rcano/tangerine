package tangerine

import language.reflectiveCalls

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.{ObservableValue, ChangeListener}
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.event.{Event, EventHandler, EventType}

/**
 * This class provides support for defining a state machine.
 * StateMachine is perfectly composable, either via composing transitions like
 * 
 * ```scala
 * def a = transition {...} orElse b
 * def b = transition {}
 * ```
 * or by composing entire state machines as in `val sm1 = new StateMachine(); val sm2 = new StateMachine(); val total = sm1 orElse sm2`
 * and the result is a state identical to having two state machines running on top of the same stream of events.
 * Note that the order of `orElse`'d state machines determines which gets to act first, and only one transition will receive the event.
 * 
 * This class is ideal for modelling complex UI behaviour by using the method [[StateMachine#subscribe(node)]], which will subscribe
 * this StateMachine to the events defined by [[T]] in the node.
 */
trait StateMachine[T] extends PartialFunction[T, Unit] {

  case class Transition(name: sourcecode.Name, line: sourcecode.Line)(f: PartialFunction[T, Transition]) extends PartialFunction[T, Transition] {
    def apply(a: T) = f(a)
    def isDefinedAt(a: T) = f.isDefinedAt(a)
    def orElse(t: Transition)(implicit line: sourcecode.Line) = Transition(sourcecode.Name(name.value + "+" + t.name.value), line)(f orElse t)

    override def toString = name.value + ":" + line.value
  }
  def transition(f: PartialFunction[T, Transition])(implicit name: sourcecode.Name, line: sourcecode.Line) = {
    Transition(name, line)(f)
  }
  def namedTransition(name: String)(f: PartialFunction[T, Transition])(implicit line: sourcecode.Line) = 
    Transition(sourcecode.Name(name), line)(f)

  private[this] val curr = new SimpleObjectProperty[Transition](this, "current", initState)
  def initState: Transition
  def apply(a: T): Unit = {
    val prev = curr
    try {
      val newCurr = curr.get.apply(a)
      if (curr == prev) curr set newCurr //only update state if it was not updated by reentrantcy of this method
    } catch {
      case e: MatchError => throw new MatchError(e.getMessage + " not handled by state " + curr.get.name)
    }
  }
  def applyIfDefined(a: T) = if (isDefinedAt(a)) apply(a)
  def isDefinedAt(a: T) = curr.get.isDefinedAt(a)

  /**
   * @return the current transition
   */
  def current = curr.get
  /**
   * Imperative change of state. Discouraged but unavoidable sometimes.
   */
  def switchTo(t: Transition) = curr set t

  /**
   * Done state, which is defined for no payload
   */
  lazy val done = namedTransition("done")(PartialFunction.empty)
  
  def subscribe[E <: Event](node: { def addEventHandler[E2 <: Event](evtType: EventType[E2], handler: EventHandler[_ >: E2]) }, eventType: EventType[E])(implicit eventEv: E <:< T): EventHandler[E] = {
    val listener: EventHandler[E] = evt => applyIfDefined(evt)
    node.addEventHandler[E](eventType, listener)
    listener
  }
  def subscribe[E](observable: ObservableValue[E])(implicit ev: (ObservableValue[_ <: E], E, E) <:< T): ChangeListener[E] = {
    val listener = new ChangeListener[E] {
      def changed(o: ObservableValue[_ <: E], oldv: E, newv: E) = applyIfDefined((o, oldv, newv))
    }
    observable addListener listener
    listener
  }
  def subscribe[E](observable: ObservableList[E])(implicit eventEv: ListChangeListener.Change[_ <: E] <:< T): ListChangeListener[E] = {
    val listener: ListChangeListener[E] = evt => applyIfDefined(evt)
    observable.addListener(listener)
    listener
  }
}