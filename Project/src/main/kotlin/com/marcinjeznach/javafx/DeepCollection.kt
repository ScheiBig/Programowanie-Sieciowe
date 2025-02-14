package com.marcinjeznach.javafx

import javafx.beans.Observable
import javafx.beans.binding.ObjectBinding
import javafx.collections.MapChangeListener
import javafx.collections.ObservableMap


open class DeepCollection<K, V, O, R>(
	private val map: ObservableMap<K, V>,
	private val valueToObsCol: (V) -> O,
	private val entryToRepr: (K, V) -> List<R>,
) : ObjectBinding<List<R>>(), MapChangeListener<K, V>
where O : Collection<Any>, O : Observable {

	init {
		bind(map)
		map.addListener(this)
		map.forEach { (_, v) -> bind(valueToObsCol(v)) }
	}

	open fun prefix(): List<R> = listOf()
	open fun suffix(): List<R> = listOf()

	override fun computeValue() = prefix() + map.flatMap { (k, v) -> entryToRepr(k, v) } + suffix()

	override fun onChanged(change: MapChangeListener.Change<out K, out V>) {
		when  {
			change.wasAdded() -> bind(valueToObsCol(change.valueAdded))
			change.wasRemoved() -> unbind(valueToObsCol(change.valueAdded))
		}
	}
}
