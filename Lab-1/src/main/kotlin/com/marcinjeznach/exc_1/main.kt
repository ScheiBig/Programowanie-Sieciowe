package com.marcinjeznach.exc_1

import com.marcinjeznach.ansi.*

fun main() {
    val bg = Thread {
        println("Hello world!!")
    }

    bg.start()
    bg.join()
}