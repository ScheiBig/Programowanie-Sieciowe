package com.marcinjeznach.lab2.client

import com.marcinjeznach.javafx.*
import com.marcinjeznach.utils.launchGUI
import javafx.beans.binding.Bindings
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.paint.Color
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import org.controlsfx.control.MaskerPane
import org.controlsfx.dialog.ExceptionDialog
import org.controlsfx.validation.ValidationResult
import org.controlsfx.validation.ValidationSupport
import org.controlsfx.validation.Validator

@OptIn(DelicateCoroutinesApi::class) // GlobalScope required for launching coroutines.
class MainView(val connection: Connection) : View<StackPane>() {

	private lateinit var _host: TextInputControl
	private lateinit var _port: TextInputControl
	private lateinit var _connect: Button

	private lateinit var _message: TextInputControl
	private lateinit var _send: Button

	private lateinit var _masker: MaskerPane

	private val _validation = ValidationSupport().apply {
		validationDecorator = IkonliValidatorDecorator()
	}
	fun initFocus() = _connect.requestFocus()

	fun revalidate() {
		_host.text = " "
		_host.text = ""
		_port.text = " "
		_port.text = ""
	}

	override var _view = node<StackPane> {
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
						text = "Connection host:"
					}
					_host = child<TextField> {
						promptText = "ex. 127.0.0.1 or localhost"
					}
					_validation.registerValidator(_host, Validator.createEmptyValidator<String>("Host is required!"))
					child<Region> {
						prefHeight = 8.0
					}
					child<Label> {
						text = "Connection port:"
					}
					_port = child<TextField> {
						promptText = "ex. 3000"
					}
					_validation.registerValidator(_port) { c: Control, v: String ->
						ValidationResult.fromErrorIf(
							c,
							"Port is required to be a value between 0 and 65535!",
							v.toIntOrNull() !in 0..65535
						)
					}
					child<Region> {
						prefHeight = 8.0
					}
					child<HBox> {
						alignment = Pos.CENTER_RIGHT

						_connect = child<Button> {
							text = Con.Connect.name

							setOnAction {
								if (_connect.text == Con.Connect.name) {
									if (_validation.isInvalid) {
										return@setOnAction
									}

									_masker.text = Con.Connect.progress
									_masker.isVisible = true

									GlobalScope.launch {
										val res = connection.connect(_host.text, _port.text.toInt())
											.await()

										GlobalScope.launch(Dispatchers.JavaFx) {
											if (res.success) {
												_host.isDisable = true
												_port.isDisable = true
												_connect.text = Con.Disconnect.name
												_message.isDisable = false
												_send.isDisable = false
											} else {
												ExceptionDialog(res.error).apply {
													title = "Error"
												}
													.showAndWait()
											}
											_masker.isVisible = false
										}
									}

								} else {

									connection.disconnect()
									_host.isDisable = false
									_port.isDisable = false
									_connect.text = Con.Connect.name
									_message.isDisable = true
									_send.isDisable = true
								}
							}

							connection.lostConnection.addListener { _, _, isLost ->
								if (isLost) {
									GlobalScope.launchGUI {
										_host.isDisable = false
										_port.isDisable = false
										_connect.text = Con.Connect.name
										_message.isDisable = true
										_send.isDisable = true
									}
								}
							}
						}
					}
				}
				child<VSeparator> {}
				child<VBox>("Message_menu") {
					padding = Insets(8.0)

					child<Label> {
						text = "Message:"
					}
					_message = child<TextArea> {
						isDisable = true
						hGrow = Priority.ALWAYS
					}
					child<Region> {
						minHeight = 8.0
					}
					child<HBox> {
						alignment = Pos.CENTER_RIGHT

						_send = child<Button> {
							isDisable = true
							text = "Send"

							setOnAction {
								connection.postMessage(_message.text)
								_message.text = ""
							}
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

						Bindings.bindContentBidirectional(children, connection.log)
					}
				}
			}
		}
		_masker = child<MaskerPane> {
			isVisible = false
		}
	}
}

enum class Con(val progress: String) {
	Connect("Connecting…"), Disconnect("Disconnecting…")
}
