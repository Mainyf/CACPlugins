package io.github.mainyf.socialsystem.module

import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.hooks.money
import io.github.mainyf.newmclib.hooks.takeMoney
import io.github.mainyf.socialsystem.SocialSystem
import io.github.mainyf.socialsystem.config.ConfigSS
import io.github.mainyf.socialsystem.storage.StorageSS
import io.papermc.paper.event.player.AsyncChatEvent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.scheduler.BukkitRunnable
import org.joda.time.DateTime
import kotlin.collections.any

object NicknameConversation : Listener {

    private val tasks = mutableListOf<BukkitRunnable>()
    private val conversations = mutableSetOf<Player>()

    fun initTasks() {
        val config = ConfigSS.nicknameConfig
        tasks.add(SocialSystem.INSTANCE.submitTask(delay = 10L, period = config.tipsPeriod) {
            conversations.forEach {
                config.tipsAction?.execute(it)
            }
        })
    }

    fun endTasks() {
        tasks.forEach {
            it.cancel()
        }
        tasks.clear()
    }

    fun join(player: Player) {
        if (!player.isOp) {
            val prevModifyTime = StorageSS.getNicknamePrevModifyTime(player.uuid)
            if (prevModifyTime != null) {
                val elapsedTime = DateTime.now().millis - prevModifyTime.millis
                val cooldownMillis = ConfigSS.nicknameConfig.modifyCooldown * 24 * 60 * 60 * 1000L
                if (elapsedTime < cooldownMillis) {
                    ConfigSS.nicknameConfig.nicknameCooldownAction?.execute(
                        player,
                        "{time}", (cooldownMillis - elapsedTime).timestampConvertTime()
                    )
                    return
                }
            }
        }
        conversations.add(player)
    }

    fun quit(player: Player, action: Boolean = true) {
        if (conversations.contains(player)) {
            conversations.remove(player)
            if (action) {
                ConfigSS.nicknameConfig.quitAction?.execute(player)
            }
        }
    }

    @EventHandler
    fun onChat(event: AsyncChatEvent) {
        val player = event.player
        if (!conversations.contains(player)) return
        event.isCancelled = true
        event.viewers().clear()
        val text = event.message().text()
        if (text.length < 0) {
            return
        }
        if (text.length < ConfigSS.nicknameConfig.minLength) {
            ConfigSS.nicknameConfig.minLengthAction?.execute(player, "{length}", ConfigSS.nicknameConfig.minLength)
            return
        }
        if (text.length > ConfigSS.nicknameConfig.maxLength) {
            ConfigSS.nicknameConfig.maxLengthAction?.execute(player, "{length}", ConfigSS.nicknameConfig.minLength)
            return
        }
        if (!text.isChinese()) {
            ConfigSS.nicknameConfig.chineseAction?.execute(player)
            return
        }
        if (!player.isOp) {
            if (ConfigSS.nicknameConfig.sensitiveWord.any {
                    text.contains(it)
                }) {
                ConfigSS.nicknameConfig.sensitiveAction?.execute(player)
                return
            }
            if (StorageSS.containsNickname(text)) {
                ConfigSS.nicknameConfig.repatNicknameAction?.execute(player)
                return
            }
        }
        val money = player.money()
        if (ConfigSS.nicknameConfig.cost > money) {
            ConfigSS.nicknameConfig.costLackAction?.execute(player, "{money}", money.toString())
            return
        }
        player.takeMoney(ConfigSS.nicknameConfig.cost)
        StorageSS.setNickname(player.uuid, text)
        quit(player, false)
        ConfigSS.nicknameConfig.successAction?.execute(player, "{nickname}", text, "{player}", player.name)
    }

    @EventHandler
    fun onSwap(event: PlayerSwapHandItemsEvent) {
        quit(event.player)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        quit(event.player)
    }

    @EventHandler
    fun onDeath(event: PlayerDeathEvent) {
        quit(event.entity)
    }

}