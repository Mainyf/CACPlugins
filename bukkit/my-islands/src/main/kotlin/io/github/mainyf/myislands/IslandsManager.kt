package io.github.mainyf.myislands

import com.plotsquared.bukkit.paperlib.PaperLib
import com.plotsquared.core.location.BlockLoc
import com.plotsquared.core.player.PlotPlayer
import com.plotsquared.core.plot.Plot
import com.shopify.promises.Promise
import dev.lone.itemsadder.api.CustomFurniture
import io.github.mainyf.myislands.config.ConfigManager
import io.github.mainyf.myislands.config.sendLang
import io.github.mainyf.myislands.exceptions.IslandException
import io.github.mainyf.myislands.menu.IslandsChooseMenu
import io.github.mainyf.myislands.storage.IslandVisibility
import io.github.mainyf.myislands.storage.PlayerIsland
import io.github.mainyf.myislands.storage.StorageManager
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.offline_player_ext.asOfflineData
import io.github.mainyf.newmclib.utils.VectorUtils
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.util.Vector
import org.joda.time.DateTime
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.collections.any

object IslandsManager {

    private val joinPlayers = mutableMapOf<UUID, Plot>()

    private val helperPermissions = listOf(
        10,
        20,
        30,
        40,
        50,
        60,
        70,
        80,
        90,
        100
    )

    fun getPlayerMaxHelperCount(player: Player): Int {
        return helperPermissions.reversed().find { player.hasPermission("myislands.helpers.${it}") } ?: 10
    }

    fun removeHelpers(plot: Plot, player: Player, island: PlayerIsland, helperUUID: UUID) {
        StorageManager.transaction {
            if (!island.helpers.toList().any { it.helperUUID == helperUUID }) {
                throw IslandException("${player.name} 尝试删除授权者 $helperUUID,但是这个授权者并不是他的授权者")
            }
            StorageManager.removeHelpers(island, helperUUID)
            plot.removeTrusted(helperUUID)
        }
    }

    fun addHelpers(plot: Plot, player: Player, island: PlayerIsland, helperUUID: UUID) {
        StorageManager.transaction {
            if (island.helpers.toList().size >= getPlayerMaxHelperCount(player)) {
                throw IslandException("${player.name} 尝试添加授权者，但授权者已满")
            }
            if (island.helpers.any { it.helperUUID == helperUUID }) {
                player.sendLang("repeatAddPermission", "{player}", (helperUUID.asOfflineData()?.name ?: "无"))
//                    player.msg("已经添加此授权者")
                return@transaction
            }
            StorageManager.addHelpers(island, helperUUID)
            plot.addTrusted(helperUUID)
        }
    }

    fun getIslandHelpers(uuid: UUID): List<UUID> {
        return StorageManager.transaction {
            val data = getIslandData(uuid)
            if (data == null) return@transaction emptyList()
            data.helpers.map { it.helperUUID }
        }
    }

    fun getIslandHelpers(island: PlayerIsland?): List<UUID> {
        return StorageManager.transaction {
            if (island == null) return@transaction emptyList()
            island.helpers.map { it.helperUUID }
        }
    }

    fun setIslandVisibility(/*player: Player, */island: PlayerIsland, visibility: IslandVisibility) {
        StorageManager.setVisibility(island, visibility)
    }

    fun addKudoToIsland(island: PlayerIsland, player: Player): Boolean {
        return if (!StorageManager.addKudos(player.uuid, island)) {
            player.sendLang("kudoRepeat")
            false
        } else {
            player.sendLang("kudoSuccess")
            true
        }
    }

    fun resetIsland(pp: PlotPlayer<*>, plot: Plot): Promise<Unit, java.lang.Exception> {
//        return removeIsland(pp, plot).then {
//            Promise {
////                tryOpenPlayerIslandMenu((pp as BukkitPlayer).player, false)
//                resolve(Unit)
//            }
//        }
        return removeIsland(pp, plot, true)
    }

    fun removeIsland(pp: PlotPlayer<*>, plot: Plot, hasReset: Boolean = false): Promise<Unit, java.lang.Exception> {
        val data = getIslandData(pp.uuid)
        return Promise {
            if (data != null) {
                val loc =
                    Location(
                        plot.worldName!!.asWorld(),
                        data.coreX.toDouble(),
                        data.coreY.toDouble(),
                        data.coreZ.toDouble()
                    )
                PaperLib.getChunkAtAsync(loc, true).thenAccept {
                    MyIslands.INSTANCE.submitTask {
                        loc.block.type = Material.AIR
                        MyIslands.plotUtils.removePlot(pp, plot).whenComplete {
                            StorageManager.removePlayerIsland(data, hasReset)
                            resolve(Unit)
                        }
                    }
                }
            } else {
                MyIslands.plotUtils.removePlot(pp, plot)
                resolve(Unit)
            }
        }
    }

    fun getIslandData(player: Player): PlayerIsland? {
        return getIslandData(player.uuid)
    }

    fun getIslandLastResetDate(islandData: PlayerIsland?): LocalDateTime? {
        return StorageManager.transaction {
            val millis = islandData?.resetCooldown?.firstOrNull()?.prevTime?.millis ?: return@transaction null
            LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.systemDefault())
        }
    }

    fun getIslandData(uuid: UUID): PlayerIsland? {
        return StorageManager.getPlayerIsland(uuid)
    }

    fun getIslandAbs(player: Player): PlayerIsland? {
        val plot = MyIslands.plotUtils.getPlotByPLoc(player)
        if (plot == null || plot.owner == null) return null
        return getIslandData(plot.owner!!)
    }

    fun hasPermissionByFeet(player: Player): Boolean {
        val plot = MyIslands.plotUtils.getPlotByPLoc(player)
        return hasPermission(player, plot)
    }

    fun hasPermission(player: Player, plot: Plot?): Boolean {
        if (plot == null || plot.owner == null) return false
        if (plot.owner == player.uuid) return true
        return StorageManager.hasPermission(player, plot.owner!!)
    }

    fun tryOpenPlayerIslandMenu(player: Player, join: Boolean = true) {
        MyIslands.INSTANCE.submitTask(delay = 20L) {
            val plotPlayer = MyIslands.plotAPI.wrapPlayer(player.uuid)
            Log.debugP(player, "获取地皮玩家数据")
            if (plotPlayer == null) {
                Log.debugP(player, "无法获取地皮玩家数据")
                player.sendLang("plotPluginPlayerCacheError")
//                player.errorMsg("未知错误 MI0x1")
                return@submitTask
            }
            Log.debugP(player, "已获取到地皮玩家数据，开始获取玩家拥有的地皮")
            val plots = MyIslands.plotAPI.getPlayerPlots(plotPlayer)
            Log.debugP(player, "已获取玩家拥有的地皮，${plots.joinToString(", ") { it.id.toString() }}")

            if (plots.isNotEmpty()) {
                Log.debugP(player, "检测到玩家已领取地皮")
                if (join) {
                    joinPlayers[player.uuid] = plots.first()
                }
                Log.debugP(player, "准备传送玩家前往地皮")
                MyIslands.INSTANCE.submitTask(delay = 10L) {
                    plots.first().getHome {
                        plotPlayer.teleport(it)
                    }
                }
                return@submitTask
            }
            Log.debugP(player, "检测到玩家没有领取过地皮，正在打开领取菜单")
            MyIslands.INSTANCE.submitTask(delay = 10L) {
                IslandsChooseMenu(antiClose = true, block = IslandsManager::chooseIslandSchematic).open(player)
            }
        }
    }

    fun handleCMITP(player: Player, event: Cancellable) {
        if (joinPlayers.containsKey(player.uuid)) {
            joinPlayers.remove(player.uuid)
            event.isCancelled = true
        }
    }

    fun chooseIslandSchematic(
        chooseMenu: IslandsChooseMenu,
        player: Player,
        plotSchematic: ConfigManager.PlotSchematicConfig
    ) {
        val plotPlayer = MyIslands.plotAPI.wrapPlayer(player.uniqueId)
        if (plotPlayer == null) {
            player.sendLang("plotPluginPlayerCacheError")
//            player.errorMsg("未知错误，请重试")
            return
        }
        if (MyIslands.plotAPI.getPlayerPlots(plotPlayer).isNotEmpty()) {
            player.sendLang("alreadyOwnIsland")
//            player.errorMsg("你的已经拥有了自己的私人岛屿")
            return
        }
        chooseMenu.ok = true
        MyIslands.plotUtils.autoClaimPlot(player, plotPlayer) {
            val plots = MyIslands.plotAPI.getPlayerPlots(plotPlayer)
            val plot = plots.first()
            MyIslands.plotUtils.paste(player, plot, plotSchematic.name) {
                if (it) {
                    createPlayerIsland(player, plot, plotSchematic)
                } else {
                    player.sendLang("loadIslandPresetError")
//                    player.errorMsg("意外的错误: 0xMI0")
                }
            }
        }
    }

    fun createPlayerIsland(player: Player, plot: Plot, plotSchematic: ConfigManager.PlotSchematicConfig) {
        val coreVector = plotSchematic.core

        val dLoc = plot.bottomAbs
        val x = dLoc.x - coreVector.blockX
        val y = dLoc.y - coreVector.blockY
        val z = dLoc.z - coreVector.blockZ
        val world = Bukkit.getWorld(dLoc.world.name)!!
        val loc = Location(
            world,
            x.toDouble(), y.toDouble(), z.toDouble()
        )
        PaperLib.getChunkAtAsync(loc, true).thenAccept {
            MyIslands.INSTANCE.submitTask {
                loc.add(0.5, 0.0, 0.5)
                MyIslands.INSTANCE.submitTask(delay = 3 * 20L) {
                    setupPlotCore(loc)
                    player.sendLang("initIslandCore")
//                    player.successMsg("岛屿水晶已放置")
                }
                val homeLoc = getHomeLoc(loc)
                setPlotHome(plot, homeLoc)
                player.teleport(homeLoc)
                StorageManager.createPlayerIsland(player.uniqueId, loc.let { Vector(it.x, it.y, it.z) })
                player.sendLang("islandClaimSuccess")
//                player.successMsg("成功领取你的私人岛屿")
            }
        }
    }

    fun getHomeLoc(loc: Location): Location {
        return VectorUtils.lookAtLoc(loc.clone().add(0.0, 0.0, -3.5), loc.clone().add(0.0, -1.0, 0.5))
            .add(0.0, 0.0, -0.5)
    }

    fun setupPlotCore(loc: Location): CustomFurniture {
        return CustomFurniture.spawnPreciseNonSolid(ConfigManager.coreId, loc)!!.apply {
            armorstand?.isInvulnerable = true
        }
//        MyIslands.INSTANCE.runTaskLaterBR(5 * 20L) {
//            CustomFurniture.spawn(ConfigManager.coreId, loc.block)
////            CustomBlock.place(ConfigManager.coreId, loc)
//        }
    }

    fun setPlotHome(plot: Plot, loc: Location) {
        val bottomAbs = plot.bottomAbs
        plot.setHome(
            BlockLoc(
                loc.blockX - bottomAbs.x,
                loc.blockY,
                loc.blockZ - bottomAbs.z,
                loc.yaw,
                loc.pitch
            )
        )
    }

    fun checkPlayerPlotTrust(player: Player) {
        val plot = MyIslands.plotUtils.findPlot(player.uuid) ?: return
        val helpers = getIslandHelpers(player.uuid)
        helpers.forEach {
            if (plot.trusted.contains(it)) return
            plot.addTrusted(it)
        }
    }

    fun checkIslandHeatsAttenuation() {
        StorageManager.updateHeat()
    }

    fun replaceVarByLoreList(
        lore: List<Component>,
        plot: Plot,
        islandData: PlayerIsland?
    ): List<Component> {
        return lore.map { comp ->
            comp.tvar(
                "owner", plot.owner?.asOfflineData()?.name ?: "无",
                "helpers", getIslandHelpers(islandData).let { list ->
                    if (list.isEmpty()) "无" else list.joinToString(",") {
                        it.asOfflineData()?.name ?: "无"
                    }
                },
                "kudos", "${islandData?.kudos ?: "无"}",
                "heats", "${islandData?.heats ?: "无"}"
            )
        }
    }

}