@file:Suppress("UNCHECKED_CAST")

package io.github.mainyf.questextension.config

import io.github.mainyf.newmclib.config.BaseLang
import io.github.mainyf.newmclib.config.asDefaultSlotConfig
import io.github.mainyf.newmclib.config.asMenuSettingsConfig
import io.github.mainyf.questextension.QuestExtension
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration

fun CommandSender.sendLang(key: String, vararg data: Any) {
    ConfigQE.lang.apply {
        sendLang(key, *data)
    }
}

object ConfigQE {

    private lateinit var mainConfigFile: FileConfiguration
    private lateinit var menuConfigFile: FileConfiguration
    private lateinit var langConfigFile: FileConfiguration

    var tutorialQuest = "tutorial"
    val questPool = mutableSetOf<String>()
    lateinit var questListMenuConfig: QuestListMenuConfig
    lateinit var questDetailMenuConfig: QuestDetailMenuConfig
    lateinit var questDetail2XMenuConfig: QuestDetail2XMenuConfig
    lateinit var questDetail3XMenuConfig: QuestDetail3XMenuConfig


    lateinit var lang: BaseLang

    fun load() {
        QuestExtension.INSTANCE.saveDefaultConfig()
        QuestExtension.INSTANCE.reloadConfig()

        kotlin.runCatching {
            val menuFile = QuestExtension.INSTANCE.dataFolder.resolve("menu.yml")
            if (!menuFile.exists()) {
                QuestExtension.INSTANCE.saveResource("menu.yml", false)
            }
            val langFile = QuestExtension.INSTANCE.dataFolder.resolve("lang.yml")
            if (!langFile.exists()) {
                QuestExtension.INSTANCE.saveResource("lang.yml", false)
            }
            mainConfigFile = QuestExtension.INSTANCE.config
            menuConfigFile = YamlConfiguration.loadConfiguration(menuFile)
            langConfigFile = YamlConfiguration.loadConfiguration(langFile)
            lang = BaseLang()

            loadMenuConfig()
            loadMainConfig()
            lang.load(langConfigFile)
        }.onFailure {
            QuestExtension.LOGGER.info("加载配置时出现错误")
            it.printStackTrace()
        }
    }

    private fun loadMenuConfig() {
        val questListMenuSect = menuConfigFile.getConfigurationSection("questListMenu")!!
        questListMenuConfig = QuestListMenuConfig(
            questListMenuSect.asMenuSettingsConfig(),
            questListMenuSect.asDefaultSlotConfig("quest1Slot"),
            questListMenuSect.asDefaultSlotConfig("quest2Slot"),
            questListMenuSect.asDefaultSlotConfig("quest3Slot"),
            questListMenuSect.asDefaultSlotConfig("quest4Slot"),
            questListMenuSect.asDefaultSlotConfig("quest5Slot"),
            questListMenuSect.asDefaultSlotConfig("rewardSlot"),
            questListMenuSect.asDefaultSlotConfig("backSlot")
        )
        val questDetailMenuSect = menuConfigFile.getConfigurationSection("questDetailMenu")!!
        questDetailMenuConfig = QuestDetailMenuConfig(
            questDetailMenuSect.asMenuSettingsConfig(),
            questDetailMenuSect.asDefaultSlotConfig("fsSlot"),
            questDetailMenuSect.asDefaultSlotConfig("xypSlot"),
            questDetailMenuSect.asDefaultSlotConfig("moneySlot"),
            questDetailMenuSect.asDefaultSlotConfig("rewardSlot"),
            questDetailMenuSect.asDefaultSlotConfig("backSlot")
        )
        val questDetail2XMenuSect = menuConfigFile.getConfigurationSection("questDetail2XMenu")!!
        questDetail2XMenuConfig = QuestDetail2XMenuConfig(
            questDetail2XMenuSect.asDefaultSlotConfig("step1Slot"),
            questDetail2XMenuSect.asDefaultSlotConfig("step2Slot")
        )
        val questDetail3XMenuSect = menuConfigFile.getConfigurationSection("questDetail3XMenu")!!
        questDetail3XMenuConfig = QuestDetail3XMenuConfig(
            questDetail3XMenuSect.getString("background")!!,
            questDetail3XMenuSect.asDefaultSlotConfig("step1Slot"),
            questDetail3XMenuSect.asDefaultSlotConfig("step2Slot"),
            questDetail3XMenuSect.asDefaultSlotConfig("step3Slot"),
        )
    }

    private fun loadMainConfig() {
        tutorialQuest = mainConfigFile.getString("tutorialQuest", tutorialQuest)!!
        this.questPool.clear()
        this.questPool.addAll(mainConfigFile.getStringList("questPool"))
    }

}