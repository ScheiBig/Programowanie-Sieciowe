package com.marcinjeznach.javafx

import javafx.scene.Node
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox

var Node.vGrow: Priority?
	get() = GridPane.getVgrow(this) ?: VBox.getVgrow(this)
	set(value) {
		GridPane.setVgrow(this, value)
		VBox.setVgrow(this, value)
	}

var Node.hGrow: Priority?
	get() = GridPane.getHgrow(this) ?: HBox.getHgrow(this)
	set(value) {
		GridPane.setHgrow(this, value)
		HBox.setHgrow(this, value)
	}
