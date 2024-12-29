package com.marcinjeznach.lab2.server.single;

import com.marcinjeznach.javafx.message
import com.marcinjeznach.javafx.quit
import com.marcinjeznach.lab2.server.Server
import com.marcinjeznach.utils.ETX
import com.marcinjeznach.utils.asyncIO
import com.marcinjeznach.utils.asyncIOResult
import com.marcinjeznach.utils.launchGUI
import javafx.collections.FXCollections
import javafx.scene.Node
import javafx.scene.paint.Color
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.controlsfx.dialog.ExceptionDialog
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.Charset

@OptIn(DelicateCoroutinesApi::class) // Single component application, uses GlobalScope
class Server : Server {

	private var _serverSocket: ServerSocket? = null
	private var _port: Int = -1
	private var _clientSocket: Socket? = null
	private var _disconnecting = true

	private val _mux = Mutex()

	override val log = FXCollections.observableArrayList<Node>()!!

	override fun listen(port: Int) = GlobalScope.asyncIOResult {
		_mux.withLock {
			if (_serverSocket == null) {
				_serverSocket = ServerSocket(port)
				_port = port
				GlobalScope.launchGUI {
					log += message(
						"Server started", Color.YELLOWGREEN, "listening on port $_port"
					)
				}
			}
		}

		GlobalScope.asyncIO {
			try {
				var clientNo = 0
				while (isActive) {
					val socket: Socket
					try {
						socket = _serverSocket!!.accept()
						_clientSocket = socket
						clientNo++
						GlobalScope.launchGUI {
							log += message(
								"Connection accepted #$clientNo",
								Color.DARKGREEN,
								"client from ${socket.inetAddress}:${socket.port}"
							)
						}
					} catch (e: Exception) {
						if (!_disconnecting) {
							GlobalScope.launchGUI {
								val dialog = ExceptionDialog(e)
								dialog.title = "Error while accepting client connection"
								dialog.showAndWait()
								quit()
							}
						}
						throw e
					}
					try {
						while (true) {
							val inn = socket.getInputStream()
							val buf = ArrayList<Byte>(0x8 shl 10)
							var char = inn.read()
							val ifEOF = {
								if (char == -1) {
									throw IOException("End of input stream - connection might be terminated")
								}
							}
							ifEOF()
							while (char != ETX.code) {
								ifEOF()
								buf += char.toByte()
								char = inn.read()
							}
							buf += ETX.code.toByte()
							val rawMsg = buf.toByteArray()
							val msg = rawMsg.toString(Charset.defaultCharset())
								.dropLast(1)
							GlobalScope.launchGUI {
								GlobalScope.launchGUI {
									log += message(
										"Message #$clientNo", Color.CADETBLUE, msg
									)
								}
							}
							socket.getOutputStream()
								.write(rawMsg)
						}
					} catch (_: Exception) {
						GlobalScope.launchGUI {
							log += message(
								"Connection terminated #$clientNo",
								Color.INDIANRED,
								"reached end of stream from client or encountered communication error"
							)
						}
					}
				}
			} catch (e: Exception) {
				if (!_disconnecting) {
					GlobalScope.launchGUI {
						val dialog = ExceptionDialog(e)
						dialog.title = "Server error"
						dialog.showAndWait()
						quit()
					}
				}
			}
		}
	}

	override fun disobey() = GlobalScope.asyncIOResult {
		_mux.withLock {
			_disconnecting = true
			try {
				_serverSocket?.close()
				_clientSocket?.close()
				GlobalScope.launchGUI {
					log += message(
						"Server stopped", Color.ORANGERED, "no longer listening on port $_port"
					)
				}
			} catch (exception: IOException) {
				GlobalScope.launchGUI {
					log += message(
						"Server stopped", Color.RED, "couldn't close server gracefully"
					)
				}
			} finally {
				_serverSocket = null
				_clientSocket = null
				_port = 1
			}
		}
	}
}

