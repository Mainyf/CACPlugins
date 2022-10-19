@file:Suppress("UNCHECKED_CAST")

package io.github.mainyf.csdungeon.config

import io.github.mainyf.csdungeon.CsDungeon
import io.github.mainyf.newmclib.config.*
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.EntityType
import org.bukkit.util.Vector

fun CommandSender.sendLang(key: String, vararg data: Any) {
    ConfigCSD.lang.apply {
        sendLang(key, *data)
    }
}

object ConfigCSD {

    private lateinit var mainConfigFile: FileConfiguration
    private lateinit var dungeonConfigFile: FileConfiguration
    private lateinit var menuConfigFile: FileConfiguration
    private lateinit var langConfigFile: FileConfiguration

    val dungeonConfigList = mutableListOf<DungeonConfig>()
    lateinit var dungeonMenuConfig: DungeonMenuConfig

    lateinit var lang: BaseLang

    fun load() {
        CsDungeon.INSTANCE.saveDefaultConfig()
        CsDungeon.INSTANCE.reloadConfig()

        kotlin.runCatching {
            val dungeonsFile = CsDungeon.INSTANCE.dataFolder.resolve("dungeons.yml")
            if (!dungeonsFile.exists()) {
                CsDungeon.INSTANCE.saveResource("dungeons.yml", false)
            }
            val menuFile = CsDungeon.INSTANCE.dataFolder.resolve("menu.yml")
            if (!menuFile.exists()) {
                CsDungeon.INSTANCE.saveResource("menu.yml", false)
            }
            val langFile = CsDungeon.INSTANCE.dataFolder.resolve("lang.yml")
            if (!langFile.exists()) {
                CsDungeon.INSTANCE.saveResource("lang.yml", false)
            }
            mainConfigFile = CsDungeon.INSTANCE.config
            dungeonConfigFile = YamlConfiguration.loadConfiguration(dungeonsFile)
            menuConfigFile = YamlConfiguration.loadConfiguration(menuFile)
            langConfigFile = YamlConfiguration.loadConfiguration(langFile)
            lang = BaseLang()

            loadMenuConfig()
            loadMainConfig()
            lang.load(langConfigFile)
        }.onFailure {
            CsDungeon.LOGGER.info("加载配置时出现错误")
            it.printStackTrace()
        }
    }

    private fun loadMenuConfig() {
        val dungeonMenuSect = menuConfigFile.getConfigurationSection("dungeonMenu")!!
        dungeonMenuConfig = DungeonMenuConfig(
            dungeonMenuSect.asMenuSettingsConfig(),
            dungeonMenuSect.asDefaultSlotConfig("level1Slot"),
            dungeonMenuSect.asDefaultSlotConfig("level2Slot"),
            dungeonMenuSect.asDefaultSlotConfig("level3Slot")
        )
    }

    private fun loadMainConfig() {
        dungeonConfigList.clear()
        val dungeonNames = dungeonConfigFile.getKeys(false)
        dungeonNames.forEach { dungeonName ->
            val dungeonSect = dungeonConfigFile.getConfigurationSection(dungeonName)!!
            val structureName = dungeonSect.getString("structureName")!!
            val protectBuild = dungeonSect.getBoolean("protectBuild", true)
            val mobs = mutableListOf<DungeonMobConfig>()
            val mobsSect = dungeonSect.getConfigurationSection("mobs")
            mobsSect?.getKeys(false)?.forEach { mobName ->
                val mobSect = mobsSect.getConfigurationSection(mobName)!!
                val total = mobSect.getInt("total")
                val spawnPeriod = mobSect.getLong("spawnPeriod")
                val max = mobSect.getInt("max")
                val mobTypes = mobSect.getStringList("mobTypes").map { EntityType.valueOf(it.uppercase()) }
                val locs = mobSect.getStringList("locations").map {
                    val pair = it.split(",")
                    Vector(pair[0].toDouble(), pair[1].toDouble(), pair[2].toDouble())
                }
                val locationSpacing = mobSect.getInt("locationSpacing")
                mobs.add(
                    DungeonMobConfig(
                        total,
                        spawnPeriod,
                        max,
                        mobTypes,
                        locs,
                        locationSpacing
                    )
                )
            }
            dungeonConfigList.add(
                DungeonConfig(
                    dungeonName,
                    structureName,
                    protectBuild,
                    mobs
                )
            )
        }
    }

}