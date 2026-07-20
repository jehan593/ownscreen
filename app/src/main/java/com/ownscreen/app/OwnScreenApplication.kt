package com.ownscreen.app

import android.app.Application
import com.ownscreen.app.di.AppContainer
import com.ownscreen.app.di.DefaultAppContainer
import com.ownscreen.app.widget.WidgetRefreshAlarmReceiver

class OwnScreenApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        WidgetRefreshAlarmReceiver.schedule(this)
    }
}
