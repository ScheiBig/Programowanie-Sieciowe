package com.marcinjeznach.exc_2

import com.marcinjeznach.ansi.clr
import com.marcinjeznach.ansi.fg
import com.marcinjeznach.ansi.fmt
import com.marcinjeznach.tui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.jline.terminal.Terminal
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

val help = buildString {
	append(fg.code(166)[" ←↓↑→"])
	appendLine(fg.code(244)[" move selection"])
	append(fg.code(166)["space"])
	appendLine(fg.code(244)[" toggle task state"])
	append(fg.code(166)["    q"])
	appendLine(fg.code(244)[" quit"])
}

const val A = 'A'.code
const val Z = 'Z'.code
const val arrU = 'A'.code
const val arrD = 'B'.code
const val arrR = 'C'.code
const val arrL = 'D'.code

fun main() = runBlocking {
	lateinit var terminal: Terminal
	lateinit var jobs: Jobs
	try {
		jobs = Jobs((0..<10).map { i ->
			// Conflated channel will throw away old signal, if new one
			// is pushed in.
			val ch = Channel<RequestState>(Channel.CONFLATED)
			JobState(launch(Dispatchers.IO) {
				var j = 0
				// Loop infinitely while coroutine is running. In most scenarios,
				// cancellation is cooperative, done by polling this variable.
				while (isActive) {
					// Wait for new state request - if request is `Resume`
					// then it enters  inner loop that produces values.
					// If `Pause` signal is received, then inner loop breaks
					// and again suspends on channel.
					// Initially, there will be no signal, so coroutine will wait for start.
					var requestedState = ch.receive()
					while (requestedState == RequestState.Resume) {
						terminal.termPrintln(
							"${(j + A).toChar()}$i",
							jobs::toString,
						)
						j = (j + 1) % (Z - A + 1)

						delay(1.seconds)
						// If no new signal is available, then assume that
						// loop should continue.
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
				' '.code -> if (jobs.selected.status == Status.Running) {
					jobs.selected.pause()
				} else {
					jobs.selected.resume()
				}
				// Arrow keys are send as "\u001b[A" .. "\u00b1[D".
				// Escape character indicates that next two characters
				// should be read - "[", and a control sequence (we expect
				// same characters that are present in ansi.cur.{up|down|right|left}
				'\u001b'.code -> {
					if (terminal.getch() != '['.code) {
						continue
					}
					when (terminal.getch()) {
						arrU, arrD -> {
							jobs.selection = (jobs.selection + 5) % 10
						}
						arrL -> {
							val prevSel = jobs.selection
							jobs.selection = (jobs.selection + 9) % 10
							if ((jobs.selection < 5) != (prevSel < 5)) {
								jobs.selection = (jobs.selection + 5) % 10
							}
						}
						arrR -> {
							val prevSel = jobs.selection
							jobs.selection = (jobs.selection + 11) % 10
							if ((jobs.selection < 5) != (prevSel < 5)) {
								jobs.selection = (jobs.selection + 5) % 10
							}
						}
					}
				}
			}
			// Print "nothing" to update footer
			terminal.termPrint("", jobs::toString)
		}

	} finally {
		// Cleanup - with small delay for nice "waterfall" effect of shutting down
		// of coroutines.
		for (j in jobs) {
			j.cancelAndJoin()
			delay(50.milliseconds)
			terminal.termPrint("", jobs::toString)
		}
		// Remove footer from stdout history
		terminal.print(clr.scr.toEnd)
			.println(fg.code(207)["-*. Goodbye .*-"])
			.flush()
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
 * Container for all jobs – exposes iterator for initialization / finalization,
 * as well as selection of job in footer.
 *
 * @property jobs List of wrapped jobs.
 * @property selection Index of selected job.
 * @property selected Exposes selected element.
 */
class Jobs(
	private val jobs: List<JobState>,
	var selection: Int = 0,
) : Iterable<JobState> {
	operator fun get(idx: Int): JobState = jobs[idx]

	/**
	 * Prints jobs as footer status bar.
	 */
	override fun toString() = buildString {
		append(clr.ln.all)
		appendLine(fg.code(66)["-".repeat(12 * 5 - 2)])

		for ((i, t) in jobs.withIndex()) {
			if (i % 5 == 0 && i != 0) {
				appendLine()
				append(clr.ln.all)
			}
			when (t.status) {
				Status.Running -> append(fg.code(if (selection == i) 193 else 64))
				Status.Stopped -> append(fg.code(if (selection == i) 159 else 66))
				Status.Offline -> append(fg.code(if (selection == i) 203 else 131))
			}
			append("$i[${t.status}]  ")
		}

		append(fmt.reset)
	}

	override fun iterator() = jobs.iterator()

	val selected
		get() = jobs[selection]
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