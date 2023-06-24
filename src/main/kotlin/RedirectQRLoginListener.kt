package top.mrxiaom.qrlogin

import net.mamoe.mirai.Bot
import net.mamoe.mirai.auth.QRCodeLoginListener
import net.mamoe.mirai.utils.info
import net.mamoe.mirai.utils.warning
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class RedirectQRLoginListener : QRCodeLoginListener {
    private var tmpFile: File? = null

    override val qrCodeMargin: Int get() = 1
    override val qrCodeSize: Int get() = 1

    override fun onFetchQRCode(bot: Bot, data: ByteArray) {
        val logger = QRLogin.logger

        logger.info { "[QRCodeLogin] 已获取登录二维码，请在手机 QQ 使用账号 ${bot.id} 扫码" }
        logger.info { "[QRCodeLogin] Fetched login qrcode, please scan via qq android with account ${bot.id}." }

        data.inputStream().use { stream ->
            try {
                val isCacheEnabled = ImageIO.getUseCache()

                try {
                    ImageIO.setUseCache(false)
                    val img = ImageIO.read(stream)
                    if (img == null) {
                        logger.warning { "[QRCodeLogin] 无法创建字符图片. 请查看文件" }
                        logger.warning { "[QRCodeLogin] Failed to create char-image. Please see the file." }
                    } else {
                        logger.info { "[QRCodeLogin] \n" + img.renderQRCode() }
                    }
                } finally {
                    ImageIO.setUseCache(isCacheEnabled)
                }

            } catch (throwable: Throwable) {
                logger.warning("[QRCodeLogin] 创建字符图片时出错. 请查看文件.", throwable)
                logger.warning("[QRCodeLogin] Failed to create char-image. Please see the file.", throwable)
            }
        }
        try {
            val tempFile: File
            if (tmpFile == null) {
                tempFile = QRLogin.temp(
                    "mirai-qrcode-${bot.id}-${System.currentTimeMillis() / 1000L}.png"
                ).apply { deleteOnExit() }

                tempFile.createNewFile()

                tmpFile = tempFile
            } else {
                tempFile = tmpFile!!
            }

            tempFile.writeBytes(data)
            logger.info("[QRCodeLogin] 二维码图片如上，若看不清图片，请查看文件:")
            logger.info(tempFile.absoluteFile.toPath().toUri().toString())
        } catch (e: Exception) {
            logger.warning("[QRCodeLogin] 无法写出二维码图片. 请尽量关闭终端个性化样式后扫描二维码字符图片", e)
            logger.warning(
                "[QRCodeLogin] Failed to export qrcode image. Please try to scan the char-image after disabling custom terminal style.",
                e
            )
        }
    }

    override fun onStateChanged(bot: Bot, state: QRCodeLoginListener.State) {
        val logger = QRLogin.logger
        logger.info {
            buildString {
                append("[QRCodeLogin] ")
                when (state) {
                    QRCodeLoginListener.State.WAITING_FOR_SCAN -> append("等待扫描二维码中")
                    QRCodeLoginListener.State.WAITING_FOR_CONFIRM -> append("扫描完成，请在手机 QQ 确认登录")
                    QRCodeLoginListener.State.CANCELLED -> append("已取消登录，将会重新获取二维码")
                    QRCodeLoginListener.State.TIMEOUT -> append("扫描超时，将会重新获取二维码")
                    QRCodeLoginListener.State.CONFIRMED -> append("已确认登录")
                    else -> append("default state")
                }
            }
        }
        logger.info {
            buildString {
                append("[QRCodeLogin] ")
                when (state) {
                    QRCodeLoginListener.State.WAITING_FOR_SCAN -> append("Waiting for scanning qrcode.")
                    QRCodeLoginListener.State.WAITING_FOR_CONFIRM -> append("Scan complete. Please confirm login.")
                    QRCodeLoginListener.State.CANCELLED -> append("Login cancelled, we will try to fetch qrcode again.")
                    QRCodeLoginListener.State.TIMEOUT -> append("Timeout scanning, we will try to fetch qrcode again.")
                    QRCodeLoginListener.State.CONFIRMED -> append("Login confirmed.")
                    else -> append("default state")
                }
            }
        }

        if (state == QRCodeLoginListener.State.CONFIRMED) {
            kotlin.runCatching { tmpFile?.delete() }.onFailure { logger.warning(it) }
        }
    }

}


private fun BufferedImage.renderQRCode(
    blackPlaceholder: String = "   ",
    whitePlaceholder: String = "   ",
    doColorSwitch: Boolean = true,
): String {
    var lastStatus: Boolean? = null

    fun isBlackBlock(rgb: Int): Boolean {
        val r = rgb and 0xff0000 shr 16
        val g = rgb and 0x00ff00 shr 8
        val b = rgb and 0x0000ff

        return r < 10 && g < 10 && b < 10
    }

    val sb = StringBuilder()
    sb.append("\n")

    val BLACK = "\u001b[30;40m"
    val WHITE = "\u001b[97;107m"
    val RESET = "\u001b[0m"

    for (y in 0 until height) {
        for (x in 0 until width) {
            val rgbcolor = getRGB(x, y)
            val crtStatus = isBlackBlock(rgbcolor)

            if (doColorSwitch && crtStatus != lastStatus) {
                lastStatus = crtStatus
                sb.append(
                    if (crtStatus) BLACK else WHITE
                )
            }

            sb.append(
                if (crtStatus) blackPlaceholder else whitePlaceholder
            )
        }

        if (doColorSwitch) {
            sb.append(RESET)
        }

        sb.append("\n")
        lastStatus = null
    }

    if (doColorSwitch) {
        sb.append(RESET)
    }

    return sb.toString()
}
