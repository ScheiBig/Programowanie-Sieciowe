package com.marcinjeznach.com.marcinjeznach.tui

import com.marcinjeznach.ansi.clr
import com.marcinjeznach.ansi.cur
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.jline.utils.InfoCmp.Capability


fun initConsole(
	initialPrompt: String,
	statusBar: () -> String,
): Terminal {
	val terminal = TerminalBuilder.terminal()
	terminal.enterRawMode()

	val bar = statusBar()

	val pos = terminal.getCursorPosition {}.y
	val height = terminal.size.rows

	repeat(height - pos - 1) { println() }
	terminal.print(cur.up(bar.lines().size))
		.flush()
	terminal.termPrint(initialPrompt) { bar }

	return terminal
}

fun Terminal.termPrint(
	msg: Any?,
	statusBar: () -> String,
) {
	val str = buildString {
		append(clr.scr.toEnd)

		val lines = msg.toString()
			.lines()
		for (s in lines.dropLast(1)) {
			appendLine(s)
			append(cur.scr.up(1))
			append(cur.up(1))
		}

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

fun Terminal.termPrintln(
	msg: Any?,
	statusBar: () -> String,
) = termPrint("$msg\n", statusBar)