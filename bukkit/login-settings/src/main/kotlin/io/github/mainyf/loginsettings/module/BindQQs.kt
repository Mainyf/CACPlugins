package io.github.mainyf.loginsettings.module

import io.github.mainyf.loginsettings.LoginSettings
import io.github.mainyf.loginsettings.config.ConfigLS
import io.github.mainyf.loginsettings.storage.StorageLS
import io.github.mainyf.newmclib.exts.*
import io.papermc.paper.event.player.AsyncChatEvent
import kotlinx.coroutines.DelicateCoroutinesApi
import me.dreamvoid.miraimc.bukkit.event.message.passive.MiraiGroupMessageEvent
import org.apache.commons.lang3.RandomStringUtils
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import java.util.*

object BindQQs : Listener {

    private val linkQQPlayers = mutableMapOf<Player, Triple<Boolean, Event?, () -> Unit>>()
    private val bindPlayers = mutableSetOf<Player>()
    private val codePlayers = mutableMapOf<Player, QQCode>()
    private val trySendCodePlayers = mutableSetOf<UUID>()

    fun hasBindingQQ(player: Player): Boolean {
        return linkQQPlayers.containsKey(player) || bindPlayers.contains(player) || codePlayers.containsKey(
            player
        )
    }

    fun hasLinkQQ(uuid: UUID): Boolean {
        return StorageLS.hasLinkQQ(uuid)
    }

    fun startLinkQQ(player: Player, hasLogin: Boolean, event: Event? = null, block: () -> Unit) {
        linkQQPlayers[player] = Triple(hasLogin, event, block)
        bindPlayers.add(player)
    }

    fun init() {
        LoginSettings.INSTANCE.submitTask(period = 20L) {
            linkQQPlayers.keys.forEach {
                if (!it.isOnline) {
                    clean(it)
                }
            }
            bindPlayers.forEach {
                bindAction(it)
            }
            codePlayers.forEach {
                codeAction(it.key, it.value)
            }
        }
    }

    private fun bindAction(player: Player) {
        ConfigLS.bindStageConfig.actions?.execute(player)
    }

    private fun codeAction(player: Player, code: QQCode) {
        ConfigLS.codeStageConfig.actions?.execute(
            player,
            "{player}", player.name,
            "{code}", code.code
        )
    }

    @EventHandler(ignoreCancelled = false)
    fun onChat(event: AsyncChatEvent) {
        val player = event.player
        if (linkQQPlayers[player]?.second == event) return
        val text = event.message().text()
        when {
            bindPlayers.contains(player) -> {
                event.isCancelled = true
                val formatError = ConfigLS.bindStageConfig.formatError
                if (!ConfigLS.bindStageConfig.qqRegex.matches(text)) {
                    formatError?.execute(player)
                    return
                }
                val qqNum = text.toLongOrNull()
                if (qqNum == null) {
                    formatError?.execute(player)
                    return
                }
                if (StorageLS.hasLinkAccount(qqNum)) {
                    ConfigLS.bindStageConfig.qqAlreadyBind?.execute(player)
                    return
                }
                bindPlayers.remove(player)
                val code = QQCode(RandomStringUtils.randomNumeric(4), qqNum)
                codePlayers[player] = code
                ConfigLS.bindStageConfig.nextStage?.execute(player)
                codeAction(player, code)
            }
        }
    }

    @EventHandler
    fun onSwap(event: PlayerSwapHandItemsEvent) {
        val player = event.player
        if (codePlayers.containsKey(player)) {
            codePlayers.remove(player)

            bindPlayers.add(player)
            bindAction(player)
        }
    }

//    @EventHandler
//    fun onStrangerMessage(event: MiraiStrangerMessageEvent) {
//        if (event.botID != ConfigManager.codeStageConfig.qqBot) return
//        handleQQMessage(event.senderID, event.message, event)
//    }
//
//    @EventHandler
//    fun onFriendMessage(event: MiraiFriendMessageEvent) {
//        if (event.botID != ConfigManager.codeStageConfig.qqBot) return
//        handleQQMessage(event.senderID, event.message, event)
//    }

    @OptIn(DelicateCoroutinesApi::class)
    @EventHandler
    fun onGroupMessage(event: MiraiGroupMessageEvent) {
        if (event.botID != ConfigLS.qqBot) return
        if (!ConfigLS.qqGroup.contains(event.groupID)) return
        val targetID = event.senderID
        val message = event.message
        synchronized(this) {
            LoginSettings.INSTANCE.submitTask {
                val p = codePlayers.findKey {
                    it.value.qqNum == targetID && it.value.code == message
                }
                if (p != null) {
                    trySendCodePlayers.add(p.uuid)
                    ConfigLS.codeStageConfig.veritySuccess?.let { veritySuccessMsg ->
                        val msg = veritySuccessMsg.tvar("player", p.name)
//                        val mEvent = event.toReflect().get<GroupMessageEvent>("event")
//
//                        GlobalScope.launch {
//                            mEvent.sender.sendMessage(
//                                MessageChainBuilder().append(QuoteReply(mEvent.message)).append(msg).build()
//                            )
//                        }

                        event.group.sendMessageMirai("[mirai:at:$targetID] $msg")
                    }
                    kotlin.runCatching {
                        val pair = linkQQPlayers.remove(p)!!
                        if (pair.first) {
                            ConfigLS.codeStageConfig.loginFinish?.execute(p)
                        } else {
                            ConfigLS.codeStageConfig.registerFinish?.execute(p)
                        }
                        StorageLS.addLinkQQ(p.uuid, targetID)
                        codePlayers.remove(p)
                        pair.third.invoke()
                    }.onFailure {
                        p.errorMsg("内部错误: LQ0x1")
                        it.printStackTrace()
                    }
                }
            }
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        if (linkQQPlayers.contains(event.player)) {
            val (hasLogin, _, _) = linkQQPlayers[event.player]!!
            val hasInputQQ = codePlayers.contains(event.player)
            LoginSettings.INSTANCE.log.info(
                "玩家: ${event.player.name} ${if (hasLogin) "登录" else "注册"} 玩家是否有输入QQ: $hasInputQQ 玩家是否有尝试发送过验证码: ${
                    trySendCodePlayers.contains(
                        event.player.uuid
                    )
                }"
            )
        }
        clean(event.player)
    }

    fun clean(player: Player) {
        linkQQPlayers.remove(player)
        bindPlayers.remove(player)
        codePlayers.remove(player)
    }

    class QQCode(
        val code: String,
        val qqNum: Long
    )

}