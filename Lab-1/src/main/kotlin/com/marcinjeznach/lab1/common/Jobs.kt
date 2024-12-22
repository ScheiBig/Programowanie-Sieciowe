package com.marcinjeznach.lab1.common

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel


/**
 * Current status of [JobState] – it is only an information, that should
 * be manually synchronized with actual lifetime of [Job].
 */
enum class Status {
	/** Currently running and producing values. */
	Running,

	/** Currently stopped and ready for resuming. */
	Stopped,

	/** Cancelled and not resumable. */
	Offline,
}

/** Signal for requesting new coroutine status. */
enum class RequestState {
	/** Request resuming of paused job.  */
	Resume,

	/** Request pausing of running job. */
	Pause,
}


/**
 * Hold state of single Job "Thread".
 *
 * @property job Reference to [Job] of coroutine.
 * @property request Buffered single-item channel, that keeps only most recent
 *           value that was sent. Used to request pausing / resuming of coroutine.
 */
class JobState(
	private val job: Job,
	private val request: Channel<RequestState>,
	status: Status,
) {
	private var _status = status

	/** Cooperatively pauses execution of this job. */
	suspend fun pause() {
		_status = Status.Stopped
		request.send(RequestState.Pause)
	}

	/** Cooperatively resumes execution of this job. */
	suspend fun resume() {
		_status = Status.Running
		request.send(RequestState.Resume)
	}

	/** Cooperatively cancels execution of this job and waits for its cancellation to finish. */
	suspend fun cancelAndJoin() {
		job.cancelAndJoin()
		_status = Status.Offline
	}
}

/**
 * Job that acts as manual-advance generator.
 *
 * It implements iterator contract, however [next] method will keep returning same value,
 * without advancing further. Call to [advance] is required to produce new value.
 *
 * As per coroutine documentation, it is safe to await [next] multiple times, as next time it will
 * return produced value immediately.
 */
abstract class JobGenerator(
	protected val cs: CoroutineScope,
	status: Status,
) : Iterator<Deferred<String>> {
	private var _status = status
	protected lateinit var generatedValue: Deferred<String>

	/**
	 * Returns `true` if iteration has more elements.
	 *
	 * It is assumed, that iteration produces values until `cancel` is called. Implementations
	 * of [advance] can check this method to determine if next value should be produced.
	 * */
	override fun hasNext() = _status != Status.Offline

	/** Returns the current element in the iteration.*/
	override fun next() = generatedValue

	/** Produces the next element in the iteration. After this call, [next] will yield new value. */
	abstract fun advance()

	/**
	 * Cancels generation of values in this iteration.
	 */
	fun cancel() {
		_status = Status.Offline
	}
}
