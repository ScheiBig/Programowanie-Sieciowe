package com.marcinjeznach.utils

import java.io.*

class DelimitedBufferedReader(inputStream: InputStream, private val delimiter: String) :
	BufferedReader(inputStream.reader()) {

	private val buffer = StringBuilder()

	fun readDelimited(): String? {
		while (true) {
			synchronized(lock) {
				val ch = read()
				if (ch == -1 || ch.toChar() in delimiter) {
					if (buffer.isEmpty()) {
						return null
					}
					val remainder = buffer.toString()
					buffer.clear()
					return remainder
				}
				buffer.append(ch.toChar())
			}
		}
	}
}

class DelimitedPrintWriter(outputStream: OutputStream, private val delimiter: String) :
	PrintWriter(outputStream) {

	fun printDelimited(message: String) {
		print(message + delimiter)
		flush()
	}
}
