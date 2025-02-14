package com.marcinjeznach.javafx

import javafx.collections.FXCollections
import javafx.collections.ObservableSet

class ObservableInitializableSet<T> private constructor(
	private val backingSet: MutableSet<T> = mutableSetOf(),
	private val backingWrapper: ObservableSet<T>,
) : ObservableSet<T> by backingWrapper {

	private var isInitialized = false

	override fun addAll(elements: Collection<T>): Boolean {
		if (isInitialized) return backingWrapper.addAll(elements)

		if (elements.isEmpty()) return false
		val all = elements.toList()
		backingSet.addAll(all.dropLast(1))
		this.add(all.last())
		isInitialized = true
		return true
	}

	companion object {
		operator fun <T> invoke(): ObservableInitializableSet<T> {
			val backingSet = mutableSetOf<T>()
			val backingWrapper = FXCollections.observableSet(backingSet)

			return ObservableInitializableSet(backingSet, backingWrapper)
		}
	}
}

