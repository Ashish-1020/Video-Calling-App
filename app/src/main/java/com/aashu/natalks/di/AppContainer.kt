package com.aashu.natalks.di

import android.content.Context
import com.aashu.natalks.call.CallController
import com.aashu.natalks.call.CallNotificationHelper
import com.aashu.natalks.data.local.ContactNameCache
import com.aashu.natalks.data.local.SessionHolder
import com.aashu.natalks.data.local.TokenStore
import com.aashu.natalks.data.remote.AuthInterceptor
import com.aashu.natalks.data.remote.NaTalksApi
import com.aashu.natalks.data.repository.AuthRepository
import com.aashu.natalks.data.repository.ContactsRepository
import com.aashu.natalks.data.repository.IceServerRepository
import com.aashu.natalks.data.signaling.SignalingClient
import com.aashu.natalks.data.signaling.addSignalMessageAdapter
import com.aashu.natalks.util.NetworkConfig
import com.aashu.natalks.webrtc.WebRtcClient
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class AppContainer(private val appContext: Context) {

    val moshi: Moshi = Moshi.Builder()
        .addSignalMessageAdapter()
        .add(KotlinJsonAdapterFactory())
        .build()

    val sessionHolder = SessionHolder()
    val tokenStore = TokenStore(appContext)
    val contactNameCache = ContactNameCache()

    private val authInterceptor = AuthInterceptor(sessionHolder)

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(NetworkConfig.BASE_HTTP_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val naTalksApi: NaTalksApi = retrofit.create(NaTalksApi::class.java)

    val authRepository = AuthRepository(naTalksApi, tokenStore, sessionHolder)
    val contactsRepository = ContactsRepository(naTalksApi)
    val iceServerRepository = IceServerRepository(naTalksApi)

    val signalingClient = SignalingClient(okHttpClient, moshi, NetworkConfig.WS_URL)

    val webRtcClient = WebRtcClient(appContext)

    val callNotificationHelper = CallNotificationHelper(appContext)

    val callController = CallController(
        appContext = appContext,
        webRtcClient = webRtcClient,
        signalingClient = signalingClient,
        iceServerRepository = iceServerRepository,
        sessionHolder = sessionHolder,
        contactNameCache = contactNameCache
    )
}
