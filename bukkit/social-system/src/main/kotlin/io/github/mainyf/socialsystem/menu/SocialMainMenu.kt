package io.github.mainyf.socialsystem.menu

import io.github.mainyf.newmclib.config.IaIcon
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.menu.AbstractMenuHandler
import io.github.mainyf.newmclib.menu.ConfirmMenu
import io.github.mainyf.newmclib.offline_player_ext.OfflinePlayerData
import io.github.mainyf.newmclib.offline_player_ext.asOfflineData
import io.github.mainyf.newmclib.utils.Heads
import io.github.mainyf.socialsystem.config.ConfigManager
import io.github.mainyf.socialsystem.config.sendLang
import io.github.mainyf.socialsystem.module.FriendHandler
import io.github.mainyf.socialsystem.storage.PlayerSocial
import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import java.util.*
import kotlin.math.ceil

class SocialMainMenu(val offlineData: OfflinePlayerData) : AbstractMenuHandler() {

    constructor(target: Player) : this(target.uuid.asOfflineData()!!)

    private var pageIndex = 1
    private var pageSize = 0
    private var maxPageIndex = 0

    private val friends = mutableListOf<OfflinePlayerData>()

    private val currentFriends = mutableListOf<OfflinePlayerData>()

    private val target = offlineData.uuid.asPlayer()

    private val isTargetOnline get() = target != null

    private lateinit var player: Player

    private lateinit var targetSocial: PlayerSocial

    private val offlinePlayer = Bukkit.getOfflinePlayer(offlineData.name)

    private val hasOwner get() = offlineData.uuid == player.uuid

    override fun open(player: Player) {
        this.player = player
        this.pageSize = ConfigManager.socialMainMenuConfig.friendsSlot.slot.size
        this.targetSocial = FriendHandler.getPlayerSocial(offlineData.uuid)
        updateFriends()
        updateCurrentFriends()
        setup(ConfigManager.socialMainMenuConfig.settings)
        val inv = createInv(player)

        updateInv(player, inv)
        player.openInventory(inv)
    }

    override fun updateTitle(player: Player): String {
        val smmConfig = ConfigManager.socialMainMenuConfig
        val icons = mutableListOf<IaIcon>()

        icons.addAll(smmConfig.prevSlot.iaIcon())
        icons.addAll(smmConfig.nextSlot.iaIcon())
        icons.addAll(smmConfig.friendsSlot.iaIcon())
        icons.addAll(smmConfig.headSlot.iaIcon())
        icons.addAll(smmConfig.cardX1Slot.iaIcon())
        icons.addAll(smmConfig.cardX2Slot.iaIcon())
        icons.addAll(smmConfig.cardX3Slot.iaIcon())
        icons.addAll(smmConfig.cardX4Slot.iaIcon())
        icons.addAll(smmConfig.onlineSlot.iaIcon(if (isTargetOnline) "online" else "offline"))
        icons.addAll(smmConfig.deleteFriendOrAllowRepairSlot.iaIcon(if (hasOwner) "allowRepair" else "delete"))
        if (!hasOwner) {
            icons.addAll(smmConfig.tpSlot.iaIcon())
        }

        return applyTitle(player, icons)
    }

    private fun updateFriends() {
        this.friends.clear()
        this.friends.addAll(FriendHandler.getFriends(player))
        this.maxPageIndex = ceil(
            friends.size.toDouble() / pageSize.toDouble()
        ).toInt()
    }

    private fun updateCurrentFriends() {
        currentFriends.clear()
        currentFriends.addAll(friends.pagination(pageIndex, pageSize))
    }

    private fun updateInv(player: Player, inv: Inventory) {
        val smmConfig = ConfigManager.socialMainMenuConfig

        inv.setIcon(smmConfig.prevSlot) {
            if (pageIndex > 1) {
                pageIndex--
                updateCurrentFriends()
                updateFriends(player, inv)
            }
        }
        inv.setIcon(smmConfig.nextSlot) {
            if (pageIndex < maxPageIndex) {
                pageIndex++
                updateCurrentFriends()
                updateFriends(player, inv)
            }
        }

        inv.setIcon(smmConfig.headSlot, itemStack = Heads.getPlayerHead(offlineData.name))

        arrayOf(
            smmConfig.cardX1Slot,
            smmConfig.cardX2Slot,
            smmConfig.cardX3Slot,
            smmConfig.cardX4Slot
        ).forEach { slotConfig ->
            inv.setIcon(slotConfig, itemBlock = {
                setPlaceholder(offlinePlayer)
            })
        }

        inv.setIcon(smmConfig.onlineSlot, if (isTargetOnline) "online" else "offline")
        if (hasOwner) {
            inv.setIcon(smmConfig.deleteFriendOrAllowRepairSlot, "allowRepair", itemBlock = {
                tvar("status", if (targetSocial.allowRepair) "开启" else "关闭")
            }) {
                FriendHandler.setAllowRepair(targetSocial, !targetSocial.allowRepair)
                updateInv(player, inv)
            }
        } else {
            inv.setIcon(smmConfig.deleteFriendOrAllowRepairSlot, "delete") {
                if (FriendHandler.isFriend(player, offlineData.uuid)) {
                    ConfirmMenu(
                        {
                            FriendHandler.removeFriend(player, offlineData.uuid)
                            player.sendLang("deleteFriendToSender", "{friend}", offlineData.name)
                            target?.sendLang("deleteFriendToRecevier", "{friend}", player.name)
                            player.closeInventory()
                        },
                        {
                            player.closeInventory()
                            SocialMainMenu(offlineData).open(it)
                        }
                    ).open(player)
                }
            }
            inv.setIcon(smmConfig.tpSlot) {

            }
        }
        updateFriends(player, inv)
    }

    private fun updateFriends(player: Player, inv: Inventory) {
        val smmConfig = ConfigManager.socialMainMenuConfig
        val friendsSlot = smmConfig.friendsSlot.slot
        val barFriends = mutableListOf<OfflinePlayerData>()
        if (!hasOwner) {
            barFriends.add(player.uuid.asOfflineData()!!)
        }
        barFriends.addAll(currentFriends)
        barFriends.forEachIndexed { index, offlinePlayerData ->
            val skullItem = Heads.getPlayerHead(offlinePlayerData.name).clone()
            skullItem.setDisplayName(offlinePlayerData.name)
            inv.setIcon(friendsSlot[index], smmConfig.friendsSlot.default()!!.toItemStack(skullItem) {
                tvar("player", offlinePlayerData.name)
            }) {
                smmConfig.friendsSlot.default()!!.execAction(it)
                SocialMainMenu(offlinePlayerData).open(it)
            }
        }

    }

}