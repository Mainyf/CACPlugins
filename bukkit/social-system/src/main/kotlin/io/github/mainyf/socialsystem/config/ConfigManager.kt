@file:Suppress("UNCHECKED_CAST")

package io.github.mainyf.socialsystem.config

import io.github.mainyf.socialsystem.SocialSystem
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration

object ConfigManager {

    private lateinit var mainConfigFile: FileConfiguration
    private lateinit var menuConfigFile: FileConfiguration
    private lateinit var langConfigFile: FileConfiguration

    fun load() {
        SocialSystem.INSTANCE.saveDefaultConfig()
        SocialSystem.INSTANCE.reloadConfig()

        kotlin.runCatching {
            val menuFile = SocialSystem.INSTANCE.dataFolder.resolve("menu.yml")
            if (!menuFile.exists()) {
                SocialSystem.INSTANCE.saveResource("menu.yml", false)
            }
            val langFile = SocialSystem.INSTANCE.dataFolder.resolve("lang.yml")
            if (!langFile.exists()) {
                SocialSystem.INSTANCE.saveResource("lang.yml", false)
            }
            mainConfigFile = SocialSystem.INSTANCE.config
            menuConfigFile = YamlConfiguration.loadConfiguration(menuFile)
            langConfigFile = YamlConfiguration.loadConfiguration(langFile)

            loadMenuConfig()
            loadMainConfig()
            Lang.load(langConfigFile)
        }.onFailure {
            SocialSystem.LOGGER.info("加载配置时出现错误")
            it.printStackTrace()
        }
    }

    private fun loadMenuConfig() {

    }

    private fun loadMainConfig() {

    }
}