package com.marcinjeznach

import org.jetbrains.annotations.Contract
import java.util.*


/**
 * Standard implementation of [Result] type.
 * @param <V> Type of value
 * @param <E> Type of error
</E></V> */
open class Res<V, E : Throwable?> private constructor(
	value: V?,
	error: E?,
) {
	private val `val`: V?
	private val err: E?

	fun `val`(): V? {
		return `val`
	}

	fun err(): E? {
		return err
	}

	init {
		this.`val` = value
		this.err = error
	}

	companion object {
		/**
		 * Creates new instance of result.
		 *
		 *
		 * `value` and `error` must be mutually exclusive in their mutability.
		 * @param value Value to wrap
		 * @param error Error to wrap
		 * @return New result
		 * @param <V> Type of value
		 * @param <E> Type of error
		 * @throws NullPointerException If both `value` and `error` are
		 * `null` or not-`null`
		</E></V> */
		@Contract("null, null -> fail; !null, !null -> fail")
		@Throws(NullPointerException::class)
		fun <V, E : Throwable?> of(
			value: V?,
			error: E?,
		): Result<V?, E?> {
			if ((value == null) == (error == null)) {
				throw NullPointerException(
					"When creating instance of `Res` result, exclusively " +
							"either value or error should be not-null"
				)
			}
			return Res<V?, E?>(value, error)
		}

		/**
		 * Creates successful result, that holds not-`null` value.
		 * @param value Value to be wrapped; must be not-null
		 * @return New result
		 * @param <V> Type of value
		</V> */
		fun <V> success(value: V): Result<V, Throwable?> {
			return Res<V, Throwable?>(value, null)
		}

		/**
		 * Creates successful result, that holds not-`null` error
		 * @param error Error to be wrapped; must be not-null
		 * @return New result
		 * @param <T> Type of Error
		</T> */
		fun <T : Throwable?> failure(error: T): Result<Any?, T> {
			return Res<Any?, T>(null, error)
		}

		/**
		 * Creates result, that wraps return values / errors produced by `call`.
		 *
		 *
		 * Rules of [Res::of][Res.of] apply to the `call`.
		 * @param call Code that returns / throws
		 * @return New result
		 * @param <V> Type of value
		</V> */
		fun <V> from(
			call: ThrowingSupplier<V>,
		): Result<V?, Throwable> {
			return try {
				success<V>(call.get())
			} catch (e: Throwable) {
				Res<Any?, Throwable>(null, e)
			}
		}

		/**
		 * Creates result, that wraps errors produced by `call`.
		 *
		 *
		 * Rules of [Res::of][Res.of] apply to the `call`.
		 * @param call Code that throws
		 * @return New result
		 */
		fun from(
			call: ThrowingRunnable,
		): Result<Void?, Throwable?> {
			return try {
				Res<Any?, Throwable?>(null, null)
			} catch (e: Throwable) {
				Res<Any?, Throwable?>(null, e)
			}
		}
	}
}
