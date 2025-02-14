package com.marcinjeznach.project.client

import java.net.InetSocketAddress
import java.util.prefs.Preferences

private const val LAST_SERVER = "last_server"

object Config {
	private val pref: Preferences = Preferences.userNodeForPackage(Main::class.java)
	var lastServer: InetSocketAddress?
		get() = pref.get(LAST_SERVER, null)
			?.let { v ->
				val (adr, port) = v.split(":")
				return@let InetSocketAddress(adr, port.toInt())
			}
		set(value) {
			if (value == null) {
				return pref.remove(LAST_SERVER)
			}
			val state = "${value.address.hostAddress}:${value.port}"
			pref.put(LAST_SERVER, state)
		}
}
