package com.marcinjeznach.javafx

import javafx.application.Application

inline fun <reified T : Application> launchApplication(vararg args: String) {
	Application.launch(T::class.java, *args)
}
