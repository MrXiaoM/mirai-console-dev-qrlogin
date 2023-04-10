package top.mrxiaom.qrlogin

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.launch
import net.mamoe.mirai.Bot
import net.mamoe.mirai.auth.BotAuthorization
import net.mamoe.mirai.console.ConsoleFrontEndImplementation
import net.mamoe.mirai.console.MiraiConsole
import net.mamoe.mirai.console.command.*
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.console.util.SemVersion
import net.mamoe.mirai.console.util.scopeWith
import net.mamoe.mirai.message.nextMessageOrNull
import net.mamoe.mirai.utils.BotConfiguration

object QRLogin : KotlinPlugin(
    JvmPluginDescription(
        id = "top.mrxiaom.qrlogin",
        name = "QRLogin",
        version = "0.1.1",
    ) {
        author("MrXiaoM")
    }
) {
    private val enable by lazy {
        logger.warning("本插件仅在 2.15.0 或以上工作，且在正式版或先行发布版(RC)中可能无法使用")
        if (SemVersion.parseRangeRequirement("<= 2.14.0").test(MiraiConsole.version)) {
            return@lazy false
        }
        return@lazy true
    }

    @OptIn(ConsoleExperimentalApi::class)
    override fun onEnable() {
        if (!enable) return
        QRAutoLoginConfig.reload()
        QRAutoLoginConfig.runAutoLogin()
        CommandManager.registerCommand(LoginCommand)
        CommandManager.registerCommand(QRAutoLoginCommand)
    }
}

/**
 * https://github.com/mamoe/mirai/blob/dev/mirai-console/backend/mirai-console/src/internal/command/builtin/LoginCommandImpl.kt
 */
object LoginCommand : SimpleCommand(
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