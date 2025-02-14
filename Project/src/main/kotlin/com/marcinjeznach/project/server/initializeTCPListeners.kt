package com.marcinjeznach.project.server

import com.marcinjeznach.javafx.message
import com.marcinjeznach.javafx.quit
import com.marcinjeznach.networking.ipv4
import com.marcinjeznach.project.common.*
import com.marcinjeznach.utils.*
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import javafx.scene.Node
import javafx.scene.paint.Color
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import org.controlsfx.dialog.ExceptionDialog
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket


private val clientConnections = mutableListOf<Job>()
private val serverDaemons = mutableListOf<Job>()

private fun dispose() {
	clientConnections.forEach(Job::cancel)
	serverDaemons.forEach(Job::cancel)
}

fun initializeTCPListeners(
	availableInterfaces: List<NetworkInterface>,
	messageLog: ObservableList<Node>,
	interfacesState: ObservableMap<NetworkInterface, NetworkInterfaceStatus>,
): Pair<Deferred<Unit>, () -> Unit> {

	val interfacesResolved = CompletableDeferred<Unit>()

	for (ni in availableInterfaces) {
		serverDaemons += runIO {
			try {
				val servSock = ServerSocket(0)
				val port = servSock.localPort
				launchGUI {
					interfacesState[ni] = NetworkInterfaceStatus(port)
					if (interfacesState.size == availableInterfaces.size) {
						interfacesResolved.complete(Unit)
					}
				}

				launchGUI {
					messageLog += message(
						"Listen ${ni.name}",
						Color.SEAGREEN,
						"Launching listener daemon: @ ${ni.ipv4 ?: ""}:$port"
					)
				}
				while (isActive) {
					val clSock = servSock.accept()
					launchGUI {
						messageLog += message(
							"Accept ${ni.name}",
							Color.DARKCYAN,
							"<- ${clSock.inetAddress.hostName}:${clSock.port}"
						)
						interfacesState[ni]?.let { nis -> nis.clientConnections += clSock.inetAddress }
					}
					launchClientConnection(clSock, ni, messageLog, interfacesState)
				}
			} catch (e: Exception) {
				launchGUI {
					messageLog += message(
						"IO Error ${ni.name}", Color.INDIANRED, e.message ?: e.toString()
					)
					ExceptionDialog(e).apply {
						title = "IO Error for interface ${ni.name}"
					}
						.showAndWait()
				}
			}
		}
	}

	return interfacesResolved to ::dispose
}

fun launchClientConnection(
	clSock: Socket,
	ni: NetworkInterface,
	messageLog: ObservableList<Node>,
	interfacesState: ObservableMap<NetworkInterface, NetworkInterfaceStatus>,
) {
	clientConnections += runIO {
		try {
			val inp = DelimitedBufferedReader(clSock.getInputStream(), "$ETX")
			val out = DelimitedPrintWriter(clSock.getOutputStream(), "$ETX")
			while (isActive) {

				val msg = inp.readDelimited()
				if (msg == null) {
					launchGUI {
						messageLog += message(
							"IO Error ${clSock.inetAddress.hostAddress}",
							Color.INDIANRED,
							"No message received from socket"
						)
						messageLog += message(
							"Disconnected ${clSock.inetAddress.hostAddress}",
							Color.DARKORANGE,
							"Lost connection with client"
						)
					}
					break
				}
				val parseResult = runCatching { CommunicationMessage.parse(msg) }
				when (parseResult.getOrNull()) {

					is Message.Shutdown -> {
						launchGUI {
							messageLog += message(
								"Disconnected ${clSock.inetAddress.hostAddress}",
								Color.CORAL,
								"Disconnected on client request"
							)
						}
						break
					}

					is Message.Request -> {
						val response = CommunicationMessage.respond()
						launchGUI {
							messageLog += message(
								"Responding ${clSock.inetAddress.hostAddress}",
								Color.CORNFLOWERBLUE,
								"Sending timestamp: ${getRichDatetime(response.RESPOND?.timestamp ?: -1)}"
							)
						}
						out.printDelimited(response.encode())
					}

					else -> {
						launchGUI {
							messageLog += message(
								"Read ${clSock.inetAddress.hostAddress}",
								Color.DARKGOLDENROD,
								"Received invalid message from client: '$msg'"
							)
						}
					}
				}
			}
		} catch (e: Exception) {
			launchGUI {
				messageLog += message(
					"IO Error ${clSock.inetAddress.hostAddress}",
					Color.INDIANRED,
					e.message ?: e.toString()
				)
				messageLog += message(
					"Disconnected ${clSock.inetAddress.hostAddress}",
					Color.INDIANRED,
					"Lost connection with client"
				)
				ExceptionDialog(e).apply {
					title = "IO Error for client ${clSock.inetAddress.hostAddress}"
				}
					.showAndWait()
				quit()
			}
		} finally {
			launchGUI {
				interfacesState[ni]?.let { nis ->
					nis.clientConnections.removeIf { a -> a.hostAddress == clSock.inetAddress.hostAddress }
				}
			}
			clSock.close()
		}
	}
}
