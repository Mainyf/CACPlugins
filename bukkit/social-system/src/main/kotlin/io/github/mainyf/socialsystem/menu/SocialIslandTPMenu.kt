package io.github.mainyf.socialsystem.menu

import io.github.mainyf.bungeesettingsbukkit.CrossServerManager
import io.github.mainyf.newmclib.config.IaIcon
import io.github.mainyf.newmclib.menu.AbstractMenuHandler
import io.github.mainyf.newmclib.offline_player_ext.asOfflineData
import io.github.mainyf.socialsystem.config.ConfigSS
import io.github.mainyf.socialsystem.module.IslandStatus
import io.github.mainyf.socialsystem.module.IslandTpReq
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory

class SocialIslandTPMenu(val tpReq: IslandTpReq) : AbstractMenuHandler() {

    private val plot1ServerName = "plot1"
    private val plot2ServerName = "plot2"

    override fun open(player: Player) {
        setup(ConfigSS.socialIslandTPMenuConfig.settings)
        val inv = createInv(player)

        updateInv(player, inv)
        player.openInventory(inv)
    }

    private fun getStatusText(serverName: String): String {
        return when (tpReq.statusMap[serverName]) {
            IslandStatus.ACCESS_NOT_ALLOWED -> "accessNotAllowed"
            IslandStatus.DEFAULT -> "default"
            else -> "empty"
        }
    }

    override fun updateTitle(player: Player): String {
        val sitpMenu = ConfigSS.socialIslandTPMenuConfig
        val icons = mutableListOf<IaIcon>()
        icons.addAll(sitpMenu.plot1Slot.iaIcon(getStatusText(plot1ServerName)))
        icons.addAll(sitpMenu.plot2Slot.iaIcon(getStatusText(plot2ServerName)))
        icons.addAll(sitpMenu.infoSlot.iaIcon())
        icons.addAll(sitpMenu.backSlot.iaIcon())
        return applyTitle(player, icons)
    }

    private fun updateInv(player: Player, inv: Inventory) {
        val sitpMenu = ConfigSS.socialIslandTPMenuConfig

        inv.setIcon(sitpMenu.plot1Slot, key = getStatusText(plot1ServerName)) {
            if(tpReq.statusMap[plot1ServerName] != IslandStatus.DEFAULT) return@setIcon
            val pair = tpReq.locMap[plot1ServerName] ?: return@setIcon
            CrossServerManager.stp(
                player,
                plot1ServerName,
                pair.first,
                pair.second
            )
        }
        inv.setIcon(sitpMenu.plot2Slot, key = (getStatusText(plot2ServerName))) {
            if(tpReq.statusMap[plot2ServerName] != IslandStatus.DEFAULT) return@setIcon
            val pair = tpReq.locMap[plot2ServerName] ?: return@setIcon
            CrossServerManager.stp(
                player,
                plot2ServerName,
                pair.first,
                pair.second
            )
        }
        inv.setIcon(sitpMenu.infoSlot)
        inv.setIcon(sitpMenu.backSlot) {
            SocialMainMenu(it.asOfflineData()).open(it)
        }
    }

}