package io.github.mainyf.loginsettings

import fr.xephi.authme.events.AuthMeAsyncPreRegisterEvent
import fr.xephi.authme.message.MessageKey
import fr.xephi.authme.permission.PlayerStatePermission
import fr.xephi.authme.settings.properties.RegistrationSettings
import fr.xephi.authme.settings.properties.RestrictionSettings
import fr.xephi.authme.util.InternetProtocolUtils
import fr.xephi.authme.util.PlayerUtils
import org.bukkit.entity.Player
import java.util.*

object AuthMeUtils {

    val validationService get() = LoginSettings.validationService
    val commonService get() = LoginSettings.commonService
    val playerCache get() = LoginSettings.playerCache
    val database get() = LoginSettings.database
    val bukkitService get() = LoginSettings.bukkitService

    fun validatePassword(player: Player, password: String): Boolean {
        val passwordValidation = validationService.validatePassword(password, player.name)
        return if (passwordValidation.hasError()) {
            commonService.send(player, passwordValidation.messageKey, *passwordValidation.args)
            false
        } else {
            true
        }
    }

    fun preRegisterCheck(player: Player): Boolean {
        val name = player.name.lowercase(Locale.getDefault())
        return if (playerCache.isAuthenticated(name)) {
            commonService.send(player, MessageKey.ALREADY_LOGGED_IN_ERROR)
            false
        } else if (!commonService.getProperty(RegistrationSettings.IS_ENABLED)) {
            commonService.send(player, MessageKey.REGISTRATION_DISABLED)
            false
        } else if (this.database.isAuthAvailable(name)) {
            commonService.send(player, MessageKey.NAME_ALREADY_REGISTERED)
            false
        } else {
            val event = this.bukkitService.createAndCallEvent { isAsync: Boolean? ->
                AuthMeAsyncPreRegisterEvent(
                    player,
                    isAsync!!
                )
            } as AuthMeAsyncPreRegisterEvent
            if (!event.canRegister()) {
                false
            } else {
                this.isPlayerIpAllowedToRegister(player)
            }
        }
    }

    private fun isPlayerIpAllowedToRegister(player: Player): Boolean {
        val maxRegPerIp = commonService.getProperty(RestrictionSettings.MAX_REGISTRATION_PER_IP) as Int
        val ip = PlayerUtils.getPlayerIp(player)
        if (maxRegPerIp > 0 && !InternetProtocolUtils.isLoopbackAddress(ip) && !commonService.hasPermission(
                player,
                PlayerStatePermission.ALLOW_MULTIPLE_ACCOUNTS
            )
        ) {
            val otherAccounts: List<String> = database.getAllAuthsByIp(ip)
            if (otherAccounts.size >= maxRegPerIp) {
                commonService.send(
                    player,
                    MessageKey.MAX_REGISTER_EXCEEDED,
                    maxRegPerIp.toString(), otherAccounts.size.toString(), java.lang.String.join(", ", otherAccounts)
                )
                return false
            }
        }
        return true
    }

}