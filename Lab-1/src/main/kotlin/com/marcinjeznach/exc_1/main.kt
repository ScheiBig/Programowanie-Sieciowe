package com.marcinjeznach.exc_1

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

// Wrapping main as blocking coroutine – this creates runtime for suspending
// functions and allows like joining to other coroutines.
fun main() = runBlocking {
    // Dispatchers.IO ensures, that coroutines are launched on a thread pool – this
    // means that on most modern computers, all of them will be running simultaneously.
    val bg = launch(Dispatchers.IO) {
        println("Hello world!!")
    }

    bg.join()
}