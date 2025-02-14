package com.marcinjeznach.networking

import java.net.Inet4Address
import java.net.NetworkInterface

val NetworkInterface.ipv4: String?
	get() = this.inetAddresses()
		.filter { a -> a is Inet4Address }
		.toList()
		.firstOrNull()
		?.hostAddress
