package com.aashu.natalks.data.repository

import com.aashu.natalks.data.remote.NaTalksApi
import org.webrtc.PeerConnection

class IceServerRepository(private val api: NaTalksApi) {

    suspend fun fetchIceServers(): List<PeerConnection.IceServer> =
        api.getIceServers().iceServers.map { entry ->
            val builder = PeerConnection.IceServer.builder(entry.urls)
            if (entry.username != null && entry.credential != null) {
                builder.setUsername(entry.username).setPassword(entry.credential)
            }
            builder.createIceServer()
        }
}
