package com.childprotect.app

enum class Command {
    LOCATE, DATA_ON, DATA_OFF, LOC_ON, RING, STOP, LOCK, LOCKDOWN, UNLOCKDOWN, STATUS, HELP, NONE
}

object CommandRouter {
    const val MIN_CODE_LEN = 4

    fun parse(body: String, code: String): Command {
        val c = code.trim()
        if (c.length < MIN_CODE_LEN || !containsToken(body, c)) return Command.NONE
        val rest = stripToken(body, c).trim().uppercase()
        return when {
            rest.contains("UNLOCKDOWN") -> Command.UNLOCKDOWN
            rest.contains("LOCKDOWN") -> Command.LOCKDOWN
            rest.contains("DATA OFF") || rest.contains("DATAOFF") -> Command.DATA_OFF
            rest.contains("DATA ON") || rest.contains("DATAON") || rest == "DATA" -> Command.DATA_ON
            rest.contains("LOC ON") || rest.contains("LOCON") -> Command.LOC_ON
            rest == "LOCK" || rest.contains(" LOCK ") -> Command.LOCK
            rest.contains("STOP") || rest.contains("SILENCE") -> Command.STOP
            rest.contains("RING") || rest.contains("ALARM") -> Command.RING
            rest.contains("STATUS") -> Command.STATUS
            rest.contains("HELP") -> Command.HELP
            else -> Command.LOCATE
        }
    }

    fun hasToken(body: String, token: String): Boolean = containsToken(body, token)
    private fun containsToken(body: String, token: String): Boolean =
        Regex("(^|\\s)" + Regex.escape(token) + "($|\\s)", RegexOption.IGNORE_CASE).containsMatchIn(body)
    private fun stripToken(body: String, token: String): String =
        Regex("(^|\\s)" + Regex.escape(token) + "($|\\s)", RegexOption.IGNORE_CASE).replaceFirst(body, " ")
}
