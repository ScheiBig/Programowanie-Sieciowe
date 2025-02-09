package com.marcinjeznach.javafx

import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.ScrollPane
import javafx.scene.layout.Pane
import javafx.scene.text.Text
import javafx.scene.text.TextFlow

/** Creates new [Node] of specified type, applies [configuration][config] and returns it. */
inline fun <reified N : Node> node(id: String? = null, config: N.() -> Unit): N {

	val node = N::class.java.getConstructor()
		.newInstance()
	if (id != null) node.id = id
	node.config()
	return node
}

/** Creates new [Node] of specified type, applies [configuration][config], adds it to this container and returns it. */
inline fun <reified N : Node> Pane.child(id: String? = null, config: N.() -> Unit): N {

	val node = N::class.java.getConstructor()
		.newInstance()
	if (id != null) node.id = id
	node.config()
	this.children += node
	return node
}


/** Creates new [Node] of specified type, applies [configuration][config], sets it as content and returns it. */
inline fun <reified N : Node> ScrollPane.content(id: String? = null, config: N.() -> Unit): N {
	val node = N::class.java.getConstructor()
		.newInstance()
	if (id != null) node.id = id
	node.config()
	this.content = node
	return node
}

/** Creates new [Text] node, applies [configuration][config], adds it to this [TextFlow] and returns it. */
inline fun TextFlow.appendText(text: String? = null, id: String? = null, config: Text.() -> Unit): Text {
	val text = Text(text)
	if (id != null) text.id = id
	text.config()
	this.children += text
	return text
}

/** Base class for view without fxml. */
abstract class View<N : Parent> {
	/** Returns root view. */
	val view get() = _view

	/** Root view. Must be provided in deriving classes. */
	protected abstract var _view: N

	fun scene() = Scene(this._view)
	fun scene(width: Number, height: Number) = Scene(this._view, width.toDouble(), height.toDouble())
}
