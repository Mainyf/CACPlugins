package io.github.mainyf.itemenchantplus.hook

import io.github.mainyf.myislands.IslandsManager
import io.github.mainyf.myislands.MyIslands
import io.github.mainyf.newmclib.exts.pluginManager
import org.bukkit.entity.Player

object MyIsLandHooks {

    private var myIsLandEnable = false

    fun init() {
        myIsLandEnable = pluginManager().isPluginEnabled("MyIslands")
    }

    fun hasAttack(player: Player): Boolean {
        if (!myIsLandEnable) return true
        val plot = MyIslands.plotUtils.getPlotByPLoc(player)
        if (plot?.owner != null && !player.isOp) {
            if (!IslandsManager.hasPermission(player, plot)) {
                return false
            }
        }
        return true
    }

}