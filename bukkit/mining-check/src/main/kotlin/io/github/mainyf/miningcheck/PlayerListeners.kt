package io.github.mainyf.miningcheck

import com.google.common.cache.CacheBuilder
import io.github.mainyf.itemenchantplus.enchants.ExpandEnchant
import io.github.mainyf.miningcheck.config.ConfigMC
import io.github.mainyf.miningcheck.storage.StorageMC
import io.github.mainyf.newmclib.exts.*
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.collections.find

object PlayerListeners : Listener {

    private val ignoreBlockKey = mutableSetOf<Long>()
    private val playerTimeMap = mutableMapOf<UUID, Long>()
    private val playerCountMap = mutableMapOf<UUID, Int>()
    private val cacheMap =
        CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES).build<UUID, MutableSet<Long>>()

    fun reset() {
        playerTimeMap.clear()
        playerCountMap.clear()
    }

    @EventHandler
    fun onPlace(event: BlockPlaceEvent) {
        if (!ConfigMC.enable) return
        if (event.player.isOp) return
        ignoreBlockKey.add(event.block.getKey())
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun saveBreakBlock(event: BlockBreakEvent) {
        val block = event.block
        val set = cacheMap.get(event.player.uuid) {
            mutableSetOf()
        }
        set.add(block.getKey())
    }

    @EventHandler
    fun onBreak(event: BlockBreakEvent) {
        if (!ConfigMC.enable) return
        val player = event.player
        val block = event.block
        val bKey = block.getKey()
        if (ignoreBlockKey.contains(bKey)) {
            ignoreBlockKey.remove(bKey)
            return
        }
        if (hasCaveAir(player, block)) {
            debug(
                player,
                "玩家: ${player.name}，此次挖掘判定为矿洞，不计入限制"
            )
            return
        }
        val worldName = block.world.name
        if (!ConfigMC.worlds.containsKey(worldName)) return
        val config = ConfigMC.worlds[worldName]!!
        if (!config.blocks.containsKey(block.type)) return
        var score = config.blocks[block.type]!!
        val currentTime = currentTime()
        if (!playerTimeMap.containsKey(player.uuid)) {
            playerTimeMap[player.uuid] = currentTime
        }
        val startTime = playerTimeMap[player.uuid]!!
        val elapsedTime = (currentTime - startTime) / 1000
        if (elapsedTime >= config.countingTime) {
            playerTimeMap[player.uuid] = currentTime
            playerCountMap[player.uuid] = 0
            debug(
                player,
                "玩家: ${player.name} 触发时间重置"
            )
            return
        }
        var pCount = playerCountMap[player.uuid] ?: 0
        val usePlusExpand =
            currentTime - ExpandEnchant.getUsePlusEnchantTime(player.uuid) <= config.expandIgnoreSecound * 1000
        if (usePlusExpand) {
            score *= config.expandPlusRatio
            debug(
                player,
                "玩家: ${player.name} 触发了 expand plus 检测"
            )
        } else {
            val useStage3Expand =
                currentTime - ExpandEnchant.getUseStage3EnchantTime(player.uuid) <= config.expandIgnoreSecound * 1000
            if (useStage3Expand) {
                score *= config.expandStage3Ratio
                debug(
                    player,
                    "玩家: ${player.name} 触发了 expand stage3 检测"
                )
            }
        }
        pCount += score
        debug(
            player,
            "玩家: ${player.name} 当前分数: ${pCount}/${config.countMax}，距离重置: ${elapsedTime}s/${config.countingTime}s"
        )
        if (pCount >= config.countMax) {
            var num = StorageMC.getPlayerActionNum(config, player.uuid)
            num++
            debug(player, "玩家: ${player.name} 分数已满，触发第 $num 次")
            val countAction = config.countActions.values.find { it.num == num }
            if (countAction != null) {
                pCount = 0
                val loc = player.location
                countAction.action?.execute(
                    player,
                    "{player}", player.name,
                    "loc", "${loc.world.name},${loc.blockX},${loc.blockY},${loc.blockZ}"
                )
                StorageMC.setPlayerActionNum(config, player.uuid, num)
                playerTimeMap.remove(player.uuid)
            }
        }
        playerCountMap[player.uuid] = pCount
    }

    private fun debug(player: Player, msg: String) {
        if (ConfigMC.debug) {
            player.msg(msg)
        }
    }

    private fun hasCaveAir(player: Player, block: Block): Boolean {
        val blockSet = cacheMap.getIfPresent(player.uuid) ?: emptySet()
        return arrayOf(
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST,
            BlockFace.UP,
            BlockFace.DOWN
        ).any {
            val loc = block.location.add(it.direction)
            val isAir = loc.block.type.isAir
            if (isAir) {
                !blockSet.contains(loc.getBlockKey())
            } else false
        }
    }

}