package io.github.mainyf.socialsystem

import io.github.mainyf.bungeesettingsbukkit.ServerPacket
import io.github.mainyf.newmclib.BasePlugin
import io.github.mainyf.newmclib.exts.pluginManager
import io.github.mainyf.newmclib.exts.tvar
import io.github.mainyf.newmclib.hooks.addPlaceholderExpansion
import io.github.mainyf.newmclib.hooks.placeholders
import io.github.mainyf.socialsystem.config.ConfigManager
import io.github.mainyf.socialsystem.listeners.PlayerListeners
import io.github.mainyf.socialsystem.module.SocialManager
import io.github.mainyf.socialsystem.storage.StorageManager
import org.apache.logging.log4j.LogManager

class SocialSystem : BasePlugin() {

    companion object {

        val LOGGER = LogManager.getLogger("SocialSystem")

        lateinit var INSTANCE: SocialSystem

        val SOCIAL_LANG = ServerPacket.registerPacket("broadcast_social_lang")

        val FRIEND_TP_REQUEST = ServerPacket.registerPacket("broadcast_social_friend_tp_req")

        val FRIEND_TP_REQUEST_AGREE = ServerPacket.registerPacket("broadcast_social_friend_tp_req_agree")

        val FRIEND_TP_INVITE = ServerPacket.registerPacket("broadcast_social_friend_tp_invite")

        val ISLAND_TP_REQ = ServerPacket.registerPacket("broadcast_island_tp_req")

        val ISLAND_TP_RES = ServerPacket.registerPacket("broadcast_island_tp_res")

        val ISLAND_EMPTY_RES = ServerPacket.registerPacket("broadcast_island_empty_res")

        val ISLAND_NOT_ALLOWED_ACCESS = ServerPacket.registerPacket("broadcast_island_empty_not_allowed_access")

    }

    override fun enable() {
        INSTANCE = this
        ConfigManager.load()
        StorageManager.init()
        CommandHandler.register()
        pluginManager().registerEvents(PlayerListeners, this)
        pluginManager().registerEvents(CrossServerHandler, this)
        addPlaceholderExpansion("socialcard") papi@{ offlinePlayer, params ->
            if (params == "qqnum") {
                return@papi SocialManager.getPlayerQQNum(offlinePlayer?.uniqueId ?: return@papi null)?.toString()
                    ?: "未绑定QQ"
            }
            val player = offlinePlayer?.player ?: return@papi null
            when (params) {
                "tab" -> ConfigManager.getPlayerTabCard(player).tvar("player", player.name).placeholders(player)
                "chat" -> ConfigManager.getPlayerChatCard(player).tvar("player", player.name).placeholders(player)
                else -> null
            }
        }
    }

    override fun onDisable() {
        StorageManager.close()
    }

}