package com.marcinjeznach.tui

import org.jline.terminal.Terminal
import java.io.IOException
import kotlin.jvm.Throws

/**
 * Prints newline character to this terminal.
 *
 * Returns `this` instance for chaining.
 * Call to [Terminal.flush()][Terminal.flush] is necessary for content to appear
 * on screen.
 */
fun Terminal.println() = this.apply { writer().println() }

/**
 * Prints message to this terminal, terminating with new line.
 *
 * Returns `this` instance for chaining.
 * Call to [Terminal.flush()][Terminal.flush] is necessary for content to appear
 * on screen.
 */
fun Terminal.println(msg: Any?) = this.apply { writer().println(msg) }

/**
 * Prints message to this terminal.
 *
 * Returns `this` instance for chaining.
 * Call to [Terminal.flush()][Terminal.flush] is necessary for content to appear
 * on screen.
 */
fun Terminal.print(msg: Any?) = this.apply { writer().print(msg) }

/**
 * Retrieves single character from terminal.
 */
@Throws(IOException::class)
fun Terminal.getch() = this.reader()
	.read()