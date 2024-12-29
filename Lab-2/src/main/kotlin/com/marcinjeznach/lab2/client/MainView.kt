package com.marcinjeznach.lab2.client

import com.marcinjeznach.javafx.*
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.control.TextInputControl
import javafx.scene.layout.*
import javafx.scene.paint.Color

class MainView(val connection: Connection) : View<VBox>() {

	private lateinit var _host: TextInputControl
	private lateinit var _port: TextInputControl
	private lateinit var _connect: Button

	private lateinit var _message: TextInputControl
	private lateinit var _send: Button

	fun initFocus() = _connect.requestFocus()

	override var _view = node<VBox>("Main_view") {
		child<HBox>("Header") {
			maxHeight = 144.0
			border = Border(
				BorderStroke(
					Color.web("#aaa"),
					BorderStrokeStyle.SOLID,
					CornerRadii.EMPTY,
					BorderWidths(0.0, 0.0, 1.0, 0.0)
				)
			)

			child<VBox>("Connection_menu") {
				minWidth = 180.0

				padding = Insets(8)

				child<Label> {
					text = "Connection host:"
				}
				child<TextField> {
					promptText = "ex. 127.0.0.1 or localhost"

					_host = this
				}
				child<Region> {
					prefHeight = 8.0
				}
				child<Label> {
					text = "Connection port:"
				}
				child<TextField> {
					promptText = "ex. 3000"

					_port = this
				}
				child<Region> {
					prefHeight = 8.0
				}
				child<HBox> {
					alignment = Pos.CENTER_RIGHT

					child<Button> {
						text = Con.Connect.name

						setOnAction {
							if (_connect.text == Con.Connect.name) {


								_connect.text = Con.Disconnect.name
							} else {

								connection.disconnect()
							}
						}

						_connect = this
					}
				}
			}
			child<VSeparator> {}
			child<VBox>("Message_menu") {
				padding = Insets(8)

				child<Label> {
					text = "Message:"
				}
				child<TextArea> {
					_message = this
					HBox.setHgrow(this, Priority.ALWAYS)
				}
				child<Region> {
					minHeight = 8.0
				}
				child<HBox> {
					alignment = Pos.CENTER_RIGHT

					child<Button> {
						text = "Send"

						_send = this
					}
				}
			}
		}
		child<HBox> {
			VBox.setVgrow(this, Priority.ALWAYS)

			child<ScrollPane> {
				vbarPolicy = ScrollPane.ScrollBarPolicy.ALWAYS

				HBox.setHgrow(this, Priority.ALWAYS)

				val self = this

				content<StackPane> {
					alignment = Pos.TOP_LEFT
					background = Background.fill(Color.web("#fff"))

					minWidthProperty().bind(self.viewportBoundsProperty().map { it.width })
					minHeightProperty().bind(self.viewportBoundsProperty().map { it.height })

					child<Label> {
						text = ":TEST:"
					}
				}
			}
		}
	}
}

enum class Con {
	Connect, Disconnect
}
