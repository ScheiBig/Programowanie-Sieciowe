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
	block: suspend CoroutineScope.() -> T
) = this.launch(Dispatchers.JavaFx, start) {
	block()
}
