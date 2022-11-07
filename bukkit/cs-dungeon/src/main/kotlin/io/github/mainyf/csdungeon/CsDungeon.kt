package io.github.mainyf.csdungeon

import dev.jorel.commandapi.arguments.DoubleArgument
import dev.jorel.commandapi.arguments.IntegerArgument
import io.github.mainyf.csdungeon.config.ConfigCSD
import io.github.mainyf.csdungeon.listeners.DungeonListeners
import io.github.mainyf.csdungeon.listeners.PlayerListeners
import io.github.mainyf.csdungeon.menu.DungeonMenu
import io.github.mainyf.csdungeon.storage.DungeonStructure
import io.github.mainyf.csdungeon.storage.StorageCSD
import io.github.mainyf.newmclib.command.apiCommand
import io.github.mainyf.newmclib.command.playerArguments
import io.github.mainyf.newmclib.exts.errorMsg
import io.github.mainyf.newmclib.exts.msg
import io.github.mainyf.newmclib.exts.pluginManager
import io.github.mainyf.newmclib.exts.successMsg
import io.github.mainyf.worldsettings.config.ConfigWS
import net.kyori.adventure.text.minimessage.MiniMessage
import org.apache.logging.log4j.LogManager
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin

class CsDungeon : JavaPlugin(), Listener {

    companion object {

        val LOGGER = LogManager.getLogger("CsDungeon")

        lateinit var INSTANCE: CsDungeon

    }

    val dungeonBattles = mutableListOf<DungeonBattle>()

    fun hasInBattles(player: Player): Boolean {
        return dungeonBattles.any {
            it.hasInBattle(player)
        }
    }

    fun addBattles(dungeonStructure: DungeonStructure, level: Int) {
        dungeonBattles.add(DungeonBattle(dungeonStructure, level))
    }

    fun getBattles(dungeonStructure: DungeonStructure, addBlock: () -> DungeonBattle): DungeonBattle {
        var rs = dungeonBattles.find {
            it.dungeon.worldName == dungeonStructure.worldName && it.dungeon.coreX == dungeonStructure.coreX && it.dungeon.coreY == dungeonStructure.coreY && it.dungeon.coreZ == dungeonStructure.coreZ
        }
        if (rs == null) {
            rs = addBlock()
            dungeonBattles.add(rs)
        }
        return rs
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
            //            "menu" {
            //                withArguments(
            //                    playerArguments("玩家")
            //                )
            //                executeOP {
            //                    val player = player()
            //                    DungeonMenu().open(player)
            //                }
            //            }
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
            "viewmonster" {
                withArguments(
                    playerArguments("玩家")
                )
                executeOP {
                    val player = player()
                    val battle = dungeonBattles.find { it.hasInBattle(player) }
                    if (battle == null) {
                        sender.errorMsg("此玩家没有开启一场遗迹战斗")
                        return@executeOP
                    }
                    battle.mobList.filter { !it.isDead }.forEach {
                        val loc = it.location
                        sender.sendMessage(
                            MiniMessage.miniMessage()
                                .deserialize("位置: <click:run_command:/tppos ${loc.x} ${loc.y} ${loc.z}><hover:show_text:点击传送>${loc.x} ${loc.y} ${loc.z}</hover></click>")
                        )
                    }
                }
            }
            "end" {
                withArguments(
                    playerArguments("玩家")
                )
                executeOP {
                    val player = player()
                    val battle = dungeonBattles.find { it.hasInBattle(player) }
                    if (battle == null) {
                        sender.errorMsg("此玩家没有开启一场遗迹战斗")
                        return@executeOP
                    }
                    battle.end()
                    sender.successMsg("此战斗已结束")
                }
            }
            "range" {
                withArguments(
                    playerArguments("玩家"),
                    DoubleArgument("范围")
                )
                executeOP {
                    val player = player()
                    val range = double()
                    val entities = player.getNearbyEntities(range, range, range)
                    entities.forEach {
                        val loc = it.location
                        sender.sendMessage(
                            MiniMessage.miniMessage()
                                .deserialize("类型: ${it.type}, 位置 <click:run_command:/tppos ${loc.x} ${loc.y} ${loc.z}><hover:show_text:点击传送>${loc.x} ${loc.y} ${loc.z}</hover></click>")
                        )
                    }
                }
            }
            "rangeKill" {
                withArguments(
                    playerArguments("玩家"),
                    DoubleArgument("范围")
                )
                executeOP {
                    val player = player()
                    val range = double()
                    val entities = player.getNearbyEntities(range, range, range)
                    entities.forEach {
                        if (it is LivingEntity) {
                            it.health = 0.0
                        } else {
                            it.remove()
                        }
                    }
                }
            }
        }.register()
        ConfigWS.addAreaWS { loc ->
            val dungeonS = StorageCSD.findDungeonByLoc(loc) ?: return@addAreaWS null
            ConfigCSD.dungeonConfigMap.values.find { it.structureName == dungeonS.structureName }?.wsConfig
        }
    }

    override fun onDisable() {
        dungeonBattles.forEach {
            it.end()
        }
    }

}