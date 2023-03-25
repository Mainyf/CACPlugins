package io.github.mainyf.itemenchantplus

import io.github.mainyf.newmclib.exts.formatYMDHMS
import java.io.File
import java.io.FileWriter
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class IEPLog(var logFile: File) {

    var executor: ExecutorService = Executors.newSingleThreadExecutor()

    fun info(text: String) {
        executor.submit {
            FileWriter(logFile, true).use {
                it.write("[${Date().formatYMDHMS()}] $text\n")
            }
        }
    }

}