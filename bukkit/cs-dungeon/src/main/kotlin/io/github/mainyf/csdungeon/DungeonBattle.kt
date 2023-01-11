package io.github.mainyf.csdungeon

import io.github.mainyf.csdungeon.config.ConfigCSD
import io.github.mainyf.csdungeon.config.sendLang
import io.github.mainyf.csdungeon.storage.DungeonStructure
import io.github.mainyf.csdungeon.storage.StorageCSD
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.utils.Cooldown
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.scheduler.BukkitRunnable
import kotlin.collections.find

class DungeonBattle(val dungeon: DungeonStructure, var level: Int) {

    var start = false
    var endCheckTask: BukkitRunnable? = null
    var checkMobValidTask: BukkitRunnable? = null
    val mobSpawnTasks = mutableListOf<BukkitRunnable>()

    //    var flyCheckTask: BukkitRunnable? = null
    var tipsTask: BukkitRunnable? = null
    val dungeonConfig get() = ConfigCSD.dungeonConfigMap[dungeon.dungeonName]
    val dungeonLevelConfig get() = dungeonConfig!!.levels.find { it.level == level }

    //    val chunks by lazy {
    //        val world = dungeon.worldName.asWorld()!!
    //        val minChunk = world.getChunkAt(dungeon.minX.toInt(), dungeon.minZ.toInt())
    //        val maxChunk = world.getChunkAt(dungeon.maxX.toInt(), dungeon.maxZ.toInt())
    //        val rs = mutableListOf<Chunk>()
    //        for (x in minChunk.x .. maxChunk.x) {
    //            for (z in minChunk.z .. maxChunk.z) {
    //                rs.add(world.getChunkAt(x, z))
    //            }
    //        }
    //        rs
    //    }
    //    val chunkEntities get() = chunks.flatMap { it.entities.toList() }
    val dungeonMobSpawnLoc by lazy { StorageCSD.getDungeonMobSpawnLoc(dungeon) }
    val players = mutableSetOf<Player>()
    val moveMsgCD = Cooldown()
    val mobList = mutableListOf<MobWrapper>()

    fun hasInBattle(player: Player): Boolean {
        return players.contains(player)
    }

    fun startBattle() {
        if (start) {
            return
        }
        start = true

        endCheckTask = CsDungeon.INSTANCE.submitTask(period = 10L) {
            if(!dungeonConfig!!.enable) {
                players.forEach {
                    it.sendLang("dungeonDisable")
                }
                end()
                return@submitTask
            }
            checkPlayers()
        }
//        checkMobValidTask = CsDungeon.INSTANCE.submitTask(period = 10L) {
//            val world = dungeon.worldName.asWorld() ?: return@submitTask
//            val entities = world.livingEntities
//            val iter = mobList.iterator()
//            while (iter.hasNext()) {
//                val mob = iter.next()
//                if (!entities.contains(mob.bukkitEntity)) {
//                    mob.setDead()
//                    iter.remove()
//                }
//            }
//        }
        //        flyCheckTask = CsDungeon.INSTANCE.submitTask(period = 10L) {
        //            if (!dungeonConfig!!.noFly) return@submitTask
        //            players.forEach {
        //                if (it.allowFlight) {
        //                    it.allowFlight = false
        //                }
        //            }
        //        }
        tipsTask = CsDungeon.INSTANCE.submitTask(period = dungeonConfig!!.tipPeriod) {
            players.forEach { player ->
                dungeonConfig!!.tipActions.execute(
                    player,
                    "{player}",
                    player.name,
                    "{kill}",
                    mobList.count { it.isDead },
                    "{total}",
                    dungeonLevelConfig!!.totalMob
                )
            }
        }
        addMobSpawnTasks()
        checkPlayers()
        players.forEach {
            dungeonConfig!!.startActions.execute(it)
        }
        val coreLoc = Location(dungeon.worldName.asWorld(), dungeon.coreX, dungeon.coreY, dungeon.coreZ)
        dungeonConfig!!.startPlays.execute(coreLoc)
    }

    private fun checkPlayers() {
        onlinePlayers().forEach {
            if (!dungeon.containsDungeonArea(it.location)) {
                if (players.contains(it)) {
                    removePlayer(it)
                }
            } else {
                if (!players.contains(it)) {
                    addPlayer(it)
                }
            }
        }
        val dungeonLevelConfig = dungeonLevelConfig ?: return
        if (mobList.size >= dungeonLevelConfig.totalMob && players.isNotEmpty() && mobList.all { it -> it.isDead }) {
            end()
        }
        if (dungeonConfig!!.noPlayerEnd && players.isEmpty()) {
            end()
        }
    }

    private fun addMobSpawnTasks() {
        val dungeonLevelConfig = dungeonLevelConfig ?: return
        dungeonLevelConfig.mobSpawns.forEach { mobConfig ->
            mobSpawnTasks.add(CsDungeon.INSTANCE.submitTask(period = mobConfig.spawnPeriod) {
                if (mobList.size >= dungeonLevelConfig.totalMob) {
                    return@submitTask
                }
                if (mobList.count { !it.isDead } >= mobConfig.max) {
                    return@submitTask
                }
                val locs = dungeonMobSpawnLoc.filter { it.mobName == mobConfig.loc }
                    .map { loc -> Location(dungeon.worldName.asWorld(), loc.x, loc.y, loc.z) }
                    .filter { it.getNearbyPlayers(mobConfig.locationSpacing.toDouble()).isEmpty() }
                if (locs.isEmpty()) return@submitTask
                val bLoc = locs.random()
                //                val bLoc = Location(dungeon.worldName.asWorld(), loc.x, loc.y, loc.z)
                val mobType = mobConfig.mobTypes.random()
                val wrapper = mobType.spawnMob(bLoc) ?: return@submitTask
                //                println("怪物生成在 ${bLoc.x} ${bLoc.y} ${bLoc.z}")
                mobList.add(wrapper)
            })
        }
    }

    fun end() {
        players.forEach { player ->
            dungeonConfig!!.endActions.execute(
                player,
                "{player}",
                player.name,
                "{kill}",
                mobList.count { it.isDead },
                "{total}",
                dungeonLevelConfig!!.totalMob
            )
        }
        val coreLoc = Location(dungeon.worldName.asWorld(), dungeon.coreX, dungeon.coreY, dungeon.coreZ)
        dungeonConfig!!.endPlays.execute(coreLoc)
        start = false

        endCheckTask?.cancel()
        endCheckTask = null

        checkMobValidTask?.cancel()
        checkMobValidTask = null

        mobSpawnTasks.forEach {
            it.cancel()
        }
        mobSpawnTasks.clear()

        //        flyCheckTask?.cancel()
        //        flyCheckTask = null

        tipsTask?.cancel()
        tipsTask = null

        players.clear()
        mobList.forEach {
            if (!it.isDead) {
                it.setDead()
            }
        }
        mobList.clear()
    }

    fun addPlayer(player: Player) {
        players.add(player)
    }

    fun removePlayer(player: Player) {
        players.remove(player)
    }

    fun onPlayerMove(player: Player, event: PlayerMoveEvent) {
        if (!start) return
        if (!players.contains(player) && !player.isOp) {
            if (dungeon.containsDungeonArea(event.to)) {
                moveMsgCD.invoke(player.uuid, 500, {
                    player.sendLang("notJoinBegunDungeon")
                }, {})
                event.isCancelled = true
            }
            return
        }
        if (players.contains(player)) {
            val boundaryDamage = dungeonConfig!!.boundaryDamage
            if (boundaryDamage == -1) return
            if (!dungeon.containsDungeonArea(event.to)) {
                event.isCancelled = true
                moveMsgCD.invoke(player.uuid, 500, {
                    if (boundaryDamage == 0) return@invoke
                    player.health =
                        (player.health - boundaryDamage).clamp(
                            0.0,
                            player.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value
                        )
                    player.sendLang("outDungeonBorder")
                }, {})
            }
        }
    }

    fun onPlayerTeleport(player: Player, event: PlayerTeleportEvent) {
        if (!start) return
        if (!players.contains(player) && !player.isOp) {
            if (dungeon.containsDungeonArea(event.to)) {
                moveMsgCD.invoke(player.uuid, 500, {
                    player.sendLang("notJoinBegunDungeon")
                }, {})
                event.isCancelled = true
            }
            return
        }
    }

}