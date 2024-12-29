package com.marcinjeznach.utils

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Discriminated union that holds result of operation that may throw an error.
 *
 * For best DX, type of result should not be nullable – type should be used for APIs
 * that explicitly produce errors when value is not available.
 */
@Suppress("PropertyName")
sealed class Result<T> {
	internal data class Value<T>(internal val _value: T) : Result<T>() {
		override val success get() = true
	}

	internal data class Error<T>(internal val _error: Throwable) : Result<T>() {
		override fun toString(): String {
			return buildString {
				append("Error(error=$_error")
				if (_error.cause != null) append(" /cause= ${_error.cause}")
				append(")")
			}
		}

		override val success get() = false
	}

	/** Returns wrapped value, assuming that result represents success. Errors otherwise. */
	val value get() = (this as Value)._value

	/** Returns wrapped value, assuming that result represents success, or `null` otherwise. */
	val valueOrNull get() = (this as? Value)?._value

	/** Returns wrapped error, assuming that result represents failure. Errors otherwise. */
	val error get() = (this as Error)._error

	/** Returns wrapped error, assuming that result represents failure or `null` otherwise. */
	val errorOrNull get() = (this as? Error)?._error

	/** Returns `true` for value result, `false` for error result.*/
	abstract val success: Boolean
}

/**
 * Wraps calculation into result.
 *
 * @see Result
 */
fun <T> result(of: () -> T): Result<T> {
	return try {
		Result.Value(of())
	} catch (e: Throwable) {
		Result.Error(e)
	}
}

suspend fun <T> susResult(of: suspend () -> T): Result<T> {
	return try {
		Result.Value(of())
	} catch (e: Throwable) {
		Result.Error(e)
	}
}

/** Produces success result. */
fun <T> success(of: T): Result<T> = Result.Value(of)

/** Produces failure result. */
fun <T> failure(of: Throwable): Result<T> = Result.Error(of)


/**
 * Produces [Deferred] that awaits to [Result].
 *
 * @see	async
 */
fun <T> CoroutineScope.asyncResult(
	context: CoroutineContext = EmptyCoroutineContext,
	start: CoroutineStart = CoroutineStart.DEFAULT,
	block: suspend CoroutineScope.() -> T
): Deferred<Result<T>> = this.async(context, start) {
	susResult {
		block()
	}
}

/**
 * Produces [Deferred] that awaits to [Result].
 *
 * Coroutine that calculates [block] is launched on [Dispatchers.IO] pool, ensuring that thread
 * starvation does not occur.
 *
 * @see	async
 */
fun <T> CoroutineScope.asyncIOResult(
	start: CoroutineStart = CoroutineStart.DEFAULT,
	block: suspend CoroutineScope.() -> T
): Deferred<Result<T>> = this.async(Dispatchers.IO, start) {
	susResult {
		block()
	}
}

