package com.aashu.natalks.data.signaling

import android.util.Log
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

private const val TAG = "SignalingClient"
private const val SIGNAL_DESTINATION = "/app/call.signal"
private const val SIGNAL_SUBSCRIPTION = "/user/queue/call.signal"
private const val RECONNECT_DELAY_MS = 3000L

enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, FAILED }

class SignalingClient(
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi,
    private val wsUrl: String
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var webSocket: WebSocket? = null
    private val signalAdapter = moshi.adapter(SignalMessage::class.java)

    private var lastJwt: String? = null
    private var shouldStayConnected = false
    private var reconnectJob: Job? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _incomingSignals = MutableSharedFlow<SignalMessage>(extraBufferCapacity = 32)
    val incomingSignals: SharedFlow<SignalMessage> = _incomingSignals

    fun connect(jwt: String) {
        lastJwt = jwt
        shouldStayConnected = true
        reconnectJob?.cancel()
        if (_connectionState.value == ConnectionState.CONNECTED || _connectionState.value == ConnectionState.CONNECTING) {
            return
        }
        openSocket(jwt)
    }

    private fun openSocket(jwt: String) {
        _connectionState.value = ConnectionState.CONNECTING
        val request = Request.Builder().url(wsUrl).build()
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                val connectFrame = StompFrameCodec.encode(
                    command = "CONNECT",
                    headers = mapOf(
                        "accept-version" to "1.1,1.2",
                        "heart-beat" to "0,0",
                        "Authorization" to "Bearer $jwt"
                    )
                )
                ws.send(connectFrame)
            }

            override fun onMessage(ws: WebSocket, text: String) {
                val frame = StompFrameCodec.decode(text) ?: return
                when (frame.command) {
                    "CONNECTED" -> {
                        _connectionState.value = ConnectionState.CONNECTED
                        val subscribeFrame = StompFrameCodec.encode(
                            command = "SUBSCRIBE",
                            headers = mapOf(
                                "id" to "sub-call-signal",
                                "destination" to SIGNAL_SUBSCRIPTION
                            )
                        )
                        ws.send(subscribeFrame)
                    }
                    "MESSAGE" -> {
                        try {
                            signalAdapter.fromJson(frame.body)?.let { _incomingSignals.tryEmit(it) }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to parse incoming signal: ${frame.body}", e)
                        }
                    }
                    "ERROR" -> {
                        Log.e(TAG, "STOMP ERROR frame: ${frame.headers} ${frame.body}")
                        _connectionState.value = ConnectionState.FAILED
                        scheduleReconnect()
                    }
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure", t)
                _connectionState.value = ConnectionState.FAILED
                scheduleReconnect()
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                _connectionState.value = ConnectionState.DISCONNECTED
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        if (!shouldStayConnected) return
        val jwt = lastJwt ?: return
        if (reconnectJob?.isActive == true) return
        reconnectJob = scope.launch {
            delay(RECONNECT_DELAY_MS)
            if (shouldStayConnected) {
                openSocket(jwt)
            }
        }
    }

    fun sendSignal(signal: SignalMessage) {
        val json = signalAdapter.toJson(signal)
        val sendFrame = StompFrameCodec.encode(
            command = "SEND",
            headers = mapOf("destination" to SIGNAL_DESTINATION),
            body = json
        )
        webSocket?.send(sendFrame)
    }

    fun disconnect() {
        shouldStayConnected = false
        reconnectJob?.cancel()
        webSocket?.close(1000, "client closing")
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }
}
