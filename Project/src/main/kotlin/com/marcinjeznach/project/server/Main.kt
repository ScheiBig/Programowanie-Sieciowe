package com.marcinjeznach.project.server

import com.marcinjeznach.javafx.DeepCollection
import com.marcinjeznach.javafx.launchApplication
import com.marcinjeznach.networking.ipv4
import com.marcinjeznach.networking.retrieveAvailableMulticastInterfaces
import com.marcinjeznach.project.common.GROUP
import com.marcinjeznach.project.common.PORT
import javafx.application.Application
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.Node
import javafx.stage.Stage
import kotlinx.coroutines.IO_PARALLELISM_PROPERTY_NAME
import kotlinx.coroutines.runBlocking
import java.net.InetAddress
import java.net.NetworkInterface


fun main(args: Array<String>) = launchApplication<Main>(*args)

data class NetworkInterfaceStatus(
	val tcpPort: Int,
	val clientConnections: ObservableList<InetAddress> = FXCollections.observableArrayList(),
)

class Main : Application() {

	override fun start(stage: Stage) {
		System.setProperty(IO_PARALLELISM_PROPERTY_NAME, "256")

		val interfacesState =
			FXCollections.observableHashMap<NetworkInterface, NetworkInterfaceStatus>()
		val datagramLog = FXCollections.observableArrayList<Node>()
		val messageLog = FXCollections.observableArrayList<Node>()

		val availableInterfaces =
			runBlocking { retrieveAvailableMulticastInterfaces(GROUP, PORT).await() }

		val (asyncListeners, disposeTCP) = initializeTCPListeners(
			availableInterfaces,
			messageLog,
			interfacesState
		)
		val disposeUDP = initializeMulticastListeners(datagramLog, interfacesState, asyncListeners)

		val sidebarState =
			object : DeepCollection<NetworkInterface, NetworkInterfaceStatus, ObservableList<InetAddress>, String>(
				interfacesState,
				{ st -> st.clientConnections },
				{ ni, nis ->
					listOf(
						"",
						"Interface <${ni.name}>",
						"${ni.ipv4 ?: ""}:${nis.tcpPort}",
					) + nis.clientConnections.map { adr -> " • ${adr.hostAddress}" }
				},
			) {
				override fun prefix() = listOf("", "UDP @ $GROUP:$PORT")
			}

		val mainView = MainView(sidebarState, datagramLog, messageLog)

		stage.title = "Projekt końcowy - Serwer Czasu"
		stage.scene = mainView.scene(800, 480)
		stage.minWidth = 640.0
		stage.minHeight = 480.0
		stage.show()

		stage.setOnCloseRequest {
			runBlocking {
				runCatching { disposeTCP() }
				runCatching { disposeUDP() }
			}
		}
	}


}

