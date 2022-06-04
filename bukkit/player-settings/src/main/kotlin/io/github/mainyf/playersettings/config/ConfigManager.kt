package io.github.mainyf.playersettings.config

import io.github.mainyf.playersettings.PlayerSettings
import org.bukkit.configuration.file.FileConfiguration

object ConfigManager {

    private lateinit var mainConfig: FileConfiguration

    lateinit var opWhiteList: List<String>

    fun load() {
        kotlin.runCatching {
            PlayerSettings.INSTANCE.saveDefaultConfig()
            PlayerSettings.INSTANCE.reloadConfig()
            mainConfig = PlayerSettings.INSTANCE.config

            opWhiteList = mainConfig.getStringList("opWhiteList")
        }.onFailure {
            PlayerSettings.INSTANCE.slF4JLogger.error("加载配置时出现错误", it)
        }
    }

}