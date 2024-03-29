package io.github.mainyf.csdungeon

import io.github.mainyf.newmclib.exts.serialize
import io.lumine.mythic.core.mobs.ActiveMob
import org.bukkit.entity.LivingEntity

class MobWrapper(
    val entity: LivingEntity?,
    val mmMob: ActiveMob?
) {

    val bukkitEntity = entity ?: mmMob!!.entity.bukkitEntity

    val isDead: Boolean
        get() {
            return entity?.isDead ?: mmMob!!.isDead
        }

    val location get() = bukkitEntity.location

    val customName get() = bukkitEntity.customName()?.serialize() ?: bukkitEntity.type.name

    fun setDead() {
        if(entity != null) {
            entity.health = 0.0
        } else {
            mmMob!!.setDead()
        }
    }

}