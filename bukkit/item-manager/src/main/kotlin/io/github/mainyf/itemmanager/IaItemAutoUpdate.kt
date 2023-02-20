package io.github.mainyf.itemmanager

import dev.lone.itemsadder.api.CustomStack
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.core.utils.MythicUtil
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractEvent

object IaItemAutoUpdate : Listener {

    fun init() {

    }

    @EventHandler
    fun onAttack(event: EntityDamageByEntityEvent) {
        val damager = event.damager as? Player ?: return
        val item = damager.inventory.itemInMainHand
        val iaStack = CustomStack.byItemStack(item) ?: return
        val namespacedID = iaStack.namespacedID
        if (!ConfigIM.iaItemAutoUpdateItems.containsKey(namespacedID)) return
        val iaItem = ConfigIM.iaItemAutoUpdateItems[namespacedID]!!
        val iaOriginalStack = CustomStack.getInstance(namespacedID) ?: return
        val originalStack = iaOriginalStack.itemStack
        val oldMeta = item.itemMeta
        if (!originalStack.itemMeta.equals(oldMeta)) {
            item.itemMeta = originalStack.itemMeta
            damager.inventory.setItemInMainHand(item)
        }
        val skillName = iaItem.triggers[ConfigIM.ItemTrigger.ATTACK]
        if (skillName != null) {
            MythicBukkit.inst().apiHelper.castSkill(
                damager,
                skillName,
                damager,
                damager.location,
                listOf(MythicUtil.getTargetedEntity(damager)),
                null,
                1.0f
            )
        }
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        println(event.action)
    }


}