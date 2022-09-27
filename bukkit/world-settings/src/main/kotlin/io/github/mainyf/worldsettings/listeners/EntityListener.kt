package io.github.mainyf.worldsettings.listeners

import io.github.mainyf.worldsettings.config.ConfigManager
import org.bukkit.entity.EntityType
import org.bukkit.entity.Frog
import org.bukkit.entity.Rabbit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.EntityTransformEvent

object EntityListener : Listener {

    @EventHandler
    fun onEntityTransform(event: EntityTransformEvent) {
        if (event.transformReason == EntityTransformEvent.TransformReason.METAMORPHOSIS) {
            val settings = ConfigManager.getSetting(event.entity.world) ?: return
            if (settings.randomFrogColor) {
                (event.entity as? Frog)?.variant = Frog.Variant.values().toList().random()
            }
        }
    }

    @EventHandler
    fun onSpawn(event: CreatureSpawnEvent) {
        val settings = ConfigManager.getSetting(event.entity.world) ?: return
        if (event.entityType == EntityType.RABBIT) {
            if (settings.randomRabbitColor) {
                (event.entity as? Rabbit)?.rabbitType = Rabbit.Type.values().toList().random()
            }
        }
        if (event.entityType == EntityType.FROG) {
            if (settings.randomFrogColor) {
                (event.entity as? Frog)?.variant = Frog.Variant.values().toList().random()
            }
        }
    }

}