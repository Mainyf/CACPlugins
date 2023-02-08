package io.github.mainyf.myislands

import com.plotsquared.bukkit.paperlib.PaperLib
import com.plotsquared.core.location.BlockLoc
import com.plotsquared.core.player.PlotPlayer
import com.plotsquared.core.plot.Plot
import com.shopify.promises.Promise
import dev.lone.itemsadder.api.CustomFurniture
import dev.lone.itemsadder.api.CustomStack
import io.github.mainyf.myislands.config.ConfigMI
import io.github.mainyf.myislands.config.sendLang
import io.github.mainyf.myislands.exceptions.IslandException
import io.github.mainyf.myislands.features.MoveIsLandCore
import io.github.mainyf.myislands.menu.IslandsChooseMenu
import io.github.mainyf.myislands.menu.IslandsHelperSelectMenu
import io.github.mainyf.myislands.storage.IslandVisibility
import io.github.mainyf.myislands.storage.PlayerIsland
import io.github.mainyf.myislands.storage.StorageMI
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.offline_player_ext.asOfflineData
import io.github.mainyf.newmclib.utils.VectorUtils
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.collections.any

object IslandsManager {

    private val joinPlayers = mutableMapOf<UUID, Plot>()
    val resetingIslands = mutableSetOf<UUID>()
    val moveingIslandCores = mutableSetOf<UUID>()

    private val helperPermissions = listOf(
        50,
        45,
        40,
        35,
        30,
        25,
        20,
        15,
        10,
        5
    )

    fun markMoveingIslandCore(uuid: UUID) {
        moveingIslandCores.add(uuid)
    }

    fun cleanMoveingIslandCore(uuid: UUID) {
        moveingIslandCores.remove(uuid)
    }

    fun getPlayerMaxHelperCount(player: Player): Int {
        var baseCount = helperPermissions.find { player.hasPermission("myislands.helpers.${it}") } ?: 5
        baseCount += helperPermissions.find { player.hasPermission("myislands.helpers.add.${it}") } ?: 0
        return baseCount
    }

    fun removeHelpers(plot: Plot, player: Player, island: PlayerIsland, helperUUID: UUID) {
        StorageMI.transaction {
            if (!island.helpers.toList().any { it.helperUUID == helperUUID }) {
                throw IslandException("${player.name} 尝试删除授权者 $helperUUID,但是这个授权者并不是他的授权者")
            }
            StorageMI.removeHelpers(island, helperUUID)
            plot.removeTrusted(helperUUID)
        }
    }

    fun addHelpers(plot: Plot, player: Player, island: PlayerIsland, helperUUID: UUID) {
        StorageMI.transaction {
            if (island.helpers.toList().size >= getPlayerMaxHelperCount(player)) {
                throw IslandException("${player.name} 尝试添加授权者，但授权者已满")
            }
            if (island.helpers.any { it.helperUUID == helperUUID }) {
                player.sendLang("repeatAddPermission", "{player}", (helperUUID.asOfflineData()?.name ?: "无"))
//                    player.msg("已经添加此授权者")
                return@transaction
            }
            StorageMI.addHelpers(island, helperUUID)
            plot.addTrusted(helperUUID)
        }
    }

    fun getIslandHelpers(uuid: UUID): List<UUID> {
        return StorageMI.transaction {
            val data = getIslandData(uuid)
            if (data == null) return@transaction emptyList()
            data.helpers.map { it.helperUUID }
        }
    }

    fun getIslandHelpers(island: PlayerIsland?): List<UUID> {
        return StorageMI.transaction {
            if (island == null) return@transaction emptyList()
            island.helpers.map { it.helperUUID }
        }
    }

    fun setIslandVisibility(/*player: Player, */island: PlayerIsland, visibility: IslandVisibility) {
        StorageMI.setVisibility(island, visibility)
    }

    fun addKudoToIsland(island: PlayerIsland, player: Player): Boolean {
        return if (!StorageMI.addKudos(player.uuid, island)) {
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
                            StorageMI.removePlayerIsland(data, hasReset)
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
        return StorageMI.transaction {
            val millis = islandData?.resetCooldown?.firstOrNull()?.prevTime?.millis ?: return@transaction null
            LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.systemDefault())
        }
    }

    fun getIslandData(uuid: UUID): PlayerIsland? {
        return StorageMI.getPlayerIsland(uuid)
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
        return StorageMI.hasPermission(player, plot.owner!!)
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
//                    joinPlayers[player.uuid] = plots.first()
                }
                Log.debugP(player, "准备传送玩家前往地皮")
                MyIslands.INSTANCE.submitTask(delay = 10L) {
                    MyIslands.plotUtils.teleportHomePlot(player)
//                    plots.first().getHome {
//                        plotPlayer.teleport(it)
//                    }
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
        plotSchematic: ConfigMI.PlotSchematicConfig
    ): Boolean {
        val plotPlayer = MyIslands.plotAPI.wrapPlayer(player.uniqueId)
        if (plotPlayer == null) {
            player.sendLang("plotPluginPlayerCacheError")
//            player.errorMsg("未知错误，请重试")
            return false
        }
        if (MyIslands.plotAPI.getPlayerPlots(plotPlayer).isNotEmpty()) {
            player.sendLang("alreadyOwnIsland")
//            player.errorMsg("你的已经拥有了自己的私人岛屿")
            return false
        }
        resetingIslands.add(player.uuid)

        // ensure remove
        MyIslands.INSTANCE.submitTask(delay = 8 * 20L) {
            resetingIslands.remove(player.uuid)
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
        return true
    }

    fun createPlayerIsland(player: Player, plot: Plot, plotSchematic: ConfigMI.PlotSchematicConfig) {
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
                fixIslandHomeLoc(loc)
                MyIslands.INSTANCE.submitTask(delay = 3 * 20L) {
                    setupPlotCore(loc)
                    resetingIslands.remove(plot.owner!!)
                    player.sendLang("initIslandCore")
//                    player.successMsg("岛屿水晶已放置")
                }
                val homeLoc = getHomeLoc(loc)
                setPlotHome(plot, homeLoc)
                player.teleport(homeLoc)
                StorageMI.createPlayerIsland(player.uniqueId, loc.let { Vector(it.x, it.y, it.z) })
                player.sendLang("islandClaimSuccess")
                player.sendLang("backIsland")
//                player.successMsg("成功领取你的私人岛屿")
            }
        }
    }

    fun fixIslandHomeLoc(loc: Location): Location {
        loc.add(0.5, 0.0, 0.5)
        return loc
    }

    fun getHomeLoc(loc: Location): Location {
        return VectorUtils.lookAtLoc(loc.clone().add(0.0, 0.0, -3.5), loc.clone().add(0.0, -1.0, 0.5))
            .add(0.0, 0.0, -0.5)
    }

    fun setupPlotCore(loc: Location): CustomFurniture {
//        Thread.currentThread().stackTrace.forEach {
//            println(it)
//        }
        return CustomFurniture.spawnPreciseNonSolid(ConfigMI.coreId, loc)!!.apply {
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

    fun getIslandCoreEntity(island: PlayerIsland): List<ArmorStand> {
        val oldLoc = getIslandCoreLoc(island)
        val world = oldLoc.world
//        val world = Bukkit.getWorld("plotworld")!!
//        val oldLoc = Location(
//            world,
//            island.coreX.toDouble(),
//            island.coreY.toDouble(),
//            island.coreZ.toDouble()
//        )
        return world.entities.filter {
            val eLoc = it.location
            eLoc.blockX == oldLoc.blockX && eLoc.blockY == oldLoc.blockY && eLoc.blockZ == oldLoc.blockZ
        }.filterIsInstance<ArmorStand>().filter {
            isIslandCore(it)
        }
    }

    fun isIslandCore(armorStand: ArmorStand): Boolean {
        if (CustomFurniture.byAlreadySpawned(armorStand) == null) {
            return false
        }
        return CustomStack.byItemStack(armorStand.equipment.helmet)?.namespacedID == ConfigMI.coreId
    }

    fun deleteIslandCore(player: Player, armorStand: ArmorStand) {
        MoveIsLandCore.markCoreRemove(player)
        armorStand.equipment.helmet = ItemStack(Material.AIR)
        CustomFurniture.remove(armorStand, true)
        armorStand.remove()
        MyIslands.INSTANCE.submitTask(delay = 20L) {
            MoveIsLandCore.unMarkCoreRemove(player)
        }
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
        StorageMI.updateHeat()
    }

    fun fixIslandCore(player: Player) {
        val plot = MyIslands.plotUtils.getPlotByPLoc(player) ?: return
        if (plot.owner == null) return
        if (resetingIslands.contains(plot.owner!!)) return
        if (moveingIslandCores.contains(plot.owner!!)) return
        val islandData = getIslandData(plot.owner!!) ?: return
        if (getIslandCoreEntity(islandData).isNotEmpty()) return
        val coreLoc = getIslandCoreLoc(islandData)
        val pLoc = player.location
        if (pLoc.world == coreLoc.world && pLoc.distanceSquared(coreLoc) <= 20) {
            setupPlotCore(fixIslandHomeLoc(coreLoc.clone()))
        }
    }

//    private fun debug(player: Player, debug: String) {
//        if (player.isOp) {
//            player.msg(debug)
//        }
//    }

    fun replaceVarByLoreList(
        lore: List<Component>?,
        plot: Plot?,
        islandData: PlayerIsland?
    ): List<Component>? {
        if (lore == null) return null
        return lore.map { comp ->
            comp.serialize().tvar(
                "owner", plot?.owner?.asOfflineData()?.name ?: "无",
                "helpers", getIslandHelpers(islandData).let { list ->
                    if (list.isEmpty()) "无" else list.joinToString(",") {
                        it.asOfflineData()?.name ?: "无"
                    }
                },
                "kudos", "${islandData?.kudos ?: "无"}",
                "heats", "${islandData?.heats ?: "无"}"
            ).deserialize()
        }
    }

    fun getIslandCoreLoc(islandData: PlayerIsland): Location {
        return Location(
            Bukkit.getWorld("plotworld")!!,
            islandData.coreX.toDouble(),
            islandData.coreY.toDouble(),
            islandData.coreZ.toDouble()
        )
    }

    fun openHelperSelectMenu(player: Player, plot: Plot, island: PlayerIsland, helpers: List<UUID?>, block: () -> Boolean): Boolean {
        if (plot.owner != player.uuid) {
            player.sendLang("noOwnerOpenHelperSelectMenu")
            return false
        }
        val players = onlinePlayers().filter { p ->
            p.uuid != plot.owner && !helpers.contains(p.uuid) && p.asPlotPlayer()?.location?.plotAbs?.owner == plot.owner
        }
        if (players.isEmpty()) {
            player.sendLang("islandPlayerAbsEmpty")
            return false
        }
        IslandsHelperSelectMenu(island, plot, players, block).open(player)
        return true
    }

}