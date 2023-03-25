package io.github.mainyf.shopmanager.listener

import io.github.mainyf.newmclib.exts.asPlayer
import io.github.mainyf.newmclib.exts.uuid
import io.github.mainyf.shopmanager.ShopManager
import io.github.mainyf.shopmanager.config.ConfigSM
import io.github.mainyf.shopmanager.config.sendLang
import io.github.mainyf.shopmanager.storage.StorageManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.maxgamer.quickshop.api.event.ShopClickEvent
import org.maxgamer.quickshop.api.event.ShopCreateEvent
import org.maxgamer.quickshop.api.event.ShopPriceChangeEvent
import org.maxgamer.quickshop.api.event.ShopPurchaseEvent
import org.maxgamer.quickshop.api.event.ShopTaxEvent
import org.maxgamer.quickshop.api.shop.ShopType

object PlayerListeners : Listener {

    @EventHandler
    fun onTax(event: ShopTaxEvent) {
        val shop = event.shop
        val shopItem = shop.item
        if (shop.isUnlimited) return
        val shopTax = ConfigSM.getShopTaxByItem(shopItem) ?: return
        event.tax = shopTax.taxValue
    }

    @EventHandler
    fun onCreateShop(event: ShopCreateEvent) {
        val player = event.creator.asPlayer()
        val shop = event.shop
        val shopItem = shop.item
        if (shop.isUnlimited) return
        val shopPriceLimit = ConfigSM.getShopPriceLimitByItem(shopItem) ?: return
        if (!shopPriceLimit.contains(shop.price)) {
            player?.sendLang(
                "shopCreatePriceLimit",
                "{player}",
                player.name,
                "{minPrice}",
                shopPriceLimit.min,
                "{maxPrice}",
                shopPriceLimit.max
            )
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onChangePrice(event: ShopPriceChangeEvent) {
        val shop = event.shop
        val player = shop.owner.asPlayer()
        val shopItem = shop.item
        if (shop.isUnlimited) return
        val shopPriceLimit = ConfigSM.getShopPriceLimitByItem(shopItem) ?: return
        if (!shopPriceLimit.contains(event.newPrice)) {
            player?.sendLang(
                "shopPriceChangePriceLimit",
                "{player}",
                player.name,
                "{minPrice}",
                shopPriceLimit.min,
                "{maxPrice}",
                shopPriceLimit.max
            )
            event.isCancelled = true
        }
    }

    @EventHandler
    fun handleShopPriceLimit(event: ShopPurchaseEvent) {
        val shop = event.shop
        val player = shop.owner.asPlayer()
        val shopItem = shop.item
        if (shop.isUnlimited) return
        val shopPriceLimit = ConfigSM.getShopPriceLimitByItem(shopItem) ?: return
        if (!shopPriceLimit.contains(shop.price)) {
            player?.sendLang(
                if (shop.shopType == ShopType.BUYING) "shopBuyingPriceLimit" else "shopSellingPriceLimit",
                "{player}",
                player.name,
                "{minPrice}",
                shopPriceLimit.min,
                "{maxPrice}",
                shopPriceLimit.max
            )
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onClickShop(event: ShopClickEvent) {
        val shop = event.shop
        val material = shop.item.type
        val sellShop = ConfigSM.getSellShop(material) ?: return
        if (shop.shopType != ShopType.BUYING) return
        if (!shop.isUnlimited) return
        if (shop.price != sellShop.price) {
            shop.price = sellShop.price
        }
    }

    @EventHandler
    fun handleHarvestLimit(event: ShopPurchaseEvent) {
        val player = event.purchaser.asPlayer() ?: return
        val totalPrice = event.total
        //        if (totalPrice > player.money()) return
        val shop = event.shop
        if (shop.shopType != ShopType.BUYING) return
        val material = shop.item.type
        val sellShop = ConfigSM.getSellShop(material) ?: return
        if (!shop.isUnlimited) return
        if (shop.price != sellShop.price) {
            shop.price = sellShop.price
        }
        val sellCount = ShopManager.INSTANCE.getSellItemCount(player, material, sellShop)
        val langArr = sellShop.getLangArr(player, material, sellCount)
        if (event.amount > sellCount) {
            player.sendLang("maxHarvest", *langArr)
            event.isCancelled = true
            return
        }
        StorageManager.updateHarvest(player.uuid, totalPrice, material)
    }

}