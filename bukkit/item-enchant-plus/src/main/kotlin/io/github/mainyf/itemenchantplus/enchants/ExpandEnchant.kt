package io.github.mainyf.itemenchantplus.enchants

import io.github.mainyf.itemenchantplus.ItemEnchantPlus
import io.github.mainyf.itemenchantplus.config.ConfigIEP
import io.github.mainyf.newmclib.exts.isEmpty
import io.github.mainyf.newmclib.exts.onlinePlayers
import io.github.mainyf.newmclib.exts.submitTask
import org.bukkit.event.Listener
import java.util.*

object ExpandEnchant : Listener {

    private val recursiveFixer = mutableSetOf<UUID>()
    private val playerHeldItem = mutableSetOf<UUID>()

    fun init() {
        ItemEnchantPlus.INSTANCE.submitTask(period = 2 * 20L) {
            if (!ConfigIEP.expandEnchant.enable) return@submitTask
            onlinePlayers().forEach { p ->
                val item = p.inventory.itemInMainHand
                if(item.isEmpty()) return@forEach
//                val data = SkillManager.getItemSkill(SkillManager.expandDataKey, item) ?: return@forEach
//                if (data.stage >= 3) {
//                    p.addPotionEffect(PotionEffect(PotionEffectType.FAST_DIGGING, 5 * 20, 0))
//                    playerHeldItem.add(p.uniqueId)
//                }
            }
        }
    }

}