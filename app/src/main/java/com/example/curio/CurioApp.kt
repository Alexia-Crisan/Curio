package com.example.curio

import android.app.Application
import com.example.curio.di.AppContainer

class CurioApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer()
    }
}
