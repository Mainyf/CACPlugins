package io.github.mainyf.myislands.menu

import io.github.mainyf.myislands.IslandsManager
import io.github.mainyf.myislands.MyIslands
import io.github.mainyf.myislands.config.ConfigManager
import io.github.mainyf.newmclib.exts.colored
import io.github.mainyf.newmclib.exts.errorMsg
import io.github.mainyf.newmclib.exts.pagination
import io.github.mainyf.newmclib.exts.runTaskLaterBR
import io.github.mainyf.newmclib.menu.AbstractMenuHandler
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import kotlin.math.ceil

class IslandsChooseMenu : AbstractMenuHandler() {

    companion object {

        const val SIZE = 6 * 9

        val ISLANDS_LIST = listOf(
            listOf(9, 10, 11, 18, 19, 20),
            listOf(12, 13, 14, 21, 22, 23),
            listOf(15, 16, 17, 24, 25, 26)
        )

        val PREV_SLOT = listOf(37, 38)

        val NEXT_SLOT = listOf(42, 43)

        val BACK_SLOT = listOf(40, 49)

    }

    private var pageIndex = 1
    private val maxPageIndex =
        ceil(ConfigManager.schematicMap.values.let { it.size.toDouble() / ISLANDS_LIST.size.toDouble() }).toInt()
    private val currentIslandList = mutableListOf<ConfigManager.PlotSchematicConfig>()

    private var ok = false

    override fun open(player: Player) {
        val inv = Bukkit.createInventory(createHolder(player), SIZE, Component.text("&a岛屿选择".colored()))

        BACK_SLOT.forEach {
            inv.setIcon(it, Material.RED_STAINED_GLASS_PANE, "返回大厅") {
                ConfigManager.backLobbyAction.execute(player)
            }
        }
        updateInv(inv)

        player.openInventory(inv)
    }

    private fun updateInv(inv: Inventory) {
        ISLANDS_LIST.forEach { slotList ->
            slotList.forEach {
                inv.unSetIcon(it)
            }
        }
        currentIslandList.clear()
        currentIslandList.addAll(ConfigManager.schematicMap.values.toList().pagination(pageIndex, ISLANDS_LIST.size))
        currentIslandList.forEachIndexed { index, islandSchematic ->
            val slotList = ISLANDS_LIST[index]
            slotList.forEach { slot ->
                val material = when (index) {
                    0 -> Material.LIGHT_BLUE_STAINED_GLASS_PANE
                    1 -> Material.LIME_STAINED_GLASS_PANE
                    2 -> Material.ORANGE_STAINED_GLASS_PANE
                    else -> Material.BLUE_STAINED_GLASS_PANE
                }
                inv.setIcon(
                    slot,
                    material,
                    islandSchematic.ui.name.colored(),
                    islandSchematic.ui.lore.map { it.colored() }) {
                    chooseIslandSchematic(it, islandSchematic)
                }
            }
        }
        PREV_SLOT.forEach {
            if (pageIndex > 1) {
                inv.setIcon(it, Material.WHITE_STAINED_GLASS_PANE, "上一页") {
                    pageIndex--
                    updateInv(inv)
                }
            } else {
                inv.unSetIcon(it)
            }
        }
        NEXT_SLOT.forEach {
            if (pageIndex < maxPageIndex) {
                inv.setIcon(it, Material.BLACK_STAINED_GLASS_PANE, "下一页") {
                    pageIndex++
                    updateInv(inv)
                }
            } else {
                inv.unSetIcon(it)
            }
        }
    }

    private fun chooseIslandSchematic(player: Player, plotSchematic: ConfigManager.PlotSchematicConfig) {
        val plotPlayer = MyIslands.plotAPI.wrapPlayer(player.uniqueId)
        if (plotPlayer == null) {
            player.errorMsg("未知错误，请重试")
            return
        }
        if (MyIslands.plotAPI.getPlayerPlots(plotPlayer).isNotEmpty()) {
            player.errorMsg("你的已经拥有了自己的私人岛屿")
            return
        }
        ok = true
        MyIslands.plotUtils.autoClaimPlot(plotPlayer) {
            val plots = MyIslands.plotAPI.getPlayerPlots(plotPlayer)
            val plot = plots.first()
            MyIslands.plotUtils.paste(player, plot, plotSchematic.name) {
                if (it) {
                    IslandsManager.createPlayerIsland(player, plot, plotSchematic)
                } else {
                    player.errorMsg("意外的错误: 0xMI0")
                }
            }
        }
    }

    override fun onClose(player: Player, inv: Inventory, event: InventoryCloseEvent) {
        if (!ok) {
            MyIslands.INSTANCE.runTaskLaterBR(10L) {
                IslandsChooseMenu().open(player)
            }
        }
    }

}