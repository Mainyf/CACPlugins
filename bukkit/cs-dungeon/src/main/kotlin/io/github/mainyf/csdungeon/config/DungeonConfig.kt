package io.github.mainyf.csdungeon.config

import io.github.mainyf.csdungeon.CsDungeon
import io.github.mainyf.csdungeon.MobWrapper
import io.github.mainyf.newmclib.config.action.MultiAction
import io.github.mainyf.newmclib.config.play.MultiPlay
import io.github.mainyf.worldsettings.config.WorldSettingConfig
import io.lumine.mythic.api.MythicProvider
import io.lumine.mythic.bukkit.BukkitAdapter
import io.lumine.mythic.core.mobs.ActiveMob
import org.apache.commons.lang3.EnumUtils
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.util.Vector

class DungeonConfig(
    val worldName: String,
    val dungeonName: String,
    val structureName: String,
    val protectBuild: Boolean,
    val boundaryDamage: Int,
    val noPlayerEnd: Boolean,
    val noFly: Boolean,
    val dungeonMaterials: List<DungeonMaterials>,
    val menuItemInfo: List<MenuItemInfo>,
    val tipPeriod: Long,
    val tipActions: MultiAction,
    val startActions: MultiAction,
    val startPlays: MultiPlay,
    val endActions: MultiAction,
    val endPlays: MultiPlay,
    val wsConfig: WorldSettingConfig,
    val levels: List<DungeonLevelConfig>
)

class DungeonMaterials(
    val item: List<DungeonMaterialItem>,
    val money: Double,
    val level: Int,
    val exp: Int
)

class DungeonMaterialItem(
    val iaName: String,
    val amount: Int,
    val displayName: String
)

class MenuItemInfo(
    val menuName: String,
    val menuLore: List<String>
)

class DungeonLevelConfig(
    val level: Int,
    val totalMob: Int,
    val mobSpawns: List<DungeonMobConfig>
)

class DungeonMobConfig(
    val loc: String,
    val spawnPeriod: Long,
    val max: Int,
    val mobTypes: List<MobType>,
    val locationSpacing: Int
)

class MobType(val mobName: String) {

    val entityType = EnumUtils.getEnum(EntityType::class.java, mobName.uppercase())

    fun spawnMob(loc: Location): MobWrapper? {
        val mob = MythicProvider.get().mobManager.getMythicMob(mobName)
        return if (entityType != null) {
            MobWrapper(loc.world.spawnEntity(loc, entityType) as LivingEntity, null)
        } else if (mob.isPresent) {
            MobWrapper(null, mob.get().spawn(BukkitAdapter.adapt(loc), 1.0))
        } else {
            CsDungeon.LOGGER.warn("$mobName 既不是一个原版怪物也不是一个MM怪物，请检查配置")
            null
        }
    }

}
