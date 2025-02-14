package com.marcinjeznach.project.server

import com.marcinjeznach.javafx.*
import javafx.beans.binding.Bindings
import javafx.beans.binding.ObjectBinding
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.Text
import org.controlsfx.control.ToggleSwitch
import java.net.InetAddress
import java.net.NetworkInterface

class MainView(
	sidebarState: ObjectBinding<List<String>>,
	datagramLog: ObservableList<Node>,
	messageLog: ObservableList<Node>,
) : View<Region>() {
	private lateinit var _stickyScrollCheckBox: ToggleSwitch

	override var _view: Region = node<HBox>("Main_view") {
		child<VBox> {
			child<ScrollPane>("Sidebar") {
				prefWidth = 240.0
				maxWidth = 240.0
				minWidth = 240.0
				hGrow = Priority.ALWAYS
				vGrow = Priority.ALWAYS
				border = Border(
					BorderStroke(
						Color.web("#aaa"),
						BorderStrokeStyle.SOLID,
						CornerRadii.EMPTY,
						BorderWidths(0.0, 0.0, 1.0, 0.0)
					)
				)

				content<VBox> {
					val mapSidebarState = { row: String ->
						node<Text> {
							text = row
							font = Font.font(
								"Monospaced",
								if (row.startsWith(" ")) FontWeight.NORMAL else FontWeight.BOLD,
								font.size,
							)
							padding = Insets(rightLeft = 8.0)
						}
					}

					children += sidebarState.get().map(mapSidebarState)

					sidebarState.addListener({ _, _, list ->
						children.clear()
						children += list.map(mapSidebarState)
					})
				}
			}
			child<HBox> {
				alignment = Pos.CENTER_RIGHT
				padding = Insets(4.0, 8.0)

				_stickyScrollCheckBox = child<ToggleSwitch> {}
				child<Label> {
					text = "Autoscroll"
					padding = Insets(top = 2.0, left = 4.0)
				}
			}
		}
		child<VBox> {
			hGrow = Priority.ALWAYS

			val right = this

			child<HBox> {
				prefHeightProperty().bind(
					right.heightProperty()
						.map { it.toInt() / 2 - 9 }
				)

				vGrow = Priority.ALWAYS

				child<ScrollPane> {
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

							Bindings.bindContentBidirectional(children, datagramLog)
						}
					}
				}
			}

			child<VSeparator> {}

			child<HBox> {
				prefHeightProperty().bind(
					right.heightProperty()
						.map { it.toInt() / 2 - 9 }
				)

				vGrow = Priority.ALWAYS

				child<ScrollPane> {
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
	}
}
