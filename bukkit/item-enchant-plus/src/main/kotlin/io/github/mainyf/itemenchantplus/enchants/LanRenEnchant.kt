package io.github.mainyf.itemenchantplus.enchants

import com.ticxo.modelengine.api.ModelEngineAPI
import com.ticxo.modelengine.api.model.ModeledEntity
import io.github.mainyf.itemenchantplus.EnchantData
import io.github.mainyf.itemenchantplus.EnchantManager
import io.github.mainyf.itemenchantplus.ItemEnchantPlus
import io.github.mainyf.itemenchantplus.config.*
import io.github.mainyf.newmclib.exts.currentTime
import io.github.mainyf.newmclib.exts.isEmpty
import io.github.mainyf.newmclib.exts.submitTask
import io.github.mainyf.newmclib.exts.uuid
import io.github.mainyf.newmclib.nms.asNmsEntity
import io.lumine.mythic.api.MythicProvider
import io.lumine.mythic.core.mobs.MobExecutor
import io.lumine.mythic.core.skills.projectiles.Projectile
import io.lumine.mythic.core.skills.projectiles.ProjectileSurfaceMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.block.Block
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.BoundingBox
import java.util.*
import kotlin.math.max

object LanRenEnchant : Listener {

    private val playerCombo = mutableMapOf<UUID, Pair<Int, Long>>()
    private val bullets = mutableListOf<Bullet>()

    //    fun launchBullet(player: Player) {
    //        Bullet(player, null).launch()
    //    }

    fun clean() {
        bullets.forEach {
            it.terminate()
        }
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (!ConfigIEP.lanrenEnchantConfig.enable) return
        if (event.action != Action.LEFT_CLICK_AIR && event.action != Action.LEFT_CLICK_BLOCK) {
            return
        }
        val player = event.player
        val item = player.inventory.itemInMainHand
        if (item.isEmpty()) return
        val data = EnchantManager.getItemEnchant(ItemEnchantType.LAN_REN, item) ?: return
        if (data.stage < 1) return
        val config = data.enchantType.enchantConfig() as LanRenEnchantConfig
        val damage = item.itemMeta.attributeModifiers?.get(Attribute.GENERIC_ATTACK_DAMAGE)?.sumOf { it.amount }
            ?: config.combo1_2.baseDamage[data.stage - 1]
        if (player.attackCooldown >= 1.0f) {
            val currentTime = currentTime()
            var combo = playerCombo[player.uuid] ?: (1 to currentTime)
            var comboCount = combo.first
            val eTime = currentTime - combo.second
            val caMillis = config.comboAttenuation * 50
            if (eTime >= caMillis) {
                comboCount = 1
            }
            val skinData = data.enchantSkin.skinConfig.data as? LanRenEnchantSkinData ?: return

            if (data.stage >= 1) {
                if (comboCount == 2) {
                    launchCombo1_2(player, damage, skinData.comboModelDatas[1], config, data)
                } else {
                    launchCombo1_2(player, damage, skinData.comboModelDatas[0], config, data)
                }
            }
            if (data.stage >= 2 && comboCount == 3) {
                launchCombo3(player, damage, skinData, config, data)
            }
            if (data.stage >= 3 && comboCount == 4) {
                launchCombo4(player, damage, skinData.comboModelDatas[3], config, data)
            }
            combo = if (comboCount >= 4) {
                1 to currentTime
            } else {
                (comboCount + 1) to currentTime
            }
            playerCombo[player.uuid] = combo
            event.isCancelled = true
        }
    }

    private fun launchCombo1_2(
        player: Player,
        damage: Double,
        modelData: LanRenModelData,
        config: LanRenEnchantConfig,
        data: EnchantData
    ) {
        val bullet = Bullet(
            player,
            modelData,
            config.combo1_2.distance[data.stage - 1],
            damage,
            config.combo1_2.size,
            config.combo1_2.throughDamage[data.stage - 1],
            config.combo1_2.pveDamage[data.stage - 1],
            null,
            config.combo1_2.hitTargetShooterBuff[data.stage - 1]
        )
        bullets.add(bullet)
        bullet.launch()
    }

    private fun launchCombo3(
        player: Player,
        damage: Double,
        skinData: LanRenEnchantSkinData,
        config: LanRenEnchantConfig,
        data: EnchantData
    ) {
        var count = 0
        ItemEnchantPlus.INSTANCE.submitTask(period = 5L) {
            if (count >= config.combo3.count) {
                cancel()
                return@submitTask
            }
            val bullet = Bullet(
                player,
                skinData.comboModelDatas[count],
                config.combo3.distance,
                damage,
                config.combo3.size,
                config.combo3.throughDamage[data.stage - 2],
                config.combo1_2.pveDamage[data.stage - 1],
                config.combo3.hitTargetBuff,
                config.combo1_2.hitTargetShooterBuff[data.stage - 1]
            )
            bullets.add(bullet)
            bullet.launch()
            count++
        }
    }

    private fun launchCombo4(
        player: Player,
        damage: Double,
        modelData: LanRenModelData,
        config: LanRenEnchantConfig,
        data: EnchantData
    ) {
        val bullet = Bullet(
            player,
            modelData,
            config.combo4.distance,
            damage,
            config.combo4.size,
            config.combo4.throughDamage,
            config.combo1_2.pveDamage[data.stage - 1],
            null,
            config.combo1_2.hitTargetShooterBuff[data.stage - 1]
        )
        bullets.add(bullet)
        bullet.launch()
    }

    @EventHandler
    fun onDead(event: PlayerDeathEvent) {
        playerCombo.remove(event.entity.uuid)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        playerCombo.remove(event.player.uuid)
    }

    @EventHandler
    fun onDamage(event: EntityDamageByEntityEvent) {
        val damager = event.damager as? Player ?: return
        val victims = event.entity
        val item = damager.inventory.itemInMainHand
        if (item.isEmpty()) return
        val data = EnchantManager.getItemEnchant(ItemEnchantType.LAN_REN, item) ?: return
        if (data.stage < 1) return
        val mobExecutor = MythicProvider.get().mobManager as MobExecutor
        val config = data.enchantType.enchantConfig() as LanRenEnchantConfig
        if (mobExecutor.isActiveMob(victims.uuid)) {
            event.damage = event.damage * config.combo1_2.pveDamage[data.stage - 1]
        }
    }

    class Bullet(
        private val shooter: Player,
        private val modelData: LanRenModelData,
        private val distance: Double,
        private val baseDamage: Double,
        private val size: ModelSizeConfig,
        private val throughDamage: Double,
        private val pveDamage: Double,
        private val hitEntityBuff: PotionEffect?,
        private val shooterBuff: PotionEffect?
    ) {

        private val damagedEntities = mutableListOf<Entity>()
        private var modeledEntity: ModeledEntity? = null
        private lateinit var startLocation: Location
        private lateinit var currentLocation: Location
        private var task: BukkitRunnable? = null

        fun launch() {
            startLocation = shooter.location.add(0.0, 0.25, 0.0)
            currentLocation = startLocation.clone()
            val entity =
                shooter.world.spawnEntity(currentLocation, EntityType.ARMOR_STAND) as ArmorStand
            applyBulletOptions(entity)
            val velocity = shooter.location.direction.multiply(1.25f)
            entity.velocity = velocity
            modeledEntity = addModelToEntity(entity)
            var entityTickLive = 200
            entity.isCollidable = false
            handleHit(
                entity,
                currentLocation,
                shooter
            )
            modelData.play?.execute(startLocation)
            task = ItemEnchantPlus.INSTANCE.submitTask(period = 1L) {
                if (startLocation.distanceSquared(currentLocation) >= (distance + 1) * (distance + 1)) {
                    terminate()
                    return@submitTask
                } else if (entityTickLive <= 0) {
                    terminate()
                    return@submitTask
                }
                if (!isPathable(currentLocation.block)) {
                    terminate()
                    return@submitTask
                }
                if (entity.isDead) {
                    cancel()
                    return@submitTask
                }

                val loc = currentLocation.clone().add(velocity).add(0.0, 0.05, 0.0)
                entity.teleport(loc)
                currentLocation = loc

                handleHit(
                    entity,
                    currentLocation,
                    shooter
                )

                entityTickLive--
            }
        }


        private fun isPathable(block: Block): Boolean {
            return if (block.isPassable) {
                true
            } else {
                val bb = BoundingBox.of(
                    currentLocation.clone(),
                    size.width,
                    size.height,
                    size.width
                )
                val overlap = bb.overlaps(block.boundingBox)
                !overlap
            }
        }

        private fun handleHit(bullet: Entity, currentLocation: Location, player: Player) {
            val bbox = BoundingBox.of(
                currentLocation.clone(),
                size.width,
                size.height,
                size.width
            )
            val entities = currentLocation.world.getNearbyEntities(bbox)
            val mobExecutor = MythicProvider.get().mobManager as MobExecutor

            var buffFlag = false
            for (hitEntity in entities) {
                if (hitEntity == bullet) continue
                if (hitEntity == player) continue
                if (hitEntity !is LivingEntity) continue
                if (hitEntity is ArmorStand) continue
                if (damagedEntities.contains(hitEntity)) continue

                damagedEntities.add(hitEntity)

                if (mobExecutor.isActiveMob(hitEntity.uuid)) {
                    hitEntity.damage(baseDamage * pveDamage, player)
                } else {
                    hitEntity.damage(baseDamage * throughDamage, player)
                }
                if (!buffFlag && shooterBuff != null) {
                    buffFlag = true
                    shooter.addPotionEffect(shooterBuff)
                }
                if (hitEntityBuff != null) {
                    hitEntity.addPotionEffect(hitEntityBuff)
                }
            }
        }

        private fun addModelToEntity(entity: ArmorStand): ModeledEntity {
            val blueprint = ModelEngineAPI.getBlueprint(modelData.modelName)
            val modeledEntity = ModelEngineAPI.createModeledEntity(entity)

            modeledEntity.isBaseEntityVisible = false
            modeledEntity.setStepHeight(0.5)
            modeledEntity.mountManager.isCanSteer = false
            modeledEntity.mountManager.isCanRide = false

            val activeModel = ModelEngineAPI.createActiveModel(blueprint)

            activeModel.isCanHurt = true
            activeModel.isLockPitch = false
            activeModel.isLockYaw = false

            modeledEntity.addModel(activeModel, true)

            return modeledEntity
        }

        private fun applyBulletOptions(entity: ArmorStand) {
            entity.ticksLived = Int.MAX_VALUE
            entity.isInvulnerable = true
            entity.setArms(true)
            entity.setGravity(false)
            entity.setAI(true)
            entity.removeWhenFarAway = true
            entity.isVisible = true
            entity.isInvisible = false
            entity.isSmall = false
            entity.isMarker = false
            entity.setBasePlate(true)
            entity.setCanTick(true)
            entity.setCanMove(true)

            entity.asNmsEntity().setBoundingBox(0.0, 0.0)
        }

        fun terminate() {
            task?.cancel()
            modeledEntity?.destroy()
            (modeledEntity?.base?.original as? Entity)?.remove()
        }

    }

}