package io.github.mainyf.bungeesettingsbukkit

import io.github.mainyf.newmclib.exts.asWorld
import io.github.mainyf.newmclib.exts.readString
import io.github.mainyf.newmclib.exts.writeString
import io.netty.buffer.ByteBuf
import org.bukkit.Location

fun ByteBuf.writeLoc(worldName: String, loc: Location) {
    writeString(worldName)
    writeDouble(loc.x)
    writeDouble(loc.y)
    writeDouble(loc.z)
    writeFloat(loc.yaw)
    writeFloat(loc.pitch)
}

fun ByteBuf.readLoc(): Location {
    val world = readString().asWorld()
    val x = readDouble()
    val y = readDouble()
    val z = readDouble()
    val yaw = readFloat()
    val pitch = readFloat()
    return Location(world, x, y, z, yaw, pitch)
}

fun ByteBuf.readLocPair(): Pair<String, Location> {
    val world = readString()
    val x = readDouble()
    val y = readDouble()
    val z = readDouble()
    val yaw = readFloat()
    val pitch = readFloat()
    return world to Location(world.asWorld(), x, y, z, yaw, pitch)
}