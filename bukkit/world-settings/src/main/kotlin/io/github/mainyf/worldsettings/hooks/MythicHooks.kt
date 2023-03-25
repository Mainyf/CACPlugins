package io.github.mainyf.worldsettings.hooks

import io.github.mainyf.newmclib.exts.isPluginEnabled
import io.github.mainyf.newmclib.exts.pluginManager
import io.github.mainyf.worldsettings.WorldSettings
import io.github.mainyf.worldsettings.hooks.mythic.AllowPlayerCondition
import io.github.mainyf.worldsettings.hooks.mythic.IsPlayerAndOffPVPCondition
import io.lumine.mythic.bukkit.events.MythicConditionLoadEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

object MythicHooks {

    fun init() {
        if (!isPluginEnabled("MythicMobs")) {
            return
        }
        WorldSettings.INSTANCE.logger.info("检测到 MythicMobs 插件，初始化挂钩")
        pluginManager().registerEvents(MythicListener, WorldSettings.INSTANCE)
    }

    object MythicListener : Listener {

        @EventHandler
        fun onLoad(event: MythicConditionLoadEvent) {
            if (event.conditionName == "allowPlayer") {
                event.register(AllowPlayerCondition("${event.conditionName} ${event.argument}"))
            }
            if (event.conditionName == "isPlayerAndOffPVP") {
                event.register(IsPlayerAndOffPVPCondition("${event.conditionName} ${event.argument}"))
            }
        }

    }

}