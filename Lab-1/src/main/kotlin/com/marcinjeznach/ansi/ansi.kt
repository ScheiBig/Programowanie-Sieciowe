@file:Suppress("ClassName", "ConstPropertyName")

package com.marcinjeznach.ansi

class Modifier(private val escapeSeq: String) {
	override fun toString() = escapeSeq

	/**
	 * Wraps message into this format.
	 *
	 * Clears all formatting at the end of returned string.
	 */
	operator fun get(msg: Any?) = "$escapeSeq$msg\u001B[0m"

	/** Produces modifier that adds [next] effect. */
	operator fun plus(next: Modifier) = Modifier(escapeSeq + next.escapeSeq)
}

/** Changes text color. */
object fg {
	val black = Modifier("\u001b[30m")
	val red = Modifier("\u001b[31m")
	val green = Modifier("\u001b[32m")
	val yellow = Modifier("\u001b[33m")
	val blue = Modifier("\u001b[34m")
	val magenta = Modifier("\u001b[35m")
	val cyan = Modifier("\u001b[36m")
	val white = Modifier("\u001b[37m")

	object br {
		val black = Modifier("\u001b[90m")
		val red = Modifier("\u001b[91m")
		val green = Modifier("\u001b[92m")
		val yellow = Modifier("\u001b[93m")
		val blue = Modifier("\u001b[94m")
		val magenta = Modifier("\u001b[95m")
		val cyan = Modifier("\u001b[96m")
		val white = Modifier("\u001b[97m")
	}

	/** Creates text color changer from 8-bit color code. */
	fun code(c: Int) = Modifier("\u001b[38;5;${c}m")
}

/** Changes background color. */
object bg {
	val black = Modifier("\u001b[40m")
	val red = Modifier("\u001b[41m")
	val green = Modifier("\u001b[42m")
	val yellow = Modifier("\u001b[43m")
	val blue = Modifier("\u001b[44m")
	val magenta = Modifier("\u001b[45m")
	val cyan = Modifier("\u001b[46m")
	val white = Modifier("\u001b[47m")

	object br {
		val black = Modifier("\u001b[100m")
		val red = Modifier("\u001b[101m")
		val green = Modifier("\u001b[102m")
		val yellow = Modifier("\u001b[103m")
		val blue = Modifier("\u001b[104m")
		val magenta = Modifier("\u001b[105m")
		val cyan = Modifier("\u001b[106m")
		val white = Modifier("\u001b[107m")
	}

	/** Creates background color changer from 8-bit color code. */
	fun code(c: Int) = Modifier("\u001b[48;5;${c}m")
}

/** Formats text. */
object fmt {
	/** Removes all formatting, including text and background color. */
	const val reset = "\u001b[0m"
	val bold = Modifier("\u001b[1m")
	val faint = Modifier("\u001b[2m")
	val italic = Modifier("\u001b[3m")
	val underline = Modifier("\u001b[4m")

	/** Inverts text and background color. */
	val inverse = Modifier("\u001b[7m")

	/** Removes specific text formatting. */
	object off {
		val bold = Modifier("\u001b[22m")
		val faint = Modifier("\u001b[22m")
		val italic = Modifier("\u001b[23m")
		val underline = Modifier("\u001b[24m")
		val inverse = Modifier("\u001b[27m")
	}
}

/** Clears text. */
object clr {
	/** Clears portion of screen. */
	object scr {
		const val toEnd = "\u001b[0J"
		const val toStart = "\u001b[1J"
		const val all = "\u001b[2J"
	}

	/** Clears portion of current line. */
	object ln {
		const val toEnd = "\u001b[0K"
		const val toStart = "\u001b[1K"
		const val all = "\u001b[2K"
	}

	/** Clears screen and scroll history. */
	const val everything = "\u001b[3J"
}

/** Changes cursor position */
object cur {
	fun up(n: Int) = "\u001b[${n}A"
	fun down(n: Int) = "\u001b[${n}B"
	fun right(n: Int) = "\u001b[${n}C"
	fun left(n: Int) = "\u001b[${n}D"

	/** Changes cursor position, returning to beginning of the line. */
	object ln {
		fun up(n: Int) = "\u001b[${n}F"
		fun down(n: Int) = "\u001b[${n}E"
	}

	/**
	 * Scrolls screen buffer in specified direction, moving cursor in reverse direction.
	 *
	 * After change, cursor stays in same place - only printed content shifts,
	 * replacing old one, instead of moving it like [println()][println] does.
 	 */

	object scr {
		fun up(n: Int) = "\u001b[${n}S"
		fun down(n: Int) = "\u001b[${n}T"
	}

	/**
	 * Moves cursor to specified absolute position.
	 */
	object goTo {
		fun yx(y: Int, x: Int) = "\u001b[${y};${x}H"
		fun x(x: Int) = "\u001b[${x}G"
	}

	/**
	 * Saves / restores cursor position.
	 *
	 * For terminals with x-term emulation, use [xmemo].
	 */
	object memo {
		const val save = "\u001b[s"
		const val restore = "\u001b[u"
	}

	/**
	 * Saves / restores cursor position.
	 *
	 * For terminals with non-x-term emulation, use [memo].
	 */
	object xmemo {
		const val save = "\u001b7"
		const val restore = "\u001b8"
	}
}
