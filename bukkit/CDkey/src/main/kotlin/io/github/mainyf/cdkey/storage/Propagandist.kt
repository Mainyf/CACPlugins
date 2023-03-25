package io.github.mainyf.cdkey.storage

import io.github.mainyf.newmclib.storage.BaseEntity
import io.github.mainyf.newmclib.storage.BaseTable
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.*

object Propagandists : BaseTable("t_Propagandists") {

    val propagandist = varchar("propagandist", 255)

    val invitee = uuid("invitee")

}

class Propagandist(uuid: EntityID<UUID>) : BaseEntity(Propagandists, uuid) {

    companion object : UUIDEntityClass<Propagandist>(Propagandists)

    var propagandist by Propagandists.propagandist

    var invitee by Propagandists.invitee

}