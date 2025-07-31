package com.myreviews.app

import android.app.Application
import com.myreviews.app.di.AppModule

class MyReviewsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppModule.initialize(this)
    }
}