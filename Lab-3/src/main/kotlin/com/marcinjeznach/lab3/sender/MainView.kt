package com.marcinjeznach.lab3.sender

import com.marcinjeznach.javafx.*
import com.marcinjeznach.udp.broadcast
import com.marcinjeznach.udp.multicast
import com.marcinjeznach.utils.runGUI
import javafx.beans.binding.Bindings
import javafx.collections.FXCollections
import javafx.event.ActionEvent
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.paint.Color
import org.controlsfx.dialog.ExceptionDialog
import java.net.InetAddress

class MainView : View<Region>() {

	fun dispose() {}

	private val _messages = FXCollections.observableArrayList<Node>()

	private lateinit var _group: TextInputControl
	private lateinit var _port: TextInputControl
	private lateinit var _broadcast: CheckBox

	private lateinit var _message: TextInputControl
	private lateinit var _send: Button


	override var _view: Region = node<StackPane> {
		child<VBox>("Main_view") {
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

					padding = Insets(8.0, 20.0, 8.0, 8.0)

					child<Label> {
						text = "Multicast group:"
					}
					_group = child<TextField> {
						promptText = "ex. 224.0.0.1"
					}

					child<Region> {
						prefHeight = 8.0
					}
					child<Label> {
						text = "Connection port:"
					}
					_port = child<TextField> {
						promptText = "ex. 3000"
					}

					child<Region> {
						prefHeight = 8.0
					}
					child<HBox> {
						child<Label> {
							text = "Broadcast message:"
						}

						child<Region> {
							hGrow = Priority.ALWAYS
						}

						_broadcast = child<CheckBox> {
							isSelected = false
							selectedProperty().addListener { _, _, isSet ->
								_group.isDisable = isSet
							}
						}
					}
				}
				child<VSeparator> {}
				child<VBox>("Message_menu") {
					hGrow = Priority.ALWAYS
					padding = Insets(8.0)

					child<Label> {
						text = "Message:"
					}
					_message = child<TextArea> {
						hGrow = Priority.ALWAYS
					}
					child<Region> {
						minHeight = 8.0
					}
					child<HBox> {
						alignment = Pos.CENTER_RIGHT

						_send = child<Button> {
							text = "Send"

							setOnAction(::sendMsg)
						}
					}
				}
			}
			child<HBox> {
				vGrow = Priority.ALWAYS

				child<ScrollPane> {
					vbarPolicy = ScrollPane.ScrollBarPolicy.ALWAYS

					hGrow = Priority.ALWAYS

					val parent = this

					content<VBox> {
						alignment = Pos.TOP_LEFT
						background = Color.web("#fff").asFill
						padding = Insets(8.0)

						minWidthProperty().bind(
							parent.viewportBoundsProperty()
								.map { it.width })
						minHeightProperty().bind(
							parent.viewportBoundsProperty()
								.map { it.height })

						Bindings.bindContentBidirectional(children, _messages)
					}
				}
			}
		}
	}

	private fun sendMsg(event: ActionEvent) {
		if (_group.text.isBlank() && !_broadcast.isSelected) {
			Alert(Alert.AlertType.WARNING).apply {
				title = "Invalid parameters"
				headerText = "Invalid destination"
				contentText = "Please specify multicast group or select broadcast message!"
			}
				.showAndWait()
			return
		}
		if (!_broadcast.isSelected && _group.text.split(".")
				.let {
					if (it.size != 4) return@let true
					val (s1, s2, s3, s4) = it
					if (s1.toIntOrNull() !in 224..239) return@let true
					if (s2.toIntOrNull() !in 0..255) return@let true
					if (s3.toIntOrNull() !in 0..255) return@let true
					if (s4.toIntOrNull() !in 0..255) return@let true
					return@let false
				}
		) {
			Alert(Alert.AlertType.WARNING).apply {
				title = "Invalid parameters"
				headerText = "Invalid destination"
				contentText = """
					Multicast group [${_group.text}] is invalid!
					Valid groups are in range [224.0.0.0]..[239.255.255.255].
					""".trimIndent()
			}
				.showAndWait()
			return
		}
		if (_port.text.toIntOrNull() !in 1..65535) {
			Alert(Alert.AlertType.WARNING).apply {
				title = "Invalid parameters"
				headerText = "Invalid port"
				contentText = """
					Connection port [${_port.text}] is invalid!
					Valid ports are in range [1]..[65535].
					""".trimIndent()
			}
				.showAndWait()
			return
		}
		if (_message.text.isNotBlank()) {
			runGUI {
				postMessage(
					_message.text,
					if (_broadcast.isSelected) "broadcast" else _group.text,
					_port.text.toInt(),
				)
			}
		}
	}

	private fun postMessage(msg: String, group: String, port: Int) {
		val res = if (group == "broadcast") {
			broadcast(msg, port)
		} else {
			multicast(msg, InetAddress.getByName(group), port)
		}

		if (res.isFailure) {
			runGUI {
				ExceptionDialog(res.exceptionOrNull()).apply {
					title = "Error while sending message"
				}
					.showAndWait()
			}
			return
		}

		_messages += message("Sent -> $group:$port", Color.SEAGREEN, msg)
	}
}
