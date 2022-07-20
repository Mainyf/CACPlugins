package io.github.mainyf.bungeesettingsbukkit

import io.netty.buffer.ByteBuf
import java.util.*

fun ByteBuf.toByteArray(): ByteArray {
    val bytes: ByteArray
    val length = readableBytes()

    if (hasArray()) {
        bytes = array()
    } else {
        bytes = ByteArray(length)
        getBytes(readerIndex(), bytes)
    }
    return bytes
}

fun ByteBuf.writeUUID(uuid: UUID) {
    writeLong(uuid.mostSignificantBits)
    writeLong(uuid.leastSignificantBits)
}

fun ByteBuf.readUUID(): UUID {
    val most = readLong()
    val least = readLong()
    return UUID(most, least)
}

fun ByteBuf.writeString(t: String) {
    writeInt(t.length)
    t.forEach {
        writeChar(it.code)
    }
}

fun ByteBuf.readString(): String {
    val l = readInt()
    val sb = StringBuilder()
    repeat(l) {
        sb.append(readChar())
    }
    return sb.toString()
}

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