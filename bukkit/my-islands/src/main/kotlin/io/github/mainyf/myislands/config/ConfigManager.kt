@file:Suppress("UNCHECKED_CAST")

package io.github.mainyf.myislands.config

import io.github.mainyf.myislands.MyIslands
import io.github.mainyf.newmclib.config.ActionParser
import io.github.mainyf.newmclib.config.action.MultiAction
import io.github.mainyf.newmclib.config.asItemDisplay
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.util.Vector

object ConfigManager {

    private lateinit var mainConfigFile: FileConfiguration
    private lateinit var menuConfigFile: FileConfiguration
    private lateinit var langConfigFile: FileConfiguration


    lateinit var coreId: String
    lateinit var backLobbyAction: MultiAction
    val schematicMap = mutableMapOf<String, PlotSchematicConfig>()
    var islandHelperMaxCount = 6

    lateinit var mainMenuConfig: IslandMainMenuConfig
    lateinit var settingsMenuConfig: IslandSettingsMenuConfig

    fun load() {
        MyIslands.INSTANCE.saveDefaultConfig()
        MyIslands.INSTANCE.reloadConfig()

        kotlin.runCatching {
            val menuFile = MyIslands.INSTANCE.dataFolder.resolve("menu.yml")
            if (!menuFile.exists()) {
                MyIslands.INSTANCE.saveResource("menu.yml", false)
            }
            val langFile = MyIslands.INSTANCE.dataFolder.resolve("lang.yml")
            if (!langFile.exists()) {
                MyIslands.INSTANCE.saveResource("lang.yml", false)
            }
            mainConfigFile = MyIslands.INSTANCE.config
            menuConfigFile = YamlConfiguration.loadConfiguration(menuFile)
            langConfigFile = YamlConfiguration.loadConfiguration(langFile)

            loadMenuConfig()
            loadMainConfig()
            Lang.load(langConfigFile)
        }.onFailure {
            MyIslands.LOGGER.info("加载配置时出现错误")
            it.printStackTrace()
        }
    }

    private fun loadMenuConfig() {
        val mainMenuSect = menuConfigFile.getConfigurationSection("mainMenu")!!
        val settingsMenuSect = menuConfigFile.getConfigurationSection("settingsMenu")!!
        mainMenuConfig = IslandMainMenuConfig(
            mainMenuSect.asSlotConfig("prevSlot"),
            mainMenuSect.asSlotConfig("nextSlot"),
            mainMenuSect.asSlotConfig("islandViewSlot"),
            mainMenuSect.asSlotConfig("switchViewIslandSlot"),
            mainMenuSect.asInfoAndKudosSlotConfig("infoAndKudosSlot"),
            mainMenuSect.asUpgradeAndBackIslandSlotSlotConfig("upgradeAndBackIslandSlot"),
            mainMenuSect.asSlotConfig("islandSettingsSlot")
        )
        settingsMenuConfig = IslandSettingsMenuConfig(
            settingsMenuSect.asSlotConfig("helpersSlot"),
            settingsMenuSect.asSlotConfig("moveCoreSlot"),
            settingsMenuSect.asSlotConfig("visibilitySlot"),
            settingsMenuSect.asSlotConfig("resetIslandSlot")
        )
    }

    private fun ConfigurationSection.asSlotConfig(key: String): SlotConfig {
        return getConfigurationSection(key)!!.let {
            SlotConfig(it.getIntegerList("slot"), it.asItemDisplay())
        }
    }

    private fun ConfigurationSection.asInfoAndKudosSlotConfig(key: String): InfoAndKudosSlotConfig {
        return getConfigurationSection(key)!!.let {
            InfoAndKudosSlotConfig(
                it.getIntegerList("slot"),
                it.getConfigurationSection("info")!!.asItemDisplay(),
                it.getConfigurationSection("kudos")!!.asItemDisplay()
            )
        }
    }

    private fun ConfigurationSection.asUpgradeAndBackIslandSlotSlotConfig(key: String): UpgradeAndBackIslandSlotSlotConfig {
        return getConfigurationSection(key)!!.let {
            UpgradeAndBackIslandSlotSlotConfig(
                it.getIntegerList("slot"),
                it.getConfigurationSection("upgrade")!!.asItemDisplay(),
                it.getConfigurationSection("back")!!.asItemDisplay()
            )
        }
    }

    private fun loadMainConfig() {
        coreId = mainConfigFile.getString("coreId") ?: "itemsadder:amethyst_block"
        backLobbyAction = ActionParser.parseAction(mainConfigFile.getStringList("backLobby"))!!
        islandHelperMaxCount = mainConfigFile.getInt("islandHelperMaxCount", 6)
        val schematicListSection = mainConfigFile.getConfigurationSection("schematics")!!
        schematicListSection.getKeys(false).forEach { schematicKey ->
            val schematicSection = schematicListSection.getConfigurationSection(schematicKey)!!
            val uiName = schematicSection.getString("ui.name")!!
            val uiLore = schematicSection.getStringList("ui.lore")
            val core = schematicSection.getString("core")!!.let {
                val pair = it.split(",")
                val x = pair[0].toDouble()
                val y = pair[1].toDouble()
                val z = pair[2].toDouble()
                Vector(x, y, z)
            }
            schematicMap[schematicKey] = PlotSchematicConfig(
                schematicKey,
                PlotSchematicUIConfig(uiName, uiLore),
                core
            )
        }
    }

    class PlotSchematicConfig(
        val name: String,
        val ui: PlotSchematicUIConfig,
        val core: Vector
    )

    class PlotSchematicUIConfig(
        val name: String,
        val lore: List<String>
    )

}