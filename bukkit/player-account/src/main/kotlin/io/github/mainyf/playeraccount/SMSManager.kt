package io.github.mainyf.playeraccount

import com.aliyun.auth.credentials.Credential
import com.aliyun.auth.credentials.provider.StaticCredentialProvider
import com.aliyun.sdk.service.dysmsapi20170525.AsyncClient
import com.aliyun.sdk.service.dysmsapi20170525.models.SendSmsRequest
import com.aliyun.sdk.service.dysmsapi20170525.models.SendSmsResponse
import darabonba.core.client.ClientOverrideConfiguration
import io.github.mainyf.newmclib.exts.currentTime
import io.github.mainyf.newmclib.exts.msg
import io.github.mainyf.newmclib.exts.uuid
import io.github.mainyf.playeraccount.config.ConfigManager
import org.apache.commons.lang3.RandomStringUtils
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.CompletableFuture


object SMSManager {

    private val provider: StaticCredentialProvider by lazy {
        StaticCredentialProvider.create(
            Credential.builder()
                .accessKeyId(ConfigManager.accessKeyId)
                .accessKeySecret(ConfigManager.accessKeySecret)
                .build()
        )
    }

    private val client by lazy {
        AsyncClient.builder()
            .region(ConfigManager.regionID)
            .credentialsProvider(provider)
            .overrideConfiguration(
                ClientOverrideConfiguration.create()
                    .setEndpointOverride("dysmsapi.aliyuncs.com")
            )
            .build()
    }

    private val playerSMSActions = mutableMapOf<UUID, SMSData>()

    fun init() {
        // init
        this.provider
        this.client
    }

    fun validateCode(player: Player, code: String): SMSData? {
        if (!playerSMSActions.containsKey(player.uuid)) return null
        val data = playerSMSActions[player.uuid]!!
        return if (data.code == code) data else null
    }

    fun send(player: Player, phoneNumbers: String) {
        if (playerSMSActions.containsKey(player.uuid)) {
            val data = playerSMSActions[player.uuid]!!
            val eTime = currentTime() - data.time
            val cooldown = 30 * 1000L
            if (eTime < cooldown) {
                player.msg("还需等待 ${(cooldown - eTime) / 1000L} 才可以再次发送")
                return
            }
        }
        val data = SMSData(RandomStringUtils.randomNumeric(6), phoneNumbers, currentTime())
        player.msg("正在发送短信")
        sendSMS(phoneNumbers, data.code).whenComplete { it, e ->
            if (e != null) {
                e.printStackTrace()
                return@whenComplete
            }
            val statusCode = it.body.code
            if (statusCode == "OK") {
                player.msg("发送成功，请查看收到的短信")
                playerSMSActions[player.uuid] = data
            } else {
                player.msg("发送失败，原因: $statusCode，请咨询管理员")
                PlayerAccount.LOGGER.info("玩家: ${player.name} 验证手机号码 $phoneNumbers 时出现错误，${statusCode} ${it.body.message} ${it.body.bizId} ${it.body.requestId}")
            }
        }
    }

    private fun sendSMS(phoneNumbers: String, code: String): CompletableFuture<SendSmsResponse> {
        val sendSmsRequest = SendSmsRequest.builder()
            .signName(ConfigManager.signName)
            .templateCode(ConfigManager.templateCode)
            .phoneNumbers(phoneNumbers)
            .templateParam("{\"code\":\"${code}\"}")
            .build()
        return client.sendSms(sendSmsRequest)
    }

    class SMSData(
        val code: String,
        val phoneNumbers: String,
        val time: Long
    )

}