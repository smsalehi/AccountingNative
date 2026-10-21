package com.myaccounting.app.domain

/**
 * Tiny dependency-free JSON reader/writer (pure Kotlin so the domain layer is unit-testable on a plain JVM).
 * Parsed values: Map<String, Any?>, List<Any?>, String, Double, Boolean, null.
 */
object Json {
    class JsonException(message: String) : Exception(message)

    fun parse(text: String): Any? {
        val p = Parser(text.removePrefix("\uFEFF"))
        p.skipWs()
        val v = p.readValue()
        p.skipWs()
        if (!p.atEnd()) throw JsonException("Unexpected trailing data at ${p.pos}")
        return v
    }

    private class Parser(val s: String) {
        var pos = 0
        fun atEnd() = pos >= s.length
        fun skipWs() { while (pos < s.length && s[pos].isWhitespace()) pos++ }
        fun readValue(): Any? {
            if (atEnd()) throw JsonException("Unexpected end")
            return when (val c = s[pos]) {
                '{' -> readObject()
                '[' -> readArray()
                '"' -> readString()
                't' -> literal("true", true)
                'f' -> literal("false", false)
                'n' -> literal("null", null)
                else -> if (c == '-' || c.isDigit()) readNumber() else throw JsonException("Unexpected '$c' at $pos")
            }
        }
        fun literal(word: String, v: Any?): Any? {
            if (!s.startsWith(word, pos)) throw JsonException("Bad literal at $pos")
            pos += word.length
            return v
        }
        fun readNumber(): Double {
            val st = pos
            if (s[pos] == '-') pos++
            while (pos < s.length && (s[pos].isDigit() || s[pos] in ".eE+-")) pos++
            return s.substring(st, pos).toDoubleOrNull() ?: throw JsonException("Bad number at $st")
        }
        fun readString(): String {
            pos++ // opening quote
            val sb = StringBuilder()
            while (true) {
                if (atEnd()) throw JsonException("Unterminated string")
                val c = s[pos++]
                when (c) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (atEnd()) throw JsonException("Bad escape")
                        when (val e = s[pos++]) {
                            '"' -> sb.append('"'); '\\' -> sb.append('\\'); '/' -> sb.append('/')
                            'b' -> sb.append('\b'); 'f' -> sb.append('\u000C'); 'n' -> sb.append('\n')
                            'r' -> sb.append('\r'); 't' -> sb.append('\t')
                            'u' -> {
                                if (pos + 4 > s.length) throw JsonException("Bad unicode escape")
                                sb.append(s.substring(pos, pos + 4).toInt(16).toChar()); pos += 4
                            }
                            else -> throw JsonException("Bad escape \\$e")
                        }
                    }
                    else -> sb.append(c)
                }
            }
        }
        fun readArray(): List<Any?> {
            pos++
            val out = ArrayList<Any?>()
            skipWs()
            if (pos < s.length && s[pos] == ']') { pos++; return out }
            while (true) {
                skipWs(); out.add(readValue()); skipWs()
                if (atEnd()) throw JsonException("Unterminated array")
                val c = s[pos++]
                if (c == ']') return out
                if (c != ',') throw JsonException("Expected ',' at ${pos - 1}")
            }
        }
        fun readObject(): Map<String, Any?> {
            pos++
            val out = LinkedHashMap<String, Any?>()
            skipWs()
            if (pos < s.length && s[pos] == '}') { pos++; return out }
            while (true) {
                skipWs()
                if (atEnd() || s[pos] != '"') throw JsonException("Expected key at $pos")
                val k = readString(); skipWs()
                if (atEnd() || s[pos] != ':') throw JsonException("Expected ':' at $pos")
                pos++; skipWs()
                out[k] = readValue(); skipWs()
                if (atEnd()) throw JsonException("Unterminated object")
                val c = s[pos++]
                if (c == '}') return out
                if (c != ',') throw JsonException("Expected ',' at ${pos - 1}")
            }
        }
    }

    fun stringify(v: Any?): String = StringBuilder().also { write(it, v) }.toString()

    private fun write(sb: StringBuilder, v: Any?) {
        when (v) {
            null -> sb.append("null")
            is String -> writeString(sb, v)
            is Boolean -> sb.append(v)
            is Int -> sb.append(v)
            is Long -> sb.append(v)
            is Double -> sb.append(numberText(v))
            is Map<*, *> -> {
                sb.append('{'); var first = true
                for ((k, value) in v) { if (!first) sb.append(','); first = false; writeString(sb, k.toString()); sb.append(':'); write(sb, value) }
                sb.append('}')
            }
            is List<*> -> {
                sb.append('['); var first = true
                for (x in v) { if (!first) sb.append(','); first = false; write(sb, x) }
                sb.append(']')
            }
            else -> throw JsonException("Unsupported type ${v::class}")
        }
    }

    /** Integral values are written without ".0" (like JS JSON.stringify). Non-finite → null (as JS). */
    fun numberText(d: Double): String {
        if (d.isNaN() || d.isInfinite()) return "null"
        if (d == Math.rint(d) && Math.abs(d) < 1e15) return d.toLong().toString()
        return java.math.BigDecimal(d.toString()).stripTrailingZeros().toPlainString()
    }

    private fun writeString(sb: StringBuilder, s: String) {
        sb.append('"')
        for (c in s) when {
            c == '"' -> sb.append("\\\"")
            c == '\\' -> sb.append("\\\\")
            c == '\n' -> sb.append("\\n")
            c == '\r' -> sb.append("\\r")
            c == '\t' -> sb.append("\\t")
            c < ' ' -> sb.append(String.format("\\u%04x", c.code))
            else -> sb.append(c)
        }
        sb.append('"')
    }
}
