package io.github.mainyf.cdkey.storage

import io.github.mainyf.cdkey.CDkey
import io.github.mainyf.cdkey.config.CDKeyConfig
import io.github.mainyf.cdkey.config.ConfigCDK
import io.github.mainyf.newmclib.exts.currentTime
import io.github.mainyf.newmclib.exts.format
import io.github.mainyf.newmclib.storage.AbstractStorageManager
import io.github.mainyf.newmclib.storage.newByID
import org.apache.commons.lang3.RandomStringUtils
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import java.util.UUID

object StorageCDK : AbstractStorageManager() {

    override fun init() {
        super.init()
        transaction {
            arrayOf(
                ConsumeCDKeys,
                PlayerClaimedCDKeys,
                Propagandists
            ).forEach {
                SchemaUtils.createMissingTablesAndColumns(it)
            }
        }
    }

    fun getValidConsumeCDkeys(): List<ConsumeCDKey> {
        return transaction {
            ConsumeCDKey.find { ConsumeCDKeys.valid eq true }.toList()
        }
    }

    fun markInValidConsoleCDKeys(consumeCDKey: ConsumeCDKey) {
        transaction {
            consumeCDKey.valid = false
        }
    }

    fun getClaimedPropagandistCDkey(uuid: UUID): PlayerClaimedCDKey? {
        val propagandistCDKeys = ConfigCDK.propagandistMap.values.map { it.codeName }
        return transaction {
            PlayerClaimedCDKey.find { (PlayerClaimedCDKeys.pUUID eq uuid) and (PlayerClaimedCDKeys.cdkey inList propagandistCDKeys) }
                .firstOrNull()
        }
    }

    fun hasClaimedPropagandistCDkey(uuid: UUID): Boolean {
        val propagandistCDKeys = ConfigCDK.propagandistMap.values.map { it.codeName }
        return transaction {
            !PlayerClaimedCDKey.find { (PlayerClaimedCDKeys.pUUID eq uuid) and (PlayerClaimedCDKeys.cdkey inList propagandistCDKeys) }
                .empty()
        }
    }

    fun hasClaimedCDKey(uuid: UUID, cdkey: String): Boolean {
        return transaction {
            !PlayerClaimedCDKey.find { (PlayerClaimedCDKeys.pUUID eq uuid) and (PlayerClaimedCDKeys.cdkey eq cdkey) }
                .empty()
        }
    }

    fun hasClaimedCDKey(cdkey: String): Boolean {
        return transaction {
            !PlayerClaimedCDKey.find { PlayerClaimedCDKeys.cdkey eq cdkey }.empty()
        }
    }

    fun claimCDKey(uuid: UUID, cdkey: String) {
        transaction {
            PlayerClaimedCDKey.newByID {
                this.pUUID = uuid
                this.cdkey = cdkey
            }
        }
    }

    fun getPropagandistCDKeys(codeName: String): List<UUID> {
        return transaction {
            Propagandist.find { Propagandists.propagandist eq codeName }.map { it.invitee }.toList()
        }
    }

    fun handlePropagandsitCDKey(codeName: String, uuid: UUID) {
        transaction {
            Propagandist.newByID {
                this.propagandist = codeName
                this.invitee = uuid
            }
        }
    }

    fun exportCDKey(config: CDKeyConfig, valid: Boolean): String? {
        var rs: String? = null
        transaction {
            val keys =
                ConsumeCDKey.find { (ConsumeCDKeys.codeName eq config.codeName) and (ConsumeCDKeys.valid eq valid) }
                    .toList()
            if (keys.isEmpty()) {
                return@transaction
            }
            val timeSuffix = currentTime().format("yyyy-MM-dd-HH-mm-ss")
            val file = CDkey.INSTANCE.dataFolder.resolve("${timeSuffix}.txt")
            file.writeText(keys.joinToString("\r\n") {
                if (it.valid) {
                    "${it.cdkey}|valid"
                } else {
                    "${it.cdkey}|invalid"
                }
            })
            rs = file.absolutePath
        }
        return rs
    }

    fun generateCDK(config: CDKeyConfig, count: Int): Int {
        return transaction {
            var rs = 0
            val cdkeys = mutableSetOf<String>()
            cdkeys.addAll(ConfigCDK.getAllDefaultCDKey())
            repeat(count) {
                val newCDKey = getNewCDKey(cdkeys) ?: return@repeat
                rs++
                cdkeys.add(newCDKey)
                ConsumeCDKey.newByID {
                    this.codeName = config.codeName
                    this.cdkey = newCDKey
                    this.valid = true
                }
            }
            rs
        }
    }

    fun getNewCDKey(cdkeys: Set<String>): String? {
        var cdkey = randomString()
        var c = 0
        while (cdkeys.contains(cdkey) || checkRepeatCDKey(cdkey)) {
            if (c >= 10) {
                return null
            }
            cdkey = randomString()
            c++
        }
        return cdkey
    }

    fun randomString(): String {
        return RandomStringUtils.random(8, 0, 0, true, true)
    }

    fun checkRepeatCDKey(cdkey: String): Boolean {
        return transaction {
            !ConsumeCDKey.find { ConsumeCDKeys.cdkey eq cdkey }.empty()
        }
    }

}