package io.github.mainyf.socialsystem.menu

import io.github.mainyf.bungeesettingsbukkit.CrossServerManager
import io.github.mainyf.newmclib.config.IaIcon
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.menu.AbstractMenuHandler
import io.github.mainyf.newmclib.menu.ConfirmMenu
import io.github.mainyf.newmclib.offline_player_ext.OfflinePlayerData
import io.github.mainyf.newmclib.offline_player_ext.asOfflineData
import io.github.mainyf.newmclib.utils.Cooldown
import io.github.mainyf.newmclib.utils.Heads
import io.github.mainyf.socialsystem.config.ConfigSS
import io.github.mainyf.socialsystem.config.sendLang
import io.github.mainyf.socialsystem.module.*
import io.github.mainyf.socialsystem.storage.PlayerSocial
import io.github.mainyf.socialsystem.storage.StorageSS
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import kotlin.math.ceil

class SocialMainMenu(val offlineData: OfflinePlayerData) : AbstractMenuHandler() {

    private var pageIndex = 1
    private var pageSize = 0
    private var maxPageIndex = 0

    private val friends = mutableListOf<OfflinePlayerData>()

    private val currentFriends = mutableListOf<OfflinePlayerData>()

    //    private val target = offlineData.uuid.asPlayer()

    private val isTargetOnline get() = CrossServerManager.isOnline(offlineData.uuid)

    private lateinit var player: Player

    private lateinit var targetSocial: PlayerSocial

    private val offlinePlayer = Bukkit.getOfflinePlayer(offlineData.name)

    private val hasOwner get() = offlineData.uuid == player.uuid

    companion object {

        val sendTPRequestCooldown = Cooldown()

    }

    override fun open(player: Player) {
        this.player = player
        this.pageSize = ConfigSS.socialMainMenuConfig.friendsSlot.slot.size
        this.targetSocial = FriendHandler.getPlayerSocial(offlineData.uuid)
        updateFriends()
        updateCurrentFriends()
        if (hasOwner) {
            setup(ConfigSS.socialMainMenuConfig.settings)
        } else {
            setup(ConfigSS.socialMainMenuConfig.settings.copy(background = ConfigSS.socialMainMenuConfig.backgroundFriend))
        }
        val inv = createInv(player)

        updateInv(player, inv)
        player.openInventory(inv)
    }

    override fun updateTitle(player: Player): String {
        val smmConfig = ConfigSS.socialMainMenuConfig
        val icons = mutableListOf<IaIcon>()

        icons.addAll(smmConfig.prevSlot.iaIcon())
        icons.addAll(smmConfig.nextSlot.iaIcon())
        icons.addAll(smmConfig.backSlot.iaIcon())
        icons.addAll(smmConfig.friendsSlot.iaIcon())
        icons.addAll(smmConfig.headSlot.iaIcon())
        icons.addAll(smmConfig.cardX1Slot.iaIcon())
        icons.addAll(smmConfig.cardX2Slot.iaIcon())
        icons.addAll(smmConfig.cardX3Slot.iaIcon())
        icons.addAll(smmConfig.cardX4Slot.iaIcon())
        icons.addAll(smmConfig.onlineSlot.iaIcon(if (isTargetOnline) "online" else "offline"))
        if (hasOwner) {
            icons.addAll(smmConfig.allowRepairSlot.iaIcon())
        } else {
            icons.addAll(smmConfig.deleteSlot.iaIcon())
        }
        if (!hasOwner) {
            icons.addAll(smmConfig.tpSlot.iaIcon())
            icons.addAll(smmConfig.tpIsland.iaIcon())
            icons.addAll(smmConfig.nickname.iaIcon())
        }

        return applyTitle(player, icons)
    }

    private fun updateFriends() {
        this.friends.clear()
        this.friends.add(player.uuid.asOfflineData()!!)
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
        val smmConfig = ConfigSS.socialMainMenuConfig

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
        inv.setIcon(smmConfig.backSlot)

        inv.setIcon(
            smmConfig.headSlot.slot,
            smmConfig.headSlot.default()!!
                .toItemStack(Heads.getPlayerHead(offlineData.name).clone()).tvar("player", offlineData.name).apply {
                    setPlaceholder(offlinePlayer)
                }
        )

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
            inv.setIcon(smmConfig.allowRepairSlot, itemBlock = {
                tvar("status", if (targetSocial.allowRepair) "允许" else "不允许")
            }) {
                FriendHandler.setAllowRepair(targetSocial, !targetSocial.allowRepair)
                updateInv(player, inv)
            }
            inv.setIcon(smmConfig.nickname, itemBlock = {
                tvar("visible", if (StorageSS.hasVisibleNickname(player.uuid)) "显示" else "隐藏")
            }, rightClickBlock = {
                if (!it.hasPermission(ConfigSS.nicknameConfig.permission)) {
                    it.sendLang("nicknameOfMenuNoPerm")
                    return@setIcon
                }
                NicknameConversation.join(it)
                it.closeInventory()
            }, leftClickBlock = {
                if (StorageSS.hasVisibleNickname(player.uuid)) {
                    StorageSS.setVisibleNickname(player.uuid, false)
                    it.sendLang("hideNickname")
                } else {
                    StorageSS.setVisibleNickname(player.uuid, true)
                    it.sendLang("showNickname")
                }
                updateInv(player, inv)
            })
        } else {
            inv.setIcon(smmConfig.deleteSlot) {
                if (FriendHandler.isFriend(player, offlineData.uuid)) {
                    ConfirmMenu(
                        {
                            FriendHandler.deleteFriend(player, offlineData)
                            player.closeInventory()
                        },
                        {
                            player.closeInventory()
                            SocialMainMenu(offlineData).open(it)
                        }
                    ).open(player)
                }
            }
            inv.setIcon(smmConfig.tpSlot, leftClickBlock = { p ->
                if (!isTargetOnline) {
                    p.sendLang("tpTargetOffline")
                    return@setIcon
                }
                sendTPRequestCooldown.invoke(p.uuid, ConfigSS.tpRequestCooldown * 1000L, {
                    FriendTPRequests.sendTPRequest(p, offlineData)
                }, {
                    p.sendLang("tpRequestCooldown", "{eTime}", it.timestampConvertTime())
                })
            }, rightClickBlock = { p ->
                if (!isTargetOnline) {
                    p.sendLang("tpTargetOffline")
                    return@setIcon
                }
                sendTPRequestCooldown.invoke(p.uuid, ConfigSS.tpRequestCooldown * 1000L, {
                    FriendInvites.sendInviteTP(p, offlineData)
                }, {
                    p.sendLang("tpRequestCooldown", "{eTime}", it.timestampConvertTime())
                })
            })
            inv.setIcon(smmConfig.tpIsland) {
                FriendIslandTPRequests.sendTpIslandReq(it.uuid, offlineData.uuid)
            }
        }
        updateFriends(player, inv)
    }

    private fun updateFriends(player: Player, inv: Inventory) {
        val smmConfig = ConfigSS.socialMainMenuConfig
        val friendsSlot = smmConfig.friendsSlot.slot
        inv.setIcon(friendsSlot, smmConfig.friendsSlot["empty"]!!.toItemStack().tvar("player", player.name))
        currentFriends.forEachIndexed { index, offlinePlayerData ->
            val skullItem = Heads.getPlayerHead(offlinePlayerData.name).clone()
            skullItem.setDisplayName(offlinePlayerData.name)
            inv.setIcon(friendsSlot[index], smmConfig.friendsSlot.default()!!.toItemStack(skullItem) {
                tvar("player", offlinePlayerData.name)
                val offlinePlayer = Bukkit.getOfflinePlayer(offlinePlayerData.uuid)
                setPlaceholder(offlinePlayer)
//                val profile = Bukkit.getServer().createProfile(offlinePlayerData.uuid, offlinePlayerData.name)
//                val offlinePlayer = Bukkit.getServer().toReflect().call<OfflinePlayer>("getOfflinePlayer", profile)
//                if (offlinePlayer != null) {
//                    setPlaceholder(offlinePlayer)
//                }
            }) {
                smmConfig.friendsSlot.default()!!.execAction(it)
                SocialMainMenu(offlinePlayerData).apply {
                    this.pageIndex = this@SocialMainMenu.pageIndex
                }.open(it)
            }
        }

    }

}