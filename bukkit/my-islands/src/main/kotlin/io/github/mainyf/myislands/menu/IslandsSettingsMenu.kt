package io.github.mainyf.myislands.menu

import com.destroystokyo.paper.profile.CraftPlayerProfile
import com.plotsquared.core.plot.Plot
import io.github.mainyf.myislands.IslandsManager
import io.github.mainyf.myislands.MyIslands
import io.github.mainyf.myislands.asPlotPlayer
import io.github.mainyf.myislands.config.ConfigManager
import io.github.mainyf.myislands.features.MoveIslandCore
import io.github.mainyf.myislands.storage.IslandVisibility
import io.github.mainyf.myislands.storage.PlayerIsland
import io.github.mainyf.myislands.storage.StorageManager
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.menu.AbstractMenuHandler
import io.github.mainyf.newmclib.offline_player_ext.asOfflineData
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.util.UUID

class IslandsSettingsMenu(
    val island: PlayerIsland,
    val plot: Plot
) : AbstractMenuHandler() {

    override fun open(player: Player) {
        val inv = Bukkit.createInventory(createHolder(player), 3 * 9, Component.text("&a岛屿设置".colored()))

        updateInv(player, inv)

        player.openInventory(inv)
    }

    private fun updateInv(player: Player, inv: Inventory) {
        val settingsMenuConfig = ConfigManager.settingsMenuConfig

        val moveCoreSlot = settingsMenuConfig.moveCoreSlot
        inv.setIcon(moveCoreSlot.slot, moveCoreSlot.itemDisplay!!.toItemStack()) {
            MoveIslandCore.tryStartMoveCore(it)
        }
        val visibilitySlot = settingsMenuConfig.visibilitySlot
        val resetIslandSlot = settingsMenuConfig.resetIslandSlot
        inv.setIcon(visibilitySlot.slot, visibilitySlot.itemDisplay!!.toItemStack {
            setDisplayName(getDisplayName().tvar("visibility", island.visibility.text))
        }) {
            IslandsManager.setIslandVisibility(
                island,
                IslandVisibility.values().find { it.count > island.visibility.count } ?: IslandVisibility.ALL)
            updateInv(player, inv)
        }
        inv.setIcon(resetIslandSlot.slot, resetIslandSlot.itemDisplay!!.toItemStack()) {
            ConfirmMenu { p ->
                IslandsManager.resetIsland(p.asPlotPlayer()!!, plot)
                p.closeInventory()
            }.open(it)
        }
        updateHelper(player, inv)
    }

    private fun updateHelper(player: Player, inv: Inventory) {
        val settingsMenuConfig = ConfigManager.settingsMenuConfig

        val helpersSlot = settingsMenuConfig.helpersSlot.slot
        inv.setIcon(helpersSlot, ItemStack(Material.BARRIER))
        val helpers = IslandsManager.getIslandHelpers(island.id.value)

        for (i in helpers.indices) {
            val helper = helpers[i]
            if (i >= helpersSlot.size) {
                break
            }
            val skullItem = ItemStack(Material.PLAYER_HEAD)
            val itemMeta = skullItem.itemMeta as SkullMeta
            val offlinePlayer = helper.asOfflineData()!!
            itemMeta.playerProfile = CraftPlayerProfile(offlinePlayer.id, offlinePlayer.name)
            skullItem.itemMeta = itemMeta
            inv.setIcon(helpersSlot[i], skullItem)
        }
    }

//    override fun onClick(slot: Int, player: Player, inv: Inventory, event: InventoryClickEvent) {
//        println("slot: $slot")
//    }

}