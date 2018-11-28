package tangerine

import better.files._
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableStringValue
import javafx.event.ActionEvent
import javafx.geometry.{Insets, Pos}
import javafx.scene.Node
import javafx.scene.layout._
import javafx.scene.control._
import javafx.stage.FileChooser
import javafx.util.StringConverter

/**
 * T collection of useful and reusable controls are defined in this object
 */
object JfxControls {

  /**
   * Setup a simple border pane with the `title` at the top and the `content` in the center
   * styled with class `titled`.
   */
  def titled(title: Node, content: Node): BorderPane = new BorderPane().tap { it =>
    it setTop title.tap(BorderPane.setMargin(_, new Insets(0, 0, 0.5.em, 0)))
    it setCenter content
    it.getStyleClass add "titled"
  }
  
  /**
   * helper method to construct HBox with convenient optional parameters
   * @param nodes nodes to add to the HBox.
   * @param alignment how to align the nodes, see [[javafx.scene.layout.HBox#setAlignment]].
   * @param spacing horizontal separation between the nodes, see [[javafx.scene.layout.HBox#setSpacing]].
   * @param fillHeight see [[javafx.scene.layout.HBox#setFillHeight]].
   * @return a HBox configured with the passed in parameters and the passed in nodes.
   */
  def hbox(nodes: Node*)(alignment: Pos, spacing: Double = 0.25.em, fillHeight: Boolean = false): HBox = new HBox(nodes:_*).tap { it =>
    it.setAlignment(alignment)
    it.setSpacing(spacing)
    it.setFillHeight(fillHeight)
  }
  /**
   * helper method to construct VBox with convenient optional parameters
   * @param nodes nodes to add to the VBox.
   * @param alignment how to align the nodes, see [[javafx.scene.layout.VBox#setAlignment]].
   * @param spacing vertical separation between the nodes, see [[javafx.scene.layout.VBox#setSpacing]].
   * @param fillWidth see [[javafx.scene.layout.VBox#setFillWidth]].
   * @return a VBox configured with the passed in parameters and the passed in nodes.
   */
  def vbox(nodes: Node*)(alignment: Pos, spacing: Double = 0.25.em, fillWidth: Boolean = false): VBox = new VBox(nodes:_*).tap { it =>
    it.setAlignment(alignment)
    it.setSpacing(spacing)
    it.setFillWidth(fillWidth)
  }
  
  /**
   * @return a filler Pane set to always grow horizontally and vertically for HBox and VBox
   */
  def filler: Pane = new Pane().tap { it => HBox.setHgrow(it, Priority.ALWAYS); VBox.setVgrow(it, Priority.ALWAYS) }
  
  /**
   * This method is intended to be used with UiLocalization
   * @example {{
   *   label(UiLocalization("Preview"))
   * }}
   * @return T normal [[Label]] with its text property bound to the passed text.
   *         This method should be used with UiLocalization
   */
  def label(text: ObservableStringValue): Label = new Label().tap(_.textProperty bind text)
  
  /**
   * Traditional text field + filechooser button combo in a horizontal row
   */
  class FileSelector(title: String) extends HBox {
    val pathTextField = new TextField().tap { it => 
      it.setPromptText(title)
      it.setMaxWidth(Double.MaxValue)
      HBox.setHgrow(it, Priority.ALWAYS)
    }
    setAlignment(Pos.BASELINE_LEFT)
    val chooserButton = new Button("📂")
    
    getChildren.addAll(pathTextField, chooserButton)
    setSpacing(0.25.em)
    
    val selectedFile = new SimpleObjectProperty[Option[File]](this, "selectedFile", None)
    val fileChooser = new FileChooser().tap(_.setTitle("Select " + title))
    
    val fileConverter = new StringConverter[Option[File]] {
      def fromString(s: String) = Option(s).filter(_.nonEmpty).map(File.apply(_))
      def toString(f: Option[File]) = f.map(_.toString) getOrElse ""
    }
    
    Bindings.bindBidirectional(pathTextField.textProperty, selectedFile, fileConverter)
    chooserButton.setOnAction { _ =>
      fileChooser.setInitialFileName(pathTextField.getText)
      fileChooser.showOpenDialog(getScene.getWindow) match {
        case null =>
        case file => selectedFile set Some(file.toScala)
      }
    }
  }
  
  /**
   * T special button that can show different faces depending on states. Typically used to represent playing/pause/replay buttons.
   * 
   * @constructor instantiates a new MultiFacedButton.
   * @param initFace initial face to be shown
   * @param faces all the possible buttons that can be shown.
   * @param transitions specifies the button transitions when a button is pressed.
   */
  class MultiFacedButton(initFace: Button, faces: Button*)(transitions: Button => Button) extends Control {
    val currentFace = new SimpleObjectProperty[Button](this, "currentFace", initFace)
    getStyleClass.add("multi-faced-button")
    override protected def createDefaultSkin = Skin
    object Skin extends javafx.scene.control.Skin[MultiFacedButton] {
      override def getSkinnable = MultiFacedButton.this
      override def dispose = ()
      override val getNode: Node = new StackPane().tap { it =>
        it.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE)
        it.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE)
        
        //register an action listener to each button to apply the transition
        for (button <- faces) {
          button.addEventFilter[ActionEvent](ActionEvent.ACTION, _ => {
              val nextButton = transitions(button)
              currentFace set nextButton
              it.getChildren setAll nextButton
              nextButton.requestFocus() //the button that we just removed had the focus, so we now pass it on to this button
            })
        }
        currentFace.addListener((_, _, b) => it.getChildren setAll b)
        it.getChildren setAll initFace
      }
    }
  }
}
