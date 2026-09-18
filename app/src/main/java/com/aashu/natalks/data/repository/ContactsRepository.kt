package com.aashu.natalks.data.repository

import com.aashu.natalks.data.remote.NaTalksApi
import com.aashu.natalks.data.remote.dto.UserSummaryDto

data class Contact(val id: String, val username: String, val online: Boolean)

class ContactsRepository(private val api: NaTalksApi) {

    suspend fun listContacts(): List<Contact> =
        api.listUsers().map { it.toContact() }

    suspend fun isOnline(userId: String): Boolean =
        api.isOnline(userId)["online"] ?: false

    private fun UserSummaryDto.toContact() = Contact(id, username, online)
}
