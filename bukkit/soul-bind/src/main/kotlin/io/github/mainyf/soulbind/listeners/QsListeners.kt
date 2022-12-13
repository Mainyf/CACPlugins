package io.github.mainyf.soulbind.listeners

import io.github.mainyf.newmclib.exts.asPlayer
import io.github.mainyf.soulbind.SBManager
import io.github.mainyf.soulbind.config.sendLang
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.maxgamer.quickshop.api.event.ShopCreateEvent

object QsListeners : Listener {

    @EventHandler
    fun onCreateShop(event: ShopCreateEvent) {
        val item = event.shop.item
        if(SBManager.hasBindItem(item)) {
            event.creator.asPlayer()?.sendLang("bindItemNotAllowedCreateShop")
            event.isCancelled = true
        }
    }

}