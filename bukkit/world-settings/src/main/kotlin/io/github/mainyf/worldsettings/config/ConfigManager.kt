@file:Suppress("UNCHECKED_CAST")

package io.github.mainyf.worldsettings.config

import io.github.mainyf.newmclib.config.ActionParser
import io.github.mainyf.newmclib.config.action.MultiAction
import io.github.mainyf.newmclib.exts.colored
import io.github.mainyf.newmclib.serverId
import io.github.mainyf.worldsettings.WorldSettings
import org.apache.commons.lang3.EnumUtils
import org.bukkit.*
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.enchantments.Enchantment
import org.joor.Reflect
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

object ConfigManager {

    var debug = false
    lateinit var ignorePermission: String

    var globalServerConfig: ServerConfig? = null

    val serverConfigMap = mutableMapOf<String, ServerConfig>()

    fun load() {
        WorldSettings.INSTANCE.saveDefaultConfig()
        WorldSettings.INSTANCE.reloadConfig()
        val config = WorldSettings.INSTANCE.config
        debug = config.getBoolean("debug", false)
        ignorePermission = config.getString("ignore-permission") ?: "worldsettings.admin"
        val gloablServerSect = config.getConfigurationSection("gloablServer")!!
        globalServerConfig = null
        serverConfigMap.clear()
        val globalConfig = loadSetting(gloablServerSect.getConfigurationSection("global")!!, null)
        globalServerConfig = ServerConfig(
            globalConfig,
            gloablServerSect.getConfigurationSection("worlds")!!.let { worldSect ->
                worldSect.getKeys(false)
                    .associateWith { loadSetting(worldSect.getConfigurationSection(it)!!, globalConfig) }
            }
        )
        val serversSect = config.getConfigurationSection("servers")
        serversSect?.getKeys(false)?.forEach { serverId ->
            val serverSect = serversSect.getConfigurationSection(serverId)!!
            val serverGlobal = loadSetting(serverSect.getConfigurationSection("global")!!, globalServerConfig!!.global)
            serverConfigMap[serverId] = ServerConfig(
                serverGlobal,
                serverSect.getConfigurationSection("worlds")?.let { worldSect ->
                    worldSect.getKeys(false)
                        .associateWith { loadSetting(worldSect.getConfigurationSection(it)!!, serverGlobal) }
                } ?: emptyMap()
            )
        }
    }

    private fun WorldSettingConfig.setValue(configKey: String, value: Any?) {
        Reflect.on(this).set(configKey, value)
    }

    private fun WorldSettingConfig.setDefault(
        configKey: String,
        parent: WorldSettingConfig?
    ) {
        if (parent != null) {
            Reflect.on(this).set(configKey, Reflect.on(parent).get(configKey))
        }
    }

    private fun loadSetting(config: ConfigurationSection, parent: WorldSettingConfig? = null): WorldSettingConfig {
        val wsc = WorldSettingConfig()
        WorldSettingConfig::class.memberProperties.forEach {
            val configKey = it.name
            val type = it.javaField!!.type
            when {
                type == Boolean::class.java -> {
                    if (config.contains(configKey, true)) {
                        wsc.setValue(configKey, config.getBoolean(configKey))
                    } else {
                        wsc.setDefault(configKey, parent)
                    }
                }
                type == Long::class.java -> {
                    if (config.contains(configKey, true)) {
                        wsc.setValue(configKey, config.getLong(configKey))
                    } else {
                        wsc.setDefault(configKey, parent)
                    }
                }
                type.isEnum -> {
                    if (config.contains(configKey, true)) {
                        wsc.setValue(
                            configKey,
                            EnumUtils.getEnum(type as Class<out Enum<*>>, config.getString(configKey)!!.uppercase())
                        )
                    } else {
                        wsc.setDefault(configKey, parent)
                    }
                }
                type == MultiAction::class.java -> {
                    if (config.contains(configKey, true)) {
                        wsc.setValue(configKey, config.getMultiAction(configKey))
                    } else {
                        wsc.setDefault(configKey, parent)
                    }
                }
                configKey == "gameRules" -> {
                    if (config.contains(configKey, true)) {
                        val map = mutableMapOf<GameRule<Boolean>, Boolean>()
                        config.getConfigurationSection(configKey)!!.run {
                            getKeys(false).forEach { ruleKey ->
                                map[GameRule.getByName(ruleKey)!! as GameRule<Boolean>] = get(ruleKey)!! as Boolean
                            }
                        }
                        wsc.setValue(configKey, map)
                    } else {
                        wsc.setDefault(configKey, parent)
                    }
                }
                configKey == "antiItemUse" -> {
                    if (config.contains(configKey, true)) {
                        wsc.setValue(configKey, config.getStringList("antiItemUse").mapNotNull {
                            kotlin.runCatching { Material.valueOf(it.uppercase()) }.getOrNull()
                        })
                    } else {
                        wsc.setDefault(configKey, parent)
                    }
                }
                configKey == "commandWhite" -> {
                    if (config.contains(configKey, true)) {
                        wsc.setValue(configKey, config.getStringList("commandWhite").mapNotNull { line ->
                            val pair = line.split(":")
                            if (pair.size < 2) {
                                return@mapNotNull null
                            }
                            return@mapNotNull when (pair[0]) {
                                "start" -> CommandMatchType.START to pair[1].trim()
                                "like" -> CommandMatchType.LIKE to pair[1].trim()
                                else -> null
                            }
                        })
                    } else {
                        wsc.setDefault(configKey, parent)
                    }
                }
                configKey == "blockInteractWhite" -> {
                    if (config.contains(configKey, true)) {
                        wsc.setValue(configKey, config.getStringList("blockInteractWhite").mapNotNull {
                            kotlin.runCatching { Material.valueOf(it.uppercase()) }.getOrNull()
                        })
                    } else {
                        wsc.setDefault(configKey, parent)
                    }
                }
                configKey == "deleteEnchants" -> {
                    if (config.contains(configKey, true)) {
                        wsc.setValue(configKey, config.getStringList("deleteEnchants").mapNotNull {
                            kotlin.runCatching { Enchantment.getByKey(NamespacedKey.fromString(it.lowercase())) }
                                .getOrNull()
                        })
                    } else {
                        wsc.setDefault(configKey, parent)
                    }
                }
            }
        }
        return wsc
    }

    fun ConfigurationSection.getMultiAction(key: String): MultiAction? {
//        if (!contains(key)) {
//            return def
//        }
        val lines = getStringList(key)
        return ActionParser.parseAction(lines.map { it.colored() })
    }

    fun getSetting(world: World?): WorldSettingConfig? {
        return if (world == null) null else getSetting(world.name)
    }

    fun getSetting(worldName: String): WorldSettingConfig? {
        val serverId = serverId()
        if (serverConfigMap.containsKey(serverId)) {
            val serverConfig = serverConfigMap[serverId]!!
            val worlds = serverConfig.worlds
            if (worlds.containsKey(worldName)) {
                return worlds[worldName]!!
            }
            if (serverConfig.global != null) {
                return serverConfig.global
            }
        }
        if (globalServerConfig!!.worlds.containsKey(worldName)) {
            return globalServerConfig!!.worlds[worldName]!!
        }
        return globalServerConfig!!.global
    }

}

class ServerConfig(
    val global: WorldSettingConfig?,
    val worlds: Map<String, WorldSettingConfig>
)

data class WorldSettingConfig(
    val difficulty: Difficulty = Difficulty.NORMAL,
    val gameMode: GameMode = GameMode.SURVIVAL,
    val gameRules: Map<GameRule<Boolean>, Boolean> = mapOf(),
    val antiCreatePortal: Boolean = true,
    val antiChangeMobSpawn: Boolean = true,
    val antiTramplingFarmland: Boolean = true,
    val antiItemUse: List<Material> = listOf(),
    val itemBlockAction: MultiAction? = null,
    val antiFly: Boolean = true,
    val flyBlockAction: MultiAction? = null,
    val antiDamageFriendEntityLiving: Boolean = true,
    val commandWhite: List<Pair<CommandMatchType, String>> = listOf(),
    val commandBlockAction: MultiAction? = null,
    val antiCursorNoEmptyPickupItem: Boolean = true,
    val antiBreakBlock: Boolean = true,
    val antiPlaceBlock: Boolean = true,
    val blockInteractWhite: List<Material> = listOf(),
    val antiInteractDisplayFrameAndPaint: Boolean = true,
    val antiInteractArmorStand: Boolean = true,
    val antiPlayerPickupOtherPlayerDropOfItem: Boolean = true,
    val pickupItemAction: MultiAction? = null,
    val antiNoPlayerPickupItem: Boolean = true,
    val pvp: Boolean = true,
    val tabComplete: Boolean = true,
    val antiVoidDamage: Boolean = true,
    val respawnAction: MultiAction? = null,
    val joinServerAction: MultiAction? = null,
    val deleteEnchants: List<Enchantment> = listOf(),
    val deleteEnchantsAction: MultiAction? = null,
    val antiTrampleTurtleEgg: Boolean = false,
    val antiGoatHornSound: Boolean = true,
    val antiCampfireInteract: Boolean = true,
    val antiSpawnEnderDragonEgg: Boolean = true,
    val antiChat: Boolean = false,
    val antiPlayerMoveToInhabitedTimeChunk: Long = 0L,
    val antiAnvilsDamage: Boolean = false,
    val randomFrogColor: Boolean = false,
    val randomRabbitColor: Boolean = false
)

enum class CommandMatchType {
    LIKE, START
}