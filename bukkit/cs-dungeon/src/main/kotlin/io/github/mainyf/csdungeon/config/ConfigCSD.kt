@file:Suppress("UNCHECKED_CAST")

package io.github.mainyf.csdungeon.config

import io.github.mainyf.csdungeon.CsDungeon
import io.github.mainyf.newmclib.config.*
import io.github.mainyf.newmclib.exts.getListAsConfigSection
import io.github.mainyf.newmclib.exts.saveResourceToFileAsConfiguration
import io.github.mainyf.worldsettings.config.ConfigWS
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.yaml.snakeyaml.Yaml
import java.io.StringReader

fun CommandSender.sendLang(key: String, vararg data: Any) {
    ConfigCSD.lang.apply {
        sendLang(key, *data)
    }
}

object ConfigCSD {

    private lateinit var mainConfigFile: FileConfiguration
    private lateinit var menuConfigFile: FileConfiguration
    private lateinit var langConfigFile: FileConfiguration

    val dungeonConfigMap = mutableMapOf<String, DungeonConfig>()
    var dungeonCoreId = "plot:plot_main"
    lateinit var dungeonMenuConfig: DungeonMenuConfig

    lateinit var lang: BaseLang

    fun load() {
        CsDungeon.INSTANCE.saveDefaultConfig()
        CsDungeon.INSTANCE.reloadConfig()

        kotlin.runCatching {
            mainConfigFile = CsDungeon.INSTANCE.saveResourceToFileAsConfiguration("config.yml")
            if(!CsDungeon.INSTANCE.dataFolder.resolve("dungeons").exists()) {
                CsDungeon.INSTANCE.saveResourceToFileAsConfiguration("dungeons/ruins_1.yml")
            }
            menuConfigFile = CsDungeon.INSTANCE.saveResourceToFileAsConfiguration("menu.yml")
            langConfigFile = CsDungeon.INSTANCE.saveResourceToFileAsConfiguration("lang.yml")

            lang = BaseLang()

            loadMenuConfig()
            loadDungeonsConfig()
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

    private fun loadDungeonsConfig() {
        dungeonConfigMap.clear()
        CsDungeon.INSTANCE.dataFolder
            .resolve("dungeons")
            .listFiles()
            ?.forEach { dungeonConfigFile ->
                kotlin.runCatching {
                    val dungeonSect = YamlConfiguration.loadConfiguration(dungeonConfigFile)
                    val dungeonName = dungeonSect.getString("dungeonName")!!
                    val worldName = dungeonSect.getString("worldName")!!
                    val structureName = dungeonSect.getString("structureName")!!
                    val protectBuild = dungeonSect.getBoolean("protectBuild", true)
                    val boundaryDamage = dungeonSect.getInt("boundaryDamage", 2)
                    val noPlayerEnd = dungeonSect.getBoolean("noPlayerEnd", true)
                    val noFly = dungeonSect.getBoolean("noFly", true)

                    val dungeonMaterials = mutableListOf<DungeonMaterials>()

                    dungeonSect.getListAsConfigSection("dungeonMaterials").forEach { dungeonMaterialSect ->
                        val item = dungeonMaterialSect.getListAsConfigSection("item").map { dmItemSect ->
                            val iaName = dmItemSect.getString("iaName")!!
                            val amount = dmItemSect.getInt("amount")
                            val displayName = dmItemSect.getString("displayName")!!
                            DungeonMaterialItem(iaName, amount, displayName)
                        }
                        val money = dungeonMaterialSect.getDouble("money")
                        val level = dungeonMaterialSect.getInt("level", 0)
                        val exp = dungeonMaterialSect.getInt("exp", 0)
                        dungeonMaterials.add(DungeonMaterials(item, money, level, exp))
                    }

                    val menuItemInfo = mutableListOf<MenuItemInfo>()

                    dungeonSect.getListAsConfigSection("menuItemInfo").forEach { dungeonMaterialSect ->
                        val menuName = dungeonMaterialSect.getString("menuName")!!
                        val menuLore = dungeonMaterialSect.getStringList("menuLore")
                        menuItemInfo.add(MenuItemInfo(menuName, menuLore))
                    }

                    val tipPeriod = dungeonSect.getLong("tipPeriod")
                    val tipActions = ActionParser.parseAction(dungeonSect, "tipActions", false)!!

                    val startActions = ActionParser.parseAction(dungeonSect, "startActions", false)!!
                    val startPlays = PlayParser.parsePlay(dungeonSect, "startPlays")!!
                    val endActions = ActionParser.parseAction(dungeonSect, "endActions", false)!!
                    val endPlays = PlayParser.parsePlay(dungeonSect, "endPlays")!!

                    val wsConfigSect = dungeonSect.getConfigurationSection("wsConfig")!!

                    val wsConfig = ConfigWS.loadSetting(wsConfigSect, ConfigWS.getSetting(worldName))

                    val levels = mutableListOf<DungeonLevelConfig>()
                    val levelsSect = dungeonSect.getConfigurationSection("levels")
                    levelsSect?.getKeys(false)?.forEach { levelName ->
                        val levelSect = levelsSect.getConfigurationSection(levelName)!!
                        val totalMob = levelSect.getInt("totalMob")
                        val mobSpawns = mutableListOf<DungeonMobConfig>()
                        val mobSpawnList = levelSect.getList("mobSpawns")!! as List<Map<Any, Any>>
                        mobSpawnList.forEach { map ->
                            val mobSpawnSect = YamlConfiguration.loadConfiguration(StringReader(Yaml().dump(map)))
                            val loc = mobSpawnSect.getString("loc")!!
                            val spawnPeriod = mobSpawnSect.getLong("spawnPeriod")
                            val max = mobSpawnSect.getInt("max")
                            val mobTypes = mobSpawnSect.getStringList("mobTypes").map { MobType(it) }
                            val locationSpacing = mobSpawnSect.getInt("locationSpacing")
                            mobSpawns.add(
                                DungeonMobConfig(
                                    loc,
                                    spawnPeriod,
                                    max,
                                    mobTypes,
                                    locationSpacing
                                )
                            )
                        }
                        levels.add(
                            DungeonLevelConfig(
                                levelName.toInt(),
                                totalMob,
                                mobSpawns
                            )
                        )
                    }
                    dungeonConfigMap[dungeonName] = DungeonConfig(
                        worldName,
                        dungeonName,
                        structureName,
                        protectBuild,
                        boundaryDamage,
                        noPlayerEnd,
                        noFly,
                        dungeonMaterials,
                        menuItemInfo,
                        tipPeriod,
                        tipActions,
                        startActions,
                        startPlays,
                        endActions,
                        endPlays,
                        wsConfig,
                        levels
                    )
                }.onFailure {
                    it.printStackTrace()
                }
            }
    }

    private fun loadMainConfig() {
        dungeonCoreId = mainConfigFile.getString("dungeonCoreId", dungeonCoreId)!!
    }

}