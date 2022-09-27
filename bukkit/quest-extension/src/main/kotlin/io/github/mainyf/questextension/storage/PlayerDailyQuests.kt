package io.github.mainyf.questextension.storage

import io.github.mainyf.newmclib.env
import io.github.mainyf.newmclib.storage.BaseEntity
import io.github.mainyf.newmclib.storage.BaseTable
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.jodatime.date
import org.jetbrains.exposed.sql.jodatime.datetime
import java.util.*

object PlayerDailyQuests : BaseTable("t_PlayerDailyQuests_${env()}") {

    val pUUID = uuid("player_uuid")

    val questStartTime = date("quest_start_time")

    val questList = varchar("quest_list", 255)

}


class PlayerDailyQuest(uuid: EntityID<UUID>) : BaseEntity(PlayerDailyQuests, uuid) {

    companion object : UUIDEntityClass<PlayerDailyQuest>(PlayerDailyQuests)

    var pUUID by PlayerDailyQuests.pUUID

    var questStartTime by PlayerDailyQuests.questStartTime

    var questListText by PlayerDailyQuests.questList

    val questList get() = questListText.split(",")

}