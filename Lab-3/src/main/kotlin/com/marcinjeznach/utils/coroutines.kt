package com.marcinjeznach.utils

import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx

fun <T> CoroutineScope.asyncIO(
	start: CoroutineStart = CoroutineStart.DEFAULT,
	block: suspend CoroutineScope.() -> T
) = this.async(Dispatchers.IO, start) {
	block()
}

fun <T> CoroutineScope.launchGUI(
	start: CoroutineStart = CoroutineStart.DEFAULT,
	block: suspend CoroutineScope.() -> T,
) = this.launch(Dispatchers.JavaFx, start) {
	block()
}

fun <T> CoroutineScope.launchIO(
	start: CoroutineStart = CoroutineStart.DEFAULT,
	block: suspend CoroutineScope.() -> T,
) = this.launch(Dispatchers.IO, start) {
	block()
}

fun <T> runAsyncIO(
	start: CoroutineStart = CoroutineStart.DEFAULT,
	block: suspend CoroutineScope.() -> T,
) = GlobalScope.asyncIO(start, block)

fun <T> runGUI(
	start: CoroutineStart = CoroutineStart.DEFAULT,
	block: suspend CoroutineScope.() -> T,
) = GlobalScope.launchGUI(start, block)

fun <T> runIO(
	start: CoroutineStart = CoroutineStart.DEFAULT,
	block: suspend CoroutineScope.() -> T,
) = GlobalScope.launchIO(start, block)
