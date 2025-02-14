package com.marcinjeznach.project.client

import com.marcinjeznach.javafx.message
import com.marcinjeznach.networking.retrieveAvailableMulticastInterfaces
import com.marcinjeznach.project.common.*
import com.marcinjeznach.utils.*
import javafx.collections.ObservableList
import javafx.collections.ObservableSet
import javafx.scene.Node
import javafx.scene.paint.Color
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.controlsfx.dialog.ExceptionDialog
import java.net.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val CLOSED_BY_CLIENT = ">>> ----- Closed by client ----- <<<"

class ConnectionManager(
	private val messageLog: ObservableList<Node>,
	private val availableConnections: ObservableSet<InetSocketAddress>,
) {
	private val shouldDiscover = AtomicBoolean(true)
	private var discoverAlert = CompletableDeferred<Unit>()

	private val addresses = mutableSetOf<InetSocketAddress>()
	private val adrMux = Mutex()

	private var connection: Socket? = null
	private var connectionDaemon: Job? = null

	private val interfaces = retrieveAvailableMulticastInterfaces(GROUP, PORT)

	private lateinit var _connectionLost: () -> Unit
	fun attachConnectionLostCallback(connectionLostCallback: () -> Unit) {
		_connectionLost = connectionLostCallback
	}

	private lateinit var _indicateOffers: (Boolean) -> Unit
	fun attachOfferIndicator(indicateOffers: (Boolean) -> Unit) {
		_indicateOffers = indicateOffers
	}

	private val discoveryDaemon = runIO {
		var mulSock: MulticastSocket? = null
		val socketAddress = InetSocketAddress(InetAddress.getByName(GROUP), PORT)
		var ni: NetworkInterface? = null
		try {
			mulSock = MulticastSocket(PORT).apply {
				ni =
					interfaces.await()
						.firstOrNull { ni -> ni.name.startsWith("en") }
						?: interfaces.await()
							.random()

				joinGroup(socketAddress, ni)
				setOption(StandardSocketOptions.SO_REUSEADDR, true)
				setOption(StandardSocketOptions.IP_MULTICAST_LOOP, false)
			}
			val reader = launchIO {
				val msgBuilder = StringBuilder()
				val buffer = ByteArray(1024)
				launchGUI {
					messageLog += message(
						"Discover", Color.SEAGREEN, "Launching discover\n@ $GROUP:$PORT"
					)
				}
				while (isActive) {
					if (!shouldDiscover()) {
						discoverAlert.await()
					}
					bufferMessage@ while (isActive) {
						val packet = DatagramPacket(buffer, buffer.size)
						mulSock.receive(packet)
						if (!shouldDiscover()) continue
						val rawMsg = String(buffer, 0, packet.length)
						if (rawMsg.contains(ETX)) {
							val messages = rawMsg.split(ETX)
							for (msg in messages.dropLast(1)) {
								msgBuilder.append(msg)
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
					when (val of = parseResult.getOrNull()) {

						is Message.Offer -> {
							adrMux.withLock {
								addresses.add(InetSocketAddress(of.address, of.port))
							}
							launchGUI {
								messageLog += message(
									"Offer", Color.MEDIUMPURPLE, "${of.address}:${of.port}"
								)
							}
						}

						is Message.Discover -> {
							continue
						}

						else -> {
							launchGUI {
								messageLog += message(
									"Read UDP",
									Color.DARKGOLDENROD,
									"Received invalid message from server\n'$msg'"
								)
							}
						}
					}
				}
			}
			val writer = launchIO {
				val discover = CommunicationMessage.discover()
					.encode()
					.plus(ETX)
					.toByteArray()
				while (isActive) {
					if (!shouldDiscover()) {
						_indicateOffers(false)
						discoverAlert.await()
						launchGUI {
							messageLog += message(
								"Discover", Color.SEAGREEN, "Launching discover\n@ $GROUP:$PORT"
							)
						}
					}

					mulSock.send(
						DatagramPacket(
							discover,
							discover.size,
							socketAddress,
						)
					)
					launchGUI { _indicateOffers(true) }
					delay(3.seconds)
					if (shouldDiscover()) launchGUI {
						adrMux.withLock {
							availableConnections.addAll(addresses)
							addresses.clear()
							if (availableConnections.isEmpty()) {
								messageLog += message(
									"No offers",
									Color.GOLDENROD,
									"No responses from anyone",
								)
							}
						}
					}
					launchGUI { _indicateOffers(false) }
					delay(7.seconds)
				}
			}

			listOf(reader, writer).joinAll()
		} catch (e: Exception) {
			launchGUI {
				messageLog += message(
					"IO Error @ discovery", Color.INDIANRED, e.message ?: e.toString()
				)
				_connectionLost()
				ExceptionDialog(e).apply {
					title = "IO Error for discovery daemon"
				}
					.showAndWait()
			}
		} finally {
			mulSock?.leaveGroup(socketAddress, ni)
		}
	}

	fun toggleConnection(address: InetSocketAddress?): Deferred<Boolean> = runAsyncIO {
		if (!connection.isTransferable && address == null) {
			launchGUI {
				messageLog += message(
					"Failed precondition",
					Color.CORAL,
					"Requested disconnecting,\nbut connection is offline"
				)
			}
			return@runAsyncIO false
		}
		if (connection.isTransferable && address != null) {
			launchGUI {
				messageLog += message(
					"Failed precondition",
					Color.CORAL,
					"Requested connecting to ${address},\nbut already connected to\n${connection?.inetAddress}"
				)
			}
			return@runAsyncIO false
		}

		if (address == null) {
			connectionDaemon?.cancel(/*CancellationException(CLOSED_BY_CLIENT)*/)
			connection?.getOutputStream()
				?.let {
					DelimitedPrintWriter(it, "$ETX").printDelimited(
						CommunicationMessage.shutdown()
							.encode()
					)
				}
			connection?.close()
			launchGUI {
				messageLog += message(
					"Disconnecting", Color.ORANGE, "..."
				)
			}

			discoverAlert.complete(Unit)
			shouldDiscover(true)

			return@runAsyncIO true
		}

		launchGUI {
			messageLog += message(
				"Connecting", Color.CADETBLUE, "..."
			)
		}

		val con = runCatching { Socket(address.address, address.port) }
		if (con.isFailure) {
			launchGUI {
				val e = con.exceptionOrNull()
				messageLog += message(
					"Error", Color.INDIANRED, e?.message ?: e.toString()
				)
			}
			return@runAsyncIO false
		}

		connection = con.getOrThrow()
		launchGUI {
			messageLog += message(
				"Connected", Color.CADETBLUE, "${address.address.hostAddress}:${address.port}:"
			)
		}

		discoverAlert = CompletableDeferred()
		shouldDiscover(false)
		Config.lastServer = address

		return@runAsyncIO true
	}

	fun toggleCommunication(freq: String?): Deferred<Boolean> = runAsyncIO {
		if (!connectionDaemon.isPerformed && freq == null) {
			launchGUI {
				messageLog += message(
					"Failed precondition",
					Color.CORAL,
					"Requested hangup,\nbut communication is offline"
				)
			}
			return@runAsyncIO false
		}
		if (connectionDaemon.isPerformed && freq != null) {
			launchGUI {
				messageLog += message(
					"Failed precondition",
					Color.CORAL,
					"Requested communication,\nbut already established"
				)
			}
			return@runAsyncIO false
		}
		if (!connection.isTransferable && freq != null) {
			launchGUI {
				messageLog += message(
					"Failed precondition",
					Color.CORAL,
					"Requested communication,\nbut connection is offline"
				)
			}
			return@runAsyncIO false
		}

		if (freq == null) {
			connectionDaemon?.cancel(/*CancellationException(CLOSED_BY_CLIENT)*/)
			launchGUI {
				messageLog += message(
					"Hanging up", Color.ORANGE, "..."
				)
			}
			return@runAsyncIO true
		}

		launchGUI {
			messageLog += message(
				"Establishing communication", Color.CADETBLUE, "..."
			)
		}

		connectionDaemon = GlobalScope.launch(Dispatchers.IO) {
			val sock = requireNotNull(connection)
			try {
				val inp = DelimitedBufferedReader(sock.getInputStream(), "$ETX")
				val out = DelimitedPrintWriter(sock.getOutputStream(), "$ETX")
				while (isActive) {
					val then = getCurrentTimestamp()
					out.printDelimited(
						CommunicationMessage.request()
							.encode()
					)

					val response = inp.readDelimited()
					if (response == null) {
						launchGUI {
							messageLog += message(
								"IO Error",
								Color.INDIANRED,
								"No message received from socket"
							)
							messageLog += message(
								"Disconnected",
								Color.DARKORANGE,
								"Lost connection with server"
							)
						}
						launchGUI { _connectionLost() }
						shouldDiscover(true)
						discoverAlert.complete(Unit)
						break
					}
					val parseResult = runCatching { CommunicationMessage.parse(response) }

					when (val msg = parseResult.getOrNull()) {

						is Message.Shutdown -> {
							launchGUI {
								messageLog += message(
									"Disconnected",
									Color.CORAL,
									"Disconnected on server request"
								)
							}
							break
						}

						is Message.Respond -> {
							val (timestamp) = msg
							val now = getCurrentTimestamp()
							val delta = timestamp + (now - then) / 2 - now

							launchGUI {
								messageLog += message(
									"Δ ${
										delta.toString()
											.padStart(6)
									}ms",
									Color.CORNFLOWERBLUE,
									getRichDatetime(now)
								)
							}
						}

						else -> {
							launchGUI {
								messageLog += message(
									"Read",
									Color.DARKGOLDENROD,
									"Received invalid message from server:\n'$response'"
								)
							}
						}
					}
					delay(freq.toInt().milliseconds)
				}
			} catch (e: Exception) {
				launchGUI {
					messageLog += message(
						"IO Error",
						Color.INDIANRED,
						e.message ?: e.toString()
					)
					messageLog += message(
						"Disconnected",
						Color.INDIANRED,
						"Lost connection with server"
					)
					_connectionLost()
					ExceptionDialog(e).apply {
						title = "IO Error for server"
					}
						.showAndWait()
				}
			} catch (e: CancellationException) {
				println("Y?")
			}
		}

		return@runAsyncIO true
	}

	fun dispose() {
		discoveryDaemon.cancel()
		connection?.getOutputStream()
		connection?.close()
		connectionDaemon?.cancel()
	}
}

private val Job?.isPerformed get() = this != null && this.isActive
private val Socket?.isTransferable get() = this != null && !this.isClosed && this.isConnected
