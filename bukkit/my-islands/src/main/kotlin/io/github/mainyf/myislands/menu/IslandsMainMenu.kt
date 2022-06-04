package io.github.mainyf.myislands.menu

import com.destroystokyo.paper.profile.CraftPlayerProfile
import com.plotsquared.core.plot.Plot
import io.github.mainyf.myislands.IslandsManager
import io.github.mainyf.myislands.MyIslands
import io.github.mainyf.myislands.asPlotPlayer
import io.github.mainyf.myislands.config.ConfigManager
import io.github.mainyf.myislands.config.SlotConfig
import io.github.mainyf.myislands.storage.PlayerIsland
import io.github.mainyf.myislands.storage.StorageManager
import io.github.mainyf.newmclib.exts.colored
import io.github.mainyf.newmclib.exts.getDisplayName
import io.github.mainyf.newmclib.exts.setDisplayName
import io.github.mainyf.newmclib.exts.tvar
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

class IslandsMainMenu : AbstractMenuHandler() {

    var pageIndex = 1
    var maxPageIndex = 0

    private var viewIslandTypeIndex = 0
    private val viewIslandType get() = IslandViewListType.values()[viewIslandTypeIndex]

    private var islandAbs: PlayerIsland? = null
    private var plotAbs: Plot? = null
    private var hasPermission = false

    override fun open(player: Player) {
        maxPageIndex = StorageManager.getIsLandsCount(player, IslandViewListType.ALL)
        islandAbs = IslandsManager.getIslandAbs(player)
        plotAbs = MyIslands.plotUtils.getPlotByPLoc(player)
        hasPermission = IslandsManager.hasPermission(player)
        val inv = Bukkit.createInventory(createHolder(player), 6 * 9, Component.text("&a岛屿主菜单".colored()))

        updateInv(player, inv)
        player.openInventory(inv)
    }

    private fun updateInv(player: Player, inv: Inventory) {
        updateInvSlot(player, inv)
        updateInvIslandList(player, inv)
    }

    private fun updateInvSlot(player: Player, inv: Inventory) {
        inv.clearIcon()
        val menuConfig = ConfigManager.mainMenuConfig
        inv.setIcon(menuConfig.prevSlot) {
            if (pageIndex > 1) {
                pageIndex--
                updateInvIslandList(player, inv)
            }
        }
        inv.setIcon(menuConfig.nextSlot) {
            if (pageIndex < maxPageIndex) {
                pageIndex++
                updateInvIslandList(player, inv)
            }
        }
        inv.setIcon(menuConfig.switchViewIslandSlot, {
            setDisplayName(getDisplayName().tvar("filterText", viewIslandType.text))
        }) {
            viewIslandTypeIndex++
            if (viewIslandTypeIndex > IslandViewListType.values().size - 1) {
                viewIslandTypeIndex = 0
            }
            maxPageIndex = StorageManager.getIsLandsCount(player, viewIslandType)
            pageIndex = 1
            updateInv(it, inv)
        }
        val iakSlot = menuConfig.infoAndKudosSlot
        if (hasPermission) {
            inv.setIcon(iakSlot.slot, iakSlot.info.toItemStack {

            })
        } else {
            inv.setIcon(iakSlot.slot, iakSlot.kudos.toItemStack()) {
                if (islandAbs != null) {
                    IslandsManager.addKudoToIsland(islandAbs!!, it)
                }
            }
        }
        val uabSlot = menuConfig.upgradeAndBackIslandSlot
        if (hasPermission) {
            inv.setIcon(uabSlot.slot, uabSlot.upgrade.toItemStack()) {

            }
        } else {
            inv.setIcon(uabSlot.slot, uabSlot.back.toItemStack()) {

            }
        }
        inv.setIcon(menuConfig.islandSettingsSlot) {
            if (islandAbs != null && plotAbs != null) {
                IslandsSettingsMenu(islandAbs!!, plotAbs!!).open(it)
            }
        }
    }

    private fun Inventory.setIcon(
        slotConfig: SlotConfig,
        itemBlock: ItemStack.() -> Unit = {},
        block: (Player) -> Unit
    ) {
        setIcon(slotConfig.slot, slotConfig.itemDisplay!!.toItemStack(itemBlock), block)
    }

    private fun updateInvIslandList(player: Player, inv: Inventory) {
        val viewListSlots = ConfigManager.mainMenuConfig.islandViewSlot
        inv.unSetIcon(viewListSlots.slot)
        val type = IslandViewListType.values()[viewIslandTypeIndex]
        val islands = StorageManager.getIsLandsOrderByKudos(pageIndex, viewListSlots.slot.size, player, type)
        for (i in islands.indices) {
            val islandData = islands[i]
            if (i >= viewListSlots.slot.size) {
                break
            }
            val skullItem = ItemStack(Material.PLAYER_HEAD)
            val itemMeta = skullItem.itemMeta as SkullMeta
            val offlinePlayer = islandData.id.value.asOfflineData()!!
            itemMeta.playerProfile = CraftPlayerProfile(offlinePlayer.id, offlinePlayer.name)
            skullItem.itemMeta = itemMeta
            inv.setIcon(viewListSlots.slot[i], skullItem)
        }
    }

    override fun onClick(slot: Int, player: Player, inv: Inventory, event: InventoryClickEvent) {
        println("slot: $slot")
    }

}

enum class IslandViewListType(val text: String) {

    ALL("全部"),
    FRIEND("好友"),
    PERMISSION("授权者");

}