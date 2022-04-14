package io.github.mainyf.itemskillsplus.skill

import io.github.mainyf.itemskillsplus.ItemSkillsPlus
import io.github.mainyf.itemskillsplus.SkillManager
import io.github.mainyf.itemskillsplus.config.ConfigManager
import io.github.mainyf.newmclib.exts.msg
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import java.util.*


object ExpandSkill : Listener {

    private val recursiveFixer = mutableSetOf<UUID>()
    private val playerHeldItem = mutableSetOf<UUID>()

    fun init() {
        Bukkit.getScheduler().runTaskTimer(ItemSkillsPlus.INSTANCE, { _ ->
            if (!ConfigManager.expandEnable) return@runTaskTimer
            Bukkit.getOnlinePlayers().forEach { p ->
                val item = p.inventory.itemInMainHand
                if (item.type == Material.AIR) {
                    tryRemovePlayerBuff(p)
                    return@forEach
                }
                val data = SkillManager.getItemSkill(SkillManager.expandDataKey, item)
                if (data == null) {
                    tryRemovePlayerBuff(p)
                    return@forEach
                }
                if (data.stage >= 3) {
                    p.addPotionEffect(PotionEffect(PotionEffectType.FAST_DIGGING, 5 * 20, 2))
                    playerHeldItem.add(p.uniqueId)
                } else {
                    tryRemovePlayerBuff(p)
                }
            }
        }, 2 * 20L, 2 * 20L)
    }

    private fun tryRemovePlayerBuff(player: Player) {
        if (playerHeldItem.contains(player.uniqueId)) {
            player.removePotionEffect(PotionEffectType.FAST_DIGGING)
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    fun onBreak(event: BlockBreakEvent) {
        if (!ConfigManager.expandEnable) return
        val item = event.player.inventory.itemInMainHand
        if (item.type == Material.AIR) return
        val data = SkillManager.getItemSkill(SkillManager.expandDataKey, item) ?: return
        val player = event.player

        val world = player.world
        val b = event.block
        val loc = player.location

        val exp = ConfigManager.getBlockExp(event.block)
        if (exp > 0.0) {
            SkillManager.addExpToItem(data, exp)
            player.msg("你破坏了 ${event.block.type.name} 获得经验 $exp, 阶段: ${data.stage} 等级: ${data.level} 当前经验: ${data.exp}/${data.maxExp}")
        }

        if (hasRecursive(player)) {
            return
        }
        var clockwiseBlockList = mutableListOf<Block>()
        val facing = player.facing
//        player.msg(facing.toString())
        val hasBottom = loc.blockY > b.y
        val hasTop = loc.blockY + 1 < b.y
        val hasFront = loc.blockX < b.x
        val hasBack = loc.blockX > b.x
        val hasLeft = loc.blockZ > b.z
        val hasRight = loc.blockZ < b.z
        when {
            hasBottom || hasTop -> {
                val eastList = mutableListOf(
                    world.getBlockAt(b.x + 1, b.y, b.z - 1),
                    world.getBlockAt(b.x + 1, b.y, b.z),
                    world.getBlockAt(b.x + 1, b.y, b.z + 1),

                    world.getBlockAt(b.x, b.y, b.z + 1),
                    world.getBlockAt(b.x - 1, b.y, b.z + 1),

                    world.getBlockAt(b.x - 1, b.y, b.z),
                    world.getBlockAt(b.x - 1, b.y, b.z - 1),
                    world.getBlockAt(b.x, b.y, b.z - 1)
                )
                if (hasBottom) {
                    val index = arrayOf(
                        BlockFace.EAST,
                        BlockFace.NORTH,
                        BlockFace.WEST,
                        BlockFace.SOUTH
                    ).indexOf(player.facing)
                    if (index != -1) {
                        repeat(index) {
                            eastList.add(0, eastList.removeLast())
                            eastList.add(0, eastList.removeLast())
                        }
                        clockwiseBlockList = eastList
                    }
                }
                if (hasTop) {
                    eastList.reverse()
                    eastList.add(eastList.removeFirst())
                    val index = arrayOf(
                        BlockFace.EAST,
                        BlockFace.NORTH,
                        BlockFace.WEST,
                        BlockFace.SOUTH
                    ).indexOf(player.facing)
                    if (index != -1) {
                        repeat(index) {
                            eastList.add(eastList.removeFirst())
                            eastList.add(eastList.removeFirst())
                        }
                        clockwiseBlockList = eastList
                    }
                }
            }
            (facing == BlockFace.EAST || facing == BlockFace.WEST) && (hasFront || hasBack) -> {
                val eastList = mutableListOf(
                    world.getBlockAt(b.x, b.y + 1, b.z - 1),
                    world.getBlockAt(b.x, b.y + 1, b.z),
                    world.getBlockAt(b.x, b.y + 1, b.z + 1),

                    world.getBlockAt(b.x, b.y, b.z + 1),
                    world.getBlockAt(b.x, b.y - 1, b.z + 1),

                    world.getBlockAt(b.x, b.y - 1, b.z),
                    world.getBlockAt(b.x, b.y - 1, b.z - 1),
                    world.getBlockAt(b.x, b.y, b.z - 1)
                )
                if (facing == BlockFace.WEST) {
                    eastList.reverse()
                    val l = mutableListOf<Block>()
                    repeat(3) {
                        l.add(eastList.removeAt(5))
                    }
                    eastList.addAll(0, l)
                }
                clockwiseBlockList = eastList
            }
            (facing == BlockFace.NORTH || facing == BlockFace.SOUTH) && hasLeft || hasRight -> {
                val northList = mutableListOf(
                    world.getBlockAt(b.x - 1, b.y + 1, b.z),
                    world.getBlockAt(b.x, b.y + 1, b.z),
                    world.getBlockAt(b.x + 1, b.y + 1, b.z),

                    world.getBlockAt(b.x + 1, b.y, b.z),
                    world.getBlockAt(b.x + 1, b.y - 1, b.z),

                    world.getBlockAt(b.x, b.y - 1, b.z),
                    world.getBlockAt(b.x - 1, b.y - 1, b.z),
                    world.getBlockAt(b.x - 1, b.y, b.z)
                )
                if (facing == BlockFace.SOUTH) {
                    northList.reverse()
                    val l = mutableListOf<Block>()
                    repeat(3) {
                        l.add(northList.removeAt(5))
                    }
                    northList.addAll(0, l)
                }
                clockwiseBlockList = northList
            }
        }
        val bList = clockwiseBlockList.filter { hasValid(it) }
        if (bList.isNotEmpty()) {
            if (data.stage == 1) {
                event.isCancelled = true
                markRecursive(player)
                bList.first().breakNaturally()
            } else if (data.stage >= 2) {
                markRecursive(player)
                bList.forEach {
                    it.breakNaturally()
                }
            }
        }

        SkillManager.updateItemMeta(item, SkillManager.expandDataKey, data)
        SkillManager.triggerItemSkinEffect(
            player,
            SkillManager.expandDataKey,
            data,
            ConfigManager.EffectTriggerType.BREAK
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

//    private fun getNearBlock(loc: Location): Block? {
//        val world = loc.world
//        val blockX = loc.blockX
//        val blockY = loc.blockY
//        val blockZ = loc.blockZ
//
//        arrayOf(
//            Vector(blockX + 1, blockY, blockZ),
//            Vector(blockX, blockY, blockZ + 1),
//            Vector(blockX + 1, blockY, blockZ + 1),
//            Vector(blockX - 1, blockY, blockZ),
//            Vector(blockX, blockY, blockZ - 1),
//            Vector(blockX - 1, blockY, blockZ - 1),
//            Vector(blockX - 1, blockY, blockZ + 1),
//            Vector(blockX + 1, blockY, blockZ - 1),
//
//            Vector(blockX, blockY + 1, blockZ),
//            Vector(blockX + 1, blockY + 1, blockZ),
//            Vector(blockX, blockY + 1, blockZ + 1),
//            Vector(blockX + 1, blockY + 1, blockZ + 1),
//            Vector(blockX - 1, blockY + 1, blockZ),
//            Vector(blockX, blockY + 1, blockZ - 1),
//            Vector(blockX - 1, blockY + 1, blockZ - 1),
//            Vector(blockX - 1, blockY + 1, blockZ + 1),
//            Vector(blockX + 1, blockY + 1, blockZ - 1),
//
//            Vector(blockX, blockY - 1, blockZ),
//            Vector(blockX + 1, blockY - 1, blockZ),
//            Vector(blockX, blockY - 1, blockZ + 1),
//            Vector(blockX + 1, blockY - 1, blockZ + 1),
//            Vector(blockX - 1, blockY - 1, blockZ),
//            Vector(blockX, blockY - 1, blockZ - 1),
//            Vector(blockX - 1, blockY - 1, blockZ - 1),
//            Vector(blockX - 1, blockY - 1, blockZ + 1),
//            Vector(blockX + 1, blockY - 1, blockZ - 1)
//        ).forEach {
//            val block = world.getBlockAt(it.blockX, it.blockY, it.blockZ)
//            if (hasValid(block)) {
//                return block
//            }
//        }
//
//        return null
//    }

    private fun hasValid(block: Block): Boolean {
        if (block.type == Material.AIR) {
            return false
        }
        if (block.isLiquid) {
            return false
        }
        if (!ConfigManager.isExpandAllowBlock(block.type)) {
            return false
        }
        return true
    }

//    private fun getNearBlocks(loc: Location, dis: Int = 1): List<Block> {
//        val world = loc.world
//        val blockX = loc.blockX
//        val blockY = loc.blockY
//        val blockZ = loc.blockZ
//        val rs = mutableListOf<Block>()
//        val lX = blockX - dis
//        val lY = blockY - dis
//        val lZ = blockZ - dis
//
//        val hX = blockX + dis
//        val hY = blockY + dis
//        val hZ = blockZ + dis
//
//        for (x in lX..hX) {
//            for (y in lY..hY) {
//                for (z in lZ..hZ) {
//                    if (x == blockX && y == blockY && z == blockZ) {
//                        continue
//                    }
//                    val block = world.getBlockAt(x, y, z)
//                    if (!hasValid(block)) {
//                        continue
//                    }
//                    rs.add(block)
//                }
//            }
//        }
//
//        return rs.sortedByDescending { it.y }
//    }

}