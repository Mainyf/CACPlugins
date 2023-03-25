package io.github.mainyf.loginsettings.module

import fr.xephi.authme.api.v3.AuthMeApi
import fr.xephi.authme.events.LoginEvent
import fr.xephi.authme.message.MessageKey
import io.github.mainyf.loginsettings.AuthMeUtils
import io.github.mainyf.loginsettings.LoginSettings
import io.github.mainyf.loginsettings.config.ConfigLS
import io.github.mainyf.loginsettings.menu.TeachingMenu
import io.github.mainyf.loginsettings.storage.StorageLS
import io.github.mainyf.newmclib.exts.*
import io.papermc.paper.event.player.AsyncChatEvent
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.*
import org.bukkit.event.player.PlayerResourcePackStatusEvent.Status.*
import org.bukkit.scheduler.BukkitRunnable
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object PlayerAuths : BukkitRunnable(), Listener {

    private val registeredStage1Set = mutableSetOf<UUID>()
    private val prevPasswordMap = ConcurrentHashMap<UUID, String>()
    private val registeredStage2Set = mutableSetOf<UUID>()
    private val unAuths = mutableSetOf<UUID>()
    private val playerResStatus = ConcurrentHashMap<UUID, PlayerResourcePackStatusEvent.Status>()
    private val playerBindings = mutableSetOf<UUID>()
    private val playerResetPasswding = mutableSetOf<UUID>()

    private val passwordWrongTimeMap = mutableMapOf<UUID, Long>()
    private val passwordWrongMap = mutableMapOf<UUID, Int>()
    private val ipBlackMap = mutableMapOf<String, Long>()

    fun init() {
        val authMeAPI = AuthMeApi.getInstance()
        LoginSettings.INSTANCE.submitTask(period = ConfigLS.playerLogin.stage1Period) {
            onlinePlayers().forEach { player ->
                if (playerBindings.contains(player.uuid)) return@forEach
                if (playerResetPasswding.contains(player.uuid)) return@forEach
                if (registeredStage1Set.contains(player.uuid)) return@forEach
                if (registeredStage2Set.contains(player.uuid)) return@forEach
                if (!authMeAPI.isRegistered(player.name)) return@forEach
                if (authMeAPI.isAuthenticated(player)) return@forEach
                ConfigLS.playerLogin.stage1Actions?.execute(player)
            }
        }
        LoginSettings.INSTANCE.submitTask(period = ConfigLS.playerRegister.stage1Period) {
            onlinePlayers().forEach { player ->
                if (playerBindings.contains(player.uuid)) return@forEach
                if (playerResetPasswding.contains(player.uuid)) return@forEach
                if (authMeAPI.isRegistered(player.name)) return@forEach
                if (authMeAPI.isAuthenticated(player)) return@forEach
                if (registeredStage1Set.contains(player.uuid)) {
                    ConfigLS.playerRegister.stage1Actions?.execute(player)
                }
            }
        }
        LoginSettings.INSTANCE.submitTask(period = ConfigLS.playerRegister.stage2Period) {
            onlinePlayers().forEach { player ->
                if (playerBindings.contains(player.uuid)) return@forEach
                if (playerResetPasswding.contains(player.uuid)) return@forEach
                if (authMeAPI.isRegistered(player.name)) return@forEach
                if (authMeAPI.isAuthenticated(player)) return@forEach
                ConfigLS.playerRegister.stage1Actions?.execute(player)
                if (registeredStage2Set.contains(player.uuid)) {
                    ConfigLS.playerRegister.stage2Actions?.execute(player)
                }
            }
        }
        //        protocolManager().addPacketListener(object :
        //            PacketAdapter(LoginSettings.INSTANCE, PacketType.Play.Client.BLOCK_DIG) {
        //
        //            override fun onPacketReceiving(event: PacketEvent) {
        //                val player = event.player
        //                val digType = event.packet.playerDigTypes.read(0)
        //                if (digType == EnumWrappers.PlayerDigType.SWAP_HELD_ITEMS) {
        //                    onSwap(player)
        //                }
        //            }
        //
        //        })
    }

    override fun run() {
        val authMeAPI = AuthMeApi.getInstance()
        onlinePlayers().forEach { player ->
            if (playerBindings.contains(player.uuid)) return@forEach
            if (playerResetPasswding.contains(player.uuid)) return@forEach

            val inRegStage1 = registeredStage2Set.contains(player.uuid)
            val inRegStage2 = registeredStage1Set.contains(player.uuid)
            val hasAuth = authMeAPI.isAuthenticated(player)

            if (!authMeAPI.isRegistered(player.name) && !hasAuth && !inRegStage1 && !inRegStage2
            ) {
                registeredStage1Set.add(player.uuid)
            } else {
                if (!hasAuth && !unAuths.contains(player.uuid) && !inRegStage1 && !inRegStage2) {
                    unAuths.add(player.uuid)
                }
            }
        }
    }

    fun cleanup(player: Player) {
        registeredStage1Set.remove(player.uuid)
        prevPasswordMap.remove(player.uuid)
        registeredStage2Set.remove(player.uuid)

        unAuths.remove(player.uuid)
        //        passwordWrongMap.remove(player.uuid)
    }

    @EventHandler
    fun onSwap(event: PlayerSwapHandItemsEvent) {
        onSwap(event.player)
    }

    private fun onSwap(player: Player) {
        if (playerResetPasswding.contains(player.uuid)) return
        if (!unAuths.contains(player.uuid)) return
        if (!ConfigLS.qqEnable) {
            ConfigLS.emergencyAction?.execute(player)
            return
        }
        if (!StorageLS.hasLinkQQ(player.uuid)) {
            ConfigLS.playerLogin.noBindQQNum?.execute(player)
            return
        }
        playerResetPasswding.add(player.uuid)
        cleanup(player)
        ConfigLS.playerLogin.nextStage?.execute(player)
        ResetPasswords.startResetPassword(player)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onCommand(event: PlayerCommandPreprocessEvent) {
        val authMeAPI = AuthMeApi.getInstance()
//        val msg = event.message.replace("/", "")
        if (!authMeAPI.isAuthenticated(event.player)) {
            ConfigLS.noUsageCommand?.execute(event.player)
            event.isCancelled = true
        }
    }

    @EventHandler(ignoreCancelled = false)
    fun onChat(event: AsyncChatEvent) {
        val player = event.player
        val text = event.message().text()
        if (handleMessage(player, text, event)) {
            event.isCancelled = true
        }
    }

    private fun handleMessage(player: Player, text: String, event: Event?): Boolean {
        var isCancelled = false
        when {
            registeredStage1Set.contains(player.uuid) -> {
                kotlin.run {
                    if (!AuthMeUtils.validatePassword(player, text)) {
                        return@run
                    }
                    registeredStage1Set.remove(player.uuid)
                    prevPasswordMap[player.uuid] = text
                    ConfigLS.playerRegister.nextStage?.execute(player)
                    registeredStage2Set.add(player.uuid)
                }
                isCancelled = true
            }

            registeredStage2Set.contains(player.uuid) && prevPasswordMap.containsKey(player.uuid) -> {
                val password = prevPasswordMap[player.uuid]!!
                kotlin.run {
                    if (text != password) {
                        LoginSettings.commonService.send(player, MessageKey.PASSWORD_MATCH_ERROR)
                        ConfigLS.playerRegister.registerNoRepeat?.execute(player)

                        // back stage1
                        cleanup(player)
                        registeredStage1Set.add(player.uuid)
                        return@run
                    }
                    if (!AuthMeUtils.preRegisterCheck(player)) {
                        return@run
                    }
                    playerBindings.add(player.uuid)
                    cleanup(player)
                    ConfigLS.playerRegister.nextStage?.execute(player)
                    if (ConfigLS.qqEnable) {
                        BindQQs.startLinkQQ(player, false, event) {
                            AuthMeApi.getInstance().registerPlayer(player.name, text)
                            AuthMeApi.getInstance().forceLogin(player)
                            ConfigLS.playerRegister.registerSuccess?.execute(player)
                        }
                    } else {
                        AuthMeApi.getInstance().registerPlayer(player.name, text)
                        AuthMeApi.getInstance().forceLogin(player)
                        ConfigLS.playerRegister.registerSuccess?.execute(player)
                    }
                }
                isCancelled = true
            }

            unAuths.contains(player.uuid) -> {
                kotlin.run {
                    if (!AuthMeApi.getInstance().checkPassword(player.name, text)) {
                        val prevTime = passwordWrongTimeMap.getOrPut(player.uuid) { currentTime() }
                        val elapsedTime = currentTime() - prevTime
                        var count = passwordWrongMap.getOrDefault(player.uuid, 0)
                        if (elapsedTime >= ConfigLS.playerLogin.passwordAttemptsMaxTime * 1000L) {
                            count = 1
                        } else {
                            count++
                        }
                        if (count >= ConfigLS.playerLogin.passwordAttempts) {
                            passwordWrongMap[player.uuid] = 0
                            passwordWrongTimeMap.remove(player.uuid)
                            val ip = player.getIP()
                            ipBlackMap[ip] = currentTime()
                            onlinePlayers().forEach {
                                if (it.getIP() == ip) {
                                    kickPlayer(it, ConfigLS.playerLogin.passwordWrongBlackListTime * 60 * 1000L)
                                }
                            }
                            //                        kickPlayer(player, ConfigManager.playerLogin.passwordWrongBlackListTime * 60 * 1000L)
                            cleanup(player)
                            return@run
                        }
                        ConfigLS.playerLogin.passwordWrong?.execute(
                            player,
                            "{player}",
                            player.name,
                            "{count}",
                            (ConfigLS.playerLogin.passwordAttempts - count).toString()
                        )
                        passwordWrongMap[player.uuid] = count
                        return@run
                    }
                    cleanup(player)
                    ConfigLS.playerLogin.loginSuccess?.execute(player)
                    passwordWrongTimeMap.remove(player.uuid)
                    passwordWrongMap[player.uuid] = 0
                    AuthMeApi.getInstance().forceLogin(player)
                }
                isCancelled = true
            }
        }
        return isCancelled
    }

    private fun Player.getIP(): String {
        var ip = address.toString()
        ip = ip.substring(ip.indexOf("/") + 1)
        return ip.split(":")[0]
    }

    private fun kickPlayer(player: Player, eTime: Long) {
        LoginSettings.INSTANCE.submitTask {
            player.kick(
                ConfigLS.playerLogin.blackListKickFormat.tvar(
                    "player", player.name,
                    "time", eTime.timestampConvertTime()
                )
            )
        }
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        val ip = player.getIP()
        if (!ipBlackMap.containsKey(ip)) return
        val prevTime = ipBlackMap[ip]!!
        val expiredTime = prevTime + ConfigLS.playerLogin.passwordWrongBlackListTime * 60 * 1000L
        val currentTime = currentTime()
        if (currentTime < expiredTime) {
            kickPlayer(player, expiredTime - currentTime)
        } else {
            ipBlackMap.remove(ip)
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        cleanup(event.player)
        playerResStatus.remove(event.player.uuid)
        playerBindings.remove(event.player.uuid)
        playerResetPasswding.remove(event.player.uuid)
    }

    @EventHandler
    fun onLogin(event: LoginEvent) {
        event.player.setResourcePack(ConfigLS.resourcePack.url, null)
    }

    @EventHandler
    fun onPack(event: PlayerResourcePackStatusEvent) {
        val player = event.player
        val authMeAPI = AuthMeApi.getInstance()
        if(!authMeAPI.isAuthenticated(player)) {
            return
        }
        playerResStatus[player.uuid] = event.status
        when (event.status) {
            SUCCESSFULLY_LOADED -> {
                if(authMeAPI.isAuthenticated(player)) {
                    passwordWrongTimeMap.remove(player.uuid)
                    cleanup(player)
                }
                ConfigLS.resourcePack.successLoad?.execute(player)
                TeachingMenu().open(player)
            }

            DECLINED -> {
                ConfigLS.resourcePack.declined?.execute(player)
            }

            FAILED_DOWNLOAD -> {
                ConfigLS.resourcePack.failDownload?.execute(player)
                event.player.setResourcePack(ConfigLS.resourcePack.url, null)
            }

            else -> {}
        }
    }

}