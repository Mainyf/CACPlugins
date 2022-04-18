package io.github.mainyf.itemskillsplus

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.PacketContainer
import com.comphenix.protocol.wrappers.WrappedChatComponent
import io.github.mainyf.newmclib.exts.colored
import org.bukkit.Material
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftPlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

fun Player.setOpenInventoryTitle(title: String) {
    val packet = PacketContainer(PacketType.Play.Server.OPEN_WINDOW)
    val nmsPlayer = (this as CraftPlayer).handle
    val inv = this.openInventory.topInventory
    val id = when (inv.size) {
        9 -> 0
        18 -> 1
        27 -> 2
        36, 41 -> 3
        45 -> 4
        54 -> 5
        else -> throw IllegalArgumentException("Unsupported custom inventory size " + inventory.size)
    }
    packet.integers.write(0, nmsPlayer.bV.j)
    packet.integers.write(1, id)
    packet.chatComponents.write(0, WrappedChatComponent.fromText(title.colored()))
    ProtocolLibrary.getProtocolManager().sendServerPacket(this, packet)
}

fun ItemStack?.isEmpty() = this == null || this.type == Material.AIR

fun Player.giveItem(item: ItemStack) {
    val leftover = inventory.addItem(item)
    leftover.forEach {
        world.dropItemNaturally(location, it.value)
    }
}