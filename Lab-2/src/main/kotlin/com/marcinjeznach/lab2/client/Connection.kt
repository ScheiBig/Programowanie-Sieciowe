package com.marcinjeznach.lab2.client

import com.marcinjeznach.javafx.message
import com.marcinjeznach.utils.*
import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.FXCollections
import javafx.scene.Node
import javafx.scene.paint.Color
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.Charset

/**
 * Allows client to connect to server via TCP/IP.
 */
@OptIn(DelicateCoroutinesApi::class) // Single component application, uses GlobalScope
class Connection {
	private var _socket: Socket? = null
	private var _disconnecting = true

	private val _mux = Mutex()

	/**
	 * Observable list of nodes to populate main text pane with.
	 *
	 * Used as log with nicely formatted messages.
	 */
	val log = FXCollections.observableArrayList<Node>()!!

	private var _lostConnection = SimpleBooleanProperty(false)

	/**
	 * Bindable property used to signal from deeply nested coroutines, that
	 * socket connection was lost.
	 */
	val lostConnection: ReadOnlyBooleanProperty get() = _lostConnection

	/**
	 * Establishes connection with specified address. Returns [Deferred] that awaits to result of this
	 * operation, which evaluates to `Unit` placeholder.
	 *
	 * It is assumed, that this function should be called when created [Connection] object,
	 * or disconnected and reusing it.
	 */
	fun connect(host: String, port: Int) = GlobalScope.asyncIOResult {
		_mux.withLock {
			if (_socket == null) {
				_socket = Socket()
			}
			try {
				_socket!!.connect(InetSocketAddress(host, port))
				GlobalScope.launchGUI { // Message builds nodes immediately, which means it is required
					log += message(     // to run on JavaFX thread.
						"Connection accepted",
						Color.DARKGREEN,
						"connected to server ${_socket!!.inetAddress}:${_socket!!.port}"
					)
				}
				_disconnecting = false
				_lostConnection.value = false
			} catch (e: Throwable) {
				_socket = null
				throw e
			}
		}

		GlobalScope.launch {
			try {
				val inn = _socket!!.getInputStream()
				while (isActive) {
					val buf = ArrayList<Byte>(0x8 shl 10)
					var char = inn.read()
					val ifEOF = {
						if (char == -1) {
							throw IOException("End of input stream - connection might be terminated")
						}
					}
					ifEOF()
					while (char != ETX.code) { // We use ETX character as delimiter of messages. Such simple
						ifEOF()                // protocol is necessary, as queued connections might send multiple
						buf += char.toByte()   // messages, that would be squished together.
						char = inn.read()
					}
					val msg = buf.toByteArray().toString(Charset.defaultCharset())
					GlobalScope.launchGUI {
						log += message("Read", Color.DARKGOLDENROD, msg)
					}
				}
			} catch (e: Exception) {
				if (!_disconnecting) {
					GlobalScope.launchGUI {
						log += message(
							"Connection lost",
							Color.INDIANRED,
							"connection terminated by server"
						)
					}
					_socket?.close()
					_socket = null
					_lostConnection.value = true
				}
			}
		}
	}

	/**
	 * Disconnects from endpoint. Returns [Deferred] that awaits to result of this operation,
	 * which evaluates to `Unit` placeholder.
	 *
	 * Connection might be reused after this operation.
	 */
	fun disconnect() = GlobalScope.asyncIOResult {
		_mux.withLock {
			_disconnecting = true
			try {
				_socket?.close()
				GlobalScope.launchGUI {
					log += message(
						"Disconnected from server",
						Color.INDIANRED,
						"connection terminated by client"
					)
				}
			} finally {
				_socket = null
			}
		}
	}

	@Throws(java.io.IOException::class)
	fun postMessage(msg: String) {
		log += message("Sent", Color.SEAGREEN, msg)
		if (_socket?.getOutputStream() == null) {
			throw IOException("Socket, or its output stream is closed")
		}
		val out = _socket!!.getOutputStream()
		out.write("$msg$ETX".toByteArray())
	}
}
