package com.marcinjeznach

import kotlin.system.exitProcess

/**
 * Main entry point for JAR generated with `mvn package`.
 */
fun main(args: Array<String>) {
	when (args.getOrNull(0)?.toIntOrNull()) {
		1 -> com.marcinjeznach.lab1.exc_1.main()
		2 -> com.marcinjeznach.lab1.exc_2.main()
		3 -> com.marcinjeznach.lab1.exc_3.main()
		4 -> com.marcinjeznach.lab1.exc_4.main()

		else -> {
			System.err.println("Please specify exercise number in program args (1..4)!")
			exitProcess(1)
		}
	}
}
