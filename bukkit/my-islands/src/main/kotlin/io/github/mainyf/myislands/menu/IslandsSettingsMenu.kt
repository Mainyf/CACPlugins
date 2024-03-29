package io.github.mainyf.myislands.menu

import com.plotsquared.core.plot.Plot
import io.github.mainyf.myislands.IslandsManager
import io.github.mainyf.myislands.MyIslands
import io.github.mainyf.myislands.asPlotPlayer
import io.github.mainyf.myislands.config.ConfigMI
import io.github.mainyf.myislands.config.sendLang
import io.github.mainyf.myislands.features.MoveIsLandCore
import io.github.mainyf.myislands.storage.IslandVisibility.*
import io.github.mainyf.myislands.storage.PlayerIsland
import io.github.mainyf.newmclib.config.IaIcon
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.hooks.money
import io.github.mainyf.newmclib.hooks.takeMoney
import io.github.mainyf.newmclib.menu.AbstractMenuHandler
import io.github.mainyf.newmclib.menu.ConfirmMenu
import io.github.mainyf.newmclib.offline_player_ext.asOfflineData
import io.github.mainyf.newmclib.utils.Heads
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import java.time.LocalDateTime
import java.util.*
import kotlin.math.ceil

class IslandsSettingsMenu(
    val island: PlayerIsland,
    val plot: Plot
) : AbstractMenuHandler() {

    var pageIndex = 1
    var maxPageIndex = 0

    private var curHelpers = 0

    private val helpers = mutableListOf<UUID?>()

    private var currentEmptySlot = 0

    private val currentHelpers = mutableListOf<UUID?>()

    private var maxHelpers = 0

    private var pageSize = 0

    override fun open(player: Player) {
        setup(ConfigMI.settingsMenuConfig.settings)
        this.maxHelpers = IslandsManager.getPlayerMaxHelperCount(player)
        this.pageSize = ConfigMI.settingsMenuConfig.helpersSlot.slot.size
        updateHelpers()
        updateHelperList()
        val inv = createInv(player)

        updateInv(player, inv)

        player.openInventory(inv)
    }

    private fun updateHelpers() {
        this.helpers.clear()
        this.helpers.addAll(IslandsManager.getIslandHelpers(island.id.value))
        this.curHelpers = this.helpers.size
        if (this.helpers.size < maxHelpers) {
            repeat(maxHelpers - this.helpers.size) {
                this.helpers.add(null)
            }
        }
        this.maxPageIndex = ceil(
            maxHelpers.toDouble() / pageSize.toDouble()
        ).toInt()
    }

    private fun updateHelperList() {
        this.currentEmptySlot =
            if (pageIndex != maxPageIndex) pageSize else pageSize - ((pageIndex * pageSize) % maxHelpers)
        currentHelpers.clear()
        currentHelpers.addAll(helpers.pagination(pageIndex, ConfigMI.settingsMenuConfig.helpersSlot.slot.size))
    }

    override fun updateTitle(player: Player): String {
        val menuConfig = ConfigMI.settingsMenuConfig
        val icons = mutableListOf<IaIcon>()
        icons.addAll(menuConfig.moveCoreSlot.iaIcon())
        when (island.visibility) {
            ALL -> icons.add(menuConfig.visibilitySlot.default()!!.iaIcons["all"]!!)
            PERMISSION -> icons.add(menuConfig.visibilitySlot.default()!!.iaIcons["permission"]!!)
            NONE -> icons.add(menuConfig.visibilitySlot.default()!!.iaIcons["none"]!!)
        }
        icons.addAll(menuConfig.resetIslandSlot.iaIcon())


        repeat(currentEmptySlot) {
            icons.add(menuConfig.helpersSlot["empty"]!!.iaIcons["v${it + 1}"]!!)
        }

        return applyTitle(player, icons)
    }

    private fun updateInv(player: Player, inv: Inventory) {
        val settingsMenuConfig = ConfigMI.settingsMenuConfig

        inv.setIcon(settingsMenuConfig.prevSlot) {
            if (pageIndex > 1) {
                pageIndex--
                updateHelperList()
                updateHelperInv(player, inv)
            }
        }
        inv.setIcon(settingsMenuConfig.nextSlot) {
            if (pageIndex < maxPageIndex && curHelpers >= pageIndex * pageSize) {
                pageIndex++
                updateHelperList()
                updateHelperInv(player, inv)
            }
        }

        val moveCoreSlot = settingsMenuConfig.moveCoreSlot
        inv.setIcon(moveCoreSlot, itemBlock = {
            setPlaceholder(player)
        }) {
            if (plot.owner != it.uuid) {
                it.sendLang("noOwnerMoveCore")
                return@setIcon
            }

//            if (!ConfigMI.tryPayMyIslandCost(it, ConfigMI.myislandCost.moveCore, "moveCore")) {
//                return@setIcon
//            }
            MoveIsLandCore.tryStartMoveCore(it, plot) {
                val money = player.money()
                if (money < ConfigMI.myislandCost.moveCore) {
                    player.sendLang("costMoneyLack.moveCore", "{money}", money, "{cost}", ConfigMI.myislandCost.moveCore)
                    return@tryStartMoveCore false
                }
                player.takeMoney(ConfigMI.myislandCost.moveCore)
                return@tryStartMoveCore true
            }
            it.closeInventory()
        }
        val visibilitySlot = settingsMenuConfig.visibilitySlot
        val resetIslandSlot = settingsMenuConfig.resetIslandSlot
        inv.setIcon(visibilitySlot, itemBlock = {
            this.tvar("visibility", island.visibility.text).setPlaceholder(player)
//            withMetaText(displayNameBlock = {
//                it?.tvar("visibility", island.visibility.text)
//            })
        }) { p ->
            if (!ConfigMI.tryPayMyIslandCost(
                    player,
                    ConfigMI.myislandCost.switchVisibility,
                    "switchVisibility"
                )
            ) {
                return@setIcon
            }
            IslandsManager.setIslandVisibility(
                island,
                values().find { it.count > island.visibility.count } ?: ALL)
            updateInv(player, inv)
        }
        inv.setIcon(resetIslandSlot, itemBlock = {
            setPlaceholder(player)
        }) { p ->
            if (plot.owner != p.uuid) {
                p.sendLang("noOwnerResetIslands")
                return@setIcon
            }
            val prevResetMilli = IslandsManager.getIslandLastResetDate(island)?.toMilli()
            if (!p.isOp && prevResetMilli != null) {
                val cur = LocalDateTime.now().toMilli()
                val eMillis = cur - prevResetMilli
                val cooldownMilli = ConfigMI.resetCooldown * 24 * 60 * 60 * 1000L
                if (eMillis < cooldownMilli) {
                    p.sendLang(
                        "resetCooldown",
                        "{player}", p.name,
                        "{surplusTime}", (cooldownMilli - eMillis).timestampConvertTime()
                    )
                    return@setIcon
                }
            }
            IslandsChooseMenu(false, { chooseMenu, player, schematicConfig ->
                if (!ConfigMI.tryPayMyIslandCost(p, ConfigMI.myislandCost.reset, "reset")) {
                    return@IslandsChooseMenu false
                }
                onlinePlayers().forEach {
                    if (it.uuid == p.uuid) return@forEach
                    val pPlot = MyIslands.plotUtils.getPlotByPLoc(it) ?: return@forEach
                    if (pPlot == plot) {
                        MyIslands.plotUtils.teleportHomePlot(it)
                        it.sendLang("resetIslandBackTourist", "{player}", player.name)
                    }
                }

                MyIslands.INSTANCE.submitTask(delay = 20L) {
                    IslandsManager.resetIsland(p.asPlotPlayer()!!, plot).whenComplete {
                        MyIslands.INSTANCE.runTaskLaterBR(3 * 20L) {
                            IslandsManager.chooseIslandSchematic(chooseMenu, player, schematicConfig)
                        }
                    }
                }
                return@IslandsChooseMenu true
            }, {
                IslandsSettingsMenu(this.island, this.plot).open(it)
            }).open(p)
        }
        updateHelperInv(player, inv)
    }

    private fun updateHelperInv(player: Player, inv: Inventory) {
        val settingsMenuConfig = ConfigMI.settingsMenuConfig

        val helpersSlot = settingsMenuConfig.helpersSlot.slot
        inv.unSetIcon(helpersSlot)
        repeat(currentEmptySlot) { i ->
            inv.setIcon(helpersSlot[i], settingsMenuConfig.helpersSlot["empty"]!!.toItemStack().apply {
//                val meta = itemMeta
                this.tvar(
                    "curHelpers", curHelpers.toString(),
                    "maxHelpers", maxHelpers.toString()
                ).setPlaceholder(player)
//                meta.displayName(
//                    meta.displayName()?.text()?.tvar(
//                        "curHelpers", curHelpers.toString(),
//                        "maxHelpers", maxHelpers.toString()
//                    )?.toComp()
//                )
//                meta.lore(meta.lore()?.map {
//                    it.text().tvar(
//                        "curHelpers", curHelpers.toString(),
//                        "maxHelpers", maxHelpers.toString()
//                    ).toComp()
//                })
//                this.itemMeta = meta
            }) {
                settingsMenuConfig.helpersSlot["empty"]!!.execAction(it)
//                if (!ConfigMI.tryPayMyIslandCost(player, ConfigMI.myislandCost.addHelper, "addHelper")) {
//                    return@setIcon
//                }
                IslandsManager.openHelperSelectMenu(it, plot, island, helpers) {
                    val money = player.money()
                    if (money < ConfigMI.myislandCost.addHelper) {
                        player.sendLang("costMoneyLack.addHelper", "{money}", money, "{cost}", ConfigMI.myislandCost.addHelper)
                        return@openHelperSelectMenu false
                    }
                    player.takeMoney(ConfigMI.myislandCost.addHelper)
                    return@openHelperSelectMenu true
                }
//                if (plot.owner != it.uuid) {
//                    it.sendLang("noOwnerOpenHelperSelectMenu")
//                    return@setIcon
//                }
//                val players = onlinePlayers().filter { p ->
//                    p.uuid != plot.owner && !helpers.contains(p.uuid) && p.asPlotPlayer()?.location?.plotAbs?.owner == plot.owner
//                }
//                if (players.isEmpty()) {
//                    it.sendLang("islandPlayerAbsEmpty")
//                    return@setIcon
//                }
//                IslandsHelperSelectMenu(this.island, this.plot, players).open(it)
            }
        }

        for (i in currentHelpers.indices) {
            val helper = currentHelpers[i]
            if (helper == null) {
                break
            }
            if (i >= helpersSlot.size) {
                break
            }
            kotlin.runCatching {
                val offlinePlayer = helper.asOfflineData()!!
                val islandsData = IslandsManager.getIslandData(helper)
                val skullItem = Heads.getPlayerHead(offlinePlayer.name).clone()
                skullItem.setDisplayName(offlinePlayer.name)

                inv.setIcon(helpersSlot[i], settingsMenuConfig.helpersSlot.default()!!.toItemStack(skullItem) {
                    val meta = itemMeta
                    meta.displayName(meta.displayName()!!.serialize().tvar("player", offlinePlayer.name).deserialize())
                    meta.lore(IslandsManager.replaceVarByLoreList(meta.lore(), plot, islandsData))
                    this.itemMeta = meta
                    setPlaceholder(player)
                }) { p ->
                    settingsMenuConfig.helpersSlot.default()!!.execAction(p)
                    if (plot.owner != p.uuid) {
                        p.sendLang("noOwnerRemoveHelpers")
                        return@setIcon
                    }
                    ConfirmMenu(
                        { _ ->
                            kotlin.runCatching {
                                IslandsManager.removeHelpers(plot, p, island, helper)
                                player.sendLang(
                                    "removeIslandHelperSuccess",
                                    "{player}", (helper.asOfflineData()?.name ?: "无")
                                )
                                helper.asPlayer()?.sendLang(
                                    "beRemoveIslandHelperSuccess",
                                    "{player}", player.name
                                )
                                IslandsSettingsMenu(island, plot).open(p)
                            }.onFailure {
                                it.printStackTrace()
                            }
                        },
                        { p ->
                            IslandsSettingsMenu(island, plot).open(p)
                        }
                    ).open(p)
                }
            }.onFailure {
                it.printStackTrace()
            }
        }
    }

}