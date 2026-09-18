package com.aashu.natalks.data.remote.dto

data class IceServerEntryDto(
    val urls: List<String>,
    val username: String? = null,
    val credential: String? = null
)

data class IceServersResponseDto(val iceServers: List<IceServerEntryDto>)
