package com.marcinjeznach.lab2.server

import com.marcinjeznach.javafx.*
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

@OptIn(DelicateCoroutinesApi::class) // GlobalScope required for launching coroutines.
class MainView(val server: Server) : View<StackPane>() {

	private lateinit var _port: TextInputControl
	private lateinit var _connect: Button

	private lateinit var _masker: MaskerPane

	private val _validation = ValidationSupport().apply {
		validationDecorator = IkonliValidatorDecorator()
	}

	fun initFocus() = _connect.requestFocus()

	fun revalidate() {
		_port.text = " "
		_port.text = ""
		_port.text = " "
		_port.text = ""
	}

	override var _view = node<StackPane> {
		child<VBox>("Main_view") {
			child<HBox>("Header") {
				maxHeight = 72.0
				alignment = Pos.CENTER_LEFT
				border = Border(
					BorderStroke(
						Color.web("#aaa"),
						BorderStrokeStyle.SOLID,
						CornerRadii.EMPTY,
						BorderWidths(0.0, 0.0, 1.0, 0.0)
					)
				)

				padding = Insets(8.0, 20.0, 8.0, 8.0)

				child<Label> {
					text = "Connection port:"
				}
				child<Region> {
					prefWidth = 8.0
				}
				_port = child<TextField> {
					promptText = "ex. 3000"
					minWidth = 180.0
				}
				_validation.registerValidator(_port) { c: Control, v: String ->
					ValidationResult.fromErrorIf(
						c,
						"Port is required to be a value between 0 and 65535!",
						v.toIntOrNull() !in 0..65535
					)
				}
				child<Region> {
					hGrow = Priority.ALWAYS
				}

				_connect = child<Button> {
					text = Con.Listen.name

					setOnAction {
						if (_connect.text == Con.Listen.name) {
							if (_validation.isInvalid) {
								return@setOnAction
							}

							_masker.text = Con.Listen.progress
							_masker.isVisible = true

							GlobalScope.launch {
								val res = server.listen(_port.text.toInt())
									.await()

								GlobalScope.launch(Dispatchers.JavaFx) {
									if (res.success) {
										_port.isDisable = true
										_connect.text = Con.Stop.name
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

							server.disobey()
							_port.isDisable = false
							_connect.text = Con.Listen.name
						}
					}
				}

			}

			child<HBox>("Log") {
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

						Bindings.bindContentBidirectional(children, server.log)
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
	Listen("Launching…"), Stop("Stopping…")
}
