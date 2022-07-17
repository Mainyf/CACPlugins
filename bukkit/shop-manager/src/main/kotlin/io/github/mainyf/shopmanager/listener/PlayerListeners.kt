package io.github.mainyf.shopmanager.listener

import io.github.mainyf.newmclib.exts.asPlayer
import io.github.mainyf.newmclib.exts.uuid
import io.github.mainyf.newmclib.hooks.money
import io.github.mainyf.shopmanager.ShopManager
import io.github.mainyf.shopmanager.config.ConfigManager
import io.github.mainyf.shopmanager.config.sendLang
import io.github.mainyf.shopmanager.storage.StorageManager
import net.kyori.adventure.text.Component
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.maxgamer.quickshop.api.event.ShopClickEvent
import org.maxgamer.quickshop.api.event.ShopPurchaseEvent
import org.maxgamer.quickshop.api.shop.ShopType

object PlayerListeners : Listener {

    @EventHandler
    fun onClickShop(event: ShopClickEvent) {
        val shop = event.shop
        val material = shop.item.type
        val sellShop = ConfigManager.getSellShop(material) ?: return
        if (!shop.isUnlimited) return
        if (shop.price != sellShop.price) {
            shop.price = sellShop.price
        }
    }

    @EventHandler
    fun onPurchase(event: ShopPurchaseEvent) {
        val player = event.purchaser.asPlayer() ?: return
        val totalPrice = event.total
        if (totalPrice > player.money()) return
        val shop = event.shop
        if (shop.shopType != ShopType.BUYING) return
        val material = shop.item.type
        val sellShop = ConfigManager.getSellShop(material) ?: return
        if (!shop.isUnlimited) return
        if (shop.price != sellShop.price) {
            shop.price = sellShop.price
        }
        val sellCount = ShopManager.INSTANCE.getSellItemCount(player, material, sellShop)
        val langArr = sellShop.getLangArr(material, sellCount)
        if (event.amount > sellCount) {
            player.sendLang("maxHarvest", *langArr)
            event.isCancelled = true
            return
        }
        StorageManager.updateHarvest(player.uuid, totalPrice, material)
    }

}