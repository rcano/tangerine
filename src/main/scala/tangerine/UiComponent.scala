package tangerine

/**
 * Extremely simple defintion of a UI Component designed to match the contract that FXML exposes (which is just a root component).
 * When using FXML, your only contract from code is CSS identifiers that you look using the root component, so it's a stringly
 * based contract. When extending UiComponent you should make public fields for the the entries that you would otherwise lookup (making
 * this already more refactoring friendly)
 */
trait UiComponent {

  /** defines the root of this UI Component, equivalent to the top element in a FXML file */
  def component: javafx.scene.Node
  
  /**
   * Optional method to setup sample data.
   * This method should only be called after the root [[component]] was added to a Scene
   */
  def setupSample(): Unit = {}
}
