@file:Suppress("UNCHECKED_CAST")

package io.github.mainyf.socialsystem.config

import io.github.mainyf.newmclib.config.asDefaultSlotConfig
import io.github.mainyf.newmclib.config.asMenuSettingsConfig
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.socialsystem.SocialSystem
import io.github.mainyf.socialsystem.storage.StorageSS
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.permissions.Permissible
import java.util.UUID
import kotlin.math.floor

object ConfigSS {

    class CardItem(
        val priority: Int,
        val value: String,
        val hasNick: String
    )

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
    private var tabCardMap = mutableMapOf<String, CardItem>()
    private var chatCardDefault = ""
    private var chatCardMap = mutableMapOf<String, CardItem>()
    private var tagCardDefault = ""
    private var tagCardMap = mutableMapOf<String, CardItem>()
    private var colorCardDefault = ""
    private var colorCardMap = mutableMapOf<String, CardItem>()

    lateinit var nicknameConfig: NicknameConfig

    private val cardTimeMap = mutableMapOf<UUID, MutableMap<String, Long>>()

    fun load() {
        kotlin.runCatching {
            mainConfigFile = SocialSystem.INSTANCE.saveResourceToFileAsConfiguration("config.yml")
            menuConfigFile = SocialSystem.INSTANCE.saveResourceToFileAsConfiguration("menu.yml")
            langConfigFile = SocialSystem.INSTANCE.saveResourceToFileAsConfiguration("lang.yml")

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
            socialMainSect.asDefaultSlotConfig("tpIsland"),
            socialMainSect.asDefaultSlotConfig("nickname")
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
        tagCardDefault = mainConfigFile.getString("card.tag.default", "")!!.colored()
        colorCardDefault = mainConfigFile.getString("card.color.default", "")!!.colored()

        tabCardMap.clear()
        tabCardMap.putAll(mainConfigFile.getConfigurationSection("card.tab")!!.getCardMap())
        chatCardMap.clear()
        chatCardMap.putAll(mainConfigFile.getConfigurationSection("card.chat")!!.getCardMap())
        tagCardMap.clear()
        tagCardMap.putAll(mainConfigFile.getConfigurationSection("card.tag")!!.getCardMap())
        colorCardMap.clear()
        colorCardMap.putAll(mainConfigFile.getConfigurationSection("card.color")!!.getCardMap())
        val nicknameSect = mainConfigFile.getSection("nickname")
        nicknameConfig = NicknameConfig(
            nicknameSect.getString("permission")!!,
            nicknameSect.getInt("modifyCooldown"),
            nicknameSect.getInt("minLength"),
            nicknameSect.getInt("maxLength"),
            nicknameSect.getDouble("cost"),
            nicknameSect.getStringList("sensitiveWord"),
            nicknameSect.getLong("tipsPeriod"),
            nicknameSect.getAction("tipsAction"),
            nicknameSect.getAction("minLengthAction"),
            nicknameSect.getAction("maxLengthAction"),
            nicknameSect.getAction("chineseAction"),
            nicknameSect.getAction("sensitiveAction"),
            nicknameSect.getAction("quitAction"),
            nicknameSect.getAction("successAction"),
            nicknameSect.getAction("costLackAction"),
            nicknameSect.getAction("nicknameCooldownAction"),
            nicknameSect.getAction("repatNicknameAction")
        )
    }

    private fun ConfigurationSection.getCardMap(): Map<String, CardItem> {
        val rs = mutableMapOf<String, CardItem>()
        getKeys(false).forEach {
            if (it == "default") return@forEach
            val priority = getInt("${it}.priority")
            val value = getString("${it}.value")!!.colored()
            val hasNick = getString("${it}.hasNick", "")!!.colored()
            rs[it] = CardItem(priority, value, hasNick)
        }
        return rs
    }

    private fun getPlayerCard(player: Player, key: String, cardMap: Map<String, CardItem>, default: String): String {
        val cards = cardMap.filter { hasCardPermission(player, it.key) }
        if (cards.isEmpty()) {
            return default
        }
        val cItem = cards.values.minByOrNull {
            it.priority
        }!!
        val nickname = StorageSS.getNickname(player.uuid)
        return if (cItem.hasNick.isNotBlank() && nickname != null && StorageSS.hasVisibleNickname(player.uuid)) {
            val timeMap = cardTimeMap.getOrPut(player.uuid) { mutableMapOf() }
            val curTime = currentTime()
            val prevTime = timeMap.getOrPut(key) { curTime }
            val elapsedTime = curTime - prevTime
            if ((elapsedTime / 1000).toInt() % 2 == 1) {
                cItem.hasNick
            } else {
                cItem.value
            }
        } else {
            cItem.value
        }
    }

    fun getPlayerTabCard(player: Player): String {
        return getPlayerCard(player, "tab", tabCardMap, tabCardDefault)
    }

    fun getPlayerChatCard(player: Player): String {
        return getPlayerCard(player, "chat", chatCardMap, chatCardDefault)
    }

    fun getPlayerTagCard(player: Player): String {
        return getPlayerCard(player, "tag", tagCardMap, tagCardDefault)
    }

    fun getPlayerColorCard(player: Player): String {
        return getPlayerCard(player, "color", colorCardMap, colorCardDefault)
    }

    fun hasCardPermission(permissible: Permissible, cardName: String): Boolean {
        return permissible.hasPermission("${cardPermission}.${cardName}")
    }

}