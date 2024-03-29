package io.github.mainyf.myislands.menu

import io.github.mainyf.myislands.IslandsManager
import io.github.mainyf.myislands.MyIslands
import io.github.mainyf.myislands.config.ConfigMI
import io.github.mainyf.myislands.config.sendLang
import io.github.mainyf.newmclib.config.IaIcon
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.menu.AbstractMenuHandler
import io.github.mainyf.newmclib.menu.ConfirmMenu
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import kotlin.math.ceil

class IslandsChooseMenu(
    var antiClose: Boolean = true,
    val block: (IslandsChooseMenu, Player, ConfigMI.PlotSchematicConfig) -> Boolean,
    val backBlock: ((Player) -> Unit)? = null
) :
    AbstractMenuHandler() {

    private val hasFirst get() = backBlock == null
    private var pageIndex = 1
    private val maxPageIndex =
        ceil(ConfigMI.schematicMap.values.let { it.size.toDouble() / ConfigMI.chooseMenuConfig.islandListSlot.slot.size.toDouble() }).toInt()
    private val currentIslandList = mutableListOf<ConfigMI.PlotSchematicConfig>()

    var ok = false

    override fun open(player: Player) {
        setup(ConfigMI.chooseMenuConfig.settings)
        updateList()
        val inv = createInv(player)

        updateInv(inv)

        player.openInventory(inv)
    }

    override fun updateTitle(player: Player): String {
        val chooseMenuConfig = ConfigMI.chooseMenuConfig
        val icons = mutableListOf<IaIcon>()
        icons.addAll(chooseMenuConfig.prevSlot.iaIcon())
        icons.addAll(chooseMenuConfig.nextSlot.iaIcon())
        if (hasFirst) {
            icons.addAll(chooseMenuConfig.backSlot.iaIcon("backCity"))
        } else {
            icons.addAll(chooseMenuConfig.backSlot.iaIcon("backPrev"))
        }
        currentIslandList.map {
            icons.addAll(it.iaIcons.icons())
        }
        return applyTitle(player, icons)
    }

    private fun updateList() {
        val chooseMenuConfig = ConfigMI.chooseMenuConfig
        val islandListSlot = chooseMenuConfig.islandListSlot.slot
        currentIslandList.clear()
        currentIslandList.addAll(ConfigMI.schematicMap.values.toList().pagination(pageIndex, islandListSlot.size))
    }

    private fun updateInv(inv: Inventory) {
        val chooseMenuConfig = ConfigMI.chooseMenuConfig
        val islandListSlot = chooseMenuConfig.islandListSlot.slot
        inv.clearIcon()
        updateList()
        currentIslandList.forEachIndexed { index, islandSchematic ->
            val slotList = islandListSlot[index]
            val ui = islandSchematic.ui
            slotList.forEach { slot ->
                inv.setIcon(
                    slot,
                    chooseMenuConfig.islandListSlot.itemSlot!!.toItemStack {
                        withMeta(
                            displayName = ui.name.toComp(),
                            lore = ui.lore.map { it.toComp() }
                        )
                    }
                ) {
                    chooseMenuConfig.islandListSlot.itemSlot.execAction(it)
                    val old = antiClose
                    antiClose = false
                    ConfirmMenu(
                        { p ->
                            if(block.invoke(this, it, islandSchematic)) {
                                p.sendLang("islandIniting")
                            }
                            p.closeInventory()
                        },
                        { p ->
                            IslandsChooseMenu(old, IslandsManager::chooseIslandSchematic, backBlock).open(p)
                        }
                    ).open(it)
                }
            }
        }

        inv.setIcon(chooseMenuConfig.prevSlot) {
            if (pageIndex > 1) {
                pageIndex--
                updateInv(inv)
            }
        }
        inv.setIcon(chooseMenuConfig.nextSlot) {
            if (pageIndex < maxPageIndex) {
                pageIndex++
                updateInv(inv)
            }
        }
        if (hasFirst) {
            inv.setIcon(chooseMenuConfig.backSlot, key = "backCity") {
                ConfigMI.backLobbyAction.execute(it)
            }
        } else {
            inv.setIcon(chooseMenuConfig.backSlot, key = "backPrev") {
                backBlock?.invoke(it)
//                ConfigManager.backLobbyAction.execute(it)
            }
        }
    }

    override fun onClose(player: Player, inv: Inventory, event: InventoryCloseEvent) {
        if (!antiClose) return
        if (!ok) {
            MyIslands.INSTANCE.runTaskLaterBR(10L) {
                IslandsChooseMenu(antiClose, IslandsManager::chooseIslandSchematic, backBlock).open(player)
            }
        }
    }

}