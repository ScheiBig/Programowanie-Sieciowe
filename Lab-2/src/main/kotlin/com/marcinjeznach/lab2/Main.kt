package com.marcinjeznach.lab2

import kotlin.system.exitProcess

/**
 * Main entry point for JAR generated with `mvn package`.
 */
fun main(args: Array<String>) {
	when (args.getOrNull(0)) {
		"client" -> com.marcinjeznach.lab2.client.main()
		"server:single" -> com.marcinjeznach.lab2.server.single.main()
		"server:multi" -> com.marcinjeznach.lab2.server.multi.main()

		else -> {
			System.err.println(
				"""
				Please specify exercise name in program args:
				- "client",
				- "server:single",
				- "server:multi"!
				""".trimIndent())
			exitProcess(1)
		}
	}
}
