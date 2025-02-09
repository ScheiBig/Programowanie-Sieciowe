package com.marcinjeznach

import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess
import com.marcinjeznach.lab3.listener.main as main_listener
import com.marcinjeznach.lab3.sender.main as main_sender

/**
 * Main entry point for JAR generated with `mvn package`.
 */
fun main(args: Array<String>): Unit = runBlocking {
	when (args.getOrNull(0)) {

		"listener" -> main_listener(args)
		"sender" -> main_sender(args)

		else -> {
			System.err.println("Please specify program type in program args (listener / sender)!")
			exitProcess(1)
		}
	}
}

