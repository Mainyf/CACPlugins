package io.github.mainyf.itemmanager

import dev.jorel.commandapi.arguments.IntegerArgument
import dev.lone.itemsadder.api.CustomStack
import io.github.mainyf.newmclib.command.apiCommand
import io.github.mainyf.newmclib.command.playerArguments
import io.github.mainyf.newmclib.exts.isEmpty
import io.github.mainyf.newmclib.exts.msg
import io.github.mainyf.newmclib.exts.pluginManager
import io.github.mainyf.newmclib.exts.successMsg
import org.apache.logging.log4j.LogManager
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.AnvilInventory
import org.bukkit.inventory.meta.Damageable
import org.bukkit.plugin.java.JavaPlugin

class ItemManager : JavaPlugin(), Listener {

    companion object {

        val LOGGER = LogManager.getLogger("ItemManager")

        lateinit var INSTANCE: ItemManager

    }

    override fun onEnable() {
        INSTANCE = this
        ConfigIM.load()
        IaItemAutoUpdate.init()
        pluginManager().registerEvents(IaItemAutoUpdate, this)
        pluginManager().registerEvents(this, this)
        apiCommand("item-manager") {
            withAliases("itemm", "im")
            "reload" {
                executeOP {
                    ConfigIM.load()
                    sender.successMsg("[ItemManager] 配置重载成功")
                }
            }
            "repair" {
                withArguments(playerArguments("玩家名"))
                executeOP {
                    val player = player()
                    val item = player.inventory.itemInMainHand
                    if (item.isEmpty()) return@executeOP
                    if (!ConfigIM.hasAllowRepair(item)) {
                        ConfigIM.notAllowedRepairMsg?.execute(player)
                        return@executeOP
                    }
                    item.editMeta {
                        if (it is Damageable) {
                            it.damage = 0
                        }
                    }
                    ConfigIM.repairSuccess?.execute(player)
                    player.updateInventory()
                }
            }
            "repair-all" {
                withArguments(playerArguments("玩家名"))
                executeOP {
                    val player = player()
                    player.inventory.forEach { item ->
                        if (item.isEmpty()) return@forEach
                        if (!ConfigIM.hasAllowRepair(item!!)) {
                            return@forEach
                        }
                        item.editMeta {
                            if (it is Damageable) {
                                it.damage = 0
                            }
                        }
                    }
//                    listOf(
                    //                        player.inventory.itemInMainHand,
                    //                        *player.inventory.armorContents
                    //                    ).forEach { item ->
                    //                        if (item.isEmpty()) return@forEach
                    //                        if (!ConfigIM.hasAllowRepair(item!!)) {
                    //                            return@forEach
                    //                        }
                    //                        item.editMeta {
                    //                            if (it is Damageable) {
                    //                                it.damage = 0
                    //                            }
                    //                        }
                    //                    }
                    ConfigIM.repairAllSuccess?.execute(player)
                    player.updateInventory()
                }
            }
            "durability" {
                withArguments(playerArguments("玩家名"), IntegerArgument("耐久"))
                executeOP {
                    val player = player()
                    val durability = int()
                    val itemStack = player.inventory.itemInMainHand
                    if(itemStack.isEmpty()) return@executeOP
                    val cStack = CustomStack.byItemStack(itemStack) ?: return@executeOP
                    cStack.durability = durability
                    sender.successMsg("设置成功")
                }
            }

            "view-durability" {
                withArguments(playerArguments("玩家名"))
                executeOP {
                    val player = player()
                    val itemStack = player.inventory.itemInMainHand
                    if(itemStack.isEmpty()) return@executeOP
                    val cStack = CustomStack.byItemStack(itemStack) ?: return@executeOP
                    CustomStack.getInstance("curse:agate_axe")
                    sender.msg("${cStack.durability}/${cStack.maxDurability}")
                }
            }
        }
    }

    @EventHandler
    fun onInvClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val inv = event.clickedInventory ?: return
        val item = event.currentItem
        if(inv is AnvilInventory && !item.isEmpty() && event.slotType == InventoryType.SlotType.RESULT) {
            val renameText = inv.renameText
            if(!renameText.isNullOrBlank()) {
                if(!ConfigIM.hasAllowRename(item!!)) {
                    ConfigIM.notAllowedRenameMsg?.execute(player)
                    event.isCancelled = true
                }
            }
        }
    }

}