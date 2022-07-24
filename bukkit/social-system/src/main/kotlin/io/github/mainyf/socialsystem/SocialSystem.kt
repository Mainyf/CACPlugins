package io.github.mainyf.socialsystem

import io.github.mainyf.bungeesettingsbukkit.ServerPacket
import io.github.mainyf.newmclib.exts.pluginManager
import io.github.mainyf.newmclib.exts.tvar
import io.github.mainyf.newmclib.hooks.addPlaceholderExpansion
import io.github.mainyf.newmclib.serverId
import io.github.mainyf.newmclib.serverName
import io.github.mainyf.socialsystem.config.ConfigManager
import io.github.mainyf.socialsystem.listeners.PlayerListeners
import io.github.mainyf.socialsystem.module.FriendHandler
import io.github.mainyf.socialsystem.storage.StorageManager
import org.apache.logging.log4j.LogManager
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class SocialSystem : JavaPlugin() {

    companion object {

        val LOGGER = LogManager.getLogger("SocialSystem")

        lateinit var INSTANCE: SocialSystem

        val SOCIAL_LANG = ServerPacket.registerPacket("broadcast_social_lang")

        val FRIEND_TP_REQUEST = ServerPacket.registerPacket("broadcast_social_friend_tp_req")

        val FRIEND_TP_REQUEST_AGREE = ServerPacket.registerPacket("broadcast_social_friend_tp_req_agree")

        val FRIEND_TP_INVITE = ServerPacket.registerPacket("broadcast_social_friend_tp_invite")

//        val FRIEND_TP_INVITE_AGREE = ServerPacket.registerPacket("broadcast_social_friend_tp_invite_agree")

    }

    override fun onEnable() {
        INSTANCE = this
        ConfigManager.load()
        StorageManager.init()
        CommandHandler.register()
        pluginManager().registerEvents(PlayerListeners, this)
        addPlaceholderExpansion("socialcard") papi@{ offlinePlayer, params ->
            val player = offlinePlayer?.player ?: return@papi null
            when (params) {
                "tab" -> ConfigManager.getPlayerTabCard(player).tvar("player", player.name)
                "chat" -> ConfigManager.getPlayerChatCard(player).tvar("player", player.name)
                else -> null
            }
        }
    }

    override fun onDisable() {
        StorageManager.close()
    }

}