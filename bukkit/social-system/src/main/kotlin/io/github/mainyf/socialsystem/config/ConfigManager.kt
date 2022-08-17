@file:Suppress("UNCHECKED_CAST")

package io.github.mainyf.socialsystem.config

import io.github.mainyf.newmclib.config.asDefaultSlotConfig
import io.github.mainyf.newmclib.config.asItemSlotConfig
import io.github.mainyf.newmclib.config.asMenuSettingsConfig
import io.github.mainyf.newmclib.exts.colored
import io.github.mainyf.socialsystem.SocialSystem
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.permissions.Permissible

object ConfigManager {

    private lateinit var mainConfigFile: FileConfiguration
    private lateinit var menuConfigFile: FileConfiguration
    private lateinit var langConfigFile: FileConfiguration

    lateinit var repairPermission: String
    var friendRequestCooldown = 60L
    var repairCooldown = 60L
    var tpRequestCooldown = 60L
    var tpReqExpired = 20L
    var inviteTpExpired = 20L

    lateinit var socialCardMenuConfig: SocialCardMenuConfig
    lateinit var socialMainMenuConfig: SocialMainMenuConfig
    lateinit var socialIslandTPMenuConfig: SocialIslandTPMenuConfig

    private var cardPermission = "social.card"
    private var tabCardDefault = ""
    private var tabCardMap = mutableMapOf<String, Pair<Int, String>>()
    private var chatCardDefault = ""
    private var chatCardMap = mutableMapOf<String, Pair<Int, String>>()

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
            socialMainSect.getString("backgroundFriend")!!,
            socialMainSect.asDefaultSlotConfig("prevSlot"),
            socialMainSect.asDefaultSlotConfig("nextSlot"),
            socialMainSect.asDefaultSlotConfig("backSlot"),
            socialMainSect.asDefaultSlotConfig("friendsSlot"),
            socialMainSect.asDefaultSlotConfig("headSlot"),
            socialMainSect.asDefaultSlotConfig("cardX1Slot"),
            socialMainSect.asDefaultSlotConfig("cardX2Slot"),
            socialMainSect.asDefaultSlotConfig("cardX3Slot"),
            socialMainSect.asDefaultSlotConfig("cardX4Slot"),
            socialMainSect.asDefaultSlotConfig("onlineSlot"),
            socialMainSect.asDefaultSlotConfig("deleteSlot"),
            socialMainSect.asDefaultSlotConfig("allowRepairSlot"),
            socialMainSect.asDefaultSlotConfig("tpSlot"),
            socialMainSect.asDefaultSlotConfig("tpIsland")
        )
        val socialIslandTPSect = menuConfigFile.getConfigurationSection("socialIslandTPMenu")!!
        socialIslandTPMenuConfig = SocialIslandTPMenuConfig(
            socialIslandTPSect.asMenuSettingsConfig(),
            socialIslandTPSect.asDefaultSlotConfig("plot1Slot"),
            socialIslandTPSect.asDefaultSlotConfig("plot2Slot"),
            socialIslandTPSect.asDefaultSlotConfig("infoSlot"),
            socialIslandTPSect.asDefaultSlotConfig("backSlot")
        )
    }

    private fun loadMainConfig() {
        repairPermission = mainConfigFile.getString("repairPermission")!!
        friendRequestCooldown = mainConfigFile.getLong("friendRequestCooldown", friendRequestCooldown)
        repairCooldown = mainConfigFile.getLong("repairCooldown", repairCooldown)
        tpRequestCooldown = mainConfigFile.getLong("tpRequestCooldown", tpRequestCooldown)
        tpReqExpired = mainConfigFile.getLong("tpReqExpired", tpReqExpired)
        inviteTpExpired = mainConfigFile.getLong("inviteTpExpired", inviteTpExpired)

        cardPermission = mainConfigFile.getString("card.permission", "social.card")!!
        tabCardDefault = mainConfigFile.getString("card.tab.default", "")!!.colored()
        chatCardDefault = mainConfigFile.getString("card.chat.default", "")!!.colored()

        tabCardMap.clear()
        val tabSect = mainConfigFile.getConfigurationSection("card.tab")!!
        tabSect.getKeys(false).forEach {
            if (it == "default") return@forEach
            val priority = tabSect.getInt("${it}.priority")
            val value = tabSect.getString("${it}.value")!!.colored()
            tabCardMap[it] = priority to value
        }
        val chatSect = mainConfigFile.getConfigurationSection("card.chat")!!
        chatSect.getKeys(false).forEach {
            if (it == "default") return@forEach
            val priority = chatSect.getInt("${it}.priority")
            val value = chatSect.getString("${it}.value")!!.colored()
            chatCardMap[it] = priority to value
        }
    }

    fun getPlayerTabCard(player: Player): String {
        val cards = tabCardMap.filter { hasCardPermission(player, it.key) }
        if (cards.isEmpty()) {
            return tabCardDefault
        }
        return cards.values.minByOrNull {
            it.first
        }!!.second
    }

    fun getPlayerChatCard(player: Player): String {
        val cards = chatCardMap.filter { hasCardPermission(player, it.key) }
        if (cards.isEmpty()) {
            return chatCardDefault
        }
        return cards.values.minByOrNull {
            it.first
        }!!.second
    }

    fun hasCardPermission(permissible: Permissible, cardName: String): Boolean {
        return permissible.hasPermission("${cardPermission}.${cardName}")
    }

}