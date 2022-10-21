package io.github.mainyf.csdungeon.config

import io.github.mainyf.csdungeon.CsDungeon
import io.lumine.mythic.api.MythicProvider
import io.lumine.mythic.bukkit.BukkitAdapter
import io.lumine.mythic.core.mobs.ActiveMob
import org.apache.commons.lang3.EnumUtils
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.util.Vector

class DungeonConfig(
    val dungeonName: String,
    val structureName: String,
    val protectBuild: Boolean,
    val mobs: List<DungeonMobConfig>
)

class DungeonMobConfig(
    val total: Int,
    val spawnPeriod: Long,
    val max: Int,
    val mobTypes: List<MobType>,
    val locs: List<Vector>,
    val locationSpacing: Int
)

class MobType(val mobName: String) {

    val entityType = EnumUtils.getEnum(EntityType::class.java, mobName.uppercase())

    fun spawnMob(loc: Location): MobWrapper? {
        val mob = MythicProvider.get().mobManager.getMythicMob(mobName)
        return if (mob.isPresent) {
            MobWrapper(null, mob.get().spawn(BukkitAdapter.adapt(loc), 1.0))
        } else if (entityType != null) {
            MobWrapper(loc.world.spawnEntity(loc, entityType), null)
        } else {
            CsDungeon.LOGGER.warn("$mobName 既不是一个MM怪物也不是一个原版怪物，请检查配置")
            null
        }
    }

}

class MobWrapper(
    val entity: Entity?,
    val mmMob: ActiveMob?
) {

}