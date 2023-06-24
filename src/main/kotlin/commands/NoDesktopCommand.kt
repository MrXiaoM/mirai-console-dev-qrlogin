package top.mrxiaom.qrlogin.commands

import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.utils.BotConfiguration
import top.mrxiaom.qrlogin.QRLogin

object NoDesktopCommand : SimpleCommand(
    QRLogin, "no-desktop",
    description = "热切换禁用桌面环境的状态，方便测试用",
) {
    @Handler
    suspend fun CommandSender.handle(
        toggle: Boolean? = null,
    ) {
        if (toggle == null) {
            sendMessage(
                """
                    支持桌面环境: %b
                    禁用桌面环境: %b
                """.trimIndent().format(
                    kotlin.runCatching { java.awt.Desktop.isDesktopSupported() }.getOrElse { false },
                    System.getProperty("mirai.no-desktop") != null
                ))
            return
        }
        if (toggle) {
            System.setProperty("mirai.no-desktop", "true")
            sendMessage("已禁用桌面环境")
        } else {
            System.clearProperty("mirai.no-desktop")
            sendMessage("已解除禁用桌面环境")
        }
    }
}