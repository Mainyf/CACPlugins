package io.github.mainyf.loginsettings

import fr.xephi.authme.api.v3.AuthMeApi
import fr.xephi.authme.events.LoginEvent
import fr.xephi.authme.message.MessageKey
import io.github.mainyf.loginsettings.config.ConfigManager
import io.github.mainyf.loginsettings.menu.TeachingMenu
import io.github.mainyf.newmclib.exts.onlinePlayers
import io.github.mainyf.newmclib.exts.submitTask
import io.github.mainyf.newmclib.exts.text
import io.github.mainyf.newmclib.exts.uuid
import io.papermc.paper.event.player.AsyncChatEvent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerResourcePackStatusEvent
import org.bukkit.event.player.PlayerResourcePackStatusEvent.Status.*
import org.bukkit.scheduler.BukkitRunnable
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object PlayerAuthHandler : BukkitRunnable(), Listener {

    private val registeredStage1Map = ConcurrentHashMap<UUID, BukkitRunnable>()
    private val prevPasswordMap = ConcurrentHashMap<UUID, String>()
    private val registeredStage2Map = ConcurrentHashMap<UUID, BukkitRunnable>()
    private val unAuthMap = ConcurrentHashMap<UUID, BukkitRunnable>()

    override fun run() {
        val authMeAPI = AuthMeApi.getInstance()
        onlinePlayers().forEach { player ->
            val inRegStage1 = registeredStage2Map.containsKey(player.uuid)
            val inRegStage2 = registeredStage1Map.containsKey(player.uuid)
            if (!authMeAPI.isRegistered(player.name) && !authMeAPI.isAuthenticated(player) && !inRegStage1 && !inRegStage2
            ) {
                if (!registeredStage1Map.containsKey(player.uuid)) {
                    startRegisterStage1(player)
                }
            } else {
                if (!authMeAPI.isAuthenticated(player) && !unAuthMap.containsKey(player.uuid) && !inRegStage1 && !inRegStage2) {
                    startLoginStage(player)
                }
            }
        }
    }

    fun startLoginStage(player: Player) {
        unAuthMap[player.uuid] =
            LoginSettings.INSTANCE.submitTask(period = ConfigManager.playerLogin.stage1Period) {
                ConfigManager.playerLogin.stage1Actions?.execute(player)
            }
    }

    fun startRegisterStage1(player: Player) {
        registeredStage1Map[player.uuid] =
            LoginSettings.INSTANCE.submitTask(period = ConfigManager.playerRegister.stage1Period) {
                ConfigManager.playerRegister.stage1Actions?.execute(player)
            }
    }

    fun startRegisterStage2(player: Player) {
        registeredStage2Map[player.uuid] =
            LoginSettings.INSTANCE.submitTask(period = ConfigManager.playerRegister.stage2Period) {
                ConfigManager.playerRegister.stage2Actions?.execute(player)
            }
    }

    fun cleanup(player: Player) {
        registeredStage1Map.remove(player.uuid)?.cancel()
        prevPasswordMap.remove(player.uuid)
        registeredStage2Map.remove(player.uuid)?.cancel()

        unAuthMap.remove(player.uuid)?.cancel()
    }

    @EventHandler(ignoreCancelled = false)
    fun onChat(event: AsyncChatEvent) {
        val player = event.player
        val text = event.message().text()
        if (registeredStage1Map.containsKey(player.uuid)) {
            kotlin.run {
                if (!AuthMeUtils.validatePassword(player, text)) {
                    return@run
                }
                registeredStage1Map.remove(player.uuid)?.cancel()
                prevPasswordMap[player.uuid] = text
                startRegisterStage2(player)
            }
            event.isCancelled = true
        } else if (registeredStage2Map.containsKey(player.uuid) && prevPasswordMap.containsKey(player.uuid)) {
            val password = prevPasswordMap[player.uuid]!!
            kotlin.run {
                if (text != password) {
                    LoginSettings.commonService.send(player, MessageKey.PASSWORD_MATCH_ERROR)
                    ConfigManager.playerRegister.registerNoRepeat?.execute(player)

                    // back stage1
                    cleanup(player)
                    startRegisterStage1(player)
                    return@run
                }
                if (!AuthMeUtils.preRegisterCheck(player)) {
                    return@run
                }
                cleanup(player)
                AuthMeApi.getInstance().registerPlayer(player.name, text)
                AuthMeApi.getInstance().forceLogin(player)
                ConfigManager.playerRegister.registerSuccess?.execute(player)
            }
            event.isCancelled = true
        } else if (unAuthMap.containsKey(player.uuid)) {
            kotlin.run {
                if (!AuthMeApi.getInstance().checkPassword(player.name, text)) {
                    ConfigManager.playerLogin.passwordWrong?.execute(player)
                    return@run
                }
                cleanup(player)
                ConfigManager.playerLogin.loginSuccess?.execute(player)
                AuthMeApi.getInstance().forceLogin(player)
            }
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        cleanup(event.player)
    }

    @EventHandler
    fun onLogin(event: LoginEvent) {
        event.player.setResourcePack(ConfigManager.resourcePack.url, null)
    }

    @EventHandler
    fun onPack(event: PlayerResourcePackStatusEvent) {
        val player = event.player
        when (event.status) {
            SUCCESSFULLY_LOADED -> {
                ConfigManager.resourcePack.successLoad?.execute(player)
                TeachingMenu().open(player)
            }
            DECLINED -> {
                ConfigManager.resourcePack.declined?.execute(player)
            }
            FAILED_DOWNLOAD -> {
                ConfigManager.resourcePack.failDownload?.execute(player)
                event.player.setResourcePack(ConfigManager.resourcePack.url, null)
            }
            else -> {}
        }
    }

}