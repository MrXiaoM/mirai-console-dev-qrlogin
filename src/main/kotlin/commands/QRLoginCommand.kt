package top.mrxiaom.qrlogin.commands

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.launch
import net.mamoe.mirai.Bot
import net.mamoe.mirai.auth.BotAuthorization
import net.mamoe.mirai.console.ConsoleFrontEndImplementation
import net.mamoe.mirai.console.MiraiConsole
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CommandSenderOnMessage
import net.mamoe.mirai.console.command.ConsoleCommandSender
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.console.util.scopeWith
import net.mamoe.mirai.message.nextMessageOrNull
import net.mamoe.mirai.utils.BotConfiguration
import top.mrxiaom.qrlogin.QRAutoLoginConfig
import top.mrxiaom.qrlogin.QRLogin
import top.mrxiaom.qrlogin.setupQRCodeLoginSolver


/**
 * https://github.com/mamoe/mirai/blob/dev/mirai-console/backend/mirai-console/src/internal/command/builtin/LoginCommandImpl.kt
 */
object QRLoginCommand : SimpleCommand(
    QRLogin, "qrLogin",
    description = "扫码登录，协议可用 ANDROID_WATCH 和 MACOS，默认 ANDROID_WATCH",
) {
    private suspend fun doLogin(bot: Bot) {
        kotlin.runCatching {
            bot.login()
            this
        }.onFailure { bot.close() }.getOrThrow()
    }

    @OptIn(ConsoleExperimentalApi::class)
    @Handler
    @JvmOverloads
    suspend fun CommandSender.handle(
        @Name("qq") id: Long,
        protocol: BotConfiguration.MiraiProtocol? = null,
    ) {
        kotlin.runCatching {
            val auth = BotAuthorization.byQRCode()
            MiraiConsole.addBot(id, auth) {
                this.protocol = BotConfiguration.MiraiProtocol.ANDROID_WATCH
                setup(id, protocol)
                setupQRCodeLoginSolver()
            }.also { doLogin(it) }
        }.fold(
            onSuccess = { scopeWith(ConsoleCommandSender).sendMessage("${it.nick} ($id) Login successful") },
            onFailure = { throwable ->
                scopeWith(ConsoleCommandSender).sendMessage(
                    "Login failed: ${throwable.localizedMessage ?: throwable.message ?: throwable.toString()}" +
                            if (this is CommandSenderOnMessage<*>) {
                                MiraiConsole.launch(CoroutineName("stacktrace delayer from Login")) {
                                    fromEvent.nextMessageOrNull(60 * 1000L) { it.message.contentEquals("stacktrace") }
                                }
                                "\n 1 分钟内发送 stacktrace 以获取堆栈信息"
                            } else ""
                )

                throw throwable
            }
        )
    }

    @OptIn(ConsoleFrontEndImplementation::class, ConsoleExperimentalApi::class)
    fun BotConfiguration.setup(id: Long, protocol: BotConfiguration.MiraiProtocol?): BotConfiguration {
        val config = QRAutoLoginConfig
        val account = config.accounts.firstOrNull { it.account == id.toString() }
        if (account != null) {
            account.configuration[QRAutoLoginConfig.Account.ConfigurationKey.protocol]?.let { proto ->
                try {
                    this.protocol = BotConfiguration.MiraiProtocol.valueOf(proto.toString())
                } catch (_: Throwable) {
                    //
                }
            }
            account.configuration[QRAutoLoginConfig.Account.ConfigurationKey.heartbeatStrategy]?.let { heartStrate ->
                try {
                    this.heartbeatStrategy = BotConfiguration.HeartbeatStrategy.valueOf(heartStrate.toString())
                } catch (_: Throwable) {
                    //
                }
            }
            account.configuration[QRAutoLoginConfig.Account.ConfigurationKey.device]?.let { device ->
                fileBasedDeviceInfo(device.toString())
            }
        }
        if (protocol != null) {
            this.protocol = protocol
        }
        return this
    }
}