package com.marcinjeznach

import kotlin.system.exitProcess
import com.marcinjeznach.lab2.client.main as client_main
import com.marcinjeznach.lab2.server.multi.main as serverMulti_main
import com.marcinjeznach.lab2.server.single.main as serverSingle_main

/**
 * Main entry point for JAR generated with `mvn package`.
 */
fun main(args: Array<String>) {
	when (args.getOrNull(0)) {
		"client" -> client_main()
		"server:single" -> serverSingle_main()
		"server:multi" -> serverMulti_main()

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
