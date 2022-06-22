package io.github.mainyf.playeraccount

import io.github.mainyf.newmclib.command.cmdParser
import io.github.mainyf.newmclib.exts.msg
import io.github.mainyf.playeraccount.storage.StorageManager
import org.bukkit.command.*
import org.bukkit.entity.Player


object CommandHandler : CommandExecutor, TabExecutor {

    private val phoneNumbersRegex = "^1[3,4,5,7,8]\\d{9}\$".toRegex()

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (!sender.hasPermission("${PlayerAccount.INSTANCE.name}.command")) return false
        cmdParser(sender, args) {
            val type = arg<String>() ?: return@cmdParser
            when (type) {
                "bind" -> {
                    val player = arg<Player>() ?: return@cmdParser
                    val phoneNumbers = arg<String>() ?: return@cmdParser
                    if (!phoneNumbersRegex.matches(phoneNumbers)) {
                        player.msg("手机号格式错误")
                        return@cmdParser
                    }
                    val data = StorageManager.getPlayerAccount(player)
                    if (data != null) {
                        player.msg("您已经绑定了手机号")
                        return@cmdParser
                    }
                    SMSManager.send(player, phoneNumbers)
                }
                "code" -> {
                    val player = arg<Player>() ?: return@cmdParser
                    val code = arg<String>() ?: return@cmdParser
                    val data = SMSManager.validateCode(player, code)
                    if (data == null) {
                        player.msg("验证码错误")
                        return@cmdParser
                    }
                    StorageManager.updatePlayerAccount(player, data.phoneNumbers)
                    player.msg("绑定成功")
                }
                "info" -> {
                    val player = arg<Player>() ?: return@cmdParser
                    val data = StorageManager.getPlayerAccount(player)
                    if (data == null) {
                        player.msg("您暂未绑定手机号")
                    } else {
                        player.msg("玩家: ${player.name}")
                        player.msg("手机号: ${data.phoneNumber}")
                    }
                }
            }
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): MutableList<String> {

        return mutableListOf()
    }
}