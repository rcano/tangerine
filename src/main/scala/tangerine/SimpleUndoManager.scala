package tangerine

import java.time.Clock
import scala.util.Try

object SimpleUndoManager {
  trait Action {
    /**
     * Attempts to execute this action, returns true if successful (in which case it gets added to the undoqueue)
     */
    def `do`(): Boolean
    def undo(): Unit
  }
}
class SimpleUndoManager(wallClock: Clock = Clock.systemDefaultZone) {
  import SimpleUndoManager._
  var _undoQueue = List.empty[Action]
  var _redoQueue = List.empty[Action]
  var _actionsLog = List.empty[String]
  
  def `do`(a: Action): Boolean = {
    _actionsLog ::= s"${wallClock.instant} doing $a"
    val res = Try(a.`do`()).fold(e => {_actionsLog ::= e.getStackTrace.mkString("\n"); false}, b => {if (b) _undoQueue ::= a; b})
    _actionsLog = _actionsLog.take(20)
    res
  }
  def redo(): Unit = if (_redoQueue != Nil) {
    val head :: tail = _redoQueue
    _redoQueue = tail
    `do`(head)
  }
  def undo(): Unit = if (_undoQueue != Nil) {
    val elem :: rest = _undoQueue
    _actionsLog ::= s"${wallClock.instant} undoing $elem"
    _undoQueue = rest
    _redoQueue ::= elem
    Try(elem.undo()).failed.foreach (e => _actionsLog ::= e.getStackTrace.mkString("\n"))
    _actionsLog = _actionsLog.take(20)
  }
  def clear(): Unit = {
    _undoQueue = Nil
    _redoQueue = Nil
    _actionsLog = Nil
  }
}
