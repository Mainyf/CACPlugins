package io.github.mainyf.myislands.menu

import com.plotsquared.bukkit.util.BukkitUtil
import com.plotsquared.core.plot.Plot
import io.github.mainyf.myislands.IslandsManager
import io.github.mainyf.myislands.MyIslands
import io.github.mainyf.myislands.asPlotPlayer
import io.github.mainyf.myislands.config.ConfigManager
import io.github.mainyf.myislands.config.sendLang
import io.github.mainyf.myislands.features.MoveIslandCore
import io.github.mainyf.myislands.storage.IslandVisibility.*
import io.github.mainyf.myislands.storage.PlayerIsland
import io.github.mainyf.newmclib.config.IaIcon
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.menu.AbstractMenuHandler
import io.github.mainyf.newmclib.offline_player_ext.asOfflineData
import io.github.mainyf.newmclib.utils.Heads
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class IslandsSettingsMenu(
    val island: PlayerIsland,
    val plot: Plot
) : AbstractMenuHandler() {

    override fun open(player: Player) {
        this.cooldownTime = ConfigManager.settingsMenuConfig.cooldown
        val inv = Bukkit.createInventory(
            createHolder(player),
            ConfigManager.settingsMenuConfig.row * 9,
            Component.text(updateTitle(player).colored())
        )

        updateInv(player, inv)

        player.openInventory(inv)
    }

    override fun updateTitle(player: Player): String {
        val menuConfig = ConfigManager.settingsMenuConfig
        val icons = mutableListOf<IaIcon>()
        icons.addAll(menuConfig.moveCoreSlot.itemDisplay!!.iaIcons.icons())
        when (island.visibility) {
            ALL -> icons.add(menuConfig.visibilitySlot.itemDisplay!!.iaIcons["all"]!!)
            PERMISSION -> icons.add(menuConfig.visibilitySlot.itemDisplay!!.iaIcons["permission"]!!)
            NONE -> icons.add(menuConfig.visibilitySlot.itemDisplay!!.iaIcons["none"]!!)
        }
        icons.addAll(menuConfig.resetIslandSlot.itemDisplay!!.iaIcons.icons())

        val title = "${menuConfig.background} ${icons.sortedBy { it.priority }.joinToString(" ") { it.value }}"
//        val title = menuConfig.background
        player.setOpenInventoryTitle(title)
        return title
    }

    private fun updateInv(player: Player, inv: Inventory) {
        val settingsMenuConfig = ConfigManager.settingsMenuConfig

        val moveCoreSlot = settingsMenuConfig.moveCoreSlot
        inv.setIcon(moveCoreSlot.slot, moveCoreSlot.itemDisplay!!.toItemStack()) {
            moveCoreSlot.action?.execute(it)
            MoveIslandCore.tryStartMoveCore(it)
            it.closeInventory()
        }
        val visibilitySlot = settingsMenuConfig.visibilitySlot
        val resetIslandSlot = settingsMenuConfig.resetIslandSlot
        inv.setIcon(visibilitySlot.slot, visibilitySlot.itemDisplay!!.toItemStack {
            setDisplayName(getDisplayName().tvar("visibility", island.visibility.text))
        }) { p ->
            visibilitySlot.action?.execute(p)
            IslandsManager.setIslandVisibility(
                island,
                values().find { it.count > island.visibility.count } ?: ALL)
            updateInv(player, inv)
        }
        inv.setIcon(resetIslandSlot.slot, resetIslandSlot.itemDisplay!!.toItemStack()) { p ->
            resetIslandSlot.action?.execute(p)
            IslandsChooseMenu(false, { chooseMenu, player, schematicConfig ->
                IslandsManager.resetIsland(p.asPlotPlayer()!!, plot).whenComplete {
                    MyIslands.INSTANCE.runTaskLaterBR(2 * 20L) {
                        IslandsManager.chooseIslandSchematic(chooseMenu, player, schematicConfig)
                    }
                }
            }, {
                IslandsSettingsMenu(this.island, this.plot).open(it)
            }).open(p)
        }
        updateHelper(player, inv)
    }

    private fun updateHelper(player: Player, inv: Inventory) {
        val settingsMenuConfig = ConfigManager.settingsMenuConfig

        val helpersSlot = settingsMenuConfig.helpersSlot.slot
        inv.setIcon(helpersSlot, settingsMenuConfig.helpersSlot.itemDisplay!!.toItemStack()) {
            settingsMenuConfig.helpersSlot.emptyAction?.execute(it)
            val players = onlinePlayers().filter { p ->
                p.uuid != plot.owner && plot.area!!.contains(BukkitUtil.adaptComplete(it.location))
            }
            if (players.isEmpty()) {
                it.sendLang("islandPlayerAbsEmpty")
                return@setIcon
            }
            IslandsHelperSelectMenu(this.island, this.plot, players).open(it)
        }
        val helpers = IslandsManager.getIslandHelpers(island.id.value)

        for (i in helpers.indices) {
            val helper = helpers[i]
            if (i >= helpersSlot.size) {
                break
            }
            val offlinePlayer = helper.asOfflineData()!!
            val skullItem = Heads.getPlayerHead(offlinePlayer.name)
            skullItem.setDisplayName(offlinePlayer.name)
//            val skullItem = ItemStack(Material.PLAYER_HEAD)
//            val itemMeta = skullItem.itemMeta as SkullMeta
//            val offlinePlayer = helper.asOfflineData()!!
//            itemMeta.playerProfile = CraftPlayerProfile(offlinePlayer.id, offlinePlayer.name)
//            skullItem.itemMeta = itemMeta
            inv.setIcon(helpersSlot[i], skullItem) {
                settingsMenuConfig.helpersSlot.action?.execute(it)
            }
        }
    }

//    override fun onClick(slot: Int, player: Player, inv: Inventory, event: InventoryClickEvent) {
//        println("slot: $slot")
//    }

}