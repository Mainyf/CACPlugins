package io.github.mainyf.socialsystem.module

import io.github.mainyf.socialsystem.storage.StorageSS
import java.util.UUID

object SocialManager {

    fun getPlayerQQNum(uuid: UUID): Long? {
        return StorageSS.getPlayerQQNum(uuid)
    }

}