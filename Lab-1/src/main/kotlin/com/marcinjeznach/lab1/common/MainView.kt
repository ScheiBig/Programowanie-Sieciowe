package com.marcinjeznach.lab1.common

import com.marcinjeznach.javafx.RGB
import com.marcinjeznach.javafx.View
import com.marcinjeznach.javafx.c
import javafx.geometry.Insets
import javafx.geometry.NodeOrientation
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.Text
import org.controlsfx.control.ToggleSwitch
import org.kordamp.ikonli.fontawesome5.FontAwesomeRegular
import org.kordamp.ikonli.javafx.FontIcon

/**
 * Main view of application.
 *
 * @property taskActionCallback Function to be called when action is performed that
 * task job should react to.
 * @param type Type of UI presentation for job manipulation.
 */
class MainView(private val taskActionCallback: TaskActionCallback, type: Type) : View<HBox>() {

	/**
	 * Functional interface that represents action that task job should react to.
	 * @see taskActionCallback
	 */
	fun interface TaskActionCallback {
		/**
		 * @param id Identifier number of task – index in panel.
		 * @param state Requested state from toggle node, or `false` from button node.
		 */
		fun onAction(id: Int, state: Boolean)
	}

	/** Type of UI presentation for job manipulation. */
	enum class Type {
		/** Toggle button, initialized to `false`. */
		ToggleFalse,
		/** Toggle button, initialized to `true`. */
		ToggleTrue,
		/** Action button that cancels. */
		ButtonCancel,
	}

	private lateinit var _scroll: ScrollPane
	private lateinit var _text: Text
	private lateinit var _stickyScrollCheckBox: CheckBox

	/**
	 * Adds text to main content.
	 *
	 * This function performs UI modifications and as such, should be called from Main thread
	 * (`JavaFX` dispatcher).
	 * */
	fun addText(msg: String) {
		_text.text = if (_text.text == "") msg else _text.text + "\n" + msg
	}

	override var _view = HBox().c {
			alignment = Pos.TOP_CENTER

			children += VBox().c {
				background = Background.fill(RGB(240, 240, 240))
				alignment = Pos.TOP_LEFT
				padding = Insets(10.0)
				spacing = 10.0

				if (type == Type.ButtonCancel) {
					for (i in 0..<10) {
						children += HBox().c {
							spacing = 5.0

							val b = Button().c {
								graphic = FontIcon(FontAwesomeRegular.TRASH_ALT)
								contentDisplay = ContentDisplay.LEFT
							}

							val l =  VBox().c {
								alignment = Pos.CENTER_LEFT

								HBox.setHgrow(this, Priority.ALWAYS)

								children += Label("Zadanie ${(i + 1) % 10}").c {
									// Delegate all label events to the button - just like CheckBox would
									setOnMouseEntered(b::fireEvent)
									setOnMouseExited(b::fireEvent)
									setOnMousePressed(b::fireEvent)
									setOnMouseClicked(b::fireEvent)
									setOnMouseReleased(b::fireEvent)
								}
							}

							children += l
							children += b

							b.setOnMouseClicked {
								b.isDisable = true
								l.isDisable = true
								taskActionCallback.onAction(i, false)
							}
						}
					}
				} else {
					for (i in 0..<10) {
						children += ToggleSwitch("Zadanie ${(i + 1) % 10}").c {
							if (type == Type.ToggleTrue) {
								isSelected = true
							}

							selectedProperty().addListener { _, _, n ->
								taskActionCallback.onAction(i, n)
							}
						}
					}
				}

				children += Pane().c {
					VBox.setVgrow(this, Priority.ALWAYS)
				}

				children += CheckBox("Auto-Przewijanie").c {
					_stickyScrollCheckBox = this

					isSelected = true
					nodeOrientation = NodeOrientation.RIGHT_TO_LEFT
				}
			}

			children += ScrollPane().c {
				_scroll = this

				HBox.setHgrow(this, Priority.ALWAYS)

				padding = Insets(10.0, 20.0, 10.0, 20.0)
				background = Background.fill(Color.WHITE)
				vbarPolicy = ScrollPane.ScrollBarPolicy.ALWAYS

				content = StackPane().c {
					background = Background.fill(Color.WHITE)
					alignment = Pos.TOP_RIGHT

					minWidthProperty().bind(
						_scroll.viewportBoundsProperty()
							.map { it.width })
					minHeightProperty().bind(
						_scroll.viewportBoundsProperty()
							.map { it.height })

					children += Text().c {
						_text = this

						font = Font.font("Monospaced", 20.0)

						heightProperty().addListener { _, _, _ ->
							if (_stickyScrollCheckBox.isSelected) {
								_scroll.vvalue = 1.0
							}
						}
					}
				}
			}
		}
}
