package com.marcinjeznach.javafx

import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.control.Control
import javafx.scene.control.Label
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import org.controlsfx.control.decoration.Decoration
import org.controlsfx.control.decoration.GraphicDecoration
import org.controlsfx.validation.Severity
import org.controlsfx.validation.ValidationMessage
import org.controlsfx.validation.decoration.GraphicValidationDecoration
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid
import org.kordamp.ikonli.javafx.FontIcon

class IkonliValidatorDecorator(
	private val info: Boolean = false,
	private val iconPlacement: Pos = Pos.CENTER_RIGHT,
	private val requiredTagPlacement: Pos = Pos.TOP_LEFT,
) : GraphicValidationDecoration() {

	override fun getGraphicBySeverity(severity: Severity?) = when (severity) {
		Severity.OK -> node<StackPane> {
			padding = Insets(left = 24.0)

			child<FontIcon> {
				iconCode = FontAwesomeSolid.CHECK_SQUARE
				iconSize = 16
				cursor = Cursor.HAND
				iconColor = Color.GREEN
			}
		}
		Severity.INFO -> if (info) node<StackPane> {
			padding = Insets(left = 24.0)

			child<FontIcon> {
				iconCode = FontAwesomeSolid.INFO_CIRCLE
				iconSize = 16
				cursor = Cursor.HAND
				iconColor = Color.CORNFLOWERBLUE
			}
		} else null
		Severity.WARNING -> node<StackPane> {
			padding = Insets(left = 24.0)

			child<FontIcon> {
				iconCode = FontAwesomeSolid.EXCLAMATION_TRIANGLE
				iconSize = 16
				cursor = Cursor.HAND
				iconColor = Color.GOLDENROD
			}
		}
		Severity.ERROR -> node<StackPane> {
			padding = Insets(left = 24.0)

			child<FontIcon> {
				iconCode = FontAwesomeSolid.PLUS_SQUARE
				iconSize = 16
				cursor = Cursor.HAND
				iconColor = Color.RED
				rotate = 45.0
			}
		}
		null -> null
	}

	override fun createDecorationNode(message: ValidationMessage?) = node<Label> {
		graphic = getGraphicBySeverity(message?.severity)
		tooltip = createTooltip(message)
		alignment = Pos.CENTER
	}

	override fun createValidationDecorations(message: ValidationMessage?) =
		mutableListOf(GraphicDecoration(createDecorationNode(message), iconPlacement))

	override fun createRequiredDecorations(target: Control?) =
		listOf<Decoration>(
			GraphicDecoration(
				node<StackPane> {
					child<Region> {
						background = Color.ORANGERED.asFill
						prefWidth = 1.5
						prefHeight = 9.0
					}
					child<Region> {
						background = Color.ORANGERED.asFill
						prefWidth = 1.5
						prefHeight = 9.0
						rotate = 120.0
					}
					child<Region> {
						background = Color.ORANGERED.asFill
						prefWidth = 1.5
						prefHeight = 9.0
						rotate = 240.0
					}

				},
				requiredTagPlacement,
			)
		)
}
