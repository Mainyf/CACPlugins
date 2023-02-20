package io.github.mainyf.itemenchantplus.enchants

import com.destroystokyo.paper.event.player.PlayerReadyArrowEvent
import io.github.mainyf.newmclib.exts.msg
import io.github.mainyf.newmclib.exts.uuid
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.UUID

object VolleyEnchant : Listener {

    private val readyArrowSet = mutableSetOf<UUID>()

    fun init() {

    }

//    @EventHandler
//    fun onReadyArrow(event: PlayerReadyArrowEvent) {
//        val player = event.player
//        if (!readyArrowSet.contains(player.uuid)) {
//            startDrawTheBow(player)
//            readyArrowSet.add(player.uuid)
//        } else {
//            endDrawTheBow(player)
//            readyArrowSet.remove(player.uuid)
//        }
//    }
//
//    private fun startDrawTheBow(player: Player) {
//        player.addPotionEffect(PotionEffect(PotionEffectType.FAST_DIGGING, 999 * 20, 2, true, true))
//        player.msg("开始拉弓")
//    }
//
//    private fun endDrawTheBow(player: Player) {
//        player.removePotionEffect(PotionEffectType.FAST_DIGGING)
//        player.msg("结束拉弓")
//    }

}