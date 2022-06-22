package io.github.mainyf.myislands.menu

import io.github.mainyf.myislands.IslandsManager
import io.github.mainyf.myislands.MyIslands
import io.github.mainyf.myislands.config.ConfigManager
import io.github.mainyf.newmclib.config.IaIcon
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.menu.AbstractMenuHandler
import io.github.mainyf.newmclib.menu.ConfirmMenu
import io.github.mainyf.newmclib.utils.Cooldown
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import kotlin.math.ceil

class IslandsChooseMenu(
    var antiClose: Boolean = true,
    val block: (IslandsChooseMenu, Player, ConfigManager.PlotSchematicConfig) -> Unit
) :
    AbstractMenuHandler() {

    private var pageIndex = 1
    private val maxPageIndex =
        ceil(ConfigManager.schematicMap.values.let { it.size.toDouble() / ConfigManager.islandChooseConfig.islandListSlot.slot.size.toDouble() }).toInt()
    private val currentIslandList = mutableListOf<ConfigManager.PlotSchematicConfig>()

    var ok = false

    override fun open(player: Player) {
        this.cooldownTime = ConfigManager.islandChooseConfig.cooldown
        updateList()
        val inv = Bukkit.createInventory(
            createHolder(player),
            ConfigManager.islandChooseConfig.row * 9,
            Component.text(updateTitle(player).colored())
        )

        updateInv(inv)

        player.openInventory(inv)
    }

    override fun updateTitle(player: Player): String {
        val chooseMenuConfig = ConfigManager.islandChooseConfig
        val icons = mutableListOf<IaIcon>()
        icons.addAll(chooseMenuConfig.prevSlot.itemDisplay!!.iaIcons.icons())
        icons.addAll(chooseMenuConfig.nextSlot.itemDisplay!!.iaIcons.icons())
        icons.addAll(chooseMenuConfig.backSlot.itemDisplay!!.iaIcons.icons())
        currentIslandList.map {
            icons.addAll(it.iaIcons.icons())
        }
        val title = "${chooseMenuConfig.background} ${icons.sortedBy { it.priority }.joinToString(" ") { it.value }}"
//        val title = menuConfig.background
        player.setOpenInventoryTitle(title)
        return title
    }

    private fun updateList() {
        val chooseMenuConfig = ConfigManager.islandChooseConfig
        val islandListSlot = chooseMenuConfig.islandListSlot.slot
        currentIslandList.clear()
        currentIslandList.addAll(ConfigManager.schematicMap.values.toList().pagination(pageIndex, islandListSlot.size))
    }

    private fun updateInv(inv: Inventory) {
        val chooseMenuConfig = ConfigManager.islandChooseConfig
        val islandListSlot = chooseMenuConfig.islandListSlot.slot
        inv.clearIcon()
        updateList()
        currentIslandList.forEachIndexed { index, islandSchematic ->
            val slotList = islandListSlot[index]
            val ui = islandSchematic.ui
            slotList.forEach { slot ->
                inv.setIcon(
                    slot,
                    chooseMenuConfig.islandListSlot.itemDisplay!!.toItemStack {
                        itemMeta = itemMeta.apply {
                            displayName(Component.text(ui.name.colored()))
                            lore(ui.lore.map { Component.text(it.colored()) })
                        }
                    }
                ) {
                    cooldown.invoke(it.uuid, chooseMenuConfig.cooldown, {
                        chooseMenuConfig.islandListSlot.action?.execute(it)
                        val old = antiClose
                        antiClose = false
                        ConfirmMenu(
                            { p ->
                                block.invoke(this, it, islandSchematic)
                                p.closeInventory()
                            },
                            { p ->
                                IslandsChooseMenu(old, IslandsManager::chooseIslandSchematic).open(p)
                            }
                        ).open(it)
                    })
                }
            }
        }

        inv.setIcon(chooseMenuConfig.prevSlot.slot, chooseMenuConfig.prevSlot.itemDisplay!!.toItemStack()) {
            if (pageIndex > 1) {
                cooldown.invoke(it.uuid, chooseMenuConfig.cooldown, {
                    chooseMenuConfig.prevSlot.action?.execute(it)
                    pageIndex--
                    updateInv(inv)
                })
            }
        }
        inv.setIcon(chooseMenuConfig.nextSlot.slot, chooseMenuConfig.nextSlot.itemDisplay!!.toItemStack()) {
            if (pageIndex < maxPageIndex) {
                cooldown.invoke(it.uuid, chooseMenuConfig.cooldown, {
                    chooseMenuConfig.nextSlot.action?.execute(it)
                    pageIndex++
                    updateInv(inv)
                })
            }
        }
        inv.setIcon(chooseMenuConfig.backSlot.slot, chooseMenuConfig.backSlot.itemDisplay!!.toItemStack()) {
            cooldown.invoke(it.uuid, chooseMenuConfig.cooldown, {
                chooseMenuConfig.backSlot.action?.execute(it)
                ConfigManager.backLobbyAction.execute(it)
            })
        }
    }

    override fun onClose(player: Player, inv: Inventory, event: InventoryCloseEvent) {
        if (!antiClose) return
        if (!ok) {
            MyIslands.INSTANCE.runTaskLaterBR(10L) {
                IslandsChooseMenu(antiClose, IslandsManager::chooseIslandSchematic).open(player)
            }
        }
    }

}