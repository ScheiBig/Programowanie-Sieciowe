package com.marcinjeznach.project.client

import com.marcinjeznach.javafx.ObservableInitializableSet
import com.marcinjeznach.javafx.launchApplication
import javafx.application.Application
import javafx.collections.FXCollections
import javafx.scene.Node
import javafx.stage.Stage
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress

fun main(args: Array<String>) = launchApplication<Main>(*args)

class Main : Application() {

	override fun start(stage: Stage) {
		val messageLog = FXCollections.observableArrayList<Node>()
		val availableConnections = ObservableInitializableSet<InetSocketAddress>()

		val conMgr = ConnectionManager(messageLog, availableConnections)

		val mainView = MainView(
			messageLog,
			availableConnections,
			conMgr::toggleConnection,
			conMgr::toggleCommunication,
		)

		conMgr.attachConnectionLostCallback(mainView::loseConnection)
		conMgr.attachOfferIndicator(mainView::indicateLandingOffers)

		stage.title = "Projekt Końcowy - Urządzenie Badawcze"
		stage.scene = mainView.scene(320, 480)
		stage.minWidth = 320.0
		stage.maxWidth = 320.0
		stage.minHeight = 480.0
		stage.show()

		stage.setOnCloseRequest {
			runBlocking {
				runCatching { conMgr.dispose() }
			}
		}
	}
}

