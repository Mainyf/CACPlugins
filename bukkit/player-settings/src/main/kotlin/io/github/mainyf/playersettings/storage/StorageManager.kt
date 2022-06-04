package io.github.mainyf.playersettings.storage

import com.alibaba.excel.EasyExcel
import io.github.mainyf.newmclib.exts.formatYMDHMS
import io.github.mainyf.newmclib.exts.uuid
import io.github.mainyf.newmclib.serverId
import io.github.mainyf.newmclib.storage.AbstractStorageManager
import io.github.mainyf.newmclib.storage.BaseTable
import io.github.mainyf.newmclib.storage.insertByID
import io.github.mainyf.newmclib.storage.newByID
import io.github.mainyf.playersettings.PlayerSettings
import io.github.mainyf.playersettings.storage.excel.RegisterCountIndexData
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.joda.time.DateTime
import org.joda.time.LocalDate
import java.io.File
import java.time.LocalDateTime
import java.util.*

object StorageManager : AbstractStorageManager() {

    private lateinit var exportFolder: File

    override fun init() {
        exportFolder = PlayerSettings.INSTANCE.dataFolder.resolve("exports")
        if (!exportFolder.exists()) {
            exportFolder.mkdirs()
        }
        super.init()
        transaction {
            arrayOf(
                PlayerCommandLogs,
                PlayerLocLogs,
                PlayerMessageLogs,
                PlayerLivelys,
                PlayerDayOnlines,
                PlayerLivelyToDayOnlines,

                CommandExecuteLogs,
                PlayerRegisterLogs
            ).forEach {
                SchemaUtils.createMissingTablesAndColumns(it)
            }
        }
    }

    fun addPlayerCommandLog(player: Player, cmd: String) {
        transaction {
            PlayerCommandLogs.insertByID {

                it[this.playerUUID] = player.uuid
                it[this.cmd] = cmd

                it[this.id] = UUID.randomUUID()
                it[this.createTime] = DateTime.now()
            }
        }
    }

    fun addPlayerLoc(player: Player) {
        transaction {
            val loc = player.location
            PlayerLocLogs.insertByID {
                it[this.createTime] = DateTime.now()

                it[this.playerUUID] = player.uuid
                it[this.serverId] = serverId()
                it[this.world] = loc.world.name
                it[this.xPos] = loc.x
                it[this.yPos] = loc.y
                it[this.zPos] = loc.z
                it[this.pitch] = loc.pitch
                it[this.yaw] = loc.yaw
            }
        }
    }

    fun addPlayerMessageLog(player: Player, message: String) {
        transaction {
            PlayerMessageLogs.insertByID {

                it[this.createTime] = DateTime.now()

                it[this.playerUUID] = player.uuid
                it[this.message] = message
            }
        }
    }

    fun handlePlayerRegister(player: Player) {
        transaction {
            val data = PlayerLively.findById(player.uuid)
            if (data == null) {
                findOrCreateLivelyData(player, 0L)
            } else {
                data.registerDate = DateTime.now()
            }

            val curDate = LocalDate.now().toDateTimeAtStartOfDay()
            val rsRow = PlayerRegisterLogs.select { PlayerRegisterLogs.date eq curDate }.firstOrNull()
            if (rsRow == null) {
                PlayerRegisterLogs.insertByID {
                    it[this.createTime] = DateTime.now()

                    it[this.date] = curDate
                    it[this.count] = 1
                }
            } else {
                val id = rsRow[PlayerRegisterLogs.id].value
//                val count = rsRow[PlayerRegisterLogs.count]
                PlayerRegisterLogs.update(
                    where = { PlayerRegisterLogs.id eq id },
                    body = {
                        it[this.count] = this.count + 1
                    }
                )
            }
        }
    }

    fun handlePlayerLogin(player: Player) {
        transaction {
            val data = PlayerLively.findById(player.uuid)
            if (data == null) {
                findOrCreateLivelyData(player, 0L)
            } else {
                data.lastLoginDate = DateTime.now()
            }
        }
    }

    fun updatePlayerOnlineTime(player: Player, time: Long) {
        findOrCreateLivelyData(player, time)
    }

    private fun findOrCreateLivelyData(player: Player, minutes: Long): PlayerLively {
        val uuid = player.uuid
        val dayOnlines = findAndUpdateDayOnline(uuid, minutes)
        return transaction {
            var data = PlayerLively.findById(uuid)
            if (data == null) {
                data = PlayerLively.new(uuid) {

                    this.createTime = DateTime.now()

                    this.registerDate = DateTime.now()
                    this.dayOnlines = SizedCollection(dayOnlines)
                    this.lastLoginDate = DateTime.now()
                }
            } else {
                data.dayOnlines = SizedCollection(dayOnlines)
            }
            data
        }
    }

    private fun findAndUpdateDayOnline(uuid: UUID, minutes: Long): List<PlayerDayOnline> {
        updateDayOnline(uuid, minutes)
        val rs = mutableListOf<PlayerDayOnline>()

        transaction {
            PlayerDayOnline.find { PlayerDayOnlines.pUUID eq uuid }.forEach {
                rs.add(it)
            }
        }

        return rs
    }

    private fun updateDayOnline(uuid: UUID, minutes: Long) {
        val curDate = LocalDate.now().toDateTimeAtStartOfDay()
        transaction {
            PlayerDayOnline.find { (PlayerDayOnlines.pUUID eq uuid) and (PlayerDayOnlines.date eq curDate) }
                .firstOrNull().let {
                    if (it == null) {
                        PlayerDayOnline.newByID {
                            this.createTime = DateTime.now()

                            this.pUUID = uuid
                            this.date = curDate
                            this.minutes = minutes
                        }
                    } else {
                        it.minutes += minutes
                        it
                    }
                }
        }
    }

    fun onCommandExecute(cmd: String) {
        val handledCmd = if (cmd.startsWith("/")) cmd.substring(1) else cmd
        val cmdPair = handledCmd.split(" ")
        val cPSize = cmdPair.size
        transaction {
//            val increaseList = mutableMapOf<Int, UUID>()
            var prevID: UUID? = null
            var updatedCmd: String? = null
//            var exeCount = 0
            for (it in CommandExecuteLogs.selectAll()) {
                val logCmd = it[CommandExecuteLogs.cmd]
                val logCmdPair = logCmd.split(" ")
                when (cPSize) {
                    // /xxx  ----  /xxx
                    logCmdPair.size -> {
                        if (cmdPair == logCmdPair) {
                            prevID = it[CommandExecuteLogs.id].value
                            updatedCmd = handledCmd
//                            exeCount = it[CommandExecuteLogs.count]
                            break
                        }
                    }
                    // /xxx arg1  ----  /xxx arg1 arg2
                    // /xxx  ----  /xxx arg1 arg2
                    // /xxx  ----  /xxx arg1 arg2
                    else -> {
                        var mCount = 0
                        for (i in cmdPair.indices) {
                            if (i >= logCmdPair.size) break
                            if (cmdPair[i] == logCmdPair.getOrNull(i)) {
                                mCount++
                            }
                        }
                        if (mCount < logCmdPair.size) {

                        }
                    }
                }
            }
            if (prevID == null) {
                CommandExecuteLogs.insertByID {
                    it[this.createTime] = DateTime.now()

                    it[this.cmd] = handledCmd
                    it[this.count] = 1
                }
            } else {
                CommandExecuteLogs.update(
                    where = { CommandExecuteLogs.id eq prevID },
                    body = {
                        it[this.cmd] = updatedCmd!!
                        it[this.count] = this.count + 1
                    }
                )
            }
        }
    }

    fun exportToFile(sender: CommandSender) {
        val file = exportFolder.resolve("export-${LocalDateTime.now().formatYMDHMS()}.xlsx")

//        EasyExcel
//            .write(file, RegisterCountIndexData::class.java)
//            .sheet()
//            .doWrite {  }

    }

}