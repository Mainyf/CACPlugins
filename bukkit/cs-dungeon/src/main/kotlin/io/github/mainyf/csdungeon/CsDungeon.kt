package io.github.mainyf.csdungeon

import dev.jorel.commandapi.arguments.IntegerArgument
import io.github.mainyf.csdungeon.config.ConfigCSD
import io.github.mainyf.csdungeon.listeners.DungeonListeners
import io.github.mainyf.csdungeon.listeners.PlayerListeners
import io.github.mainyf.csdungeon.menu.DungeonMenu
import io.github.mainyf.csdungeon.storage.DungeonStructure
import io.github.mainyf.csdungeon.storage.StorageCSD
import io.github.mainyf.newmclib.command.apiCommand
import io.github.mainyf.newmclib.command.playerArguments
import io.github.mainyf.newmclib.exts.msg
import io.github.mainyf.newmclib.exts.pluginManager
import io.github.mainyf.newmclib.exts.successMsg
import net.kyori.adventure.text.minimessage.MiniMessage
import org.apache.logging.log4j.LogManager
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin

class CsDungeon : JavaPlugin(), Listener {

    companion object {

        val LOGGER = LogManager.getLogger("CsDungeon")

        lateinit var INSTANCE: CsDungeon

    }

    val dungeonBattles = mutableListOf<DungeonBattle>()

    fun addBattles(dungeonStructure: DungeonStructure) {
        dungeonBattles.add(DungeonBattle(dungeonStructure))
    }

    fun getBattles(dungeonStructure: DungeonStructure): DungeonBattle? {
        return dungeonBattles.find {
            it.dungeon.worldName == dungeonStructure.worldName && it.dungeon.coreX == dungeonStructure.coreX && it.dungeon.coreY == dungeonStructure.coreY && it.dungeon.coreZ == dungeonStructure.coreZ
        }
    }

    override fun onEnable() {
        INSTANCE = this
        ConfigCSD.load()
        StorageCSD.init()

        pluginManager().registerEvents(DungeonListeners, this)
        pluginManager().registerEvents(PlayerListeners, this)
        apiCommand("csdungeon") {
            withAliases("csdun", "csd")
            "reload" {
                executeOP {
                    ConfigCSD.load()
                    sender.successMsg("[CsDungeon] 重载成功")
                }
            }
            "menu" {
                withArguments(
                    playerArguments("玩家")
                )
                executeOP {
                    val player = player()
                    DungeonMenu().open(player)
                }
            }
            "list" {
                withArguments(
                    IntegerArgument("页码")
                )
                executeOP {
                    val pageIndex = int()
                    val dungeons = StorageCSD.findAllDungeon(pageIndex)
                    if (dungeons.isEmpty()) {
                        sender.msg("未检测到遗迹")
                        return@executeOP
                    }
                    dungeons.forEach {
                        sender.sendMessage(
                            MiniMessage.miniMessage()
                                .deserialize("结构名: ${it.structureName}, 位置: <click:run_command:/tppos ${it.coreX} ${it.coreY} ${it.coreZ}><hover:show_text:点击传送>${it.coreX} ${it.coreY} ${it.coreZ}</hover></click>")
                        )
                    }
                }
            }
        }.register()
    }

}