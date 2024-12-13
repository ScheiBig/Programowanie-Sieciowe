package com.marcinjeznach.exc_4

import com.marcinjeznach.ansi.clr
import com.marcinjeznach.ansi.fg
import com.marcinjeznach.ansi.fmt
import com.marcinjeznach.tui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jline.terminal.Terminal
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

val help = buildString {
	append(fg.code(166)["    e"])
	appendLine(fg.code(244)[" launch application"])
	append(fg.code(166)[" ←↓↑→"])
	appendLine(fg.code(244)[" move selection"])
	append(fg.code(166)["space"])
	appendLine(fg.code(244)[" kill task"])
	append(fg.code(166)["    q"])
	appendLine(fg.code(244)[" quit"])
}


const val A = 'A'.code
const val Z = 'Z'.code
const val ArrowUp = 0x1b5b41
const val ArrowDown = 0x1b5b42
const val ArrowRight = 0x1b5b43
const val ArrowLeft = 0x1b5b44
const val arrU = ArrowUp and 0xff
const val arrD = ArrowDown and 0xff
const val arrR = ArrowRight and 0xff
const val arrL = ArrowLeft and 0xff

fun main() = runBlocking<Unit> {

	lateinit var terminal: Terminal
	lateinit var jobs: Jobs
	try {
		// In asynchronous version of program, each job is replaced with
		// asynchronous generator pattern – iterator that returns asynchronous
		// handles (`Deferred` in Kotlin, `Future` / `Promise` in other languages).
		// Additionally, special `ManualAdvanceIterator` (name invented by me,
		// that's why it's bad :D) is used – it returns the same element from `Iterator.next()`
		// and requires manual advancement (hence the name) to produce next element – this will
		// be important in `select` pattern below.
		jobs = Jobs((0..<10).map { i -> JobState(this, i, Status.Stopped) })
		terminal = initConsole(help, jobs::toString)

		// Handling keyboard input will also be wrapped as generator for singular way
		// of handling events.
		val keys = KeyEmitter(CoroutineScope(Dispatchers.IO), terminal)
		val handleArrows: (Int) -> Unit = { key ->
			when (key) {
				ArrowUp, ArrowDown -> jobs.selection = (jobs.selection + 5) % 10
				ArrowLeft -> {
					val prevSel = jobs.selection
					jobs.selection = (jobs.selection + 9) % 10
					if ((jobs.selection < 5) != (prevSel < 5)) {
						jobs.selection = (jobs.selection + 5) % 10
					}
				}
				ArrowRight -> {
					val prevSel = jobs.selection
					jobs.selection = (jobs.selection + 11) % 10
					if ((jobs.selection < 5) != (prevSel < 5)) {
						jobs.selection = (jobs.selection + 5) % 10
					}
				}
			}
		}

		// Initial loop – waits for signal to start application, or quit.
		var quit = false
		keys.advance()
		for (key in keys) {
			when (val aKey = key.await()) {
				'q'.code -> {
					quit = true
					break
				}
				'e'.code -> break
				ArrowUp, ArrowDown, ArrowLeft, ArrowRight -> handleArrows(aKey)
			}
			terminal.termPrint("", jobs::toString)
			keys.advance()
		}

		// When initially advancing jobs, we add small delay, so they won't
		// be all printing at the same time (not a synchronization issue,
		// it just looks ugly).
		jobs.forEach { j ->
			j.advance()
			delay(50.milliseconds)
		}

		// Main loop.
		//
		// It is constructed in a way, that could resemble an event loop – it uses
		// `select` pattern, to select (like name suggests) first generator,
		// which has "next" element already awaited.
		// Because `next()` call is used to obtain `Deferred` job which produces
		// value to be printed, `ManualAdvanceIterator.advance()` must be called
		// in `onAwait` clause, so that consumed job can be thrown away and new one
		// could be produced. Without such pattern (with normal iterator), all jobs
		// that were not awaited first, would be thrown away – manual advancement
		// ensures that calling `next()` again will return same async job,
		// until it is actually consumed (advanced to next one).
		// This also means, that keyboard events are not dropped, but queued waiting
		// for selection.
		while (!quit) select {
			jobs.forEach { j ->
				if (j.status == Status.Offline) return@forEach
				j.next()
					.onAwait { msg ->
						terminal.termPrintln(msg, jobs::toString)
						j.advance()
					}
			}

			keys.next()
				.onAwait { key ->
					when (key) {
						'q'.code -> quit = true
						' '.code -> jobs.selected.cancel()
						ArrowUp, ArrowDown, ArrowLeft, ArrowRight -> handleArrows(key)
					}
					terminal.termPrint("", jobs::toString)
					keys.advance()
				}
		}

	} finally {
		// Cleanup - with small delay for nice effect of shutting down
		// of coroutines
		for (j in jobs) {
			j.cancel()
			delay(50.milliseconds)
			terminal.termPrint("", jobs::toString)
		}
		// Remove footer from stdout history
		terminal.print(clr.scr.toEnd)
		terminal.println(fg.code(207)["-*. Goodbye .*-"])
		terminal.flush()
		terminal.close()
	}
}

abstract class ManualAdvanceIterator<T : Any>() : Iterator<T> {
	protected lateinit var buffer: T

	override fun next() = buffer

	val isAvailable get() = ::buffer.isInitialized

	abstract fun advance()
}

class JobState(
	private val cs: CoroutineScope,
	private val i: Int,
	status: Status,
) : ManualAdvanceIterator<Deferred<String>>() {

	private var _status = status
	val status get() = _status
	private var j = -1

	override fun hasNext() = true

	override fun advance() {
		buffer = cs.async {
			delay(1.seconds)
			j = (j + 1) % (Z - A + 1)
			return@async "${(j + A).toChar()}$i"
		}
	}

	fun cancel() {
		_status = Status.Offline
	}
}

class KeyEmitter(
	private val cs: CoroutineScope,
	private val terminal: Terminal,
) : ManualAdvanceIterator<Deferred<Int>>() {

	override fun hasNext() = true

	override fun advance() {
		buffer = cs.async {
			lock.withLock {
				val key = terminal.getch()
				if (key != '\u001b'.code) return@async key
				if (terminal.getch() != '['.code) return@async -1
				when (terminal.getch()) {
					arrU -> ArrowUp
					arrD -> ArrowDown
					arrL -> ArrowLeft
					arrR -> ArrowRight
					else -> -1
				}
			}
		}
	}

	companion object {
		val lock = Mutex()
	}
}

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

//fun valueEmitter():