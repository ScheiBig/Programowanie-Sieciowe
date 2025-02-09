package com.marcinjeznach.javafx

import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.TextFlow


fun message(header: String, color: Color, message: String) = node<TextFlow> {
	appendText("[$header]") {
		fill = color
		font = font.let { f -> Font.font("Monospaced", FontWeight.BOLD, f.size) }
	}

	val ind = {
		appendText("${"-".repeat("[$header]".length)}: ") {
			fill = Color.web("#bbb")
			font = Font.font("Monospaced")
		}
	}

	val msg = message.lines()
	appendText(if (msg.size == 1) ": ${msg.first()}" else ": ${msg.first()}\n") {
		font = Font.font("Monospaced")
	}

	val rest = msg.drop(1)
	for (l in rest.dropLast(1)) {
		ind()
		appendText("$l\n") {
			font = Font.font("Monospaced")
		}
	}
	rest.lastOrNull()?.let { l ->
		ind()
		appendText("$l") {
			font = Font.font("Monospaced")
		}
	}
}
