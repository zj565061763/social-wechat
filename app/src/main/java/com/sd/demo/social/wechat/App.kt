package com.sd.demo.social.wechat

import android.app.Application
import com.sd.lib.social.wechat.FSocialWechat

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        FSocialWechat.isDebug = true
        FSocialWechat.init(this, "wxdc1e388c3822c80b", "3baf1193c85774b3fd9d18447d76cab0")
    }
}