package io.github.mainyf.myislands.menu

import com.plotsquared.core.plot.Plot
import io.github.mainyf.myislands.IslandsManager
import io.github.mainyf.myislands.config.ConfigManager
import io.github.mainyf.myislands.config.sendLang
import io.github.mainyf.myislands.storage.PlayerIsland
import io.github.mainyf.newmclib.config.IaIcon
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.menu.AbstractMenuHandler
import io.github.mainyf.newmclib.utils.Heads
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
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
        this.cooldownTime = ConfigManager.helperSelectMenuConfig.cooldown

        maxPageIndex =
            ceil(players.size.toDouble() / ConfigManager.helperSelectMenuConfig.playerListSlot.slot.size.toDouble()).toInt()

        val inv = Bukkit.createInventory(
            createHolder(player),
            ConfigManager.helperSelectMenuConfig.row * 9,
            Component.text(updateTitle(player).colored())
        )

        updateInv(player, inv)

        player.openInventory(inv)
    }

    override fun updateTitle(player: Player): String {
        val menuConfig = ConfigManager.helperSelectMenuConfig
        val icons = mutableListOf<IaIcon>()
        icons.addAll(menuConfig.prevSlot.itemDisplay!!.iaIcons.icons())
        icons.addAll(menuConfig.nextSlot.itemDisplay!!.iaIcons.icons())
        icons.addAll(menuConfig.backSlot.itemDisplay!!.iaIcons.icons())

        val title = "${menuConfig.background} ${icons.sortedBy { it.priority }.joinToString(" ") { it.value }}"
//        val title = menuConfig.background
        player.setOpenInventoryTitle(title)
        return title
    }

    private fun updateInv(player: Player, inv: Inventory) {
        val menuConfig = ConfigManager.helperSelectMenuConfig

        inv.setIcon(menuConfig.prevSlot) {
            menuConfig.prevSlot.action?.execute(it)
            if (pageIndex > 1) {
                pageIndex--
                updatePlayerList(player, inv)
            }
        }
        inv.setIcon(menuConfig.nextSlot) {
            menuConfig.nextSlot.action?.execute(it)
            if (pageIndex < maxPageIndex) {
                pageIndex++
                updatePlayerList(player, inv)
            }
        }

        inv.setIcon(menuConfig.backSlot) {
            menuConfig.backSlot.action?.execute(it)
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
            val skullItem = Heads.getPlayerHead(p.name)
            skullItem.setDisplayName(p.name)
            val pName = p.name
            val uuid = p.uuid
            inv.setIcon(slot, skullItem) {
                menuConfig.playerListSlot.action?.execute(it)
                IslandsManager.addHelpers(plot, player, island, uuid)
                it.sendLang("addIslandHelperSuccess", mapOf("{player}" to pName))
                p.sendLang("beAddIslandHelperSuccess", mapOf("{player}" to it.name))
                IslandsSettingsMenu(island, plot).open(it)
            }
        }
    }

}