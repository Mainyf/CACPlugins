package io.github.mainyf.mcrmbtools

import com.mcrmb.event.QrDoneEvent
import com.mcrmb.event.QrRequestEvent
import io.github.mainyf.newmclib.exts.pluginManager
import org.apache.logging.log4j.LogManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin

class McrmbTools : JavaPlugin(), Listener {

    companion object {

        val LOGGER = LogManager.getLogger("McrmbTools")

        lateinit var INSTANCE: McrmbTools

    }

    override fun onEnable() {
        INSTANCE = this
        pluginManager().registerEvents(this, this)
    }

    @EventHandler
    fun onQrDone(event: QrDoneEvent) {
//        println("qrdone: ${event.response}")
    }

}