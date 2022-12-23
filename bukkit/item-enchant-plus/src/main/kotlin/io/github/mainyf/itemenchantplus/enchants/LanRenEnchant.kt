package io.github.mainyf.itemenchantplus.enchants

import com.ticxo.modelengine.api.ModelEngineAPI
import com.ticxo.modelengine.api.model.ModeledEntity
import io.github.mainyf.itemenchantplus.EnchantData
import io.github.mainyf.itemenchantplus.EnchantManager
import io.github.mainyf.itemenchantplus.ItemEnchantPlus
import io.github.mainyf.itemenchantplus.config.*
import io.github.mainyf.newmclib.config.action.MultiAction
import io.github.mainyf.newmclib.config.play.MultiPlay
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.nms.asNmsEntity
import io.github.mainyf.newmclib.nms.getDamageBonus
import io.github.mainyf.soulbind.SBManager
import io.github.mainyf.worldsettings.config.ConfigWS
import io.lumine.mythic.api.MythicProvider
import io.lumine.mythic.core.mobs.MobExecutor
import me.rerere.matrix.api.HackType
import me.rerere.matrix.api.MatrixAPIProvider
import org.apache.commons.lang3.RandomStringUtils
import org.bukkit.Location
import org.bukkit.attribute.Attribute
import org.bukkit.block.Block
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.potion.PotionEffect
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.BoundingBox
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

object LanRenEnchant : Listener {

    private val playerCombo = mutableMapOf<UUID, Pair<Int, Long>>()
    private val bullets = mutableListOf<Bullet>()
    private val hitEntities = mutableListOf<Entity>()
    private val stageToMaxComboCount = mapOf(
        1 to 2,
        2 to 3,
        3 to 4
    )

    //    fun launchBullet(player: Player) {
    //        Bullet(player, null).launch()
    //    }

    fun clean() {
        bullets.forEach {
            it.terminate()
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onArmorStandBreak(event: EntityDamageByEntityEvent) {
        if (event.entity is ArmorStand) {
            if (bullets.any { it ->
                    it.equalsEntity(event.entity)
                }) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (!ConfigIEP.lanrenEnchantConfig.enable) return
        if (event.action != Action.LEFT_CLICK_AIR && event.action != Action.LEFT_CLICK_BLOCK) {
            return
        }
        if (handleSkill(event.player)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onDamage(event: EntityDamageByEntityEvent) {
        val damager = event.damager as? Player ?: return
        val victims = event.entity
        if (hitEntities.contains(victims)) {
            return
        }
        if (handleSkill(damager)) {
            event.isCancelled = true
            return
        }
        damager.msg("释放失败")
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

    private fun handleSkill(player: Player): Boolean {
        val item = player.inventory.itemInMainHand
        if (item.isEmpty()) return false
        val bindData = SBManager.getBindItemData(item)
        if (!player.isOp && bindData != null && bindData.ownerUUID != player.uuid) {
            return false
        }

        EnchantManager.updateItemMeta(item)
        val data = EnchantManager.getItemEnchant(ItemEnchantType.LAN_REN, item) ?: return false

        if (data.stage < 1) return false
        val config = data.enchantType.enchantConfig() as LanRenEnchantConfig
        if (player.attackCooldown >= 1.0f) {
            player.msg("发出了剑气 ${RandomStringUtils.randomAlphabetic(4)}")
            val currentTime = currentTime()
            var combo = playerCombo[player.uuid] ?: (1 to currentTime)
            var comboCount = combo.first
            val eTime = currentTime - combo.second
            val caMillis = config.comboAttenuation * 50
            if (eTime >= caMillis) {
                comboCount = 1
            }
            val skinData = data.enchantSkin.skinConfig.data as? LanRenEnchantSkinData ?: return false

            handleComboSkill(
                comboCount,
                player,
                item,
                skinData,
                config,
                data
            )
            val maxComboCount = stageToMaxComboCount[data.stage]!!
            combo = if (comboCount >= maxComboCount) {
                1 to currentTime
            } else {
                (comboCount + 1) to currentTime
            }
            playerCombo[player.uuid] = combo
            return true
        }
        return false
    }

    private fun handleComboSkill(
        comboCount: Int,
        player: Player,
        item: ItemStack,
        skinData: LanRenEnchantSkinData,
        config: LanRenEnchantConfig,
        data: EnchantData
    ) {
        MatrixAPIProvider.getAPI().tempBypass(player, HackType.MOVE, config.cheatBypassMove * 50L)
        MatrixAPIProvider.getAPI().tempBypass(player, HackType.HITBOX, config.cheatBypassHitBox * 50L)
        val meta = item.itemMeta
        var durabilityLoss = 1
        when (comboCount) {
            1 -> {
                durabilityLoss = config.combo1_2.itemDurabilityLoss
                launchCombo1_2(player, item, skinData.comboModelDatas[0], config, data)
            }

            2 -> {
                durabilityLoss = config.combo1_2.itemDurabilityLoss
                launchCombo1_2(player, item, skinData.comboModelDatas[1], config, data)
            }

            3 -> {
                durabilityLoss = config.combo3.itemDurabilityLoss
                launchCombo3(player, item, skinData, config, data)
            }

            4 -> {
                durabilityLoss = config.combo4.itemDurabilityLoss
                launchCombo4(player, item, skinData.comboModelDatas[3], config, data)
            }
        }
        val damageable = meta as Damageable
        val durabilityLevel = item.getEnchantmentLevel(Enchantment.DURABILITY)
        if (Random.nextInt(0, 100) < 100 / (durabilityLevel + 1)) {
            damageable.damage += durabilityLoss
        }
        item.itemMeta = meta
    }

    private fun launchCombo1_2(
        player: Player,
        itemStack: ItemStack,
        modelData: LanRenModelData,
        config: LanRenEnchantConfig,
        data: EnchantData
    ) {
        val bullet = Bullet(
            player,
            config.debug,
            itemStack,
            modelData,
            modelData.play,
            config.combo1_2.distance[data.stage - 1],
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
        itemStack: ItemStack,
        skinData: LanRenEnchantSkinData,
        config: LanRenEnchantConfig,
        data: EnchantData
    ) {
        var count = 0
        ItemEnchantPlus.INSTANCE.submitTask(period = config.combo3.speed) {
            if (count >= config.combo3.count) {
                cancel()
                return@submitTask
            }
            val bullet = Bullet(
                player,
                config.debug,
                itemStack,
                skinData.comboModelDatas[count],
                skinData.comboModelDatas[2].play,
                config.combo3.distance,
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
        itemStack: ItemStack,
        modelData: LanRenModelData,
        config: LanRenEnchantConfig,
        data: EnchantData
    ) {
        val bullet = Bullet(
            player,
            config.debug,
            itemStack,
            modelData,
            modelData.play,
            config.combo4.distance,
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

    class Bullet(
        private val shooter: Player,
        private val debug: Boolean,
        private val itemStack: ItemStack,
        private val modelData: LanRenModelData,
        private val play: MultiPlay?,
        private val distance: Double,
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

        fun equalsEntity(entity: Entity): Boolean {
            return modeledEntity?.base?.original == entity
        }

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
            play?.execute(startLocation)
            task = ItemEnchantPlus.INSTANCE.submitTask(period = 1L) {
                if (startLocation.distanceSquared(currentLocation) >= (distance + 1) * (distance + 1)) {
                    terminate()
                    return@submitTask
                } else if (entityTickLive <= 0) {
                    terminate()
                    return@submitTask
                }
                //                if (!isPathable(currentLocation.block)) {
                //                    terminate()
                //                    return@submitTask
                //                }
                if (entity.isDead) {
                    terminate()
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

            val buffFlag = AtomicBoolean(false)
            for (hitEntity in entities) {
                if (hitEntity == bullet) continue
                if (hitEntity == player) continue
                if (hitEntity !is LivingEntity) continue
                if (hitEntity is ArmorStand) continue
                if (damagedEntities.contains(hitEntity)) continue

                damagedEntities.add(hitEntity)

                kotlin.run {
                    if (hitEntity is Player) {
                        val settings = ConfigWS.getSetting(hitEntity.location)
                        if (settings != null && !settings.pvp) {
                            return@run
                        }
                    }
                    attackTarget(mobExecutor, hitEntity, player, buffFlag)
                }
            }
        }

        private fun attackTarget(
            mobExecutor: MobExecutor,
            hitEntity: LivingEntity,
            player: Player,
            buffFlag: AtomicBoolean
        ) {
            val baseDamage =
                (player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)?.value ?: 1.0) + itemStack.getDamageBonus(
                    hitEntity
                )
            val fDamage = if (mobExecutor.isActiveMob(hitEntity.uuid)) {
                baseDamage * throughDamage * pveDamage
            } else {
                baseDamage * throughDamage
            }
            val noDamageTicks = hitEntity.noDamageTicks
            hitEntity.noDamageTicks = 5

            hitEntities.add(hitEntity)
            hitEntity.damage(fDamage, player)
            hitEntities.remove(hitEntity)

            val fireEnchantLevel = itemStack.getEnchantmentLevel(Enchantment.FIRE_ASPECT)
            if (fireEnchantLevel > 0 && hitEntity.fireTicks <= 0) {
                hitEntity.fireTicks = fireEnchantLevel * 4 * 20
            }

            hitEntity.noDamageTicks = noDamageTicks
            if (!buffFlag.get() && shooterBuff != null) {
                buffFlag.set(true)
                shooter.addPotionEffect(shooterBuff)
            }
            if (hitEntityBuff != null) {
                hitEntity.addPotionEffect(hitEntityBuff)
            }

            if (debug) {
                val entityName = hitEntity.customName() ?: hitEntity.name()
                player.sendMessage(
                    "对".toComp().append(entityName).append("造成了 ${fDamage.toInt()} 点伤害".toComp())
                )
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