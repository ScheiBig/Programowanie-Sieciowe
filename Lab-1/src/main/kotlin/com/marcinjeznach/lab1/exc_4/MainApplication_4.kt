package com.marcinjeznach.lab1.exc_4

import com.marcinjeznach.lab1.common.JobGenerator
import com.marcinjeznach.lab1.common.MainView
import com.marcinjeznach.lab1.common.Status
import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.selects.select
import kotlin.time.Duration.Companion.seconds

fun main() {
	Application.launch(HelloApplication::class.java)
}

const val A = 'A'.code
const val Z = 'Z'.code

@OptIn(DelicateCoroutinesApi::class) // Using `GlobalScope` is recommended in coroutines-ui documentation
class HelloApplication : Application() {

	override fun start(stage: Stage) {

		lateinit var jobs: List<JobGenerator>
		val toggleTask = MainView.TaskActionCallback { i, _ ->
			jobs[i].cancel()
			println("Cancelled job $i")
		}

		val mainView = MainView(toggleTask, MainView.Type.ButtonCancel)

		jobs = (0..<10).map { i ->
			object : JobGenerator(GlobalScope, Status.Running) {
				var j = -1

				override fun advance() {
					generatedValue = cs.async {
						delay(1.seconds)
						j = (j + 1) % (Z - A + 1)
						return@async "${(j + A).toChar()}${(i + 1) % 10}"
					}
				}
			}
		}

		jobs.forEach(JobGenerator::advance)

		var jobRefs = jobs.toList()

		GlobalScope.launch(Dispatchers.IO) {
			while (isActive) select {
				jobRefs = jobRefs.filter(JobGenerator::hasNext)
				jobRefs.forEach { j ->
					j.next()
						.onAwait { msg ->
							launch(Dispatchers.JavaFx) { mainView.addText(msg) }
							j.advance()
						}
				}
			}
		}

		val scene = Scene(mainView.view, 320.0, 480.0)
		stage.title = "Laboratorium 1 – Zadanie 1"
		stage.scene = scene
		stage.show()
		stage.setOnCloseRequest {
			jobs.forEachIndexed { i, js ->
				GlobalScope.launch(Dispatchers.JavaFx) {
					js.cancel()
					println("Cancelled Job ${(i + 1) % 10}")
				}
			}
		}
	}
}
