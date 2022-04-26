package io.github.mainyf.itemskillsplus

import dev.lone.itemsadder.api.CustomStack
import io.github.mainyf.itemskillsplus.config.ConfigManager
import io.github.mainyf.itemskillsplus.menu.MainMenu
import io.github.mainyf.itemskillsplus.storage.StorageManager
import io.github.mainyf.newmclib.command.cmdParser
import io.github.mainyf.newmclib.exts.errorMsg
import io.github.mainyf.newmclib.exts.msg
import io.github.mainyf.newmclib.exts.successMsg
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import kotlin.system.measureTimeMillis

class CommandHandler : CommandExecutor {

    override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<String>): Boolean {
        cmdParser(sender, args) cmd@{
            val type = arg<String>() ?: return@cmd
            when (type) {
                "testI" -> {
                    StorageManager.testInsert()
                }
                "test" -> {
                    val player = arg<Player>() ?: return@cmd
                    println()
                }
                "mmenu" -> {
                    val player = arg<Player>() ?: return@cmd
                    MainMenu().open(player)
                }
                "get" -> {
                    val player = sender as? Player ?: return@cmd
                    val skillType = arg<String>() ?: return@cmd
                    val customStack = CustomStack.getInstance("itemskills:${skillType}")
                    val itemStack = customStack?.itemStack
                    if (itemStack == null || itemStack.type == Material.AIR) {
                        player.errorMsg("该物品不存在")
                        return@cmd
                    }
                    val dataKey = SkillManager.getSkillByName(skillType)
                    SkillManager.initItemSkill(player, dataKey, itemStack)
                    val data = SkillManager.getItemSkill(dataKey, itemStack)
                    SkillManager.updateItemMeta(itemStack, dataKey, data!!)
                    player.inventory.addItem(itemStack)
                    player.updateInventory()
                    player.successMsg("获取成功")
                }
                "stage" -> {
                    val player = sender as? Player ?: return@cmd
                    val stage = arg<Int>() ?: 1
                    val item = player.inventory.itemInMainHand
                    if (item.type == Material.AIR) return@cmd
                    val data = SkillManager.getItemSkill(item)
                    if (data == null) {
                        player.errorMsg("该物品无进阶附魔数据")
                        return@cmd
                    }
                    data.stage = stage
                    SkillManager.setItemSkillData(item, data)
                    player.msg("&6设置成功")
                }
                "addExp" -> {
                    val player = sender as? Player ?: return@cmd
                    val exp = arg<Double>() ?: return@cmd
                    val item = player.inventory.itemInMainHand
                    if (item.type == Material.AIR) return@cmd
                    val ms = measureTimeMillis {
                        val data = SkillManager.getItemSkill(item)
                        if (data == null) {
                            player.errorMsg("该物品无进阶附魔数据")
                            return@cmd
                        }
                        SkillManager.addExpToItem(data, exp)
                        SkillManager.updateItemMeta(item, SkillManager.getSkillByName(data.skillType), data)
                        player.msg("获得经验 $exp, 阶段: ${data.stage} 等级: ${data.level} 当前经验: ${data.exp}/${data.maxExp}")
                    }
                    println("${ms}ms")
                }
                "view" -> {
                    val player = sender as? Player ?: return@cmd
                    val item = player.inventory.itemInMainHand
                    if (item.type == Material.AIR) return@cmd
                    val data = SkillManager.getItemSkill(item)
                    if (data == null) {
                        player.msg("手上的物品没有数据")
                        return@cmd
                    }
                    player.msg("物品uuid: ${data.itemUID}")
                    player.msg("主人uuid: ${data.ownerUID}")
                    player.msg("主人名字: ${data.ownerName}")
                    player.msg("阶段: ${data.stage}")
                    player.msg("等级: ${data.level}")
                    player.msg("经验: ${data.exp}/${data.maxExp}")
                }
                "reload" -> {
                    ConfigManager.load()
                    sender.successMsg("重载成功")
                }
            }
        }
        return true
    }

}