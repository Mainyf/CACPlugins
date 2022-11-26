package io.github.mainyf.itemenchantplus.config

import io.github.mainyf.newmclib.exts.uuid
import io.lumine.mythic.bukkit.MythicBukkit
import org.apache.commons.lang3.EnumUtils
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType

class EnchantEntity(val mobName: String) {

    val entityType = EnumUtils.getEnum(EntityType::class.java, mobName.uppercase())

    fun equalsEntity(entity: Entity): Boolean {
        val mobManager = MythicBukkit.inst().mobManager
        return if (mobManager.isActiveMob(entity.uuid)) {
            val activeMob = mobManager.getMythicMobInstance(entity)
            activeMob.mobType == mobName
        } else {
            entity.type == entityType
        }
    }

}
