package io.github.mainyf.myislands.config

import org.bukkit.entity.Animals
import org.bukkit.entity.Entity
import org.bukkit.entity.Monster
import org.bukkit.entity.Slime
import org.bukkit.entity.Villager

enum class CreatureType(val text: String) {
    ANIMALS("动物"),
    MONSTER("怪物"),
    VILLAGER("村民");

    companion object {

        fun from(entity: Entity): CreatureType? {
            return when (entity) {
                is Animals -> ANIMALS
                is Monster, is Slime -> MONSTER
                is Villager -> VILLAGER
                else -> null
            }
        }

    }

}