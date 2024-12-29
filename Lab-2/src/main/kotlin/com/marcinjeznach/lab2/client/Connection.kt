package com.marcinjeznach.lab2.client

import java.io.IOException
import java.lang.IllegalArgumentException
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.jvm.Throws

class Connection {

	private var host: String = ""
	private var port: Int = -1

	var socket: Socket? = null

	@Throws(IOException::class, IllegalArgumentException::class, SecurityException::class)
	fun bindAddress(host: String, port: Int) {
		if (socket == null) {
			socket = Socket()
		}
		socket!!.bind(InetSocketAddress(host, port))
	}

	fun connect() {
		if (this.socket != null && this.socket!!.isConnected) {
			return
		}
		this.socket = Socket()
	}

	@Throws(IOException::class)
	fun disconnect() {
		this.socket?.close()
		this.socket = null
	}
}
