@file:Suppress("ClassName")

package com.marcinjeznach.lab2.client

import com.marcinjeznach.javafx.launchApplication
import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage
import kotlinx.coroutines.DelicateCoroutinesApi

fun main() = launchApplication<Main_client>()

@OptIn(DelicateCoroutinesApi::class) // Using `GlobalScope` is recommended in coroutines-ui documentation
class Main_client : Application() {

	override fun start(stage: Stage) {
		val mainView = MainView(Connection())

		val scene = Scene(mainView.view, 480.0, 320.0)
		stage.title = "Laboratorium 2 – Klient"
		stage.scene = scene
		stage.minWidth = 420.0
		stage.minHeight = 300.0
		stage.show()
		mainView.initFocus()
		println(stage.scene.lookup("#Connection_menu").layoutBounds)
		stage.setOnCloseRequest {

		}
	}
}
