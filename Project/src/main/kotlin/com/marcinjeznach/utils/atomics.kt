package com.marcinjeznach.utils

import java.util.concurrent.atomic.AtomicBoolean

operator fun AtomicBoolean.invoke(value: Boolean? = null) = if (value == null) this.get()
else this.getAndSet(value)
