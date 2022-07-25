package io.github.mainyf.myislands.menu

import com.plotsquared.bukkit.util.BukkitUtil
import com.plotsquared.core.plot.Plot
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
import kotlin.system.measureTimeMillis

class IslandsMainMenu : AbstractMenuHandler() {

    var pageIndex = 1
    var maxPageIndex = 0

    private var viewIslandTypeIndex = 0
    private val viewIslandType get() = values()[viewIslandTypeIndex]

    private var islandAbs: PlayerIsland? = null
    private var plotAbs: Plot? = null
    private var hasPermission = false
    private var hasOwner = false

    override fun open(player: Player) {
        setup(ConfigManager.mainMenuConfig.settings)
        updateMaxPageIndex(player)
        islandAbs = IslandsManager.getIslandAbs(player)
        plotAbs = MyIslands.plotUtils.getPlotByPLoc(player)

        hasPermission = IslandsManager.hasPermissionByFeet(player)
        hasOwner = plotAbs?.owner == player.uuid
        val inv = createInv(player)

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
        icons.addAll(menuConfig.prevSlot.iaIcon())
        icons.addAll(menuConfig.nextSlot.iaIcon())
        icons.addAll(menuConfig.islandViewSlot.iaIcon())

        val switchViewIslandIcons = menuConfig.switchViewIslandSlot.default()!!.iaIcons
        when (viewIslandType) {
            ALL -> icons.add(switchViewIslandIcons["all"]!!)
            ONLINE -> icons.add(switchViewIslandIcons["online"]!!)
            FRIEND -> icons.add(switchViewIslandIcons["friend"]!!)
            PERMISSION -> icons.add(switchViewIslandIcons["permission"]!!)
        }

        if (hasOwner) {
            icons.addAll(menuConfig.infoAndKudosSlot.iaIcon("info"))
            icons.addAll(menuConfig.upgradeAndBackIslandSlot.iaIcon("upgrade"))
        } else {
            icons.addAll(menuConfig.infoAndKudosSlot.iaIcon("kudos"))
            icons.addAll(menuConfig.upgradeAndBackIslandSlot.iaIcon("back"))
        }
        if (hasPermission) {
            icons.add(menuConfig.islandSettingsSlot.default()!!.iaIcons["permission"]!!)
        } else {
            icons.add(menuConfig.islandSettingsSlot.default()!!.iaIcons["unfamiliar"]!!)
        }
        return applyTitle(player, icons)
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
        inv.setIcon(menuConfig.switchViewIslandSlot, itemBlock = {
            setDisplayName {
                it?.text()?.tvar("filterText", viewIslandType.text)?.toComp()
            }
        }) {
            viewIslandTypeIndex++
            if (viewIslandTypeIndex > values().size - 1) {
                viewIslandTypeIndex = 0
            }
            updateMaxPageIndex(player)
//            maxPageIndex = StorageManager.getIsLandsCount(player, viewIslandType)
            pageIndex = 1
            updateInv(it, inv)
        }
        val iakSlot = menuConfig.infoAndKudosSlot
        if (hasOwner) {
            inv.setIcon(iakSlot, "info", itemBlock = {
                lore(IslandsManager.replaceVarByLoreList(lore(), plotAbs!!, islandAbs))
            })
        } else {
            inv.setIcon(iakSlot, "kudos", itemBlock = {
                lore(IslandsManager.replaceVarByLoreList(lore(), plotAbs!!, islandAbs))
            }) {
                if (islandAbs != null && IslandsManager.addKudoToIsland(islandAbs!!, it)) {
                    updateInv(player, inv)
                }
            }
        }
        val uabSlot = menuConfig.upgradeAndBackIslandSlot
        if (hasOwner) {
            inv.setIcon(uabSlot, "upgrade")
        } else {
            inv.setIcon(uabSlot, "back") {
                MyIslands.plotUtils.teleportHomePlot(it)
            }
        }
        inv.setIcon(menuConfig.islandSettingsSlot) {
            if (islandAbs != null && plotAbs != null) {
                if (!hasPermission) {
                    it.sendLang("noIslandPermission")
                    return@setIcon
                }
                IslandsSettingsMenu(islandAbs!!, plotAbs!!).open(it)
            }
        }
    }

//    private fun replaceVarByLoreList(
//        lore: List<Component>,
//        plot: Plot = plotAbs!!,
//        islandData: PlayerIsland? = islandAbs
//    ): List<Component> {
//        return lore.map { comp ->
//            Component.text(
//                comp.text()
//                    .tvar("owner", plot.owner?.asOfflineData()?.name ?: "空")
//                    .tvar(
//                        "helpers",
//                        IslandsManager.getIslandHelpers(islandData).let { list ->
//                            if (list.isEmpty()) "空" else list.joinToString(",") {
//                                it.asOfflineData()?.name ?: "空"
//                            }
//                        }
//                    )
//                    .tvar("kudos", "${islandData?.heatValue ?: "空"}")
//            )
//        }
//    }

    private fun updateInvIslandList(player: Player, inv: Inventory) {
        val viewListSlots = ConfigManager.mainMenuConfig.islandViewSlot
        inv.unSetIcon(viewListSlots.slot)
        val type = values()[viewIslandTypeIndex]
        val islands = StorageManager.getIsLandsOrderByKudos(
            pageIndex,
            viewListSlots.slot.size,
            player,
            type
        )
        for (i in islands.indices) {
            val islandData = islands[i]
            if (i >= viewListSlots.slot.size) {
                break
            }
            kotlin.runCatching {
                val offlinePlayer = islandData.id.value.asOfflineData()?.name ?: ""
                val skullItem = Heads.getPlayerHead(offlinePlayer).clone()
//                val skullItem = Heads.getPlayerHead(offlinePlayer).clone()
                val plot = MyIslands.plotUtils.findPlot(islandData.id.value)

                inv.setIcon(viewListSlots.slot[i], viewListSlots.default()!!.toItemStack(skullItem) {
                    val meta = itemMeta
                    meta.displayName(Component.text(meta.displayName()!!.text().tvar("player", offlinePlayer)))
                    meta.lore(IslandsManager.replaceVarByLoreList(meta.lore(), plot, islandData))
                    this.itemMeta = meta
                }) {
                    viewListSlots.default()!!.execAction(it)
                    MyIslands.plotUtils.findPlot(islandData.id.value)
                        .let { plot ->
                            if (plot == null) {
                                it.sendLang("otherPlayerNoPlot")
//                            it.errorMsg("此玩家没有岛屿")
                                return@let
                            }
                            val coreLoc = IslandsManager.getIslandCoreLoc(islandData)
                            val homeLoc = IslandsManager.getHomeLoc(coreLoc)
                            MyIslands.plotUtils.teleportPlayerToLoc(player, homeLoc, coreLoc)
//                            plot.getHome { loc ->
//                                it.teleport(BukkitUtil.adapt(loc).add(0.5, 0.0, 0.5))
//                            }
                        }
                }
            }.onFailure {
                it.printStackTrace()
            }
        }
    }

//    override fun onClick(slot: Int, player: Player, inv: Inventory, event: InventoryClickEvent) {
//        println("slot: $slot")
//    }

}

enum class IslandViewListType(val text: String) {

    ALL("全部"),
    ONLINE("在线"),
    FRIEND("好友"),
    PERMISSION("授权岛");

}