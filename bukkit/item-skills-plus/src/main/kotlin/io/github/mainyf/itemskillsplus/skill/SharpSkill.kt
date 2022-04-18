package io.github.mainyf.itemskillsplus.skill

import io.github.mainyf.itemskillsplus.ItemSkillData
import io.github.mainyf.itemskillsplus.SkillManager
import io.github.mainyf.itemskillsplus.config.ConfigManager
import io.github.mainyf.newmclib.exts.msg
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Damageable
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*

object SharpSkill : Listener {

    private val recursiveFixer = mutableSetOf<UUID>()
    private val damageRatio = mutableMapOf<UUID, Double>()

    fun init() {

    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (!ConfigManager.sharpEnable) return
        if (event.action != Action.LEFT_CLICK_AIR && event.action != Action.LEFT_CLICK_BLOCK) {
            return
        }
        val p = event.player
        val loc = p.eyeLocation.clone().add(p.location.direction)
        val item = event.item ?: return
        val data = SkillManager.getItemSkill(SkillManager.sharpDataKey, item)
        if (data != null) {
            if (data.stage >= 1) {
                p.spawnParticle(Particle.SWEEP_ATTACK, loc, 0)
            }
            if (p.attackCooldown >= 1f && data.stage >= 3) {
                sendSkill(p, 1 * getDamageRatio(p, data))
            }
        }
    }

    private fun sendSkill(player: Player, damage: Double, filter: (Entity) -> Boolean = { true }) {
        val loc = player.location.add(player.location.direction.multiply(3f))
        player.world.entities.forEach {
            if (it is Damageable && filter(it) && it.location.distanceSquared(loc) <= 3 * 3) {
                it.damage(damage, player)
            }
        }
    }

    @EventHandler
    fun onDead(event: EntityDeathEvent) {
        if (!ConfigManager.sharpEnable) return
        val ede = event.entity.lastDamageCause as? EntityDamageByEntityEvent ?: return
        val damager = ede.damager as? Player ?: return
        val item = damager.inventory.itemInMainHand
        if (item.type == Material.AIR) return
        val data = SkillManager.getItemSkill(SkillManager.sharpDataKey, item) ?: return

        val exp = ConfigManager.getEntityExp(event.entity)
        if (exp > 0.0) {
            SkillManager.addExpToItem(data, exp)
            SkillManager.updateItemMeta(item, SkillManager.sharpDataKey, data)
            SkillManager.triggerItemSkinEffect(
                damager,
                SkillManager.sharpDataKey,
                data,
                ConfigManager.EffectTriggerType.KILL
            )
            damager.msg("你杀死了 ${event.entity.type.name} 获得经验 $exp, 阶段: ${data.stage} 等级: ${data.level} 当前经验: ${data.exp}/${data.maxExp}")
        }
    }

    @EventHandler
    fun onDamage(event: EntityDamageByEntityEvent) {
        if (!ConfigManager.sharpEnable) return
        val damager = event.damager
        if (damager !is Player) return

        val item = damager.inventory.itemInMainHand
        if (item.type == Material.AIR) return

        val enchantments = item.enchantments
        if (enchantments.containsKey(Enchantment.DAMAGE_UNDEAD) || enchantments.containsKey(Enchantment.DAMAGE_ARTHROPODS)) {
            return
        }

        val data = SkillManager.getItemSkill(SkillManager.sharpDataKey, item) ?: return

        if (hasRecursive(damager)) {
            damager.msg("递归")
            return
        }

        if (data.stage >= 1) {
            (event.entity as? LivingEntity)?.addPotionEffect(PotionEffect(PotionEffectType.SLOW, 2 * 20, 1))
        }

        if (damager.attackCooldown >= 1f && data.stage >= 2) {
            var r = damageRatio.getOrDefault(damager.uniqueId, 0.0)
            if (r < 0.5) {
                r += 0.05
                damageRatio[damager.uniqueId] = r
            }
            damager.msg("满蓄力攻击触发, 当前蓄力攻击倍率: ${r}%")
        }

        val ratio = getDamageRatio(damager, data)
        damager.msg("当前总攻击倍率: ${ratio}%")
        val damage = event.damage * ratio

        if (damager.attackCooldown >= 1f && data.stage >= 3 && event.cause != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
            markRecursive(damager)
            sendSkill(damager, damage) { event.entity.uniqueId != it.uniqueId }
            damager.msg("发射剑气")
        }
        event.setDamage(EntityDamageEvent.DamageModifier.BASE, damage)
        unMarkRecursive(damager)
    }

    private fun getDamageRatio(player: Player, data: ItemSkillData): Double {
        var ratio = 1.0 + ConfigManager.getSharpDamage(data.stage)
        if (damageRatio.containsKey(player.uniqueId)) {
            ratio += damageRatio[player.uniqueId]!!
        }
        return ratio
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

}