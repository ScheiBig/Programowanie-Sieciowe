package com.marcinjeznach.udp

import com.marcinjeznach.utils.ETX
import com.marcinjeznach.utils.runIO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.delay
import java.io.IOException
import java.net.*

private fun String.getPacket(group: InetAddress, port: Int) = "$this$ETX".toByteArray()
	.let {
		DatagramPacket(it, it.size, group, port)
	}

private fun InetAddress.socketAddress(port: Int = 0) = InetSocketAddress(this, port)


private val BroadcastAddr = InetAddress.getByName("255.255.255.255")

fun broadcast(msg: String, port: Int) = runCatching {
	DatagramSocket().use { s ->
		s.broadcast = true
		s.setOption(StandardSocketOptions.IP_MULTICAST_LOOP, false)

		s.send(msg.getPacket(BroadcastAddr, port))
	}
}

fun multicast(msg: String, addr: InetAddress, port: Int) = runCatching {
	MulticastSocket().use { s ->
		s.timeToLive = 1
		s.broadcast = false
		s.setOption(StandardSocketOptions.IP_MULTICAST_LOOP, false)
		s.joinGroup(addr.socketAddress(port), NetworkInterface.getByName("lo0"))

		s.send(msg.getPacket(addr, port))
	}
}

data class Message(
	val origin: String,
	val msg: String,
)

typealias MessageChannel = ReceiveChannel<Result<Message>>

data class UDPListenerState(
	val comm: MessageChannel,
	val close: () -> Unit,
)

fun listen(group: InetAddress, port: Int, nInterface: NetworkInterface) = runCatching {
	val comm = Channel<Result<Message>>(Channel.UNLIMITED)
	var listening = true

	val socket = MulticastSocket(port).apply {
		joinGroup(group.socketAddress(port), nInterface)
		reuseAddress = true
		setOption(StandardSocketOptions.IP_MULTICAST_LOOP, false)
	}
	val res = UDPListenerState(comm) {
		listening = false
		socket.leaveGroup(group.socketAddress(), nInterface)
		socket.close()
		comm.close()
	}

	val msgBuilder = StringBuilder()
	val buffer = ByteArray(1024)

	runIO {
		while (listening) try {
			val packet = DatagramPacket(buffer, buffer.size)
			socket.receive(packet)
			val rawMsg = String(buffer, 0, packet.length)
			if (rawMsg.contains(ETX)) {
				val messages = rawMsg.split(ETX)
				for (msg in messages.dropLast(1)) {
					msgBuilder.append(msg)
					comm.send(Result.success(Message(
						"${packet.address.hostAddress}:${packet.port}",
							msgBuilder.toString(),
					)))
					msgBuilder.clear()
				}
				msgBuilder.append(messages.last())
			} else {
				msgBuilder.append(rawMsg)
			}
		} catch (e: IOException) {
			if (listening) {
				comm.send(Result.failure(e))
			}
		}
	}

	return@runCatching res
}

