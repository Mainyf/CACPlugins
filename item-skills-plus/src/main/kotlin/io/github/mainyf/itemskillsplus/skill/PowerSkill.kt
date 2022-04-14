package io.github.mainyf.itemskillsplus.skill

import io.github.mainyf.itemskillsplus.SkillManager
import io.github.mainyf.itemskillsplus.config.ConfigManager
import io.github.mainyf.newmclib.exts.msg
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*

object PowerSkill : Listener {

    private val projDamage = mutableMapOf<UUID, Double>()

    fun init() {

    }

    @EventHandler
    fun onProjLaunch(event: ProjectileLaunchEvent) {
        if (!ConfigManager.powerEnable) return
        val proj = event.entity
        val shooter = proj.shooter
        if (shooter !is Player) return
        val item = shooter.inventory.itemInMainHand
        if (item.type == Material.AIR) return
        val data = SkillManager.getItemSkill(SkillManager.powerDataKey, item) ?: return
        if (data.stage >= 1) {
            shooter.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 2 * 20, 1))
        }
        if (data.stage >= 2) {
            proj.velocity = proj.velocity.multiply(ConfigManager.powerSpeed)
        }
        projDamage[proj.uniqueId] = 1.0 + ConfigManager.getPowerDamage(data.stage)
    }

    @EventHandler
    fun onDead(event: EntityDeathEvent) {
        if (!ConfigManager.powerEnable) return
        val ede = event.entity.lastDamageCause as? EntityDamageByEntityEvent ?: return
        val proj = ede.damager as? Projectile ?: return
        val damager = proj.shooter as? Player ?: return
        val item = damager.inventory.itemInMainHand
        if (item.type == Material.AIR) return
        val data = SkillManager.getItemSkill(SkillManager.sharpDataKey, item) ?: return

        val exp = ConfigManager.getEntityExp(event.entity)
        if (exp > 0.0) {
            SkillManager.addExpToItem(data, exp)
            SkillManager.updateItemMeta(item, SkillManager.sharpDataKey, data)
            SkillManager.triggerItemSkinEffect(
                damager,
                SkillManager.powerDataKey,
                data,
                ConfigManager.EffectTriggerType.KILL
            )
            damager.msg("你杀死了 ${event.entity.type.name} 获得经验 $exp, 阶段: ${data.stage} 等级: ${data.level} 当前经验: ${data.exp}/${data.maxExp}")
        }

    }

    @EventHandler
    fun onDamage(event: EntityDamageByEntityEvent) {
        if (!ConfigManager.powerEnable) return
        val proj = event.damager as? Projectile ?: return
        val damager = proj.shooter
        if (damager !is Player) return
        val item = damager.inventory.itemInMainHand
        if (item.type == Material.AIR) return
        val data = SkillManager.getItemSkill(SkillManager.powerDataKey, item) ?: return
        val damage = event.damage

        var ro = if (projDamage.containsKey(proj.uniqueId)) {
            val ro = projDamage.remove(proj.uniqueId)!!
            ro
        } else 1.0

        val min = 10
        val max = 30
        if (data.stage >= 3) {
            val dis = damager.location.distance(event.entity.location)
            damager.msg("距离为: $dis")
            if (dis > min) {
                val r = if (dis >= max) {
                    1.0
                } else {
                    (dis - min) / (max - min)
                }
                damager.msg("距离加成倍率为: ${(r * 100).toInt()}%")
                ro += r
            }

        }
        damager.msg("总倍率为: ${(ro * 100).toInt()}%")
        event.setDamage(EntityDamageEvent.DamageModifier.BASE, damage * ro)
    }

    fun Double.clamp(min: Double, max: Double): Double {
        return if (this < min) {
            min
        } else {
            if (this > max) max else this
        }
    }

}