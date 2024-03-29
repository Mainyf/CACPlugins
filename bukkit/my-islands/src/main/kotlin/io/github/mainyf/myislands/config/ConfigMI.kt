@file:Suppress("UNCHECKED_CAST")

package io.github.mainyf.myislands.config

import io.github.mainyf.myislands.MyIslands
import io.github.mainyf.newmclib.config.*
import io.github.mainyf.newmclib.config.action.MultiAction
import io.github.mainyf.newmclib.exts.getAction
import io.github.mainyf.newmclib.hooks.money
import io.github.mainyf.newmclib.hooks.takeMoney
import org.bukkit.command.CommandSender
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.util.Vector

fun CommandSender.sendLang(key: String, vararg data: Any) {
    ConfigMI.lang.apply {
        sendLang(key, *data)
    }
}

object ConfigMI {

    var debug = false

    private lateinit var mainConfigFile: FileConfiguration
    private lateinit var menuConfigFile: FileConfiguration
    private lateinit var langConfigFile: FileConfiguration


    lateinit var coreId: String
    lateinit var backLobbyAction: MultiAction
    lateinit var myislandCost: IsLandsCost
    val schematicMap = mutableMapOf<String, PlotSchematicConfig>()
//    var islandHelperMaxCount = 6

    var resetCooldown = 1L

    lateinit var mainMenuConfig: IslandMainMenuConfig
    lateinit var settingsMenuConfig: IslandSettingsMenuConfig
    lateinit var chooseMenuConfig: IslandChooseMenuConfig
    lateinit var helperSelectMenuConfig: IslandHelperSelectMenuConfig

    var moveCoreAction: MultiAction? = null
    lateinit var creatureLimitConfig: IsLandsCreatureLimitConfig
    var tutorialQuestEndAction: MultiAction? = null

    lateinit var lang: BaseLang

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
            lang = BaseLang()

            loadMenuConfig()
            loadMainConfig()
            lang.load(langConfigFile)
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
            mainMenuSect.asDefaultSlotConfig("infoAndKudosSlot"),
            mainMenuSect.asDefaultSlotConfig("upgradeAndBackIslandSlot"),
            mainMenuSect.asDefaultSlotConfig("islandSettingsSlot")
        )
        settingsMenuConfig = IslandSettingsMenuConfig(
            settingsMenuSect.asMenuSettingsConfig(),
            settingsMenuSect.asDefaultSlotConfig("helpersSlot"),
            settingsMenuSect.asDefaultSlotConfig("prevSlot"),
            settingsMenuSect.asDefaultSlotConfig("nextSlot"),
            settingsMenuSect.asDefaultSlotConfig("moveCoreSlot"),
            settingsMenuSect.asDefaultSlotConfig("visibilitySlot"),
            settingsMenuSect.asDefaultSlotConfig("resetIslandSlot")
        )
        chooseMenuConfig = IslandChooseMenuConfig(
            chooseMenuSect.asMenuSettingsConfig(),
            chooseMenuSect.asIslandPresetSlotConfig("islandListSlot"),
            chooseMenuSect.asDefaultSlotConfig("prevSlot"),
            chooseMenuSect.asDefaultSlotConfig("nextSlot"),
            chooseMenuSect.asDefaultSlotConfig("backSlot")
        )
        helperSelectMenuConfig = IslandHelperSelectMenuConfig(
            helperSelectMenuSect.asMenuSettingsConfig(),
            helperSelectMenuSect.asDefaultSlotConfig("playerListSlot"),
            helperSelectMenuSect.asDefaultSlotConfig("prevSlot"),
            helperSelectMenuSect.asDefaultSlotConfig("nextSlot"),
            helperSelectMenuSect.asDefaultSlotConfig("backSlot")
        )
    }

    //    private fun ConfigurationSection.asIslandHelperSlotConfig(key: String): IslandHelperSlotConfig {
//        return getConfigurationSection(key)!!.let {
//            IslandHelperSlotConfig(
//                it.getIntegerList("slot"),
//                it.asItemSlotConfig(),
//                it.getConfigurationSection("empty")!!.asItemSlotConfig()
//            )
//        }
//    }
//
//    private fun ConfigurationSection.asIslandChooseBackSlotConfig(key: String): IslandChooseBackSlotConfig {
//        return getConfigurationSection(key)!!.let {
//            IslandChooseBackSlotConfig(
//                it.getIntegerList("slot"),
//                it.getConfigurationSection("backCity")!!.asItemSlotConfig(),
//                it.getConfigurationSection("backPrev")!!.asItemSlotConfig()
//            )
//        }
//    }
//
    private fun ConfigurationSection.asIslandPresetSlotConfig(key: String): IslandPresetSlotConfig {
        return getConfigurationSection(key)!!.let {
            IslandPresetSlotConfig(
                it.get("slot") as List<List<Int>>,
                it.getConfigurationSection("default")!!.asItemSlotConfig()
            )
        }
    }
//
//    private fun ConfigurationSection.asInfoAndKudosSlotConfig(key: String): InfoAndKudosSlotConfig {
//        return getConfigurationSection(key)!!.let {
//            InfoAndKudosSlotConfig(
//                it.getIntegerList("slot"),
//                it.getConfigurationSection("info")!!.asItemSlotConfig(),
//                it.getConfigurationSection("kudos")!!.asItemSlotConfig()
//            )
//        }
//    }
//
//    private fun ConfigurationSection.asUpgradeAndBackIslandSlotSlotConfig(key: String): UpgradeAndBackIslandSlotSlotConfig {
//        return getConfigurationSection(key)!!.let {
//            UpgradeAndBackIslandSlotSlotConfig(
//                it.getIntegerList("slot"),
//                it.getConfigurationSection("upgrade")!!.asItemSlotConfig(),
//                it.getConfigurationSection("back")!!.asItemSlotConfig()
//            )
//        }
//    }

    private fun loadMainConfig() {
        debug = mainConfigFile.getBoolean("debug", false)
        coreId = mainConfigFile.getString("coreId") ?: "itemsadder:amethyst_block"
        backLobbyAction = ActionParser.parseAction(mainConfigFile.getStringList("backLobby"))!!
        myislandCost = IsLandsCost(
            mainConfigFile.getDouble("cost.reset", 10.0),
            mainConfigFile.getDouble("cost.moveCore", 10.0),
            mainConfigFile.getDouble("cost.switchVisibility", 10.0),
            mainConfigFile.getDouble("cost.addHelper", 10.0)
        )
//        islandHelperMaxCount = mainConfigFile.getInt("islandHelperMaxCount", 6)
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
        creatureLimitConfig = IsLandsCreatureLimitConfig(
            mainConfigFile.getLong("creatureLimit.period", 20),
            mapOf(
                CreatureType.ANIMALS to CreatureLimitConfig(
                    mainConfigFile.getInt("creatureLimit.animals.count", 10),
                    ActionParser.parseAction(mainConfigFile.getStringList("creatureLimit.animals.msg"))
                ),
                CreatureType.MONSTER to CreatureLimitConfig(
                    mainConfigFile.getInt("creatureLimit.monster.count", 10),
                    ActionParser.parseAction(mainConfigFile.getStringList("creatureLimit.monster.msg"))
                ),
                CreatureType.VILLAGER to CreatureLimitConfig(
                    mainConfigFile.getInt("creatureLimit.villager.count", 10),
                    ActionParser.parseAction(mainConfigFile.getStringList("creatureLimit.villager.msg"))
                )
            )
        )
        tutorialQuestEndAction = mainConfigFile.getAction("tutorialQuestEndAction")
    }

    fun tryPayMyIslandCost(player: Player, cost: Double, langKey: String): Boolean {
        val money = player.money()
        if (money < cost) {
            player.sendLang("costMoneyLack.${langKey}", "{money}", money, "{cost}", cost)
            return false
        }
        player.takeMoney(cost)
        return true
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

    class IsLandsCost(
        val reset: Double,
        val moveCore: Double,
        val switchVisibility: Double,
        val addHelper: Double
    )

    class IsLandsCreatureLimitConfig(
        val period: Long,
        val map: Map<CreatureType, CreatureLimitConfig>
    ) {

        fun get(creatureType: CreatureType): CreatureLimitConfig {
            return map[creatureType]!!
        }

    }

    class CreatureLimitConfig(
        val count: Int,
        val msg: MultiAction?
    )

}