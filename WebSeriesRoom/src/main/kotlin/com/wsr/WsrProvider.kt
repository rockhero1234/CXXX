package com.wsr

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class WsrProvider: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(Wsr())
    }
}