package io.github.mainyf.bungeesettingsbukkit

import io.github.mainyf.newmclib.exts.readString
import io.github.mainyf.newmclib.exts.writeString
import io.netty.buffer.ByteBuf

fun ByteBuf.readStringList(): List<String> {
    val rs = mutableListOf<String>()
    val len = readInt()
    repeat(len) {
        rs.add(readString())
    }
    return rs
}

fun ByteBuf.writeStringList(list: List<String>) {
    writeInt(list.size)
    list.forEach {
        writeString(it)
    }
}