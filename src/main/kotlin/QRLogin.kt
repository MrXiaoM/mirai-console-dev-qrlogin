package top.mrxiaom.qrlogin

import net.mamoe.mirai.console.MiraiConsole
import net.mamoe.mirai.console.command.*
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.extension.PluginComponentStorage
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.console.plugin.version
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.console.util.SemVersion
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.events.MessagePostSendEvent
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.utils.BotConfiguration.MiraiProtocol.ANDROID_WATCH
import top.mrxiaom.qrlogin.commands.QRAutoLoginCommand
import top.mrxiaom.qrlogin.commands.QRLoginCommand
import java.io.File
import kotlin.system.exitProcess

object QRLogin : KotlinPlugin(
    JvmPluginDescription(
        id = "top.mrxiaom.qrlogin",
        name = "QRLogin",
        version = BuildConstants.VERSION,
    ) {
        author("MrXiaoM")
    }
) {
    private val enable by lazy {
        logger.warning("本插件仅在 2.15.0 或以上工作，且在正式版或先行发布版(RC)中可能无法使用")
        if (SemVersion.parseRangeRequirement("<= 2.14.0").test(MiraiConsole.version)) {
            exitProcess(255)
        }
        return@lazy true
    }

    override fun PluginComponentStorage.onLoad() {
        logger.info("Plugin version: $version")
        if (!enable) return
    }
    @OptIn(ConsoleExperimentalApi::class)
    override fun onEnable() {
        if (!enable) return
        cleanTempFiles()
        QRAutoLoginConfig.reload()
        QRAutoLoginConfig.runAutoLogin()
        CommandManager.registerCommand(QRLoginCommand)
        CommandManager.registerCommand(QRAutoLoginCommand)
        val channel = globalEventChannel()
        channel.subscribeAlways<MessagePostSendEvent<Group>> {
            if (bot.configuration.protocol != ANDROID_WATCH) return@subscribeAlways
            val ids = receipt?.source?.ids ?: IntArray(0)
            if (ids.isEmpty() || ids.any { it < 0 }) {
                logger.warning("群消息发送失败，你的账号可能已被风控，详见本插件帖子")
            }
        }
    }
    override fun onDisable() {
        cleanTempFiles()
    }
    fun cleanTempFiles() {
        for (file in dataFolder.listFiles()) {
            kotlin.runCatching { file.delete() }
        }
    }
    fun temp(file: String): File {
        return File(dataFolder, file)
    }
}
