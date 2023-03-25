package io.github.mainyf.itemenchantplus.enchants

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import com.destroystokyo.paper.event.player.PlayerReadyArrowEvent
import io.github.mainyf.itemenchantplus.EnchantData
import io.github.mainyf.itemenchantplus.EnchantManager
import io.github.mainyf.itemenchantplus.ItemEnchantPlus
import io.github.mainyf.itemenchantplus.config.ConfigIEP
import io.github.mainyf.itemenchantplus.config.ItemEnchantType
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.protocolManager
import io.github.mainyf.soulbind.SBManager
import io.github.mainyf.worldsettings.WorldSettings
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerToggleFlightEvent
import org.bukkit.inventory.ItemStack
import java.util.*

object VolleyEnchant : Listener {

    private val drawBowMap = mutableMapOf<UUID, EnchantData>()

    fun init() {
        //        pluginManager().registerEvents(Fixer, ItemEnchantPlus.INSTANCE)
        //        protocolManager().addPacketListener(object : PacketAdapter(ItemEnchantPlus.INSTANCE, PacketType.Play.Client.ABILITIES) {
        //
        //            override fun onPacketReceiving(event: PacketEvent) {
        //                val s = event.packet.booleans.read(0)
        //                println()
        //            }
        //
        //        })
        ItemEnchantPlus.INSTANCE.submitTask(delay = 2L) {
            onlinePlayers().forEach {
                if (!WorldSettings.INSTANCE.hasIgnoreFly(it.uuid)) {
                    WorldSettings.INSTANCE.ignoreFly(it.uuid)
                }
                if (!it.allowFlight) {
                    it.allowFlight = true
                }
            }
        }
    }

    @EventHandler
    fun onReadyArrow(event: PlayerReadyArrowEvent) {
        val player = event.player
        if (drawBowMap.containsKey(player.uuid)) {
            endDrawTheBow(player)
        } else {
            val item = player.inventory.itemInMainHand
            if (item.isEmpty()) return
            val bindData = SBManager.getBindItemData(item)
            if (!player.isOp && bindData != null && bindData.ownerUUID != player.uuid) {
                return
            }
            val data = EnchantManager.getItemEnchant(ItemEnchantType.VOLLEY, item) ?: return
            drawBowMap[player.uuid] = data
            startDrawTheBow(player, data)
        }
    }

    @EventHandler
    fun onShootBow(event: EntityShootBowEvent) {
        val player = event.entity as? Player ?: return
        val item = player.inventory.itemInMainHand
        if (item.isEmpty()) return
        val data = EnchantManager.getItemEnchant(ItemEnchantType.VOLLEY, item) ?: return
        if (data.stage >= 1) {
            player.velocity = player.location.direction.multiply(-ConfigIEP.volleyEnchantConfig.knockbackPower)
        }
    }

    @EventHandler
    fun onToggleFlight(event: PlayerToggleFlightEvent) {
        println(event.isFlying)
    }

    private fun startDrawTheBow(player: Player, data: EnchantData) {
        if (ConfigIEP.volleyEnchantConfig.debug) {
            player.msg("[Volley] start of draw bow")
        }
        if (data.stage >= 1) {
            ConfigIEP.volleyEnchantConfig.volleyBuff.forEach {
                player.addPotionEffect(it)
            }
        }
    }

    private fun endDrawTheBow(player: Player) {
        if (!drawBowMap.containsKey(player.uuid)) return
        if (ConfigIEP.volleyEnchantConfig.debug) {
            player.msg("[Volley] end of draw bow")
        }
        drawBowMap.remove(player.uuid)!!
        ConfigIEP.volleyEnchantConfig.volleyBuff.forEach {
            player.removePotionEffect(it.type)
        }
    }

    object Fixer : Listener {

        @EventHandler
        fun onQuit(event: PlayerQuitEvent) {
            endDrawTheBow(event.player)
        }

        @EventHandler
        fun onDrop(event: PlayerDropItemEvent) {
            endDrawTheBow(event.player)
        }

        @EventHandler
        fun onHeld(event: PlayerItemHeldEvent) {
            endDrawTheBow(event.player)
        }

        @EventHandler
        fun onInvOpen(event: InventoryOpenEvent) {
            val player = event.player as? Player ?: return
            endDrawTheBow(player)
        }

        @EventHandler
        fun onInvClick(event: InventoryClickEvent) {
            val player = event.whoClicked as? Player ?: return
            endDrawTheBow(player)
        }

    }

}