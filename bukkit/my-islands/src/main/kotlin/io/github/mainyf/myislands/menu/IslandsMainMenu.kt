package io.github.mainyf.myislands.menu

import com.plotsquared.bukkit.util.BukkitUtil
import com.plotsquared.core.plot.Plot
import com.plotsquared.core.util.query.PlotQuery
import io.github.mainyf.myislands.IslandsManager
import io.github.mainyf.myislands.MyIslands
import io.github.mainyf.myislands.config.ConfigManager
import io.github.mainyf.myislands.config.sendLang
import io.github.mainyf.myislands.menu.IslandViewListType.*
import io.github.mainyf.myislands.storage.PlayerIsland
import io.github.mainyf.myislands.storage.StorageManager
import io.github.mainyf.newmclib.config.IaIcon
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.menu.AbstractMenuHandler
import io.github.mainyf.newmclib.offline_player_ext.asOfflineData
import io.github.mainyf.newmclib.utils.Cooldown
import io.github.mainyf.newmclib.utils.Heads
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import kotlin.math.ceil

class IslandsMainMenu : AbstractMenuHandler() {

    var pageIndex = 1
    var maxPageIndex = 0

    private var viewIslandTypeIndex = 0
    private val viewIslandType get() = values()[viewIslandTypeIndex]

    private var islandAbs: PlayerIsland? = null
    private var plotAbs: Plot? = null
    private var hasPermission = false

    companion object {

        val cooldown = Cooldown()

    }

    override fun open(player: Player) {
        this.cooldownTime = ConfigManager.mainMenuConfig.cooldown
        updateMaxPageIndex(player)
        islandAbs = IslandsManager.getIslandAbs(player)
        plotAbs = MyIslands.plotUtils.getPlotByPLoc(player)
        hasPermission = IslandsManager.hasPermissionByFeet(player)
        val inv = Bukkit.createInventory(createHolder(player), ConfigManager.mainMenuConfig.row * 9, Component.text(updateTitle(player).colored()))

        updateInv(player, inv)
        player.openInventory(inv)
    }

    private fun updateMaxPageIndex(player: Player) {
        maxPageIndex = ceil(
            StorageManager.getIsLandsCount(player, viewIslandType)
                .toDouble() / ConfigManager.mainMenuConfig.islandViewSlot.slot.size.toDouble()
        ).toInt()
    }

    private fun updateInv(player: Player, inv: Inventory) {
        updateInvSlot(player, inv)
        updateInvIslandList(player, inv)
    }

    override fun updateTitle(player: Player): String {
        val menuConfig = ConfigManager.mainMenuConfig
        val icons = mutableListOf<IaIcon>()
        icons.addAll(menuConfig.prevSlot.itemDisplay!!.iaIcons.icons())
        icons.addAll(menuConfig.nextSlot.itemDisplay!!.iaIcons.icons())
        icons.addAll(menuConfig.islandViewSlot.itemDisplay!!.iaIcons.icons())

        val switchViewIslandIcons = menuConfig.switchViewIslandSlot.itemDisplay!!.iaIcons
        when (viewIslandType) {
            ALL -> icons.add(switchViewIslandIcons["all"]!!)
            FRIEND -> icons.add(switchViewIslandIcons["friend"]!!)
            PERMISSION -> icons.add(switchViewIslandIcons["permission"]!!)
        }

        if (hasPermission) {
            icons.addAll(menuConfig.infoAndKudosSlot.info.iaIcons.icons())
            icons.addAll(menuConfig.upgradeAndBackIslandSlot.upgrade.iaIcons.icons())
            icons.add(menuConfig.islandSettingsSlot.itemDisplay!!.iaIcons["permission"]!!)
        } else {
            icons.addAll(menuConfig.infoAndKudosSlot.kudos.iaIcons.icons())
            icons.addAll(menuConfig.upgradeAndBackIslandSlot.back.iaIcons.icons())
            icons.add(menuConfig.islandSettingsSlot.itemDisplay!!.iaIcons["unfamiliar"]!!)
        }
        val title = "${menuConfig.background} ${icons.sortedBy { it.priority }.joinToString(" ") { it.value }}"
//        val title = menuConfig.background
        player.setOpenInventoryTitle(title)
        return title
    }

    private fun updateInvSlot(player: Player, inv: Inventory) {
        inv.clearIcon()
        val menuConfig = ConfigManager.mainMenuConfig
        inv.setIcon(menuConfig.prevSlot) {
            cooldown.invoke(it.uuid, menuConfig.cooldown, {
                menuConfig.prevSlot.action?.execute(it)
                if (pageIndex > 1) {
                    pageIndex--
                    updateInvIslandList(player, inv)
                }
            })
        }
        inv.setIcon(menuConfig.nextSlot) {
            cooldown.invoke(it.uuid, menuConfig.cooldown, {
                menuConfig.nextSlot.action?.execute(it)
                if (pageIndex < maxPageIndex) {
                    pageIndex++
                    updateInvIslandList(player, inv)
                }
            })
        }
        inv.setIcon(menuConfig.switchViewIslandSlot, {
            setDisplayName(getDisplayName().tvar("filterText", viewIslandType.text))
        }) {
            cooldown.invoke(it.uuid, menuConfig.cooldown, {
                menuConfig.switchViewIslandSlot.action?.execute(it)
                viewIslandTypeIndex++
                if (viewIslandTypeIndex > values().size - 1) {
                    viewIslandTypeIndex = 0
                }
                updateMaxPageIndex(player)
//            maxPageIndex = StorageManager.getIsLandsCount(player, viewIslandType)
                pageIndex = 1
                updateInv(it, inv)
            })
        }
        val iakSlot = menuConfig.infoAndKudosSlot
        if (hasPermission) {
            inv.setIcon(iakSlot.slot, iakSlot.info.toItemStack()) {
                cooldown.invoke(it.uuid, menuConfig.cooldown, {
                    iakSlot.infoAction?.execute(it)
                })
            }
        } else {
            inv.setIcon(iakSlot.slot, iakSlot.kudos.toItemStack()) {
                cooldown.invoke(it.uuid, menuConfig.cooldown, {
                    iakSlot.kudosAction?.execute(it)
                    if (islandAbs != null) {
                        IslandsManager.addKudoToIsland(islandAbs!!, it)
                    }
                })
            }
        }
        val uabSlot = menuConfig.upgradeAndBackIslandSlot
        if (hasPermission) {
            inv.setIcon(uabSlot.slot, uabSlot.upgrade.toItemStack()) {
                cooldown.invoke(it.uuid, menuConfig.cooldown, {
                    uabSlot.upgradeAction?.execute(it)
                })
            }
        } else {
            inv.setIcon(uabSlot.slot, uabSlot.back.toItemStack()) {
                cooldown.invoke(it.uuid, menuConfig.cooldown, {
                    uabSlot.backAction?.execute(it)
                    PlotQuery
                        .newQuery()
                        .ownedBy(it.uuid)
                        .asSet()
                        .firstOrNull()
                        .let { plot ->
                            if (plot == null) {
                                it.sendLang("playerNoPlot")
//                            it.errorMsg("你没有岛屿")
                                return@let
                            }
                            plot.getHome { loc ->
                                it.teleport(BukkitUtil.adapt(loc))
                            }
                        }
                })
            }
        }
        inv.setIcon(menuConfig.islandSettingsSlot) {
            cooldown.invoke(it.uuid, menuConfig.cooldown, {
                menuConfig.islandSettingsSlot.action?.execute(it)
                if (islandAbs != null && plotAbs != null) {
                    if (!hasPermission) {
                        it.sendLang("noIslandPermission")
//                    it.errorMsg("你没有该岛屿的授权")
                        return@invoke
                    }
                    IslandsSettingsMenu(islandAbs!!, plotAbs!!).open(it)
                }
            })
        }
    }

    private fun updateInvIslandList(player: Player, inv: Inventory) {
        val viewListSlots = ConfigManager.mainMenuConfig.islandViewSlot
        inv.unSetIcon(viewListSlots.slot)
        val type = values()[viewIslandTypeIndex]
        val islands = StorageManager.getIsLandsOrderByKudos(pageIndex, viewListSlots.slot.size, player, type)
        for (i in islands.indices) {
            val islandData = islands[i]
            if (i >= viewListSlots.slot.size) {
                break
            }
            val offlinePlayer = islandData.id.value.asOfflineData()?.name ?: ""
            val skullItem = Heads.getPlayerHead(offlinePlayer)
            skullItem.setDisplayName(offlinePlayer)

            inv.setIcon(viewListSlots.slot[i], skullItem) {
                cooldown.invoke(it.uuid, ConfigManager.mainMenuConfig.cooldown, {
                    viewListSlots.action?.execute(it)
                    PlotQuery
                        .newQuery()
                        .ownedBy(islandData.id.value)
                        .asSet()
                        .firstOrNull()
                        .let { plot ->
                            if (plot == null) {
                                it.sendLang("otherPlayerNoPlot")
//                            it.errorMsg("此玩家没有岛屿")
                                return@let
                            }
                            plot.getHome { loc ->
                                it.teleport(BukkitUtil.adapt(loc))
                            }
                        }
                })
            }
        }
    }

//    override fun onClick(slot: Int, player: Player, inv: Inventory, event: InventoryClickEvent) {
//        println("slot: $slot")
//    }

}

enum class IslandViewListType(val text: String) {

    ALL("全部"),
    FRIEND("好友"),
    PERMISSION("授权者");

}