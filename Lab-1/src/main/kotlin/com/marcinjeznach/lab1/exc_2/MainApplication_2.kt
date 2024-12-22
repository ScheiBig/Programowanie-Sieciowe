package com.marcinjeznach.lab1.exc_2

import com.marcinjeznach.lab1.common.*
import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.getOrElse
import kotlinx.coroutines.javafx.JavaFx
import kotlin.time.Duration.Companion.seconds

fun main() {
	Application.launch(HelloApplication::class.java)
}

const val A = 'A'.code
const val Z = 'Z'.code

@OptIn(DelicateCoroutinesApi::class) // Using `GlobalScope` is recommended in coroutines-ui documentation
class HelloApplication : Application() {

	override fun start(stage: Stage) {

		lateinit var jobs: List<JobState>
		val toggleTask = MainView.TaskActionCallback { i, s ->
			GlobalScope.launch(Dispatchers.JavaFx) {
				if (s) {
					jobs[i].resume()
				} else {
					jobs[i].pause()
				}
			}
		}

		val mainView = MainView(toggleTask, MainView.Type.ToggleFalse)

		jobs = (0..<10).map { i ->
			val ch = Channel<RequestState>(Channel.CONFLATED)
			JobState(GlobalScope.launch(Dispatchers.IO) {
				var j = 0
				while (isActive) {
					var requestedState = ch.receive()
					while (requestedState == RequestState.Resume) {
						launch(Dispatchers.JavaFx) {
							mainView.addText("${(j + A).toChar()}${(i + 1) % 10}")
						}

						j = (j + 1) % (Z - A + 1)
						delay(1.seconds)
						requestedState = ch.tryReceive()
							.getOrElse { RequestState.Resume }
					}
				}
			}, ch, Status.Stopped)
		}


		val scene = Scene(mainView.view, 320.0, 480.0)
		stage.title = "Laboratorium 1 – Zadanie 1"
		stage.scene = scene
		stage.show()
		stage.setOnCloseRequest { e ->
			jobs.forEachIndexed { i, js ->
				GlobalScope.launch(Dispatchers.JavaFx) {
					js.cancelAndJoin()
					println("Cancelled Job ${(i + 1) % 10}")
				}
			}
		}
	}
}
