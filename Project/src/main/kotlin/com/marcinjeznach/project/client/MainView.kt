package com.marcinjeznach.project.client

import com.marcinjeznach.javafx.*
import com.marcinjeznach.utils.runGUI
import com.marcinjeznach.utils.runIO
import javafx.beans.binding.Bindings
import javafx.collections.ObservableList
import javafx.collections.ObservableSet
import javafx.collections.SetChangeListener
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.util.Callback
import kotlinx.coroutines.Deferred
import org.controlsfx.control.ToggleSwitch
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid
import org.kordamp.ikonli.javafx.FontIcon
import java.net.InetSocketAddress
import java.net.SocketAddress

class MainView(
	private val messageLog: ObservableList<Node>,
	private val availableConnections: ObservableSet<InetSocketAddress>,
	private val connectionCallback: (InetSocketAddress?) -> Deferred<Boolean>,
	private val communicationCallback: (String?) -> Deferred<Boolean>,
) : View<Region>() {

	private lateinit var _interface: ComboBox<InetSocketAddress>
	private lateinit var _connect: Button

	private lateinit var _frequency: TextField
	private lateinit var _begin: Button

	private lateinit var _scroll: ScrollPane
	private lateinit var _stickyScrollCheckBox: ToggleSwitch

	private lateinit var _parachuteIcon: FontIcon

	fun loseConnection() {
		_interface.value = null
		_connect.text = Con.Connect.name
		_frequency.isDisable = false
		_begin.isDisable = false
		_begin.text = Com.Start.name
	}

	fun indicateLandingOffers(inProgress: Boolean) {
		_parachuteIcon.isVisible = !inProgress
	}

	override var _view: Region = node<VBox>("Main_view") {
		child<VBox>("Header") {
			border = Border(
				BorderStroke(
					Color.web("#aaa"),
					BorderStrokeStyle.SOLID,
					CornerRadii.EMPTY,
					BorderWidths(0.0, 0.0, 1.0, 0.0)
				)
			)
			maxHeight = 144.0
			minWidth = 180.0

			padding = Insets(8.0, 20.0, 8.0, 8.0)

			child<Label> {
				text = "Time Server:"
			}
			child<HBox> {
				alignment = Pos.CENTER
				_interface = child<ComboBox<InetSocketAddress>> {
					minWidth = 180.0

					buttonCell = AddressCell()
					cellFactory = Callback { _ -> AddressCell() }

					items += availableConnections
					value = Config.lastServer?.let { last ->
						if (availableConnections.isNotEmpty() && last !in availableConnections) null
						else last
					}
					isDisable = availableConnections.isEmpty()

					val reloadConnections = {
						val valCache = value
						items.clear()
						items += availableConnections
						value = if (valCache !in availableConnections) null
						else valCache
						isDisable = availableConnections.isEmpty()
					}
					availableConnections.addListener(SetChangeListener {
						reloadConnections()
					})
				}
				child<Region> {
					hGrow = Priority.ALWAYS
				}
				_parachuteIcon = child<FontIcon> {
					iconCode = FontAwesomeSolid.PARACHUTE_BOX
					iconSize = 16
					iconColor = Color.DIMGRAY
					isVisible = false
				}
				child<Region> {
					hGrow = Priority.ALWAYS
				}
				_connect = child<Button> {
					text = Con.Connect.name

					setOnAction {
						runGUI {
							if (text == Con.Connect.name) {
								val suc =
									if (_interface.value != null) connectionCallback(_interface.value) else return@runGUI

								if (!suc.await()) return@runGUI
								text = Con.Disconnect.name
								_interface.isDisable = true
								_begin.isDisable = false
							} else {
								connectionCallback(null)
								text = Con.Connect.name
								_interface.isDisable = false
								_begin.text = Com.Start.name
								_begin.isDisable = true
								_frequency.isDisable = false
							}
						}
					}
				}
			}

			child<Region> {
				prefHeight = 16.0
			}
			child<Label> {
				text = "Query frequency [ms]:"
			}
			child<HBox> {
				prefWidth = 240.0
				_frequency = child<TextField> {
					promptText = "10 - 1000"
					prefWidth = 70.0

					textProperty().addListener { _, old, new ->
						if (new.isEmpty()) return@addListener
						if (new.isBlank()) {
							text = ""
							return@addListener
						}
						val actualNew = new.filter { it.isDigit() }
							.toIntOrNull()
						if (actualNew == null) {
							text = old.filter { it.isDigit() }
							return@addListener
						}
						if (actualNew > 1000) {
							text = "1000"
							return@addListener
						}
						text = actualNew.toString()
					}
				}
				child<Region> {
					prefWidth = 8.0
				}
				_begin = child<Button> {
					text = Com.Start.name
					isDisable = true

					setOnAction {
						runGUI {
							if (text == Com.Start.name) {
								val txt = _frequency.text
								if (txt.isNullOrBlank() || txt.toIntOrNull() !in 10..1000) return@runGUI
								val suc = communicationCallback(txt)
								if (!suc.await()) return@runGUI
								text = Com.Stop.name
								_frequency.isDisable = true
							} else {
								connectionCallback(null)
								text = Com.Start.name
								_frequency.isDisable = false
							}
						}
					}
				}
				child<Region> {
					hGrow = Priority.ALWAYS
				}
				_stickyScrollCheckBox = child<ToggleSwitch> {}
				child<Label> {
					text = "Autoscroll"
					padding = Insets(top = 2.0, left = 4.0)
				}
			}
		}

		child<HBox> {
			vGrow = Priority.ALWAYS

			_scroll = child<ScrollPane> {
				vbarPolicy = ScrollPane.ScrollBarPolicy.ALWAYS

				hGrow = Priority.ALWAYS

				also { scroll ->

					content<VBox> {
						alignment = Pos.TOP_LEFT
						background = Color.web("#fff").asFill
						padding = Insets(8.0)

						heightProperty().addListener { _, _, _ ->
							if (_stickyScrollCheckBox.isSelected) {
								scroll.vvalue = 1.0
							}
						}
						_stickyScrollCheckBox.selectedProperty().addListener { _, _, sel ->
							if (sel) scroll.vvalue = 1.0
						}

						minWidthProperty().bind(
							scroll.viewportBoundsProperty()
								.map { it.width })
						minHeightProperty().bind(
							scroll.viewportBoundsProperty()
								.map { it.height })

						Bindings.bindContentBidirectional(children, messageLog)
					}
				}
			}
		}
	}

	private fun postMessage(header: String, color: Color, msg: String) {
		runGUI {
			messageLog += message(header, color, msg)
		}
	}

	enum class Con {
		Connect, Disconnect
	}

	enum class Com {
		Start, Stop
	}

	class AddressCell : ListCell<InetSocketAddress>() {
		override fun updateItem(item: InetSocketAddress?, empty: Boolean) {
			super.updateItem(item, empty)

			text =
				item?.let { "${it.address.hostAddress}:${it.port}" }
		}
	}
}
