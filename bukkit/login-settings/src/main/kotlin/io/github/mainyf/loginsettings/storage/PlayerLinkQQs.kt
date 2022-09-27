package io.github.mainyf.loginsettings.storage

import io.github.mainyf.newmclib.env
import io.github.mainyf.newmclib.storage.BaseTable

object PlayerLinkQQs : BaseTable("t_PlayerLinkQQs_${env()}") {

    val qqNum = long("qq_num")

}