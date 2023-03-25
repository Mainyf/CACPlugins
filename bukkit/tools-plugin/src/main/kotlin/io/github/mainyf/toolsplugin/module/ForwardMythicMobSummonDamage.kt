package io.github.mainyf.toolsplugin.module

import io.github.mainyf.newmclib.exts.asPlayer
import io.github.mainyf.newmclib.exts.uuid
import io.github.mainyf.toolsplugin.config.ConfigTP
import io.lumine.mythic.api.MythicProvider
import io.lumine.mythic.core.mobs.MobExecutor
import me.rerere.matrix.api.HackType
import me.rerere.matrix.api.MatrixAPIProvider
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import kotlin.jvm.optionals.getOrNull

object ForwardMythicMobSummonDamage : Listener {

    fun init() {

    }

    @OptIn(ExperimentalStdlibApi::class)
    @EventHandler(priority = EventPriority.LOWEST)
    fun onDamage(event: EntityDamageByEntityEvent) {
        if (!ConfigTP.forwardMythicMobSummonDamage) return
        val damager = event.damager
        val mobExecutor = MythicProvider.get().mobManager as MobExecutor
        val activeMob = mobExecutor.getActiveMob(damager.uuid).getOrNull() ?: return
        if (!activeMob.owner.isPresent) return
        val owner = activeMob.owner.get()
        if (owner != null && activeMob.displayName == "ownerSummon") {
            val player = owner.asPlayer() ?: return
            MatrixAPIProvider.getAPI().tempBypass(player, HackType.HITBOX, 1000L)
            event.isCancelled = true
            player.attack(event.entity)
        }
    }

}