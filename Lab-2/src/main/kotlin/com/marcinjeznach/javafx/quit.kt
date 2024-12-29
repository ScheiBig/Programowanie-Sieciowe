package com.marcinjeznach.javafx

import javafx.application.Platform

fun quit(): Nothing {
	Platform.exit()
	throw IllegalStateException("Should be dead by now")
}
