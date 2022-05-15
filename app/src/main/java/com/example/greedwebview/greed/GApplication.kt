package com.example.greedwebview.greed

import android.app.Application
import android.util.Log

/**
 *
 * create time 2022/5/14 9:58 下午
 * create by 胡汉君
 */
class GApplication : Application() {


    override fun onCreate() {
        super.onCreate()
        Log.d(OdysManager.TAG, "onCreate $applicationContext")
        OdysManager.init(applicationContext)
        OdysManager.preCreate(1)
    }
}