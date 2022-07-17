@file:Suppress("UNCHECKED_CAST")

package io.github.mainyf.socialsystem.config

import io.github.mainyf.newmclib.config.asDefaultSlotConfig
import io.github.mainyf.newmclib.config.asItemSlotConfig
import io.github.mainyf.newmclib.config.asMenuSettingsConfig
import io.github.mainyf.socialsystem.SocialSystem
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration

object ConfigManager {

    private lateinit var mainConfigFile: FileConfiguration
    private lateinit var menuConfigFile: FileConfiguration
    private lateinit var langConfigFile: FileConfiguration

    lateinit var repairPermission: String
    var friendRequestCooldown: Long = 60L

    lateinit var socialCardMenuConfig: SocialCardMenuConfig
    lateinit var socialMainMenuConfig: SocialMainMenuConfig

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
        val socialCardSect = menuConfigFile.getConfigurationSection("socialCardMenu")!!
        socialCardMenuConfig = SocialCardMenuConfig(
            socialCardSect.asMenuSettingsConfig(),
            socialCardSect.asDefaultSlotConfig("requestSlot"),
            socialCardSect.asDefaultSlotConfig("repairSlot"),
            socialCardSect.asDefaultSlotConfig("headSlot"),
            socialCardSect.asDefaultSlotConfig("cardX1Slot"),
            socialCardSect.asDefaultSlotConfig("cardX2Slot"),
            socialCardSect.asDefaultSlotConfig("cardX3Slot"),
            socialCardSect.asDefaultSlotConfig("cardX4Slot"),
            socialCardSect.asDefaultSlotConfig("onlineSlot"),
            socialCardSect.asDefaultSlotConfig("helmetSlot"),
            socialCardSect.asDefaultSlotConfig("chestplateSlot"),
            socialCardSect.asDefaultSlotConfig("leggingsSlot"),
            socialCardSect.asDefaultSlotConfig("bootsSlot")
        )
        val socialMainSect = menuConfigFile.getConfigurationSection("socialMainMenu")!!
        socialMainMenuConfig = SocialMainMenuConfig(
            socialMainSect.asMenuSettingsConfig(),
            socialMainSect.asDefaultSlotConfig("prevSlot"),
            socialMainSect.asDefaultSlotConfig("nextSlot"),
            socialMainSect.asDefaultSlotConfig("friendsSlot"),
            socialMainSect.asDefaultSlotConfig("headSlot"),
            socialMainSect.asDefaultSlotConfig("cardX1Slot"),
            socialMainSect.asDefaultSlotConfig("cardX2Slot"),
            socialMainSect.asDefaultSlotConfig("cardX3Slot"),
            socialMainSect.asDefaultSlotConfig("cardX4Slot"),
            socialMainSect.asDefaultSlotConfig("onlineSlot"),
            socialMainSect.asDefaultSlotConfig("deleteFriendOrAllowRepairSlot"),
            socialMainSect.asDefaultSlotConfig("tpSlot")
        )
    }

    private fun loadMainConfig() {
        repairPermission = mainConfigFile.getString("repairPermission")!!
        friendRequestCooldown = mainConfigFile.getLong("friendRequestCooldown", 60L)
    }

//    private fun ConfigurationSection.asSocialOnlineSlot(key: String): SocialOnlineSlot {
//        return getConfigurationSection(key)!!.let {
//            SocialOnlineSlot(
//                it.getIntegerList("slot"),
//                it.getConfigurationSection("online")!!.asItemSlotConfig(),
//                it.getConfigurationSection("offline")!!.asItemSlotConfig()
//            )
//        }
//    }
//
//    private fun ConfigurationSection.asDeleteFriendOrAllowRepairSlot(key: String): DeleteFriendOrAllowRepairSlot {
//        return getConfigurationSection(key)!!.let {
//            DeleteFriendOrAllowRepairSlot(
//                it.getIntegerList("slot"),
//                it.getConfigurationSection("delete")!!.asItemSlotConfig(),
//                it.getConfigurationSection("allowRepair")!!.asItemSlotConfig()
//            )
//        }
//    }

}