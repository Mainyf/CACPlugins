package io.github.mainyf.cdkey

import dev.jorel.commandapi.arguments.BooleanArgument
import dev.jorel.commandapi.arguments.GreedyStringArgument
import dev.jorel.commandapi.arguments.IntegerArgument
import io.github.mainyf.cdkey.config.CDKeyType
import io.github.mainyf.cdkey.config.ConfigCDK
import io.github.mainyf.cdkey.config.sendLang
import io.github.mainyf.cdkey.storage.StorageCDK
import io.github.mainyf.newmclib.command.apiCommand
import io.github.mainyf.newmclib.command.offlinePlayerArguments
import io.github.mainyf.newmclib.command.stringArguments
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.offline_player_ext.asOfflineData
import org.apache.logging.log4j.LogManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import kotlin.collections.find

class CDkey : JavaPlugin(), Listener {

    companion object {

        val LOGGER = LogManager.getLogger("CDkey")

        lateinit var INSTANCE: CDkey

    }

    private val playerJoinTimeMap = mutableMapOf<UUID, Long>()
    private lateinit var cdKeyLog: CDKeyLog

    override fun onEnable() {
        INSTANCE = this
        dataFolder.mkdirs()
        cdKeyLog = CDKeyLog(dataFolder.resolve("兑换码日志.log"))
        ConfigCDK.load()
        StorageCDK.init()
        pluginManager().registerEvents(this, this)
        apiCommand("code") {
            withArguments(GreedyStringArgument("兑换码"))
            executePlayer {
                val cdkey = text()
                val defineKeys = ConfigCDK.codeMap.values.filter { it.type == CDKeyType.DEFINE }
                val defineKey = defineKeys.find { it.code == cdkey }
                if (defineKey != null) {
                    if (StorageCDK.hasClaimedCDKey(sender.uuid, defineKey.codeName)) {
                        sender.sendLang("ordinaryCdkeyAlreadyClaimed")
                        return@executePlayer
                    }
                    StorageCDK.claimCDKey(sender.uuid, defineKey.codeName)
                    defineKey.actions?.execute(sender)
                    cdKeyLog.info("玩家: ${sender.name} 类型: define 兑换码: $cdkey")
                    return@executePlayer
                }
                val propagandistKey = ConfigCDK.propagandistMap.values.find { it.code == cdkey }
                if (propagandistKey != null) {
                    if (!playerJoinTimeMap.containsKey(sender.uuid)) {
                        sender.sendLang("prpagandistTimeout")
                        return@executePlayer
                    }
                    val elapsedTime = (currentTime() - playerJoinTimeMap[sender.uuid]!!) / 1000L
                    if (elapsedTime >= ConfigCDK.propagandistCDKeyValidTime) {
                        sender.sendLang("prpagandistTimeout")
                        return@executePlayer
                    }
                    if (StorageCDK.hasClaimedPropagandistCDkey(sender.uuid)) {
                        sender.sendLang("prpagandistSecondTime")
                        return@executePlayer
                    }
                    if (StorageCDK.hasClaimedCDKey(sender.uuid, propagandistKey.codeName)) {
                        sender.sendLang("ordinaryCdkeyAlreadyClaimed")
                        return@executePlayer
                    }
                    StorageCDK.claimCDKey(sender.uuid, propagandistKey.codeName)
                    propagandistKey.actions?.execute(sender)
                    StorageCDK.handlePropagandsitCDKey(propagandistKey.codeName, sender.uuid)
                    cdKeyLog.info("玩家: ${sender.name} 类型: propagandist 兑换码: $cdkey")
                    return@executePlayer
                }
                val consumeCDKeys = StorageCDK.getValidConsumeCDkeys()
                val consumeCDKey = consumeCDKeys.find { it.cdkey == cdkey }
                if (consumeCDKey != null) {
                    if (StorageCDK.hasClaimedCDKey(cdkey)) {
                        sender.sendLang("consumeCdkeyAlreadyClaimed")
                        return@executePlayer
                    }
                    val cdkConfig = ConfigCDK.codeMap[consumeCDKey.codeName]!!
                    StorageCDK.markInValidConsoleCDKeys(consumeCDKey)
                    StorageCDK.claimCDKey(sender.uuid, cdkey)
                    cdkConfig.actions?.execute(sender)
                    cdKeyLog.info("玩家: ${sender.name} 类型: consume 兑换码: $cdkey")
                    return@executePlayer
                }
                sender.sendLang("invalidCDKey")
            }
        }
        apiCommand("cdkey") {
            "player-ppd" {
                withArguments(
                    offlinePlayerArguments("玩家名")
                )
                executeOP {
                    val player = offlinePlayer()
                    val pptCKeyData = StorageCDK.getClaimedPropagandistCDkey(player.uuid)
                    if (pptCKeyData == null) {
                        sender.msg("该玩家没有被任何主播邀请")
                        return@executeOP
                    }
                    sender.msg("该玩家由: ${pptCKeyData.cdkey} 邀请")
                }
            }
            "ppd-invitee" {
                withArguments(
                    stringArguments("主播") { _ -> ConfigCDK.propagandistMap.keys.toTypedArray() }
                )
                executeOP {
                    val codeName = text()
                    val invitees = StorageCDK.getPropagandistCDKeys(codeName)
                    if (invitees.isEmpty()) {
                        sender.msg("该主播没有邀请任何人")
                        return@executeOP
                    }
                    sender.msg(
                        "$codeName 邀请了 ${
                            invitees.joinToString(", ") {
                                it.asOfflineData()!!.name
                            }
                        }"
                    )
                }
            }
            "gen" {
                withArguments(
                    stringArguments("兑换码名字") { _ ->
                        ConfigCDK.codeMap.values.filter { it.type == CDKeyType.CONSUME }.map { it.codeName }
                            .toTypedArray()
                    },
                    IntegerArgument("数量")
                )
                executeOP {
                    val cdkeyName = text()
                    val count = int()
                    val config = ConfigCDK.codeMap[cdkeyName]!!
                    val rs = StorageCDK.generateCDK(config, count)
                    sender.successMsg("[CDkey] 生成成功，共计生成 $rs 个 key")
                }
            }
            "export-cdkey" {
                withArguments(
                    stringArguments("兑换码名字") { _ ->
                        ConfigCDK.codeMap.values.filter { it.type == CDKeyType.CONSUME }.map { it.codeName }
                            .toTypedArray()
                    },
                    BooleanArgument("是否只导出有效的cdkey")
                )
                executeOP {
                    val cdkeyName = text()
                    val valid = value<Boolean>()
                    val config = ConfigCDK.codeMap[cdkeyName]!!
                    val filename = StorageCDK.exportCDKey(config, valid)
                    sender.successMsg("[CDkey] 导出成功，位置: $filename")
                }
            }
            "reload" {
                executeOP {
                    ConfigCDK.load()
                    sender.successMsg("[CDkey] 重载成功")
                }
            }
        }
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        if (!event.player.hasPlayedBefore()) {
            playerJoinTimeMap[event.player.uuid] = currentTime()
        }
    }

}

//fun main() {
//    val zipFile = ZipFile(File("G:\\PCL\\.minecraft\\versions\\1.19.2\\pack.zip"))
//    val output = File("G:\\PCL\\.minecraft\\versions\\1.19.2\\output2")
//    zipFile.entries().toList().forEach {
//        val input = zipFile.getInputStream(it)
//        val bytes = input.readBytes()
//        val outputFile = output.resolve(it.name)
//        if(!outputFile.exists()) {
//            outputFile.parentFile.mkdirs()
//            outputFile.writeBytes(bytes)
//        }
//    }
//}