package io.github.mainyf.itemskillsplus.exts

import org.bukkit.block.Block

val Block.blockKey: Long
    get() {
        return getKey(x, y, z)
    }

fun getKey(x: Int, y: Int, z: Int) =
    x.toLong() and 0x7FFFFFF or (z.toLong() and 0x7FFFFFF shl 27) or (y.toLong() shl 54)