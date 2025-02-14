package com.marcinjeznach.project.server

import com.marcinjeznach.javafx.message
import com.marcinjeznach.javafx.quit
import com.marcinjeznach.networking.ipv4
import com.marcinjeznach.project.common.*
import com.marcinjeznach.project.common.GROUP
import com.marcinjeznach.project.common.PORT
import com.marcinjeznach.utils.ETX
import com.marcinjeznach.utils.launchGUI
import com.marcinjeznach.utils.launchIO
import com.marcinjeznach.utils.runIO
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import javafx.scene.Node
import javafx.scene.paint.Color
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import org.controlsfx.dialog.ExceptionDialog
import java.net.*
import kotlin.system.exitProcess


private val serverDaemons = mutableListOf<Job>()

private fun dispose() {
	serverDaemons.forEach(Job::cancel)
}

fun initializeMulticastListeners(
	datagramLog: ObservableList<Node>,
	interfacesState: ObservableMap<NetworkInterface, NetworkInterfaceStatus>,
	asyncListeners: Deferred<Unit>,
): () -> Unit {
	runIO {
		asyncListeners.await()

		for ((ni, stat) in interfacesState) {
			serverDaemons += launchIO {
				var mulSock: MulticastSocket? = null
				val socketAddress = InetSocketAddress(InetAddress.getByName(GROUP), PORT)
				try {
					mulSock = MulticastSocket(PORT).apply {
						println(ni)
						joinGroup(socketAddress, ni)
						setOption(StandardSocketOptions.SO_REUSEADDR, true)
						setOption(StandardSocketOptions.IP_MULTICAST_LOOP, false)
					}

					val msgBuilder = StringBuilder()
					val buffer = ByteArray(1024)

					launchGUI {
						datagramLog += message(
							"Listen ${ni.name}",
							Color.SEAGREEN,
							"Launching listener daemon\n@ $GROUP:$PORT"
						)
					}

					while (isActive) {
						var adr: InetSocketAddress? = null
						bufferMessage@ while (isActive) {
							val packet = DatagramPacket(buffer, buffer.size)
							mulSock.receive(packet)
							val rawMsg = String(buffer, 0, packet.length)
							if (rawMsg.contains(ETX)) {
								val messages = rawMsg.split(ETX)
								for (msg in messages.dropLast(1)) {
									msgBuilder.append(msg)
									adr = packet.socketAddress as? InetSocketAddress
									break@bufferMessage
								}
								msgBuilder.append(messages.last())
							} else {
								msgBuilder.append(rawMsg)
							}
						}
						val msg = msgBuilder.toString()
						msgBuilder.clear()

						val parseResult = runCatching { CommunicationMessage.parse(msg) }
						when (parseResult.getOrNull()) {

							is Message.Discover -> {
								launchGUI {
									datagramLog += message(
										"Offering $adr",
										Color.MEDIUMPURPLE,
										"Sending offer\n${ni.ipv4}:${stat.tcpPort}"
									)
								}
								val response = CommunicationMessage.offer(ni.ipv4!!, stat.tcpPort)
									.encode() + ETX
								val byteResp = response.toByteArray()
								mulSock.send(DatagramPacket(byteResp, byteResp.size, InetSocketAddress(adr!!.address, PORT)))
								println(response to mulSock)
							}

							is Message.Offer -> { continue }

							else -> {
								launchGUI {
									datagramLog += message(
										"Read ${ni.name}",
										Color.DARKGOLDENROD,
										"Received invalid message from client\n'$msg'"
									)
								}
							}
						}
					}
				} catch (e: Exception) {
					launchGUI {
						datagramLog += message(
							"IO Error ${ni.name}",
							Color.INDIANRED,
							e.message ?: e.toString()
						)
						ExceptionDialog(e).apply {
							title = "IO Error for interface ${ni.name}"
						}
							.showAndWait()
						quit()
					}
				} finally {
					mulSock?.leaveGroup(socketAddress, ni)
				}
			}
		}
	}
	return ::dispose
}
