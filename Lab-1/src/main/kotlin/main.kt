package com.marcinjeznach

import com.marcinjeznach.ansi.fg

fun main(args: Array<String>) {
	when (args.firstOrNull()) {
		"1" -> com.marcinjeznach.exc_1.main()
		"2" -> com.marcinjeznach.exc_2.main()
		"3" -> com.marcinjeznach.exc_3.main()
		else -> error(fg.red["Proszę podać numer zadania (1..4)!"])
	}
}