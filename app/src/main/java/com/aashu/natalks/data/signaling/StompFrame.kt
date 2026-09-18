package com.aashu.natalks.data.signaling

private val NULL_BYTE = 0x00.toChar()

data class StompFrame(
    val command: String,
    val headers: Map<String, String>,
    val body: String
)

object StompFrameCodec {

    fun encode(command: String, headers: Map<String, String> = emptyMap(), body: String = ""): String {
        val sb = StringBuilder()
        sb.append(command).append('\n')
        headers.forEach { (key, value) -> sb.append(key).append(':').append(value).append('\n') }
        if (body.isNotEmpty()) {
            sb.append("content-length:").append(body.toByteArray(Charsets.UTF_8).size).append('\n')
        }
        sb.append('\n')
        sb.append(body)
        sb.append(NULL_BYTE)
        return sb.toString()
    }

    fun decode(raw: String): StompFrame? {
        val trimmed = raw.trim('\n', '\r').removeSuffix(NULL_BYTE.toString())
        if (trimmed.isBlank()) return null

        val lines = trimmed.split("\n")
        val command = lines.firstOrNull() ?: return null
        val headers = mutableMapOf<String, String>()
        var bodyStartIndex = lines.size

        for (i in 1 until lines.size) {
            val line = lines[i]
            if (line.isEmpty()) {
                bodyStartIndex = i + 1
                break
            }
            val separatorIndex = line.indexOf(':')
            if (separatorIndex > 0) {
                headers[line.substring(0, separatorIndex)] = line.substring(separatorIndex + 1)
            }
        }

        val body = if (bodyStartIndex < lines.size) {
            lines.subList(bodyStartIndex, lines.size).joinToString("\n")
        } else {
            ""
        }
        return StompFrame(command, headers, body)
    }
}
