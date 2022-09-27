package io.github.mainyf.loginsettings.module

import fr.xephi.authme.events.UnregisterByAdminEvent
import fr.xephi.authme.events.UnregisterByPlayerEvent
import io.github.mainyf.loginsettings.LoginSettings
import io.github.mainyf.loginsettings.storage.StorageManager
import io.github.mainyf.newmclib.exts.uuid
import io.github.mainyf.newmclib.offline_player_ext.asOfflineData
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

object AuthmeListeners : Listener {

    @EventHandler
    fun onUnRegisterByAdmin(event: UnregisterByAdminEvent) {
        val pUUID = event.playerName.asOfflineData()?.uuid
        if (pUUID == null) {
            LoginSettings.LOGGER.error("没有找到 ${event.playerName} 的玩家数据")
            return
        }
        StorageManager.removeLinkQQ(pUUID)
    }

    @EventHandler
    fun onUnRegisterByPlayer(event: UnregisterByPlayerEvent) {
        StorageManager.removeLinkQQ(event.player.uuid)
    }

}