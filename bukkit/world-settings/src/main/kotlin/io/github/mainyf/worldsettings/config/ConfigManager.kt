@file:Suppress("UNCHECKED_CAST")

package io.github.mainyf.worldsettings.config

import io.github.mainyf.newmclib.config.ActionParser
import io.github.mainyf.newmclib.config.action.MultiAction
import io.github.mainyf.newmclib.exts.colored
import org.bukkit.Difficulty
import org.bukkit.GameMode
import org.bukkit.GameRule
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.configuration.ConfigurationSection

object ConfigManager {

    var debug = false
    lateinit var ignorePermission: String
    lateinit var global: WorldSettingConfig
    val settingMap = mutableMapOf<String, WorldSettingConfig>()

    fun load(config: ConfigurationSection) {
        debug = config.getBoolean("debug", false)
        ignorePermission = config.getString("ignore-permission") ?: "worldsettings.admin"
        loadGlobalSetting(config.getConfigurationSection("global")!!)
        settingMap.clear()
        val worldsSection = config.getConfigurationSection("worlds")!!
        worldsSection.getKeys(false).forEach {
            settingMap[it] = loadSetting(worldsSection.getConfigurationSection(it)!!)
        }
    }

    private fun loadGlobalSetting(config: ConfigurationSection) {
        val difficulty = Difficulty.valueOf(config.getString("difficulty")!!.uppercase())
        val gameMode = GameMode.valueOf(config.getString("gameMode")!!.uppercase())
        val gameRules = config.getConfigurationSection("gameRules")!!.run {
            val map = mutableMapOf<GameRule<Boolean>, Boolean>()
            getKeys(false).forEach { ruleKey ->
                map[GameRule.getByName(ruleKey)!! as GameRule<Boolean>] = get(ruleKey)!! as Boolean
            }
            map
        }
        val antiCreatePortal = config.getBoolean("antiCreatePortal")
        val antiChangeMobSpawn = config.getBoolean("antiChangeMobSpawn")
        val antiTramplingFarmland = config.getBoolean("antiTramplingFarmland")
        val antiItemUse = config.getStringList("antiItemUse")
            .mapNotNull { kotlin.runCatching { Material.valueOf(it.uppercase()) }.getOrNull() }
        val itemBlockAction = config.getMultiAction("itemBlockAction")
        val antiFly = config.getBoolean("antiFly")
        val flyBlockAction = config.getMultiAction("flyBlockAction")
        val antiDamageFriendEntityLiving = config.getBoolean("antiDamageFriendEntityLiving")
        val commandWhite = config.getStringList("commandWhite").mapNotNull {
            val pair = it.split(":")
            if (pair.size < 2) {
                return@mapNotNull null
            }
            return@mapNotNull when (pair[0]) {
                "start" -> CommandMatchType.START to pair[1].trim()
                "like" -> CommandMatchType.LIKE to pair[1].trim()
                else -> null
            }
        }
        val commandBlockAction = config.getMultiAction("commandBlockAction")
        val antiCursorNoEmptyPickupItem = config.getBoolean("antiCursorNoEmptyPickupItem")
        val antiBreakBlock = config.getBoolean("antiBreakBlock")
        val antiPlaceBlock = config.getBoolean("antiPlaceBlock")
        val blockInteractWhite = config.getStringList("blockInteractWhite")
            .mapNotNull { kotlin.runCatching { Material.valueOf(it.uppercase()) }.getOrNull() }
        val antiInteractDisplayFrameAndPaint = config.getBoolean("antiInteractDisplayFrameAndPaint")
        val antiInteractArmorStand = config.getBoolean("antiInteractArmorStand")
        val antiPlayerPickupOtherPlayerDropOfItem = config.getBoolean("antiPlayerPickupOtherPlayerDropOfItem")
        val antiNoPlayerPickupItem = config.getBoolean("antiNoPlayerPickupItem")
        val pvp = config.getBoolean("pvp")
        val tabComplete = config.getBoolean("tabComplete")
        val antiVoidDamage = config.getBoolean("antiVoidDamage")
        global = WorldSettingConfig(
            difficulty,
            gameMode,
            gameRules,
            antiCreatePortal,
            antiChangeMobSpawn,
            antiTramplingFarmland,
            antiItemUse,
            itemBlockAction,
            antiFly,
            flyBlockAction,
            antiDamageFriendEntityLiving,
            commandWhite,
            commandBlockAction,
            antiCursorNoEmptyPickupItem,
            antiBreakBlock,
            antiPlaceBlock,
            blockInteractWhite,
            antiInteractDisplayFrameAndPaint,
            antiInteractArmorStand,
            antiPlayerPickupOtherPlayerDropOfItem,
            antiNoPlayerPickupItem,
            pvp,
            tabComplete,
            antiVoidDamage
        )
    }

    private fun loadSetting(config: ConfigurationSection): WorldSettingConfig {
        val difficulty = Difficulty.valueOf(config.getString("difficulty", global.difficulty.name)!!.uppercase())
        val gameMode = GameMode.valueOf(config.getString("gameMode", global.gameMode.name)!!.uppercase())
        val gameRules = config.getConfigurationSection("gameRules")?.run {
            val map = mutableMapOf<GameRule<Boolean>, Boolean>()
            getKeys(false).forEach { ruleKey ->
                map[GameRule.getByName(ruleKey)!! as GameRule<Boolean>] = get(ruleKey)!! as Boolean
            }
            map
        } ?: global.gameRules
        val antiCreatePortal = config.getBoolean("antiCreatePortal", global.antiCreatePortal)
        val antiChangeMobSpawn = config.getBoolean("antiChangeMobSpawn", global.antiChangeMobSpawn)
        val antiTramplingFarmland = config.getBoolean("antiTramplingFarmland", global.antiTramplingFarmland)
        val antiItemUse = if (config.contains("antiItemUse")) config.getStringList("antiItemUse").mapNotNull {
            kotlin.runCatching { Material.valueOf(it.uppercase()) }.getOrNull()
        } else global.antiItemUse
        val itemBlockAction = config.getMultiAction("itemBlockAction", global.itemBlockAction)

        val antiFly = config.getBoolean("antiFly", global.antiFly)
        val flyBlockAction = config.getMultiAction("flyBlockAction", global.flyBlockAction)

        val antiDamageFriendEntityLiving =
            config.getBoolean("antiDamageFriendEntityLiving", global.antiDamageFriendEntityLiving)
        val commandWhite = if (config.contains("commandWhite")) config.getStringList("commandWhite").mapNotNull {
            val pair = it.split(":")
            if (pair.size < 2) {
                return@mapNotNull null
            }
            return@mapNotNull when (pair[0]) {
                "start" -> CommandMatchType.START to pair[1].trim()
                "like" -> CommandMatchType.LIKE to pair[1].trim()
                else -> null
            }
        } else global.commandWhite

        val commandBlockAction = config.getMultiAction("commandBlockAction", global.commandBlockAction)

        val antiCursorNoEmptyPickupItem =
            config.getBoolean("antiCursorNoEmptyPickupItem", global.antiCursorNoEmptyPickupItem)
        val antiBreakBlock = config.getBoolean("antiBreakBlock", global.antiBreakBlock)
        val antiPlaceBlock = config.getBoolean("antiPlaceBlock", global.antiPlaceBlock)
        val blockInteractWhite =
            if (config.contains("blockInteractWhite")) config.getStringList("blockInteractWhite").mapNotNull {
                kotlin.runCatching { Material.valueOf(it.uppercase()) }.getOrNull()
            } else global.blockInteractWhite
        val antiInteractDisplayFrameAndPaint =
            config.getBoolean("antiInteractDisplayFrameAndPaint", global.antiInteractDisplayFrameAndPaint)
        val antiInteractArmorStand = config.getBoolean("antiInteractArmorStand", global.antiInteractArmorStand)
        val antiPlayerPickupOtherPlayerDropOfItem =
            config.getBoolean("antiPlayerPickupOtherPlayerDropOfItem", global.antiPlayerPickupOtherPlayerDropOfItem)
        val antiNoPlayerPickupItem = config.getBoolean("antiNoPlayerPickupItem", global.antiNoPlayerPickupItem)
        val pvp = config.getBoolean("pvp", global.pvp)
        val tabComplete = config.getBoolean("tabComplete", global.tabComplete)
        val antiVoidDamage = config.getBoolean("antiVoidDamage", global.antiVoidDamage)
        return WorldSettingConfig(
            difficulty,
            gameMode,
            gameRules,
            antiCreatePortal,
            antiChangeMobSpawn,
            antiTramplingFarmland,
            antiItemUse,
            itemBlockAction,
            antiFly,
            flyBlockAction,
            antiDamageFriendEntityLiving,
            commandWhite,
            commandBlockAction,
            antiCursorNoEmptyPickupItem,
            antiBreakBlock,
            antiPlaceBlock,
            blockInteractWhite,
            antiInteractDisplayFrameAndPaint,
            antiInteractArmorStand,
            antiPlayerPickupOtherPlayerDropOfItem,
            antiNoPlayerPickupItem,
            pvp,
            tabComplete,
            antiVoidDamage
        )
    }

    fun ConfigurationSection.getMultiAction(key: String, def: MultiAction? = null): MultiAction? {
        if (!contains(key)) {
            return def
        }
        val lines = getStringList(key)
        return ActionParser.parseAction(lines.map { it.colored() })
    }

    fun getSetting(world: World?): WorldSettingConfig? {
        return if (world == null) null else getSetting(world.name)
    }

    fun getSetting(worldName: String) = settingMap[worldName] ?: global

}

data class WorldSettingConfig(
    val difficulty: Difficulty,
    val gameMode: GameMode,
    val gameRules: Map<GameRule<Boolean>, Boolean>,
    val antiCreatePortal: Boolean,
    val antiChangeMobSpawn: Boolean,
    val antiTramplingFarmland: Boolean,
    val antiItemUse: List<Material>,
    val itemBlockAction: MultiAction?,
    val antiFly: Boolean,
    val flyBlockAction: MultiAction?,
    val antiDamageFriendEntityLiving: Boolean,
    val commandWhite: List<Pair<CommandMatchType, String>>,
    val commandBlockAction: MultiAction?,
    val antiCursorNoEmptyPickupItem: Boolean,
    val antiBreakBlock: Boolean,
    val antiPlaceBlock: Boolean,
    val blockInteractWhite: List<Material>,
    val antiInteractDisplayFrameAndPaint: Boolean,
    val antiInteractArmorStand: Boolean,
    val antiPlayerPickupOtherPlayerDropOfItem: Boolean,
    val antiNoPlayerPickupItem: Boolean,
    val pvp: Boolean,
    val tabComplete: Boolean,
    val antiVoidDamage: Boolean
)

enum class CommandMatchType {
    LIKE, START
}