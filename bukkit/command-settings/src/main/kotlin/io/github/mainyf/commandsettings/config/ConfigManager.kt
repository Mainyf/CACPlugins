package io.github.mainyf.commandsettings.config

import io.github.mainyf.commandsettings.CommandSettings
import io.github.mainyf.newmclib.config.ActionParser
import io.github.mainyf.newmclib.config.PlayParser
import io.github.mainyf.newmclib.config.action.MultiAction
import io.github.mainyf.newmclib.config.play.MultiPlay
import io.github.mainyf.newmclib.exts.colored
import org.bukkit.configuration.file.FileConfiguration

object ConfigManager {

    private lateinit var mainConfig: FileConfiguration

    private val actionMap = mutableMapOf<String, ItemAction>()

    fun load() {
        kotlin.runCatching {
            CommandSettings.INSTANCE.saveDefaultConfig()
            CommandSettings.INSTANCE.reloadConfig()
            mainConfig = CommandSettings.INSTANCE.config

            actionMap.clear()
            mainConfig.getKeys(false).forEach { id ->
                val idSect = mainConfig.getConfigurationSection(id)!!
                val demandItems = idSect.getStringList("demandItems").map {
                    val pair = it.split("|")
                    pair[0] to pair[1].toInt()
                }
                val noDemandActions =
                    ActionParser.parseAction(idSect.getStringList("noDemandActions").map { it.colored() })
                val actions = ActionParser.parseAction(idSect.getStringList("actions").map { it.colored() })
                val plays = PlayParser.parsePlay(idSect.getStringList("plays").map { it.colored() })
                actionMap[id] = ItemAction(demandItems, noDemandActions, actions, plays)
            }
        }.onFailure {
            CommandSettings.INSTANCE.slF4JLogger.error("加载配置时出现错误", it)
        }
    }

    fun getAction(id: String): ItemAction? {
        return actionMap[id]
    }

}

class ItemAction(
    val demandItems: List<Pair<String, Int>>,
    val noDemandActions: MultiAction?,
    val actions: MultiAction?,
    val plays: MultiPlay?
)
