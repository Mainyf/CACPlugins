package io.github.mainyf.csdungeon.config

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
    val mobTypes: List<EntityType>,
    val locs: List<Vector>,
    val locationSpacing: Int
)