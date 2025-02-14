package com.marcinjeznach

import com.marcinjeznach.utils.printErr
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess
import com.marcinjeznach.project.server.main as main_server
import com.marcinjeznach.project.client.main as main_client

/**
 * Main entry point for JAR generated with `mvn package`.
 */
fun main(args: Array<String>): Unit = runBlocking {
	when (args.getOrNull(0)) {

		"server" -> main_server(args)
		"client" -> main_client(args)

		else -> {
			printErr("Please specify program type in program args (server / client)!")
			exitProcess(1)
		}
	}
}
