package tangerine

/**
 * defines extractors for javafx event types, useful for pattern matching on events.
 */
import javafx.event.Event
import javafx.scene.input._

object EventMatchers {

  class ExtractedEvent[T <: Event](val event: T) extends AnyVal {
    def isEmpty = false
    def get = this
    def _1 = event.getSource
    def _2 = event.getTarget
    def _3 = event.getEventType
    def _4 = event
  }
  trait EventExtractor[T <: Event] {
    def unapply(evt: T) = new ExtractedEvent[T](evt)
  }
  
  object Event extends EventExtractor[Event]
  object MouseEvent extends EventExtractor[MouseEvent]
  object KeyEvent extends EventExtractor[KeyEvent]
  object InputEvent extends EventExtractor[InputEvent]
}
