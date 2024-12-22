package com.marcinjeznach.lab1.exc_1

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
	val bg = launch(Dispatchers.IO) {
		println("Hello world!!")
	}

	bg.join()
}
