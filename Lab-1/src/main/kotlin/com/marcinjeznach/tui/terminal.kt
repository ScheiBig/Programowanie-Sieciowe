package com.marcinjeznach.com.marcinjeznach.tui

import org.jline.terminal.Terminal

fun Terminal.println() = this.apply { writer().println() }

fun Terminal.println(msg: Any?) = this.apply { writer().println(msg) }

fun Terminal.print(msg: Any?) = this.apply { writer().print(msg) }

fun Terminal.getch() = this.reader()
	.read()