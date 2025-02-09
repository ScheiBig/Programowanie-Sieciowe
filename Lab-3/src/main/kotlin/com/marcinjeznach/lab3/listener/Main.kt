package com.marcinjeznach.lab3.listener

import com.marcinjeznach.javafx.launchApplication
import javafx.application.Application
import javafx.stage.Stage
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) = launchApplication<Main>(*args)

class Main : Application() {

	override fun start(stage: Stage) {
		val mainView = MainView()

		stage.title = "Laboratorium 3 - odbiorca"
		stage.scene = mainView.scene(480, 320)
		stage.minWidth = 480.0
		stage.minHeight = 320.0
		stage.show()

		stage.setOnCloseRequest {
			runBlocking {
				mainView.dispose()
			}
		}
	}
}
