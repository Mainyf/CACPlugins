@file:Suppress("UNCHECKED_CAST")

package io.github.mainyf.toolsplugin.config

import io.github.mainyf.toolsplugin.ToolsPlugin

object ConfigManager {

    var recycleEnderDragonEgg = false

    fun load() {
        ToolsPlugin.INSTANCE.saveDefaultConfig()
        ToolsPlugin.INSTANCE.reloadConfig()
        val config = ToolsPlugin.INSTANCE.config
        recycleEnderDragonEgg = config.getBoolean("recycleEnderDragonEgg", recycleEnderDragonEgg)
    }

}