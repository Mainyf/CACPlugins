@file:Suppress("UNCHECKED_CAST")

package io.github.mainyf.myislands.config

import io.github.mainyf.myislands.MyIslands
import io.github.mainyf.newmclib.config.*
import io.github.mainyf.newmclib.config.action.MultiAction
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.util.Vector

object ConfigManager {

    var debug = false

    private lateinit var mainConfigFile: FileConfiguration
    private lateinit var menuConfigFile: FileConfiguration
    private lateinit var langConfigFile: FileConfiguration


    lateinit var coreId: String
    lateinit var backLobbyAction: MultiAction
    val schematicMap = mutableMapOf<String, PlotSchematicConfig>()
    var islandHelperMaxCount = 6

    var resetCooldown = 1L

    lateinit var mainMenuConfig: IslandMainMenuConfig
    lateinit var settingsMenuConfig: IslandSettingsMenuConfig
    lateinit var chooseMenuConfig: IslandChooseMenuConfig
    lateinit var helperSelectMenuConfig: IslandHelperSelectMenuConfig

    var moveCoreAction: MultiAction? = null

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
        val chooseMenuSect = menuConfigFile.getConfigurationSection("chooseMenu")!!
        val helperSelectMenuSect = menuConfigFile.getConfigurationSection("helperSelectMenu")!!
        mainMenuConfig = IslandMainMenuConfig(
            mainMenuSect.asMenuSettingsConfig(),
            mainMenuSect.asDefaultSlotConfig("prevSlot"),
            mainMenuSect.asDefaultSlotConfig("nextSlot"),
            mainMenuSect.asDefaultSlotConfig("islandViewSlot"),
            mainMenuSect.asDefaultSlotConfig("switchViewIslandSlot"),
            mainMenuSect.asInfoAndKudosSlotConfig("infoAndKudosSlot"),
            mainMenuSect.asUpgradeAndBackIslandSlotSlotConfig("upgradeAndBackIslandSlot"),
            mainMenuSect.asDefaultSlotConfig("islandSettingsSlot")
        )
        settingsMenuConfig = IslandSettingsMenuConfig(
            settingsMenuSect.asMenuSettingsConfig(),
            settingsMenuSect.asIslandHelperSlotConfig("helpersSlot"),
            settingsMenuSect.asDefaultSlotConfig("moveCoreSlot"),
            settingsMenuSect.asDefaultSlotConfig("visibilitySlot"),
            settingsMenuSect.asDefaultSlotConfig("resetIslandSlot")
        )
        chooseMenuConfig = IslandChooseMenuConfig(
            chooseMenuSect.asMenuSettingsConfig(),
            chooseMenuSect.asIslandPresetSlotConfig("islandListSlot"),
            chooseMenuSect.asDefaultSlotConfig("prevSlot"),
            chooseMenuSect.asDefaultSlotConfig("nextSlot"),
            chooseMenuSect.asIslandChooseBackSlotConfig("backSlot")
        )
        helperSelectMenuConfig = IslandHelperSelectMenuConfig(
            helperSelectMenuSect.asMenuSettingsConfig(),
            helperSelectMenuSect.asDefaultSlotConfig("playerListSlot"),
            helperSelectMenuSect.asDefaultSlotConfig("prevSlot"),
            helperSelectMenuSect.asDefaultSlotConfig("nextSlot"),
            helperSelectMenuSect.asDefaultSlotConfig("backSlot")
        )
    }

    private fun ConfigurationSection.asIslandHelperSlotConfig(key: String): IslandHelperSlotConfig {
        return getConfigurationSection(key)!!.let {
            IslandHelperSlotConfig(
                it.getIntegerList("slot"),
                it.asItemSlotConfig(),
                it.getConfigurationSection("empty")!!.asItemSlotConfig()
            )
        }
    }

    private fun ConfigurationSection.asIslandChooseBackSlotConfig(key: String): IslandChooseBackSlotConfig {
        return getConfigurationSection(key)!!.let {
            IslandChooseBackSlotConfig(
                it.getIntegerList("slot"),
                it.getConfigurationSection("backCity")!!.asItemSlotConfig(),
                it.getConfigurationSection("backPrev")!!.asItemSlotConfig()
            )
        }
    }

    private fun ConfigurationSection.asIslandPresetSlotConfig(key: String): IslandPresetSlotConfig {
        return getConfigurationSection(key)!!.let {
            IslandPresetSlotConfig(
                it.get("slot") as List<List<Int>>,
                it.asItemSlotConfig()
            )
        }
    }

    private fun ConfigurationSection.asInfoAndKudosSlotConfig(key: String): InfoAndKudosSlotConfig {
        return getConfigurationSection(key)!!.let {
            InfoAndKudosSlotConfig(
                it.getIntegerList("slot"),
                it.getConfigurationSection("info")!!.asItemSlotConfig(),
                it.getConfigurationSection("kudos")!!.asItemSlotConfig()
            )
        }
    }

    private fun ConfigurationSection.asUpgradeAndBackIslandSlotSlotConfig(key: String): UpgradeAndBackIslandSlotSlotConfig {
        return getConfigurationSection(key)!!.let {
            UpgradeAndBackIslandSlotSlotConfig(
                it.getIntegerList("slot"),
                it.getConfigurationSection("upgrade")!!.asItemSlotConfig(),
                it.getConfigurationSection("back")!!.asItemSlotConfig()
            )
        }
    }

    private fun loadMainConfig() {
        debug = mainConfigFile.getBoolean("debug", false)
        coreId = mainConfigFile.getString("coreId") ?: "itemsadder:amethyst_block"
        backLobbyAction = ActionParser.parseAction(mainConfigFile.getStringList("backLobby"))!!
        islandHelperMaxCount = mainConfigFile.getInt("islandHelperMaxCount", 6)
        resetCooldown = mainConfigFile.getLong("resetCooldown", 1)
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
                schematicSection.asIaIcons(),
                schematicKey,
                PlotSchematicUIConfig(uiName, uiLore),
                core
            )
        }
        moveCoreAction = ActionParser.parseAction(mainConfigFile.getStringList("moveCoreAction"))!!
    }

    class PlotSchematicConfig(
        val iaIcons: IaIcons,
        val name: String,
        val ui: PlotSchematicUIConfig,
        val core: Vector
    )

    class PlotSchematicUIConfig(
        val name: String,
        val lore: List<String>
    )

}