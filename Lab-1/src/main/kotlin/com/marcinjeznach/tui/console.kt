package com.marcinjeznach.tui

import com.marcinjeznach.ansi.clr
import com.marcinjeznach.ansi.cur
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.jline.utils.InfoCmp.Capability

/**
 * Initializes terminal instance that allows printing from bottom
 * of the screen with status bar.
 *
 * Returns initialized, raw terminal without keyboard echo.
 */
fun initConsole(
	initialPrompt: String,
	statusBar: () -> String,
): Terminal {
	val terminal = TerminalBuilder.terminal()
	terminal.enterRawMode()

	val bar = statusBar()

	val pos = terminal.getCursorPosition {}.y
	val height = terminal.size.rows

	// Print enough empty lines on screen, so that status bar can be actually
	// printed from bottom of the screen.
	repeat(height - pos - 1) { println() }
	terminal.print(cur.up(bar.lines().size))
		.flush()
	terminal.termPrint(initialPrompt) { bar }

	return terminal
}

/**
 * Prints message above specified status bar.
 */
fun Terminal.termPrint(
	msg: Any?,
	statusBar: () -> String,
) {
	// Build entire screen buffer – as each single call to Writer.print(…)
	// is synchronized, this ensures that frame in terminal is printed in
	// its entirety.
	val str = buildString {
		append(clr.scr.toEnd)

		// Manually print each line, moving shifting cursor position
		// and buffer placement. This ensures, that content doesn't start
		// printing on top of previous status bar.
		val lines = msg.toString()
			.lines()
		for (s in lines.dropLast(1)) {
			appendLine(s)
			append(cur.scr.up(1))
			append(cur.up(1))
		}

		// This condition is necessary, as XTerm compatible terminals handle
		// cursor position memory with different control sequences
		val query = this@termPrint.getStringCapability(Capability.save_cursor)
		val save = if (query.takeLast(1) == "7") cur.xmemo.save
		else cur.memo.save
		val restore = if (query.takeLast(1) == "7") cur.xmemo.restore
		else cur.memo.restore

		append(lines.last())
		append(save)
		appendLine()
		append(statusBar())
		append(restore)
	}

	this.print(str)
		.flush()
}

/**
 * Prints message above specified status bar, moving cursor to new empty line.
 */
fun Terminal.termPrintln(
	msg: Any?,
	statusBar: () -> String,
) = termPrint("$msg\n", statusBar)