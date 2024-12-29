package com.marcinjeznach.javafx

import javafx.application.Application
import javafx.geometry.Insets as FxInsets
import javafx.scene.Node
import javafx.scene.control.MenuItem
import javafx.scene.control.ScrollPane
import javafx.scene.effect.Effect
import javafx.scene.layout.Pane
import javafx.scene.paint.Color

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

/** Base class for view without fxml. */
abstract class View<N : Node> {
	/** Returns root view. */
	val view get() = _view

	/** Root view. Must be provided in deriving classes. */
	protected abstract var _view: N
}

/**
 * Creates [Color] from integer values:
 * - red in 0..255,
 * - green in 0..255,
 * - blue in 0..255,
 * opacity is assumed to be 100%.
 */
fun RGB(red: Int, green: Int, blue: Int) = Color(red / 255.0, green / 255.0, blue / 255.0, 1.0)

/**
 * Creates [Color] from integer values:
 * - red in 0..255,
 * - green in 0..255,
 * - blue in 0..255,
 * - opacity in 0..100.
 */
fun RGBA(red: Int, green: Int, blue: Int, alpha: Int) =
	Color(red / 255.0, green / 255.0, blue / 255.0, alpha / 100.0)

fun Insets(
	top: Number = 0,
	right: Number = 0,
	bottom: Number = 0,
	left: Number = 0,
) = FxInsets(top.toDouble(), right.toDouble(), bottom.toDouble(), left.toDouble())

fun Insets(
	top: Number,
	rightLeft: Number,
	bottom: Number,
) = Insets(top, rightLeft, bottom, rightLeft)

fun Insets(
	topBottom: Number,
	rightLeft: Number,
) = Insets(topBottom, rightLeft, topBottom, rightLeft)

fun Insets(
	topRightBottomLeft: Number,
) = Insets(topRightBottomLeft, topRightBottomLeft, topRightBottomLeft, topRightBottomLeft)

inline fun <reified T : Application> launchApplication(vararg args: String) {
	Application.launch(T::class.java, *args)
}

