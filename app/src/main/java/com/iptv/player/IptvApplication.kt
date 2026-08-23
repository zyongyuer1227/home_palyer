package com.iptv.player

import android.app.Application
import com.iptv.player.data.local.FileLogger
import com.iptv.player.data.local.ServerStore
import com.iptv.player.data.repo.RecentRepo
import com.iptv.player.data.repo.ServerRepo
import com.iptv.player.player.PlaybackController

class IptvApplication : Application() {

    lateinit var serverRepo: ServerRepo
        private set

    override fun onCreate() {
        super.onCreate()
        FileLogger.init(this)
        PlaybackController.init(this)
        RecentRepo.init(this)
        serverRepo = ServerRepo(ServerStore(this))
        FileLogger.i("App", "app started, version=1.0.0")
    }
}
