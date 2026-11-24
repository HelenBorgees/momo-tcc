package com.example

import android.app.Application
import com.example.data.storage.BabyPhotoStorageManager

class MomoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        com.example.data.remote.LocalSetupTransport.initialize(this)
        BabyPhotoStorageManager.initialize(this)
    }
}
