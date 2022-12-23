package io.github.mainyf.celebration

import io.github.mainyf.bungeesettingsbukkit.CrossServerManager
import io.github.mainyf.bungeesettingsbukkit.ServerPacket
import io.github.mainyf.bungeesettingsbukkit.events.ServerPacketReceiveEvent
import io.github.mainyf.celebration.config.ConfigCEL
import io.github.mainyf.celebration.config.TogethersReward
import io.github.mainyf.newmclib.command.apiCommand
import io.github.mainyf.newmclib.command.stringArguments
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.offline_player_ext.asOfflineData
import io.github.mainyf.newmclib.serverId
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.event.HoverEvent
import org.apache.logging.log4j.LogManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

class Celebration : JavaPlugin(), Listener {

    companion object {

        val LOGGER = LogManager.getLogger("Celebration")

        lateinit var INSTANCE: Celebration

        val CELEBRATION_ALL = ServerPacket.registerPacket("broadcast_celebration_all")

    }

    private val activeTogethersRewards = mutableMapOf<String, ActiveTogethersReward>()

    override fun onEnable() {
        INSTANCE = this
        ConfigCEL.load()
        pluginManager().registerEvents(this, this)
        apiCommand("celebration") {
            withAliases("celeb", "cel")
            "reload" {
                executeOP {
                    ConfigCEL.load()
                    sender.successMsg("[ToolsPlugin] 重载成功")
                }
            }
            "active" {
                withArguments(
                    stringArguments("庆祝名") { _ -> ConfigCEL.togethers.keys.toTypedArray() },
                    stringArguments("玩家名")
                )
                executeOP {
                    val togetherName = text()
                    val playerName = text()
                    val togetherReward = ConfigCEL.togethers[togetherName]
                    if (togetherReward == null) {
                        sender.errorMsg("名为 $togetherName 的同庆奖励不存在")
                        return@executeOP
                    }
                    if (activeTogethersRewards.containsKey(togetherName)) {
                        sender.errorMsg("名为 $togetherName 的同庆奖励已经开启")
                        return@executeOP
                    }
                    startCelebration(togetherName, togetherReward, playerName)
                }
            }
            "active-allserver" {
                withArguments(
                    stringArguments("庆祝名") { _ -> ConfigCEL.togethers.keys.toTypedArray() },
                    stringArguments("玩家名")
                )
                executeOP {
                    val togetherName = text()
                    val playerName = text()
                    val togetherReward = ConfigCEL.togethers[togetherName]
                    if (togetherReward == null) {
                        sender.errorMsg("名为 $togetherName 的同庆奖励不存在")
                        return@executeOP
                    }
                    CrossServerManager.sendData(CELEBRATION_ALL) {
                        writeString(togetherName)
                        writeString(playerName)
                    }
                }
            }
            "unactive" {
                withArguments(
                    stringArguments("庆祝名") { _ -> activeTogethersRewards.keys.toTypedArray() }
                )
                executeOP {
                    val togetherName = text()
                    activeTogethersRewards.remove(togetherName)
                }
            }
            "clear" {
                executeOP {
                    activeTogethersRewards.clear()
                }
            }
            "info" {
                executeOP {
                    if (activeTogethersRewards.isEmpty()) {
                        sender.msg("&4当前没有已开启的同庆奖励")
                    } else {
                        activeTogethersRewards.forEach { (togetherName, activeTogethersReward) ->
                            val togetherReward = ConfigCEL.togethers[togetherName] ?: return@forEach
                            sender.sendMessage(
                                "奖励名: $togetherName ".toComp()
                                    .append(
                                        "可领取的玩家 ".toComp().hoverEvent(
                                            HoverEvent.showText(
                                                activeTogethersReward.availablePlayers.joinToString(", ") {
                                                    it.asOfflineData()?.name ?: ""
                                                }.toComp()
                                            )
                                        )
                                    )
                                    .append(
                                        "已领取的玩家 ".toComp().hoverEvent(
                                            HoverEvent.showText(
                                                activeTogethersReward.players.joinToString(", ") {
                                                    it.asOfflineData()?.name ?: ""
                                                }.toComp()
                                            )
                                        )
                                    )
                                    .append(
                                        "剩余: ${(togetherReward.duration / 20) - (currentTime() - activeTogethersReward.startTime) / 1000} 秒".toComp()
                                    )
                            )
                        }
                    }
                }
            }
        }
        submitTask(period = 20L) {
            val removeRewards = mutableSetOf<String>()
            activeTogethersRewards.forEach { (key, activeTogethersReward) ->
                val togetherName = activeTogethersReward.togetherName
                val togetherReward = ConfigCEL.togethers[togetherName] ?: return@forEach
                val eTime = currentTime() - activeTogethersReward.startTime
                val duration = togetherReward.duration * 50
                if (eTime > duration) {
                    removeRewards.add(key)
                    return@forEach
                }
            }
            removeRewards.forEach {
                activeTogethersRewards.remove(it)
            }
        }
    }

    private fun startCelebration(togetherName: String, togetherReward: TogethersReward, playerName: String) {
        activeTogethersRewards["${togetherName}-${currentTime()}"] = ActiveTogethersReward(
            togetherName,
            currentTime(),
            onlinePlayers().map { it.uuid },
            mutableSetOf()
        )
        onlinePlayers().forEach {
            togetherReward.startActions?.execute(it, "{player}", playerName)
        }
    }

    @EventHandler
    fun onChat(event: AsyncChatEvent) {
        submitTask {
            val player = event.player
            val message = event.message().text()
            val removeRewards = mutableSetOf<String>()
            activeTogethersRewards.forEach { (key, activeTogethersReward) ->
                val togetherName = activeTogethersReward.togetherName
                if (!activeTogethersReward.availablePlayers.contains(player.uuid)) return@forEach
                if (activeTogethersReward.players.contains(player.uuid)) return@forEach

                val togetherReward = ConfigCEL.togethers[togetherName] ?: return@forEach
                val eTime = currentTime() - activeTogethersReward.startTime
                val duration = togetherReward.duration * 50
                if (eTime > duration) {
                    removeRewards.add(key)
                    return@forEach
                }
                if (message.length < togetherReward.msgLength) {
                    return@forEach
                }
                if (togetherReward.chinese && !isChinese(message)) {
                    return@forEach
                }
                togetherReward.reward?.execute(player)
                activeTogethersReward.players.add(player.uuid)
                if (activeTogethersReward.players.size == activeTogethersReward.availablePlayers.size) {
                    removeRewards.add(key)
                }
            }
            removeRewards.forEach {
                activeTogethersRewards.remove(it)
            }
        }
    }

    private fun isChinese(string: String): Boolean {
        var n: Int
        for (element in string) {
            n = element.code
            if (n !in 19968 .. 40868) {
                return false
            }
        }
        return true
    }

    @EventHandler
    fun onPacket(event: ServerPacketReceiveEvent) {
        val buf = event.buf
        when (event.packet) {
            CELEBRATION_ALL -> {
                val togetherName = buf.readString()
                val playerName = buf.readString()
                val togetherReward = ConfigCEL.togethers[togetherName] ?: return
                startCelebration(togetherName, togetherReward, playerName)
            }
        }
    }

    class ActiveTogethersReward(
        val togetherName: String,
        val startTime: Long,
        val availablePlayers: List<UUID>,
        val players: MutableSet<UUID>
    )

}