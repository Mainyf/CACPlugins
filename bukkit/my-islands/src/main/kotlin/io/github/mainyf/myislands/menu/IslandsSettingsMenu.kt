package io.github.mainyf.myislands.menu

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
import io.github.mainyf.newmclib.menu.ConfirmMenu
import io.github.mainyf.newmclib.offline_player_ext.asOfflineData
import io.github.mainyf.newmclib.utils.Heads
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import java.time.LocalDateTime
import java.util.*

class IslandsSettingsMenu(
    val island: PlayerIsland,
    val plot: Plot
) : AbstractMenuHandler() {

    private val helpers = mutableListOf<UUID>()

    override fun open(player: Player) {
        setup(ConfigManager.settingsMenuConfig.settings)
        updateHelpers()
        val inv = createInv(player)

        updateInv(player, inv)

        player.openInventory(inv)
    }

    private fun updateHelpers() {
        this.helpers.clear()
        this.helpers.addAll(IslandsManager.getIslandHelpers(island.id.value))
    }

    override fun updateTitle(player: Player): String {
        val menuConfig = ConfigManager.settingsMenuConfig
        val icons = mutableListOf<IaIcon>()
        icons.addAll(menuConfig.moveCoreSlot.itemSlot.iaIcons.icons())
        when (island.visibility) {
            ALL -> icons.add(menuConfig.visibilitySlot.itemSlot.iaIcons["all"]!!)
            PERMISSION -> icons.add(menuConfig.visibilitySlot.itemSlot.iaIcons["permission"]!!)
            NONE -> icons.add(menuConfig.visibilitySlot.itemSlot.iaIcons["none"]!!)
        }
        icons.addAll(menuConfig.resetIslandSlot.itemSlot.iaIcons.icons())

        repeat(7) {
            icons.add(menuConfig.helpersSlot.emptyItemSlot!!.iaIcons["v${it + 1}"]!!)
        }

        return applyTitle(player, icons)
    }

    private fun updateInv(player: Player, inv: Inventory) {
        val settingsMenuConfig = ConfigManager.settingsMenuConfig

        val moveCoreSlot = settingsMenuConfig.moveCoreSlot
        inv.setIcon(moveCoreSlot.slot, moveCoreSlot.itemSlot.toItemStack()) {
            moveCoreSlot.itemSlot.execAction(it)
            MoveIslandCore.tryStartMoveCore(it, plot)
            it.closeInventory()
        }
        val visibilitySlot = settingsMenuConfig.visibilitySlot
        val resetIslandSlot = settingsMenuConfig.resetIslandSlot
        inv.setIcon(visibilitySlot.slot, visibilitySlot.itemSlot.toItemStack {
            setDisplayName(getDisplayName().tvar("visibility", island.visibility.text))
        }) { p ->
            visibilitySlot.itemSlot.execAction(p)
            IslandsManager.setIslandVisibility(
                island,
                values().find { it.count > island.visibility.count } ?: ALL)
            updateInv(player, inv)
        }
        inv.setIcon(resetIslandSlot.slot, resetIslandSlot.itemSlot.toItemStack()) { p ->
            if (plot.owner != p.uuid) {
                p.sendLang("noOwnerResetIslands")
                return@setIcon
            }
            resetIslandSlot.itemSlot.execAction(p)
            val prevResetMilli = IslandsManager.getIslandLastResetDate(island)?.toMilli()
            if (!p.isOp && prevResetMilli != null) {
                val cur = LocalDateTime.now().toMilli()
                val eMillis = cur - prevResetMilli
                val cooldownMilli = ConfigManager.resetCooldown * 24 * 60 * 60 * 1000L
                if (eMillis < cooldownMilli) {
                    p.sendLang(
                        "resetCooldown",
                        mapOf(
                            "{player}" to p.name,
                            "{surplusTime}" to (cooldownMilli - eMillis).timestampConvertTime()
                        )
                    )
                    return@setIcon
                }
            }
            IslandsChooseMenu(false, { chooseMenu, player, schematicConfig ->

                onlinePlayers().forEach {
                    if (it.uuid == p.uuid) return@forEach
                    val pPlot = MyIslands.plotUtils.getPlotByPLoc(it) ?: return@forEach
                    if (pPlot == plot) {
                        MyIslands.plotUtils.teleportHomePlot(it)
                        it.sendLang("resetIslandBackTourist", mapOf("{player}" to player.name))
                    }
                }

                MyIslands.INSTANCE.submitTask(delay = 20L) {
                    IslandsManager.resetIsland(p.asPlotPlayer()!!, plot).whenComplete {
                        MyIslands.INSTANCE.runTaskLaterBR(3 * 20L) {
                            IslandsManager.chooseIslandSchematic(chooseMenu, player, schematicConfig)
                        }
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
        inv.setIcon(helpersSlot, settingsMenuConfig.helpersSlot.emptyItemSlot!!.toItemStack()) {
            settingsMenuConfig.helpersSlot.emptyItemSlot.execAction(it)
            if (plot.owner != it.uuid) {
                it.sendLang("noOwnerOpenHelperSelectMenu")
                return@setIcon
            }
            val players = onlinePlayers().filter { p ->
                p.uuid != plot.owner && !helpers.contains(p.uuid) && p.asPlotPlayer()?.location?.plotAbs?.owner == plot.owner
            }
            if (players.isEmpty()) {
                it.sendLang("islandPlayerAbsEmpty")
                return@setIcon
            }
            IslandsHelperSelectMenu(this.island, this.plot, players).open(it)
        }

        for (i in helpers.indices) {
            val helper = helpers[i]
            if (i >= helpersSlot.size) {
                break
            }
            kotlin.runCatching {
                val offlinePlayer = helper.asOfflineData()!!
                val islandsData = IslandsManager.getIslandData(helper)
                val skullItem = Heads.getPlayerHead(offlinePlayer.name).clone()
                skullItem.setDisplayName(offlinePlayer.name)

                inv.setIcon(helpersSlot[i], settingsMenuConfig.helpersSlot.itemSlot!!.toItemStack(skullItem) {
                    val meta = itemMeta
                    meta.displayName(Component.text(meta.displayName()!!.text().tvar("player", offlinePlayer.name)))
                    meta.lore(IslandsManager.replaceVarByLoreList(meta.lore()!!, plot, islandsData))
                    this.itemMeta = meta
                }, leftClickBlock = {
                    settingsMenuConfig.helpersSlot.itemSlot.execAction(it)
                }, rightClickBlock = { p ->
                    settingsMenuConfig.helpersSlot.itemSlot.execAction(p)
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
                                    mapOf("{player}" to (helper.asOfflineData()?.name ?: "无"))
                                )
                                helper.asPlayer()?.sendLang(
                                    "beRemoveIslandHelperSuccess",
                                    mapOf("{player}" to player.name)
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
                })
            }.onFailure {
                it.printStackTrace()
            }
        }
    }

    fun Long.timestampConvertTime(): String {
        val second = (this / 1000).toDouble()
//        val hour = this / 1000 / 60 / 60
        if (second <= 60) return "${second}秒"
        val surplusSecond = (second % 60).toInt()
        val minute = (second / 60).toInt()
        if (minute <= 60) {
            return "${minute}分钟${surplusSecond}秒"
        }
        val surplusMinute = minute % 60
        val hour = minute / 60
        if (hour <= 24) {
            return "${hour}小时${surplusMinute}分钟${surplusSecond}秒"
        }
        val surplusHour = hour % 24
        val day = hour / 24
        return "${day}天${surplusHour}小时${surplusMinute}分钟${surplusSecond}秒"
    }

}