@file:Suppress("EXPOSED_SUPER_CLASS", "INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package top.mrxiaom.qrlogin

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import net.mamoe.mirai.auth.BotAuthorization
import net.mamoe.mirai.console.MiraiConsole
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.descriptor.CommandValueArgumentParser
import net.mamoe.mirai.console.command.descriptor.InternalCommandValueArgumentParserExtensions
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.events.AutoLoginEvent
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.event.broadcast
import net.mamoe.mirai.utils.BotConfiguration
import net.mamoe.mirai.utils.MiraiInternalApi
import net.mamoe.yamlkt.Comment
import net.mamoe.yamlkt.YamlDynamicSerializer

@ConsoleExperimentalApi
@ValueDescription("自动登录配置")
object QRAutoLoginConfig : AutoSavePluginConfig("AutoLogin") {

    @Serializable
    data class Account(
        @Comment("账号, 现只支持 QQ 数字账号")
        val account: String,
        @Comment(
            """
            账号配置. 可用配置列表 (注意大小写):
            "protocol": "ANDROID_WATCH" / "MACOS"
            "device": "device.json" 
            "enable": true
            "heartbeatStrategy": "STAT_HB" / "REGISTER" / "NONE"
        """
        )
        val configuration: Map<ConfigurationKey, @Serializable(with = YamlDynamicSerializer::class) Any> = mapOf(
            ConfigurationKey.protocol to "MACOS",
            ConfigurationKey.device to "device.json",
            ConfigurationKey.enable to true,
            ConfigurationKey.heartbeatStrategy to "STAT_HB"
        ),
    ) {
        @Serializable
        @Suppress("EnumEntryName")
        enum class ConfigurationKey {
            protocol,
            device,
            enable,
            heartbeatStrategy,

            ;

            object Parser : CommandValueArgumentParser<ConfigurationKey>,
                InternalCommandValueArgumentParserExtensions<ConfigurationKey>() {
                override fun parse(raw: String, sender: CommandSender): ConfigurationKey {
                    val key = values().find { it.name.equals(raw, ignoreCase = true) }
                    if (key != null) return key
                    illegalArgument("未知配置项, 可选值: ${values().joinToString()}")
                }
            }
        }
    }

    val accounts: MutableList<Account> by value(
        mutableListOf(
            Account(
                account = "123456",
                configuration = mapOf(
                    Account.ConfigurationKey.protocol to "MACOS",
                    Account.ConfigurationKey.device to "device.json",
                    Account.ConfigurationKey.enable to true,
                    Account.ConfigurationKey.heartbeatStrategy to "STAT_HB"
                )
            )
        )
    )

    @OptIn(MiraiInternalApi::class)
    /**
     * https://github.com/mamoe/mirai/blob/dev/mirai-console/backend/mirai-console/src/internal/MiraiConsoleImplementationBridge.kt
     */
    fun runAutoLogin() {
        runBlocking {
            val mainLogger = QRLogin.logger
            val accounts = accounts.toList()
            for (account in accounts.filter {
                it.configuration[Account.ConfigurationKey.enable]?.toString()?.equals("true", true) ?: true
            }) {
                val id = kotlin.runCatching {
                    account.account.toLong()
                }.getOrElse {
                    error("Bad auto-login account: '${account.account}'")
                }
                if (id == 123456L) continue
                fun BotConfiguration.configBot() {
                    this.protocol = BotConfiguration.MiraiProtocol.MACOS
                    account.configuration[Account.ConfigurationKey.protocol]?.let { protocol ->
                        this.protocol = runCatching {
                            BotConfiguration.MiraiProtocol.valueOf(protocol.toString())
                        }.getOrElse {
                            throw IllegalArgumentException(
                                "Bad auto-login config value for `protocol` for account $id",
                                it
                            )
                        }
                    }
                    account.configuration[Account.ConfigurationKey.heartbeatStrategy]?.let { heartStrate ->
                        this.heartbeatStrategy = runCatching {
                            BotConfiguration.HeartbeatStrategy.valueOf(heartStrate.toString())
                        }.getOrElse {
                            throw IllegalArgumentException(
                                "Bad auto-login config value for `heartbeatStrategy` for account $id",
                                it
                            )
                        }
                    }
                    account.configuration[Account.ConfigurationKey.device]?.let { device ->
                        fileBasedDeviceInfo(device.toString())
                    }

                    setupQRCodeLoginSolver()
                    mainLogger.info("Auto-login ${account.account}, protocol: ${this.protocol}, heartbeatStrategy: ${this.heartbeatStrategy}")
                }

                val bot = MiraiConsole.addBot(id, BotAuthorization.byQRCode(), BotConfiguration::configBot)

                runCatching {
                    bot.login()
                }.onSuccess {
                    launch {
                        AutoLoginEvent.Success(bot = bot).broadcast()
                    }
                }.onFailure {
                    mainLogger.error(it)
                    bot.close()
                    launch {
                        AutoLoginEvent.Failure(bot = bot, cause = it).broadcast()
                    }
                }
            }
        }
    }
}