package io.github.mainyf.itemenchantplus

import io.github.mainyf.newmclib.exts.uuid
import org.bukkit.entity.Player
import java.util.*

val blockBreakRecursiveFixer = RecursiveFixer()

class RecursiveFixer {

    private val recursiveFixer = mutableSetOf<UUID>()

    fun mark(player: Player) {
        if (!recursiveFixer.contains(player.uuid)) {
            recursiveFixer.add(player.uuid)
        }
    }

    fun has(player: Player): Boolean {
        return recursiveFixer.contains(player.uuid)
    }

    fun unMark(player: Player) {
        recursiveFixer.remove(player.uuid)
    }

}