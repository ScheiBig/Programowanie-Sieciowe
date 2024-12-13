package com.marcinjeznach.exc_3

import com.marcinjeznach.ansi.clr
import com.marcinjeznach.ansi.fg
import com.marcinjeznach.ansi.fmt
import com.marcinjeznach.tui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jline.terminal.Terminal
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

val help = buildString {
	append(fg.code(166)[" e"])
	appendLine(fg.code(244)[" launch application"])
	append(fg.code(166)[" q"])
	appendLine(fg.code(244)[" quit"])
}

const val A = 'A'.code
const val Z = 'Z'.code

fun main() = runBlocking {
	val mutex = Mutex()

	lateinit var terminal: Terminal
	lateinit var jobs: Jobs
	try {
		jobs = Jobs((0..<10).map { i ->
			val ch = Channel<RequestState>(Channel.CONFLATED)
			JobState(launch(Dispatchers.IO) {
				var j = 0
				while (isActive) {
					var requestedState = ch.receive()
					while (requestedState == RequestState.Resume) {
						// Creating critical section with only printing to terminal,
						// as waiting inside of it would cause issues with keyboard.
						// Due to the fact, that Writer.print(…) is usually an atomic
						// operation on JVM, this isn't really that necessary.
						mutex.withLock {
							terminal.termPrintln(
								"${(j + A).toChar()}$i",
								jobs::toString,
							)
							j = (j + 1) % (Z - A + 1)
						}

						delay(1.seconds)
						requestedState = ch.tryReceive()
							.getOrNull() ?: RequestState.Resume
					}
				}
			}, ch, Status.Stopped)
		})

		terminal = initConsole(help, jobs::toString)
		while (true) {
			when (terminal.getch()) {
				'q'.code -> break
				'e'.code -> jobs.forEach { j ->
					// When launching jobs, we add small delay, so they won't
					// be all printing at the same time (not a synchronization issue,
					// it just looks ugly).
					if (j.status == Status.Stopped) {
						j.resume()
						delay(50.milliseconds)
					}
				}
			}
			mutex.withLock { terminal.termPrint("", jobs::toString) }
		}

	} finally {
		for (j in jobs) {
			j.cancelAndJoin()
			delay(50.milliseconds)
			mutex.withLock { terminal.termPrint("", jobs::toString) }
		}
		terminal.print(clr.scr.toEnd)
		terminal.println(fg.code(207)["-*. Goodbye .*-"])
		terminal.close()
	}
}

/**
 * Hold state of single Job "Thread".
 *
 * @property job Reference to [Job] of coroutine.
 * @property request Buffered single-item channel, that keeps only most recent
 *           value that was sent. Used to request pausing / resuming of coroutine.
 * @property status
 */

class JobState(
	private val job: Job,
	private val request: Channel<RequestState>,
	status: Status,
) {
	private var _status = status
	val status get() = _status

	suspend fun pause() {
		_status = Status.Stopped
		request.send(RequestState.Pause)
	}

	suspend fun resume() {
		_status = Status.Running
		request.send(RequestState.Resume)
	}

	suspend fun cancelAndJoin() {
		job.cancelAndJoin()
		_status = Status.Offline
	}
}

/**
 * Container for all jobs – exposes iterator for initialization / finalization.
 *
 * @property jobs List of wrapped jobs.
 */

class Jobs(
	private val jobs: List<JobState>,
) : Iterable<JobState> {
	operator fun get(idx: Int): JobState = jobs[idx]

	override fun toString() = buildString {
		append(clr.ln.all)
		appendLine(fg.code(66)["-".repeat(12 * 5 - 2)])

		for ((i, t) in jobs.withIndex()) {
			if (i % 5 == 0 && i != 0) {
				appendLine()
				append(clr.ln.all)
			}
			when (t.status) {
				Status.Running -> append(fg.code(64))
				Status.Stopped -> append(fg.code(66))
				Status.Offline -> append(fg.code(131))
			}
			append("$i[${t.status}]  ")
		}

		append(fmt.reset)
	}

	override fun iterator() = jobs.iterator()
}


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

/**
 * Signal for requesting new coroutine status.
 */
enum class RequestState {
	/** Request resuming of paused job.  */
	Resume,
	/** Request pausing of running job. */
	Pause,
}