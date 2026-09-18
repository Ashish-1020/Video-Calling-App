package com.aashu.natalks.data.local

import java.util.concurrent.ConcurrentHashMap

class ContactNameCache {
    private val names = ConcurrentHashMap<String, String>()

    fun putAll(idToUsername: Map<String, String>) {
        names.putAll(idToUsername)
    }

    fun nameFor(userId: String): String = names[userId] ?: userId
}
