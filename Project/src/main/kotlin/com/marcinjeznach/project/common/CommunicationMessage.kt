@file:Suppress("PropertyName")

package com.marcinjeznach.project.common

import com.marcinjeznach.utils.getCurrentTimestamp
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@ExperimentalSerializationApi
private val json = Json {
	explicitNulls = false
}

@Serializable
data class CommunicationMessage(
	val DISCOVER: Message.Discover? = null,
	val OFFER: Message.Offer? = null,
	val REQUEST: Message.Request? = null,
	val RESPOND: Message.Respond? = null,
	val SHUTDOWN: Message.Shutdown? = null,
) {

	companion object {
		fun discover() = CommunicationMessage(DISCOVER = Message.Discover())
		fun offer(address: String, port: Int) = CommunicationMessage(OFFER = Message.Offer(
			address,
			port
		)
		)
		fun request() = CommunicationMessage(REQUEST = Message.Request())
		fun respond(timestamp: Long = getCurrentTimestamp()) =
			CommunicationMessage(RESPOND = Message.Respond(timestamp))

		fun shutdown() = CommunicationMessage(SHUTDOWN = Message.Shutdown())

		fun parse(allegedMessage: String) =
			json.decodeFromString<CommunicationMessage>(allegedMessage)
				.run {
					when {
						this.DISCOVER != null -> this.DISCOVER
						this.OFFER != null -> this.OFFER
						this.REQUEST != null -> this.REQUEST
						this.RESPOND != null -> this.RESPOND
						this.SHUTDOWN != null -> this.SHUTDOWN
						else -> null
					}
				}
	}

	fun encode() = json.encodeToString(this)

}

sealed interface Message {
	@Serializable
	data class Discover(
		val version: Long = 1,
	) : Message

	@Serializable
	data class Offer(
		val address: String,
		val port: Int,
		val version: Long = 1,
	) : Message

	@Serializable
	data class Request(
		val version: Long = 1,
	) : Message

	@Serializable
	data class Respond(
		val timestamp: Long,
		val version: Long = 1,
	) : Message

	@Serializable
	data class Shutdown(
		val version: Long = 1,
	) : Message
}

