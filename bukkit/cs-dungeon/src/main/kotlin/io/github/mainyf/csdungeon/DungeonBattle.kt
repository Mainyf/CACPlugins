package io.github.mainyf.csdungeon

import io.github.mainyf.csdungeon.config.ConfigCSD
import io.github.mainyf.csdungeon.storage.DungeonStructure
import io.github.mainyf.csdungeon.storage.StorageCSD
import io.github.mainyf.newmclib.exts.submitTask
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable

class DungeonBattle(val dungeon: DungeonStructure) {

    var start = false
    var mobSpawnTask: BukkitRunnable? = null
    val dungeonConfig get() = ConfigCSD.dungeonConfigMap[dungeon.dungeonName]
    val dungeonMobSpawnLoc by lazy { StorageCSD.getDungeonMobSpawnLoc(dungeon) }
    val players = mutableSetOf<Player>()

    fun startBattle() {
        if (start) {
            return
        }
        start = true

        mobSpawnTask = CsDungeon.INSTANCE.submitTask(period = 20L) {
            dungeonMobSpawnLoc
            dungeonConfig!!.mobs.forEach { mobConfig ->

            }
        }
    }

    fun end() {
        start = false
        mobSpawnTask?.cancel()
        mobSpawnTask = null
        players.clear()
    }

    fun addPlayer(player: Player) {
        players.add(player)
    }

    fun removePlayer(player: Player) {
        players.remove(player)
    }

}