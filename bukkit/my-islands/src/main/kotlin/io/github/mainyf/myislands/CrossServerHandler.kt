package io.github.mainyf.myislands

import io.github.mainyf.bungeesettingsbukkit.CrossServerManager
import io.github.mainyf.bungeesettingsbukkit.events.ServerPacketReceiveEvent
import io.github.mainyf.bungeesettingsbukkit.writeLoc
import io.github.mainyf.myislands.storage.IslandVisibility
import io.github.mainyf.newmclib.exts.readUUID
import io.github.mainyf.newmclib.exts.writeString
import io.github.mainyf.newmclib.exts.writeUUID
import io.github.mainyf.newmclib.serverId
import io.netty.buffer.ByteBuf
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

object CrossServerHandler : Listener {

    @EventHandler
    fun onPacket(event: ServerPacketReceiveEvent) {
        val buf = event.buf
        when (event.packet) {
            MyIslands.ISLAND_TP_REQ -> {
                handleTPReq(buf)
            }
        }
    }

    private fun handleTPReq(buf: ByteBuf) {
        val sender = buf.readUUID()
        val receiver = buf.readUUID()
        val islandData = IslandsManager.getIslandData(receiver)
        if (islandData == null) {
            CrossServerManager.sendData(MyIslands.ISLAND_EMPTY_RES) {
                writeString(serverId())
                writeUUID(sender)
                writeUUID(receiver)
            }
            return
        }
        if(islandData.visibility == IslandVisibility.NONE) {
            CrossServerManager.sendData(MyIslands.ISLAND_NOT_ALLOWED_ACCESS) {
                writeString(serverId())
                writeUUID(sender)
                writeUUID(receiver)
            }
            return
        }
        val coreLoc = IslandsManager.getIslandCoreLoc(islandData)
        val homeLoc = IslandsManager.getHomeLoc(coreLoc)
        val newLoc = MyIslands.plotUtils.findSafeLoc(null, IslandsManager.fixIslandHomeLoc(homeLoc), coreLoc)
        CrossServerManager.sendData(MyIslands.ISLAND_TP_RES) {
            writeString(serverId())
            writeUUID(sender)
            writeUUID(receiver)
            writeLoc(newLoc.world.name, newLoc)
        }
    }

}