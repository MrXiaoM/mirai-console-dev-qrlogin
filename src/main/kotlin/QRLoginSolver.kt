package top.mrxiaom.qrlogin

import net.mamoe.mirai.Bot
import net.mamoe.mirai.auth.QRCodeLoginListener
import net.mamoe.mirai.utils.*
import java.awt.Desktop

class QRLoginSolver(
    private val parentSolver: LoginSolver,
    private val isDesktopSupported: Boolean
): LoginSolver() {
    companion object {
        val logger = MiraiLogger.Factory.create(this::class, "QRLoginSolver")
    }
    override suspend fun onSolvePicCaptcha(bot: Bot, data: ByteArray): String? {
        return parentSolver.onSolvePicCaptcha(bot, data)
    }

    override suspend fun onSolveSliderCaptcha(bot: Bot, url: String): String? {
        return parentSolver.onSolveSliderCaptcha(bot, url)
    }

    override val isSliderCaptchaSupported: Boolean
        get() = parentSolver.isSliderCaptchaSupported

    override fun createQRCodeLoginListener(bot: Bot): QRCodeLoginListener {
        return if (isDesktopSupported) SwingQRLoginListener() else RedirectQRLoginListener()
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
   if (kotlin.runCatching {
           System.getProperty("mirai.no-desktop") != null
           || !Desktop.isDesktopSupported() }.getOrElse { false }
    if (isDesktopNotSupported) {
       QRLoginSolver.logger.warning("当前没有桌面环境，将不使用窗口式扫码登录处理器。")
       return
   }
    QRLoginSolver.logger.info("登录解决器更换为 QRLoginSolver")
    loginSolver = QRLoginSolver(loginSolver ?: StandardCharImageLoginSolver(), !isDesktopNotSupported)
}
