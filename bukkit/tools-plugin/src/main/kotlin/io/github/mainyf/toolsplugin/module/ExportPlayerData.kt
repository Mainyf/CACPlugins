package io.github.mainyf.toolsplugin.module

import io.github.mainyf.newmclib.exts.submitTask
import io.github.mainyf.newmclib.storage.AbstractStorageManager
import io.github.mainyf.toolsplugin.ToolsPlugin
import java.util.UUID
import java.util.concurrent.CompletableFuture

object ExportPlayerData : AbstractStorageManager() {

    fun exportPlayerGroup(groupName: String, hasWriteQQNum: Boolean): CompletableFuture<String> {
        val future = CompletableFuture<String>()
        ToolsPlugin.INSTANCE.submitTask(async = true) {
            transaction {
                exec("""
                    select lp.uuid,
                           lp.username,
                           lp.primary_group
                    from luckperms_players as lp
                """.trimIndent()) { resultSet ->

                }
            }
            future.complete("")
        }
        return future
    }

    fun exportVIPPlayer(groupName: String, hasWriteQQNum: Boolean): CompletableFuture<String> {
        return CompletableFuture.completedFuture("")
    }

    data class LPPlayerData(
        val uuid: UUID,
        val username: String,
        val groupName: String
    )

}