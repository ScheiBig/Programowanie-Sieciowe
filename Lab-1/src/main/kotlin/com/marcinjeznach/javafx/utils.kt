package com.marcinjeznach.javafx

import javafx.scene.Node
import javafx.scene.control.MenuItem
import javafx.scene.effect.Effect
import javafx.scene.paint.Color

/** Configures this node and returns it. */
fun <N: Node> N.c(config: N.() -> Unit) = this.apply(config)
/** Configures this menu item and returns it. */
fun <M: MenuItem> M.c(config: M.() -> Unit) = this.apply(config)
/** Configures this effect and returns it. */
fun <E: Effect> E.c(config: E.() -> Unit) = this.apply(config)

/** Base class for view without fxml. */
abstract class View<N: Node> {
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
fun RGBA(red: Int, green: Int, blue: Int, alpha: Int) = Color(red / 255.0, green / 255.0, blue / 255.0, alpha / 100.0)
