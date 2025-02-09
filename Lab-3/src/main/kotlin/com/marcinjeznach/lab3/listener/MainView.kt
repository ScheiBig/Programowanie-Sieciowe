package com.marcinjeznach.lab3.listener

import com.marcinjeznach.javafx.*
import com.marcinjeznach.udp.MessageChannel
import com.marcinjeznach.udp.listen
import com.marcinjeznach.utils.launchGUI
import com.marcinjeznach.utils.runGUI
import com.marcinjeznach.utils.runIO
import javafx.beans.binding.Bindings
import javafx.collections.FXCollections
import javafx.event.ActionEvent
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.paint.Color
import kotlinx.coroutines.flow.receiveAsFlow
import org.controlsfx.dialog.ExceptionDialog
import java.net.InetAddress
import java.net.NetworkInterface

class MainView : View<Region>() {

	fun dispose() {
		_close?.invoke()
	}

	private val _messages = FXCollections.observableArrayList<Node>()

	private lateinit var _group: TextInputControl
	private lateinit var _port: TextInputControl
	private lateinit var _interface: ComboBox<NetworkInterface>
	private lateinit var _listen: Button

	private var _close: (() -> Unit)? = null
	private var _msgQueue: MessageChannel? = null

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
				child<VBox>("Connection_menu_1") {
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
				}
				child<VSeparator> {}
				child<VBox>("Connection_menu_2") {
					minWidth = 180.0

					padding = Insets(8.0, 20.0, 8.0, 8.0)

					child<Label> {
						text = "Network interface:"
					}
					_interface = child<ComboBox<NetworkInterface>> {
						items.addAll(
							NetworkInterface.networkInterfaces()
							.filter { it.supportsMulticast() }
							.filter { true }
							.toList())

						buttonCell = object : ListCell<NetworkInterface>() {
							override fun updateItem(item: NetworkInterface?, empty: Boolean) {
								super.updateItem(item, empty)

								text = if (item != null) {
									"${item.displayName} (${item.name})"
								} else {
									null
								}
							}
						}
					}

					child<Region> {
						prefHeight = 8.0
					}
					child<HBox> {
						child<Region> {
							hGrow = Priority.ALWAYS
						}

						_listen = child<Button> {
							text = Con.Listen.name

							setOnAction(::handleListening)
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

	private fun handleListening(actionEvent: ActionEvent?) {
		if (_listen.text == Con.Stop.name) {
			dispose()
			_listen.text = Con.Listen.name
			postMessage(
				"Stopped listening",
				"${_group.text}:${_port.text} @ ${_interface.value.name}",
				Color.CHOCOLATE
			)
			_group.isDisable = false
			_port.isDisable = false
			_interface.isDisable = false
			return
		}


		if (_group.text.isBlank()) {
			Alert(Alert.AlertType.WARNING).apply {
				title = "Invalid parameters"
				headerText = "Invalid destination"
				contentText = "Please specify multicast group!"
			}
				.showAndWait()
			return
		}
		if (_group.text.split(".")
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
		if (_interface.value == null) {
			Alert(Alert.AlertType.WARNING).apply {
				title = "Invalid parameters"
				headerText = "Invalid network interface"
				contentText = """
					Please select interface from the list
					""".trimIndent()
			}
				.showAndWait()
			return
		}

		val res = listen(InetAddress.getByName(_group.text), _port.text.toInt(), _interface.value)
		if (res.isFailure) {
			runGUI {
				ExceptionDialog(res.exceptionOrNull()).apply {
					title = "Error while creating listener"
				}
					.showAndWait()
			}
			return
		}

		_close = res.getOrThrow().close
		_msgQueue = res.getOrThrow().comm

		runIO {
			_msgQueue?.receiveAsFlow()
				?.collect {
					it.fold(
						{ suc ->
							launchGUI {
								postMessage("Received <- ${suc.origin}", suc.msg, Color.SEAGREEN)
							}
						},
						{ err ->
							launchGUI {
								postMessage("Error", err.message ?: err.toString(), Color.ORANGERED)
							}
						},
					)
				}
		}

		_listen.text = Con.Stop.name

		postMessage(
			"Started listening",
			"${_group.text}:${_port.text} @ ${_interface.value.name}",
			Color.DARKSLATEBLUE
		)
		_group.isDisable = true
		_port.isDisable = true
		_interface.isDisable = true
	}

	private fun postMessage(header: String, msg: String, color: Color) {
		_messages += message(header, color, msg)
	}

	enum class Con {
		Listen, Stop
	}
}
