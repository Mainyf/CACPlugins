package io.github.mainyf.itemenchantplus

import io.github.mainyf.itemenchantplus.config.ConfigIEP
import org.apache.logging.log4j.LogManager
import org.bukkit.plugin.java.JavaPlugin

class ItemEnchantPlus : JavaPlugin() {

    companion object {

        val LOGGER = LogManager.getLogger("ItemEnchantPlus")

        lateinit var INSTANCE: ItemEnchantPlus

    }

    override fun onEnable() {
        INSTANCE = this
        ConfigIEP.init()
    }
}