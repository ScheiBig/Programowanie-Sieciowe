package com.marcinjeznach.javafx

import javafx.scene.layout.*
import javafx.scene.paint.Color

class VSeparator : VBox() {
	var line: Region

	init {
		padding = Insets(8.0, 4.0)

		line = child<Region> {
			background = Background.fill(Color.web("#ccc"))
			minWidth = 1.0
			prefWidth = 1.0
			setVgrow(this, Priority.ALWAYS)
		}
	}

	var fill: Background
		get() = line.background
		set(value) {
			line.background = value
		}
}

class HSeparator : HBox() {
	var line: Region

	init {
		padding = Insets(4.0, 8.0)

		line = child<Region> {
			background = Background.fill(Color.web("#ccc"))
			minHeight = 1.0
			prefHeight = 1.0
			setHgrow(this, Priority.ALWAYS)
		}
	}

	var fill: Background
		get() = line.background
		set(value) {
			line.background = value
		}
}
