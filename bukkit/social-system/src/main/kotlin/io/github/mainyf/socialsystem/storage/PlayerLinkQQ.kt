package io.github.mainyf.socialsystem.storage

import io.github.mainyf.newmclib.env
import io.github.mainyf.newmclib.storage.BaseTable

object PlayerLinkQQs : BaseTable("t_PlayerLinkQQs_${env()}") {

    val qqNum = long("qq_num")

}