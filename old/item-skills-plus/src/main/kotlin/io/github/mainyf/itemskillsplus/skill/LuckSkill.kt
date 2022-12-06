package io.github.mainyf.itemskillsplus.skill

import io.github.mainyf.itemskillsplus.SkillManager
import io.github.mainyf.itemskillsplus.config.ConfigManager
import io.github.mainyf.itemskillsplus.config.EffectTriggerType
import io.github.mainyf.itemskillsplus.exts.blockKey
import io.github.mainyf.itemskillsplus.exts.getKey
import io.github.mainyf.newmclib.exts.currentTime
import io.github.mainyf.newmclib.exts.msg
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.block.BlockPlaceEvent
import java.util.*

object LuckSkill : Listener {

    private val recursiveFixer = mutableSetOf<UUID>()
    private val blockKey = mutableSetOf<Long>()
    private val breakBlock = mutableMapOf<Long, BlockData>()

    fun init() {

    }

    @EventHandler
    fun onPlace(event: BlockPlaceEvent) {
        if (!ConfigManager.luckEnable) return
        blockKey.add(event.block.blockKey)
    }

    @EventHandler(ignoreCancelled = true)
    fun onBreak(event: BlockBreakEvent) {
        if (!ConfigManager.luckEnable) return
        val item = event.player.inventory.itemInMainHand
        if (item.type == Material.AIR) return
        val data = SkillManager.getItemSkill(SkillManager.luckDataKey, item) ?: return
        val player = event.player

        val exp = ConfigManager.getBlockExp(event.block)
        if (exp > 0.0) {
            SkillManager.addExpToItem(data, exp)
//            player.msg("你破坏了 ${event.block.type.name} 获得经验 $exp, 阶段: ${data.stage} 等级: ${data.level} 当前经验: ${data.exp}/${data.maxExp}")
        }

        if (hasRecursive(player)) {
            return
        }
        val s = currentTime()
        if (data.stage >= 3) {
            breakBlock[event.block.blockKey] = BlockData(event.block.type, event.block.isLiquid)
        }

        when {
            data.stage == 1 -> {
                val block = getNearBlock(event.block.location)
                if (block != null) {
                    markRecursive(player)
                    event.player.breakBlock(block)
                }
            }
            data.stage >= 2 -> {
                markRecursive(player)
                getNearBlocks(event.block.location).forEach {
                    if (data.stage >= 3) {
                        breakBlock[it.blockKey] = BlockData(event.block.type, event.block.isLiquid)
                    }
                    event.player.breakBlock(it)
                }
            }
        }

        player.msg("${currentTime() - s}ms")
        SkillManager.updateItemMeta(item, SkillManager.luckDataKey, data)
        SkillManager.triggerItemSkinEffect(
            player,
            SkillManager.luckDataKey,
            data,
            EffectTriggerType.BREAK
        )
        unMarkRecursive(player)
    }

    private fun markRecursive(player: Player) {
        if (!recursiveFixer.contains(player.uniqueId)) {
            recursiveFixer.add(player.uniqueId)
        }
    }

    private fun hasRecursive(player: Player): Boolean {
        return recursiveFixer.contains(player.uniqueId)
    }

    private fun unMarkRecursive(player: Player) {
        recursiveFixer.remove(player.uniqueId)
    }

    @EventHandler
    fun onDropItem(event: BlockDropItemEvent) {
        if (!ConfigManager.luckEnable) return
        val item = event.player.inventory.itemInMainHand
        if (item.type == Material.AIR) return
        val data = SkillManager.getItemSkill(SkillManager.luckDataKey, item) ?: return
        val block = breakBlock[event.block.blockKey]
        if (data.stage >= 3 && hasValid(block)) {
            event.items.forEach {
                event.block.world.dropItem(event.block.location, it.itemStack)
            }
        }
        breakBlock.remove(event.block.blockKey)
    }

    private fun addBlockToList(
        x: Int,
        y: Int,
        z: Int,
        oKey: Long,
        world: World,
        list: MutableList<Block>,
        map: MutableMap<Long, Block>,
        max: Int
    ) {
        if (map.size < max) {
            val key = getKey(x, y, z)
            if (oKey != key && !map.containsKey(key)) {
                val block = world.getBlockAt(x, y, z)
                if (hasValid(block)) {
                    map[key] = block
                    list.add(block)
                }
            }
        }
    }

    private fun addConnectedBlockToMap(
        loc: Location,
        map: MutableMap<Long, Block>,
        max: Int
    ): List<Block> {
        val world = loc.world!!
        val blockX = loc.blockX
        val blockY = loc.blockY
        val blockZ = loc.blockZ

        val rs = mutableListOf<Block>()
        val oKey = getKey(blockX, blockY, blockZ)
        var x = blockX
        var y = blockY + 1
        var z = blockZ
        addBlockToList(x, y, z, oKey, world, rs, map, max)
        y = blockY - 1
        addBlockToList(x, y, z, oKey, world, rs, map, max)
        x = blockX + 1
        y = blockY
        addBlockToList(x, y, z, oKey, world, rs, map, max)
        x = blockX - 1
        addBlockToList(x, y, z, oKey, world, rs, map, max)
        x = blockX
        z = blockZ + 1
        addBlockToList(x, y, z, oKey, world, rs, map, max)
        z = blockZ - 1
        addBlockToList(x, y, z, oKey, world, rs, map, max)

        return rs
    }

    private fun getNearBlock(loc: Location): Block? {
        return addConnectedBlockToMap(
            loc,
            mutableMapOf(),
            ConfigManager.luckMax
        )
            .sortedBy { if (it.y == loc.blockY) 0 else 1 }
            .firstOrNull()
    }

    private fun hasValid(data: BlockData?): Boolean {
        if (data == null) {
            return false
        }
        if (data.type == Material.AIR) {
            return false
        }
        if (data.isLiquid) {
            return false
        }
        if (!ConfigManager.isLuckAllowBlock(data.type)) {
            return false
        }
//        if (blockKey.contains(block.blockKey)) {
//            return false
//        }
        return true
    }

    private fun hasValid(block: Block?): Boolean {
        if (block == null) {
            return false
        }
        if (block.type == Material.AIR) {
            return false
        }
        if (block.isLiquid) {
            return false
        }
        if (!ConfigManager.isLuckAllowBlock(block.type)) {
            return false
        }
//        if (blockKey.contains(block.blockKey)) {
//            return false
//        }
        return true
    }

    private fun getNearBlocks(loc: Location): List<Block> {
        val map = mutableMapOf<Long, Block>()
        val luckMax = ConfigManager.luckMax
        var list = addConnectedBlockToMap(loc, map, luckMax)
        if (list.isEmpty()) {
            return emptyList()
        } else {
            while (map.size < luckMax) {
                val l = mutableListOf<Block>()
                list.forEach {
                    l.addAll(addConnectedBlockToMap(it.location, map, luckMax))
                }
                if (l.isEmpty()) {
                    break
                } else {
                    list = l
                }
            }
        }
        return map.values.sortedByDescending { it.y }
    }

    data class BlockData(
        val type: Material,
        val isLiquid: Boolean
    )

}