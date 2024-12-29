package com.marcinjeznach.lab2.server.multi

import com.marcinjeznach.javafx.launchApplication
import com.marcinjeznach.lab2.server.MainView
import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.runBlocking

fun main() = launchApplication<Main_server_multi>()

@Suppress("ClassName")
@OptIn(DelicateCoroutinesApi::class) // Using `GlobalScope` is recommended in coroutines-ui documentation
class Main_server_multi : Application() {

	override fun start(stage: Stage) {
		val mainView = MainView(Server())

		val scene = Scene(mainView.view, 480.0, 320.0)
		stage.title = "Laboratorium 2 – Server Multi"
		stage.scene = scene
		stage.minWidth = 420.0
		stage.minHeight = 300.0
		stage.show()
		mainView.initFocus()
		stage.setOnCloseRequest {
			runBlocking {
				mainView.server.disobey()
					.await()
			}
		}
		mainView.revalidate()
	}
}
