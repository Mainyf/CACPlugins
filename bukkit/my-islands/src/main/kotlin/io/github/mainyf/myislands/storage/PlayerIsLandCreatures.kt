package io.github.mainyf.myislands.storage

import io.github.mainyf.myislands.config.CreatureType
import io.github.mainyf.newmclib.storage.BaseEntity
import io.github.mainyf.newmclib.storage.BaseTable
import org.bukkit.entity.Animals
import org.bukkit.entity.Entity
import org.bukkit.entity.Monster
import org.bukkit.entity.Villager
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.*

object PlayerIsLandCreatures : BaseTable("t_PlayerIsLandCreatures", true) {

    val island = reference("island", PlayerIslands)

    val creatureType = enumerationByName<CreatureType>("creature_type", 255)

    val count = integer("count")

}

class PlayerIsLandCreature(uuid: EntityID<UUID>) : BaseEntity(PlayerIsLandCreatures, uuid) {

    companion object : UUIDEntityClass<PlayerIsLandCreature>(PlayerIsLandCreatures)

    var island by PlayerIsLandCreatures.island

    var creatureType by PlayerIsLandCreatures.creatureType

    var count by PlayerIsLandCreatures.count

}