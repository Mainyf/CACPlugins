package io.github.mainyf.socialsystem.module

import io.github.mainyf.socialsystem.storage.StorageManager
import java.util.UUID

object SocialManager {

    fun getPlayerQQNum(uuid: UUID): Long? {
        return StorageManager.getPlayerQQNum(uuid)
    }

}