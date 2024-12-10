package com.marcinjeznach.exc_2

import com.marcinjeznach.ansi.clr
import com.marcinjeznach.ansi.fg
import com.marcinjeznach.ansi.fmt
import com.marcinjeznach.com.marcinjeznach.tui.*
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
	// Create thread pool - same number of threads as coroutines,
	// to ensure that application will run just as if it were using
	// raw threads
	val thrPool = newFixedThreadPoolContext(10, "Exercise 2")

	lateinit var terminal: Terminal
	lateinit var jobs: Jobs
	try {
		jobs = Jobs((0..<10).map { i ->
			val ch = Channel<RequestState>(Channel.CONFLATED)
			JobState(launch(thrPool) {
				var j = 1
				while (isActive) {
					var requestedState = ch.receive()
					while (requestedState == RequestState.Resume) {
						terminal.termPrintln("${(j + A).toChar()}$i", jobs::toString)
						j = (j + 1) % (Z - A + 1)

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
				' '.code -> if (jobs.selected.status == Status.Running) {
					jobs.selected.status = Status.Stopped
					jobs.selected.request.send(RequestState.Pause)
				} else {
					jobs.selected.status = Status.Running
					jobs.selected.request.send(RequestState.Resume)
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
							if ((jobs.selection < 5) != (prevSel < 5)) jobs.selection =
								(jobs.selection + 5) % 10
						}
						arrR -> {
							val prevSel = jobs.selection
							jobs.selection = (jobs.selection + 11) % 10
							if ((jobs.selection < 5) != (prevSel < 5)) jobs.selection =
								(jobs.selection + 5) % 10
						}
					}
				}
			}
			// Print "nothing" to update footer
			terminal.termPrint("", jobs::toString)
		}

	} finally {
		// Cleanup - with small delay for nice effect of shutting down
		// of coroutines
		for (j in jobs) {
			j.job.cancelAndJoin()
			j.status = Status.Offline
			delay(50.milliseconds)
			terminal.termPrint("", jobs::toString)
		}
		// Remove footer from stdout history
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
	val job: Job,
	val request: Channel<RequestState>,
	var status: Status,
)

class Jobs(
	private val tasks: List<JobState>,
	var selection: Int = 0,
) : Iterable<JobState> {
	operator fun get(idx: Int): JobState = tasks[idx]

	override fun toString() = buildString {
		append(clr.ln.all)
		appendLine(fg.code(66)["-".repeat(12 * 5 - 2)])

		for ((i, t) in tasks.withIndex()) {
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

	override fun iterator() = tasks.iterator()

	val selected
		get() = tasks[selection]
}

enum class Status {
	Running, Stopped, Offline,
}

enum class RequestState {
	Resume, Pause,
}