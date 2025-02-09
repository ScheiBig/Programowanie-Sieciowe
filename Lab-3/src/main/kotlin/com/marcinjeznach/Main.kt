package com.marcinjeznach

import com.marcinjeznach.udp.listen
import com.marcinjeznach.udp.multicast
import com.marcinjeznach.utils.ETX
import com.marcinjeznach.utils.launchIO
import com.marcinjeznach.utils.printlnErr
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import java.net.InetAddress
import kotlin.system.exitProcess

/**
 * Main entry point for JAR generated with `mvn package`.
 */
fun main(args: Array<String>): Unit = runBlocking {
	when (args.getOrNull(0)
		?.toIntOrNull()) {

		else -> {
			System.err.println("Please specify program type in program args (listener / sender)!")
			exitProcess(1)
		}
	}
}
//	System.setProperty("java.net.preferIPv4Stack", "true")
//
//	val group = InetAddress.getByName("224.0.0.10")
//	val port = 1234
//
//	val listenResult = listen(group, port)
//	if (listenResult.isFailure) {
//		printlnErr(listenResult)
//	}
//	val (msgChan, close) = listenResult.getOrThrow()
//
//	launchIO {
//		msgChan.receiveAsFlow()
//			.collect {
//				println("[consumer]: $it")
//			}
//	}
//
//	launchIO {
//		val letters = ('A'..'Z').toList()
//		val counts = (1..5).toList()
//
//		while (isActive) {
//			val letter = (1..counts.random()).map { letters.random() }
//				.joinToString(separator = ETX.toString())
//
//			multicast(letter, group, port)
//			println("[producer]: $letter")
//
//			Thread.sleep(2000)
//		}
//	}
//
//	println("Press [return] to stop...")
//	readln()
//	println("Exiting...")


