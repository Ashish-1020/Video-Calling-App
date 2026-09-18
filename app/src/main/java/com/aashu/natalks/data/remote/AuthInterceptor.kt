package com.aashu.natalks.data.remote

import com.aashu.natalks.data.local.SessionHolder
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val sessionHolder: SessionHolder) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (original.url.encodedPath.startsWith("/api/auth/")) {
            return chain.proceed(original)
        }
        val token = sessionHolder.current()?.token ?: return chain.proceed(original)
        val authed = original.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(authed)
    }
}
