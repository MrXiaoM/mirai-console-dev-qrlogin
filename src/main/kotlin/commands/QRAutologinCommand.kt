package top.mrxiaom.qrlogin.commands

import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.command.descriptor.buildCommandArgumentContext
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import top.mrxiaom.qrlogin.QRAutoLoginConfig
import top.mrxiaom.qrlogin.QRLogin

/**
 * https://github.com/mamoe/mirai/blob/dev/mirai-console/backend/mirai-console/src/command/BuiltInCommands.kt
 */
@OptIn(ConsoleExperimentalApi::class)
object QRAutoLoginCommand : CompositeCommand(
    QRLogin, "qrAutoLogin",
    description = "自动登录设置(二维码登录)",
    overrideContext = buildCommandArgumentContext {
        QRAutoLoginConfig.Account.ConfigurationKey::class with QRAutoLoginConfig.Account.ConfigurationKey.Parser
    }
) {
    @Description("查看(扫码登录)自动登录账号列表")
    @SubCommand
    suspend fun CommandSender.list() {
        val config = QRAutoLoginConfig
        sendMessage(buildString {
            for (account in config.accounts) {
                if (account.account == "123456") continue
                append("- ")
                append("账号: ")
                append(account.account)
                appendLine()

                if (account.configuration.isNotEmpty()) {
                    appendLine("  配置:")
                    for ((key, value) in account.configuration) {
                        append("    $key = $value")
                    }
                    appendLine()
                }
            }
        })
    }

    @Description("添加(扫码登录)自动登录")
    @SubCommand
    suspend fun CommandSender.add(account: Long) {
        val config = QRAutoLoginConfig
        val accountStr = account.toString()
        if (config.accounts.any { it.account == accountStr }) {
            sendMessage("已有相同账号在自动登录配置中. 请先删除该账号.")
            return
        }
        config.accounts.add(QRAutoLoginConfig.Account(accountStr))
        sendMessage("已成功添加 '$account'.")
    }

    @Description("清除(扫码登录)自动登录的所有配置")
    @SubCommand
    suspend fun CommandSender.clear() {
        val config = QRAutoLoginConfig
        config.accounts.clear()
        sendMessage("已清除所有自动登录配置.")
    }

    @Description("删除一个(扫码登录)自动登录账号")
    @SubCommand
    suspend fun CommandSender.remove(account: Long) {
        val config = QRAutoLoginConfig
        val accountStr = account.toString()
        if (config.accounts.removeIf { it.account == accountStr }) {
            sendMessage("已成功删除 '$account'.")
            return
        }
        sendMessage("账号 '$account' 未配置自动登录.")
    }

    @Description("设置一个账号(扫码登录)自动登录的一个配置项")
    @SubCommand
    suspend fun CommandSender.setConfig(
        account: Long,
        configKey: QRAutoLoginConfig.Account.ConfigurationKey,
        value: String
    ) {
        val config = QRAutoLoginConfig
        val accountStr = account.toString()

        val oldAccount = config.accounts.find { it.account == accountStr } ?: kotlin.run {
            sendMessage("未找到账号 $account.")
            return
        }

        if (value.isEmpty()) return removeConfig(account, configKey)

        val newAccount = oldAccount.copy(configuration = oldAccount.configuration.toMutableMap().apply {
            put(configKey, value)
        })

        config.accounts.remove(oldAccount)
        config.accounts.add(newAccount)

        sendMessage("成功修改 '$account' 的配置 '$configKey' 为 '$value'")
    }

    @Description("删除一个账号(扫码登录)自动登录的一个配置项")
    @SubCommand
    suspend fun CommandSender.removeConfig(account: Long, configKey: QRAutoLoginConfig.Account.ConfigurationKey) {
        val config = QRAutoLoginConfig
        val accountStr = account.toString()

        val oldAccount = config.accounts.find { it.account == accountStr } ?: kotlin.run {
            sendMessage("未找到账号 $account.")
            return
        }

        val newAccount = oldAccount.copy(configuration = oldAccount.configuration.toMutableMap().apply {
            remove(configKey)
        })

        config.accounts.remove(oldAccount)
        config.accounts.add(newAccount)

        sendMessage("成功删除 '$account' 的配置 '$configKey'.")
    }
}
