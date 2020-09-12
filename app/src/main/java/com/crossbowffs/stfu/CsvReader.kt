package com.crossbowffs.stfu

import java.io.BufferedReader
import java.lang.StringBuilder

class CsvReader(private val reader: BufferedReader) {
    fun nextLine(): List<String>? {
        val columns = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        while (true) {
            val rawLine = reader.readLine()
            if (rawLine == null) {
                if (inQuotes) {
                    throw IllegalStateException("Malformed CSV: EOF reached in middle of quoted string")
                }
                return null
            }

            var i = 0
            while (i < rawLine.length) {
                val c = rawLine[i]
                if (!inQuotes && c == '"') {
                    if (sb.isEmpty()) {
                        inQuotes = true
                    } else {
                        throw IllegalStateException("Malformed CSV: Quote in middle of unquoted string")
                    }
                } else if (inQuotes && c == '"') {
                    val cc = if (i < rawLine.length - 1) { rawLine[i + 1] } else { null }
                    if (cc == null || cc == ',') {
                        inQuotes = false
                    } else if (inQuotes && c == '"' && cc == '"') {
                        sb.append('"')
                        i++
                    } else {
                        throw IllegalStateException("Malformed CSV: Unescaped quote in middle of quoted string")
                    }
                } else if (!inQuotes && c == ',') {
                    columns.add(sb.toString())
                    sb.clear()
                } else {
                    sb.append(c)
                }
                i++
            }

            if (inQuotes) {
                sb.append('\n')
            } else {
                if (sb.isNotEmpty() || columns.isNotEmpty()) {
                    columns.add(sb.toString())
                }
                return columns
            }
        }
    }
}
