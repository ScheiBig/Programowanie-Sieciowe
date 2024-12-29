package com.marcinjeznach.lab2.server

import com.marcinjeznach.utils.Result
import javafx.collections.ObservableList
import javafx.scene.Node
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job

/**
 * Allows server to listen for Socket requests and process them.
 */
interface Server {

	/**
	 * Observable list of nodes to populate main text pane with.
	 *
	 * Used as log with nicely formatted messages.
	 */
	val log: ObservableList<Node>

	/**
	 * Starts listening for new connections in separate coroutine.
	 *
	 * This method is also responsible for launching coroutines that are processing
	 * client communication.
	 */
	fun listen(port: Int): Deferred<Result<Deferred<Unit>>>

	/**
	 * Stops listening for new connections and terminates immediately all current connections.
	 */
	fun disobey(): Deferred<Result<Job>>
}
