package io.github.mainyf.loginsettings.module

import io.github.mainyf.loginsettings.LoginSettings
import io.github.mainyf.loginsettings.config.ConfigManager
import io.github.mainyf.loginsettings.storage.StorageManager
import io.github.mainyf.newmclib.exts.*
import io.papermc.paper.event.player.AsyncChatEvent
import me.dreamvoid.miraimc.bukkit.event.message.passive.MiraiGroupMessageEvent
import org.apache.commons.lang3.RandomStringUtils
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

object ResetPasswords : Listener {

    private val unVerityPlayers = mutableMapOf<Player, Pair<Long, String>>()
    private val resetPasswordStage1 = mutableSetOf<Player>()
    private val resetPasswordStage2 = mutableMapOf<Player, String>()

    fun hasResetPasswd(player: Player): Boolean {
        return unVerityPlayers.containsKey(player) || resetPasswordStage1.contains(player) || resetPasswordStage2.containsKey(
            player
        )
    }

    fun init() {
        LoginSettings.INSTANCE.submitTask(period = 20L) {
            unVerityPlayers.keys.forEach {
                if(!it.isOnline) {
                    clean(it)
                }
            }
            unVerityPlayers.forEach {
                ConfigManager.resetPasswdConfig.actions?.execute(
                    it.key,
                    "{player}", it.key.name,
                    "{qqNum}", it.value.first,
                    "{code}", it.value.second
                )
            }
            resetPasswordStage1.forEach {
                if(!it.isOnline) {
                    clean(it)
                }
            }
            resetPasswordStage1.forEach {
                ConfigManager.resetPasswdConfig.sendNewPasswd?.execute(it)
            }
            resetPasswordStage2.keys.forEach {
                if(!it.isOnline) {
                    clean(it)
                }
            }
            resetPasswordStage2.forEach {
                ConfigManager.resetPasswdConfig.confirmNewPasswd?.execute(it.key)
            }
        }
    }

    fun startResetPassword(player: Player) {
        val qqNum = StorageManager.getLinkQQNum(player.uuid)
        if (qqNum == null) {
            player.errorMsg("内部错误: LQ0x2")
            LoginSettings.LOGGER.error("内部错误: LQ0x2")
            return
        }
        unVerityPlayers[player] = qqNum to RandomStringUtils.randomNumeric(4)
    }

    @EventHandler
    fun onGroupMessage(event: MiraiGroupMessageEvent) {
        if (event.botID != ConfigManager.qqBot) return
        if (!ConfigManager.qqGroup.contains(event.groupID)) return
        val senderId = event.senderID
        val message = event.message
        val p = unVerityPlayers.findKey { it.value.first == senderId && it.value.second == message } ?: return
        ConfigManager.resetPasswdConfig.veritySuccess?.let { veritySuccessMsg ->
            val msg = veritySuccessMsg.tvar("player", p.name)
            event.group.sendMessageMirai("[mirai:at:$senderId] $msg")
        }
//        ConfigManager.resetPasswdConfig.veritySuccess?.let { veritySuccessMsg ->
//            val msg = veritySuccessMsg.tvar("player", p.name)
//            event.reply(msg)
//        }
        unVerityPlayers.remove(p)
        ConfigManager.resetPasswdConfig.nextStage?.execute(p)
        resetPasswordStage1.add(p)
        ConfigManager.resetPasswdConfig.sendNewPasswd?.execute(p)
    }

    @EventHandler(ignoreCancelled = false)
    fun onChat(event: AsyncChatEvent) {
        val player = event.player
        val text = event.message().text()
        when {
            resetPasswordStage1.contains(player) -> {
                event.isCancelled = true
                val passwordValidation = LoginSettings.validationService.validatePassword(text, player.name)
                if (passwordValidation.hasError()) {
                    LoginSettings.commonService.send(player, passwordValidation.messageKey, *passwordValidation.args)
                    return
                }
                resetPasswordStage1.remove(player)
                ConfigManager.resetPasswdConfig.nextStage?.execute(player)
                resetPasswordStage2[player] = text
                ConfigManager.resetPasswdConfig.confirmNewPasswd?.execute(player)
            }
            resetPasswordStage2.contains(player) -> {
                event.isCancelled = true
                val prevPasswd = resetPasswordStage2[player]!!
                if (text != prevPasswd) {
                    resetPasswordStage2.remove(player)
                    ConfigManager.resetPasswdConfig.passwdDiscrepancy?.execute(player)
                    ConfigManager.resetPasswdConfig.nextStage?.execute(player)
                    resetPasswordStage1.add(player)
                    ConfigManager.resetPasswdConfig.sendNewPasswd?.execute(player)
                    return
                }
                resetPasswordStage2.remove(player)
                LoginSettings.asyncChangePassword.changePasswordAsAdmin(console(), player.name, prevPasswd)
                ConfigManager.resetPasswdConfig.finish?.execute(player)
            }
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        clean(event.player)
    }

    fun clean(player: Player) {
        unVerityPlayers.remove(player)
        resetPasswordStage1.remove(player)
        resetPasswordStage2.remove(player)
    }

}