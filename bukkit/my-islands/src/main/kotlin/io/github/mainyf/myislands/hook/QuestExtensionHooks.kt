package io.github.mainyf.myislands.hook

import io.github.mainyf.newmclib.exts.pluginManager
import io.github.mainyf.questextension.QuestExtension
import org.bukkit.entity.Player

object QuestExtensionHooks {

    private var questExtensionEnable = false

    fun init() {
        questExtensionEnable = pluginManager().isPluginEnabled("QuestExtension")
    }

    fun hasInTutorialQuest(player: Player): Boolean {
        if (!questExtensionEnable) return false
        return QuestExtension.INSTANCE.hasInTutorialQuest(player)
    }

}