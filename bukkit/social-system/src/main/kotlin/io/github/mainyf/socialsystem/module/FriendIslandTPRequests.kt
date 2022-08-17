package io.github.mainyf.socialsystem.module

import io.github.mainyf.bungeesettingsbukkit.CrossServerManager
import io.github.mainyf.bungeesettingsbukkit.readLocPair
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.offline_player_ext.asOfflineData
import io.github.mainyf.socialsystem.SocialSystem
import io.github.mainyf.socialsystem.config.sendLang
import io.github.mainyf.socialsystem.menu.SocialIslandTPMenu
import io.netty.buffer.ByteBuf
import org.bukkit.Location
import java.util.*

object FriendIslandTPRequests {

    private val islandTPReqs = mutableMapOf<UUID, IslandTpReq>()

    fun sendTpIslandReq(sender: UUID, receiver: UUID) {
        islandTPReqs[sender] = IslandTpReq(
            receiver,
            CrossServerManager.serverIds.filter { it.startsWith("plot") }.size,
            0
        )
        CrossServerManager.sendData(SocialSystem.ISLAND_TP_REQ) {
            writeUUID(sender)
            writeUUID(receiver)
        }
    }

    fun handleIslandEmpty(buf: ByteBuf) {
        val serverId = buf.readString()
        val sender = buf.readUUID()
        if (!islandTPReqs.containsKey(sender)) return
        val tpReq = islandTPReqs[sender]!!
        tpReq.receivePlotServer++
        val receiver = buf.readUUID()
        tpReq.locMap[serverId] = null
        tpReq.statusMap[serverId] = IslandStatus.NONE
        if (tpReq.receivePlotServer >= tpReq.plotServerCount) {
            handleIslandRes(tpReq, sender, receiver)
        }
    }

    fun handleIslandTPAccessNotAllowed(buf: ByteBuf) {
        val serverId = buf.readString()
        val sender = buf.readUUID()
        if (!islandTPReqs.containsKey(sender)) return
        val tpReq = islandTPReqs[sender]!!
        tpReq.receivePlotServer++
        val receiver = buf.readUUID()
        tpReq.locMap[serverId] = null
        tpReq.statusMap[serverId] = IslandStatus.ACCESS_NOT_ALLOWED
        if (tpReq.receivePlotServer >= tpReq.plotServerCount) {
            handleIslandRes(tpReq, sender, receiver)
        }
    }

    fun handleIslandTPRes(buf: ByteBuf) {
        val serverId = buf.readString()
        val sender = buf.readUUID()
        if (!islandTPReqs.containsKey(sender)) return
        val tpReq = islandTPReqs[sender]!!
        tpReq.receivePlotServer++
        val receiver = buf.readUUID()
        val locPair = buf.readLocPair()
        tpReq.locMap[serverId] = locPair
        tpReq.statusMap[serverId] = IslandStatus.DEFAULT
        if (tpReq.receivePlotServer >= tpReq.plotServerCount) {
            handleIslandRes(tpReq, sender, receiver)
        }
    }

    private fun handleIslandRes(tpReq: IslandTpReq, sender: UUID, receiver: UUID) {
        islandTPReqs.remove(sender)
        if(tpReq.statusMap.values.all { it == IslandStatus.NONE }) {
            sender.asPlayer()?.sendLang("friendNonOwnIsland", "{friend}", receiver.asOfflineData()?.name ?: "")
            return
        }

        val player = sender.asPlayer() ?: return
        SocialIslandTPMenu(tpReq).open(player)
    }


}

data class IslandTpReq(
    val receiver: UUID,
    val plotServerCount: Int,
    var receivePlotServer: Int,
    val statusMap: MutableMap<String, IslandStatus> = mutableMapOf(),
    val locMap: MutableMap<String, Pair<String, Location>?> = mutableMapOf()
)

enum class IslandStatus {
    NONE,
    ACCESS_NOT_ALLOWED,
    DEFAULT
}