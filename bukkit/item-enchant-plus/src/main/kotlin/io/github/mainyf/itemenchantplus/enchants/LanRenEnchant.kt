package io.github.mainyf.itemenchantplus.enchants

import com.ticxo.modelengine.api.ModelEngineAPI
import com.ticxo.modelengine.api.model.ModeledEntity
import io.github.mainyf.itemenchantplus.ItemEnchantPlus
import io.github.mainyf.newmclib.exts.submitTask
import io.github.mainyf.newmclib.exts.toReflect
import io.github.mainyf.newmclib.exts.uuid
import net.minecraft.world.entity.EntitySize
import net.minecraft.world.phys.AxisAlignedBB
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftEntity
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.Listener

object LanRenEnchant : Listener {

    private val entities = mutableSetOf<ModeledEntity>()

    fun launchBullet(player: Player) {
        val startLocation = player.location.add(0.0, 0.25, 0.0)
        var currentLocation = startLocation.clone()
        val entity =
            player.world.spawnEntity(currentLocation, EntityType.ARMOR_STAND) as ArmorStand
        applyBulletOptions(entity)
        val velocity = player.location.direction.multiply(1.25f)
        entity.velocity = velocity
        addModelToEntity(entity)
        var entityTickLive = 200
        entity.isCollidable = false
        ItemEnchantPlus.INSTANCE.submitTask(period = 1L) {
            if (startLocation.distanceSquared(currentLocation) >= 200) {
                removeBullet(entity)
            } else if (entityTickLive <= 0) {
                removeBullet(entity)
            }
            if (entity.isDead) {
                cancel()
                return@submitTask
            }
            val loc = currentLocation.clone().add(velocity).add(0.0, 0.05, 0.0)
            entity.teleport(loc)
            currentLocation = loc
            entityTickLive--
        }
    }

    private fun addModelToEntity(entity: ArmorStand) {
        val blueprint = ModelEngineAPI.getBlueprint("soul_skill_sword_1")
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
        entities.add(modeledEntity)
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

        val width = 0.0
        val height = 0.0
        val ent = (entity as CraftEntity).handle
        val bb = AxisAlignedBB(
            ent.df() - width / 2.0,
            ent.dh(),
            ent.dl() - width / 2.0,
            ent.df() + width / 2.0,
            ent.dh() + height,
            ent.dl() + width / 2.0
        )
        ent.a(bb)
        val field = net.minecraft.world.entity.Entity::class.java.declaredFields.find { it.name == "aZ" }!!
        field.isAccessible = true
        field.set(ent, EntitySize(width.toFloat(), height.toFloat(), true))
        ent.toReflect().set("ba", (height * 0.8).toFloat())
    }

    private fun removeBullet(entity: Entity) {
        val modeledEntity = ModelEngineAPI.getModeledEntity(entity.uuid)
        modeledEntity?.destroy()
        entity.remove()
    }

}