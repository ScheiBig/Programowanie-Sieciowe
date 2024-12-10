package com.marcinjeznach.exc_3

import com.marcinjeznach.ansi.clr
import com.marcinjeznach.ansi.fg
import com.marcinjeznach.ansi.fmt
import com.marcinjeznach.com.marcinjeznach.tui.*
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
	val thrPool = newFixedThreadPoolContext(10, "Exercise 2")
	val mutex = Mutex()

	lateinit var terminal: Terminal
	lateinit var jobs: Jobs
	try {
		jobs = Jobs(Array(10) { i ->
			val ch = Channel<RequestState>(Channel.CONFLATED)
			JobState(launch(thrPool) {
				var j = 1
				while (isActive) {
					var requestedState = ch.receive()
					while (requestedState == RequestState.Resume) {
						mutex.withLock {
							terminal.termPrintln("${(j + A).toChar()}$i", jobs::toString)
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
					if (j.status == Status.Stopped) {
						j.status = Status.Running
						j.request.send(RequestState.Resume)
					}
				}
			}
			mutex.withLock { terminal.termPrint("", jobs::toString) }
		}

	} finally {
		for (j in jobs) {
			j.job.cancelAndJoin()
			j.status = Status.Offline
			delay(50.milliseconds)
			mutex.withLock { terminal.termPrint("", jobs::toString) }
		}
		terminal.print(clr.scr.toEnd)
		terminal.println(fg.code(207)["-*. Goodbye .*-"])
		terminal.close()
	}
}

class JobState(
	val job: Job,
	val request: Channel<RequestState>,
	var status: Status,
)

class Jobs(
	private val tasks: Array<JobState>,
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
				Status.Running -> append(fg.code(64))
				Status.Stopped -> append(fg.code(66))
				Status.Offline -> append(fg.code(131))
			}
			append("$i[${t.status}]  ")
		}

		append(fmt.reset)
	}

	override fun iterator() = tasks.iterator()
}

enum class Status {
	Running, Stopped, Offline,
}

enum class RequestState {
	Resume, Pause,
}