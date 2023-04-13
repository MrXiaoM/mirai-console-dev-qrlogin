package top.mrxiaom.qrlogin

import net.mamoe.mirai.Bot
import net.mamoe.mirai.auth.QRCodeLoginListener
import net.mamoe.mirai.utils.*
import java.awt.Desktop

class QRLoginSolver(
    private val parentSolver: LoginSolver
): LoginSolver() {
    val logger = MiraiLogger.Factory.create(this::class, "QRLoginSolver")
    val enable = kotlin.runCatching { Desktop.isDesktopSupported() }.getOrElse { false }
    override suspend fun onSolvePicCaptcha(bot: Bot, data: ByteArray): String? {
        return parentSolver.onSolvePicCaptcha(bot, data)
    }

    override suspend fun onSolveSliderCaptcha(bot: Bot, url: String): String? {
        return parentSolver.onSolveSliderCaptcha(bot, url)
    }

    override val isSliderCaptchaSupported: Boolean
        get() = parentSolver.isSliderCaptchaSupported

    override fun createQRCodeLoginListener(bot: Bot): QRCodeLoginListener {
        if (enable) {
            return SwingQRLoginListener(this)
        }

        return parentSolver.createQRCodeLoginListener(bot)
    }

    override suspend fun onSolveDeviceVerification(
        bot: Bot,
        requests: DeviceVerificationRequests
    ): DeviceVerificationResult {
        return parentSolver.onSolveDeviceVerification(bot, requests)
    }

    override suspend fun onSolveUnsafeDeviceLoginVerify(bot: Bot, url: String): String? {
        return parentSolver.onSolveUnsafeDeviceLoginVerify(bot, url)
    }
}

fun BotConfiguration.setupQRCodeLoginSolver() {
    loginSolver = QRLoginSolver(loginSolver ?: StandardCharImageLoginSolver())
}
