package com.marcinjeznach.utils

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val customFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

fun getCurrentTimestamp() = Instant.now()
	.toEpochMilli()

fun getRichDatetime(timestamp: Long) = customFormatter.format(
	LocalDateTime.ofInstant(
		Instant.ofEpochMilli(timestamp), ZoneOffset.UTC
	)
)!!
