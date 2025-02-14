package com.marcinjeznach.networking

import com.marcinjeznach.utils.runAsyncIO
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.NetworkInterface

fun retrieveAvailableMulticastInterfaces(group: String, port: Int) = runAsyncIO {

	val potentialInterfaces = NetworkInterface.networkInterfaces()
		.filter{ it.isUp}
		.filter{ it.supportsMulticast() }
		.filter { !it.isPointToPoint }
		.filter { !it.isLoopback }
		.filter { it.ipv4 != null }
		.toList()

	val multicastInterfaces = potentialInterfaces.filter { ni ->
		runCatching {
			val where = InetSocketAddress(group, port)
			MulticastSocket(port).apply {
				joinGroup(where, ni)
				leaveGroup(where, ni)
			}
		}.isSuccess
	}

	return@runAsyncIO multicastInterfaces
}
