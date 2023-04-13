package top.mrxiaom.qrlogin

import net.mamoe.mirai.console.MiraiConsole
import net.mamoe.mirai.console.command.*
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.console.util.SemVersion
import top.mrxiaom.qrlogin.commands.QRAutoLoginCommand
import top.mrxiaom.qrlogin.commands.QRLoginCommand

object QRLogin : KotlinPlugin(
    JvmPluginDescription(
        id = "top.mrxiaom.qrlogin",
        name = "QRLogin",
        version = "0.1.2",
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
        CommandManager.registerCommand(QRLoginCommand)
        CommandManager.registerCommand(QRAutoLoginCommand)
    }
}
