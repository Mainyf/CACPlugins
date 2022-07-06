package io.github.mainyf.myislands.menu

import com.plotsquared.core.plot.Plot
import io.github.mainyf.myislands.IslandsManager
import io.github.mainyf.myislands.config.ConfigManager
import io.github.mainyf.myislands.config.sendLang
import io.github.mainyf.myislands.storage.PlayerIsland
import io.github.mainyf.newmclib.config.IaIcon
import io.github.mainyf.newmclib.exts.pagination
import io.github.mainyf.newmclib.exts.setDisplayName
import io.github.mainyf.newmclib.exts.uuid
import io.github.mainyf.newmclib.menu.AbstractMenuHandler
import io.github.mainyf.newmclib.utils.Heads
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import kotlin.math.ceil

class IslandsHelperSelectMenu(
    val island: PlayerIsland,
    val plot: Plot,
    val players: List<Player>
) : AbstractMenuHandler() {

    var pageIndex = 1
    var maxPageIndex = 0

    private val currentPlayers = mutableListOf<Player>()

    override fun open(player: Player) {
        setup(ConfigManager.helperSelectMenuConfig.settings)

        maxPageIndex =
            ceil(players.size.toDouble() / ConfigManager.helperSelectMenuConfig.playerListSlot.slot.size.toDouble()).toInt()

        val inv = createInv(player)

        updateInv(player, inv)

        player.openInventory(inv)
    }

    override fun updateTitle(player: Player): String {
        val menuConfig = ConfigManager.helperSelectMenuConfig
        val icons = mutableListOf<IaIcon>()
        icons.addAll(menuConfig.prevSlot.itemSlot.iaIcons.icons())
        icons.addAll(menuConfig.nextSlot.itemSlot.iaIcons.icons())
        icons.addAll(menuConfig.backSlot.itemSlot.iaIcons.icons())

        return applyTitle(player, icons)
    }

    private fun updateInv(player: Player, inv: Inventory) {
        val menuConfig = ConfigManager.helperSelectMenuConfig

        inv.setIcon(menuConfig.prevSlot) {
            menuConfig.prevSlot.itemSlot.execAction(it)
            if (pageIndex > 1) {
                pageIndex--
                updatePlayerList(player, inv)
            }
        }
        inv.setIcon(menuConfig.nextSlot) {
            menuConfig.nextSlot.itemSlot.execAction(it)
            if (pageIndex < maxPageIndex) {
                pageIndex++
                updatePlayerList(player, inv)
            }
        }

        inv.setIcon(menuConfig.backSlot) {
            menuConfig.backSlot.itemSlot.execAction(it)
            IslandsSettingsMenu(island, plot).open(it)
        }
        updatePlayerList(player, inv)
    }

    private fun updateList() {
        val menuConfig = ConfigManager.helperSelectMenuConfig
        currentPlayers.clear()
        currentPlayers.addAll(players.pagination(pageIndex, menuConfig.playerListSlot.slot.size))
    }

    private fun updatePlayerList(player: Player, inv: Inventory) {
        updateList()
        val menuConfig = ConfigManager.helperSelectMenuConfig
        inv.unSetIcon(menuConfig.playerListSlot.slot)
        currentPlayers.forEachIndexed { index, p ->
            val slot = menuConfig.playerListSlot.slot.getOrNull(index) ?: return@forEachIndexed
            val skullItem = Heads.getPlayerHead(p.name).clone()
            skullItem.setDisplayName(p.name)
            val pName = p.name
            val uuid = p.uuid
            inv.setIcon(slot, skullItem) {
                menuConfig.playerListSlot.itemSlot.execAction(it)
                IslandsManager.addHelpers(plot, player, island, uuid)
                it.sendLang("addIslandHelperSuccess", mapOf("{player}" to pName))
                p.sendLang("beAddIslandHelperSuccess", mapOf("{player}" to it.name))
                IslandsSettingsMenu(island, plot).open(it)
            }
        }
    }

}