package io.github.mainyf.itemmanager

import dev.lone.itemsadder.api.CustomStack
import io.github.mainyf.newmclib.exts.isEmpty
import io.github.mainyf.newmclib.exts.msg
import io.github.mainyf.newmclib.exts.pluginManager
import io.github.mainyf.newmclib.exts.uuid
import io.lumine.mythic.api.MythicProvider
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.bukkit.events.MythicDamageEvent
import io.lumine.mythic.core.mobs.MobExecutor
import io.lumine.mythic.core.utils.MythicUtil
import org.bukkit.attribute.Attribute
import org.bukkit.command.CommandSender
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerAnimationEvent
import org.bukkit.event.player.PlayerAnimationType
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import java.util.*

object IaItemAutoUpdate : Listener {

    private val recursiveFixer = mutableSetOf<UUID>()

    fun init() {

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onAttack(event: EntityDamageByEntityEvent) {
        val damager = event.damager as? Player ?: return
        val item = damager.inventory.itemInMainHand
        if (recursiveFixer.contains(damager.uuid)) return
        handleIaItem(damager, item, ConfigIM.ItemTriggerType.ATTACK, event) {
            damager.inventory.setItemInMainHand(it)
        }
    }

    @EventHandler
    fun onAnimation(event: PlayerAnimationEvent) {
        if (event.animationType == PlayerAnimationType.ARM_SWING) {
            val player = event.player
            val item = player.inventory.itemInMainHand
            handleIaItem(event.player, item, ConfigIM.ItemTriggerType.LEFT_CLICK, event) {
                player.inventory.setItemInMainHand(it)
            }
        }
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        val player = event.player
        val item = player.inventory.itemInMainHand
        //        if (event.action.isLeftClick) {
        //            handleIaItem(event.player, item, ConfigIM.ItemTriggerType.LEFT_CLICK) {
        //                player.inventory.setItemInMainHand(it)
        //            }
        //        }
        if (event.action.isRightClick) {
            handleIaItem(event.player, item, ConfigIM.ItemTriggerType.RIGHT_CLICK, event) {
                player.inventory.setItemInMainHand(it)
            }
        }
    }

    @EventHandler
    fun onMMDamage(event: MythicDamageEvent) {
        val damager = event.caster.entity.bukkitEntity as? LivingEntity ?: return
        val damagerEquipment = damager.equipment ?: return
        val item = damagerEquipment.itemInMainHand
        if (item.isEmpty()) return
        val iaStack = CustomStack.byItemStack(item) ?: return
        val namespacedID = iaStack.namespacedID
        if (!ConfigIM.iaItemSkillItems.containsKey(namespacedID)) return
        val iaItem = ConfigIM.iaItemSkillItems[namespacedID]!!
        val triggerConfig = iaItem.triggers[ConfigIM.ItemTriggerType.ATTACK]
        if (triggerConfig != null) {
            if (iaItem.pveDamage != 1.0) {
                val mobExecutor = MythicProvider.get().mobManager as MobExecutor
                if (mobExecutor.isActiveMob(event.target.uniqueId)) {
                    event.damage *= iaItem.pveDamage
                }
            }
        }
    }

    private fun handleIaItem(
        player: Player,
        item: ItemStack,
        trigger: ConfigIM.ItemTriggerType,
        event: Cancellable?,
        updateItem: (ItemStack) -> Unit
    ) {
        var useItem = item
        val iaStack = CustomStack.byItemStack(useItem) ?: return
        val namespacedID = iaStack.namespacedID
        if (ConfigIM.iaItemAutoUpdate.contains(namespacedID)) {
            val iaOriginalStack = CustomStack.getInstance(namespacedID)
            if (iaOriginalStack != null) {
                if (iaOriginalStack.durability != iaOriginalStack.maxDurability) {
                    iaOriginalStack.durability = iaOriginalStack.maxDurability
                }
                var originalStack = iaOriginalStack.itemStack
                val beEqualsStack =
                    CustomStack.byItemStack(useItem.clone())!!
                        .apply { this.durability = iaOriginalStack.durability }
                val newMeta = originalStack.itemMeta
                val oldMeta = beEqualsStack.itemStack.itemMeta

                if (
                    newMeta.displayName() != oldMeta.displayName()
                    || newMeta.lore() != oldMeta.lore()
                    || iaStack.maxDurability != iaOriginalStack.maxDurability
                    || newMeta.customModelData != oldMeta.customModelData
                    || originalStack.type != beEqualsStack.itemStack.type
                    || newMeta.attributeModifiers != oldMeta.attributeModifiers
                ) {
                    //                    player.msg("物品更新")
                    originalStack.itemMeta = oldMeta
                    iaOriginalStack.durability = iaStack.durability
                    originalStack = iaOriginalStack.itemStack
                    originalStack.amount = useItem.amount

                    originalStack.editMeta { meta ->
                        meta.displayName(newMeta.displayName())
                        meta.lore(newMeta.lore())
                        val attributeModifiers = newMeta.attributeModifiers
                        if (attributeModifiers != null) {
                            Attribute.values().forEach { attr ->
                                val modifiers = attributeModifiers.get(attr)
                                meta.removeAttributeModifier(attr)
                                modifiers.forEach {
                                    meta.addAttributeModifier(attr, it)
                                }
                            }
                        }
                        meta.setCustomModelData(newMeta.customModelData)
                    }

                    useItem = originalStack
                    updateItem.invoke(originalStack)
                    pluginManager().callEvent(
                        PlayerItemDamageEvent(
                            player,
                            useItem,
                            0,
                            0
                        )
                    )
                }
            }
        }

        if (!ConfigIM.iaItemSkillItems.containsKey(namespacedID)) return
        if (player.attackCooldown < 1.0) return
        val iaItem = ConfigIM.iaItemSkillItems[namespacedID]!!
        val triggerConfig = iaItem.triggers[trigger]
        if (triggerConfig != null) {
            recursiveFixer.add(player.uuid)
            MythicBukkit.inst().apiHelper.castSkill(
                player,
                triggerConfig.skillName,
                player,
                player.location,
                listOf(MythicUtil.getTargetedEntity(player)),
                null,
                1.0f
            )
            recursiveFixer.remove(player.uuid)
            if (iaItem.pveDamage != 1.0 && trigger == ConfigIM.ItemTriggerType.ATTACK && event is EntityDamageByEntityEvent) {
                val mobExecutor = MythicProvider.get().mobManager as MobExecutor
                if (mobExecutor.isActiveMob(event.entity.uuid)) {
                    event.damage *= iaItem.pveDamage
                }
            }
            if (triggerConfig.itemDurabilityDamage > 0) {
                val durabilityDamage = triggerConfig.itemDurabilityDamage
                CustomStack.byItemStack(useItem)!!.apply {
                    if (durability <= 0) {
                        durability = 1
                    }
                }
                pluginManager().callEvent(
                    PlayerItemDamageEvent(
                        player,
                        useItem,
                        durabilityDamage,
                        durabilityDamage
                    )
                )
            }
            if (triggerConfig.cancelEvent && event != null) {
                event.isCancelled = true
            }
        }
    }

    fun debug(sender: CommandSender, msg: String) {
        if (ConfigIM.debug) {
            sender.msg(msg)
        }
    }

}