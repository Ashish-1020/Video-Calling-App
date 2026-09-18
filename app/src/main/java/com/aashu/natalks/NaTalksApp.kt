package com.aashu.natalks

import android.app.Application
import com.aashu.natalks.di.AppContainer

class NaTalksApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
