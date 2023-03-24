package top.mrxiaom.qrlogin

import net.mamoe.mirai.Bot
import net.mamoe.mirai.auth.QRCodeLoginListener
import net.mamoe.mirai.network.CustomLoginFailedException
import net.mamoe.mirai.utils.MiraiLogger
import net.mamoe.mirai.utils.info
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import javax.sound.midi.SysexMessage
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JWindow

class SwingQRLoginListener : QRCodeLoginListener {
    val logger = MiraiLogger.Factory.create(this::class, "QRLoginSolver")
    private var window: JFrame
    private var image: JLabel
    private var tempBot: Bot? = null
    private var tmpFile: File? = null
    override val qrCodeMargin: Int get() = 4
    override val qrCodeSize: Int get() = 6
    init {
        window = JFrame("扫码登录").apply {
            JFrame.setDefaultLookAndFeelDecorated(true)
            defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
            setLocationRelativeTo(null)
            if (isAlwaysOnTopSupported) isAlwaysOnTop = true
            addWindowListener(object : WindowAdapter() {
                override fun windowClosed(e: WindowEvent?) {
                    tempBot?.close(UserCancelledLoginException())?.also {
                        logger.info("用户主动取消登录")
                    }
                }
            })
        }
        image = JLabel().apply {
            verticalAlignment = JLabel.CENTER
            horizontalAlignment = JLabel.CENTER
        }
        window.add(image)
    }
    private fun updateQRCode(id: Long, img: ByteArray) {
        val icon = ImageIcon(img)
        val hasIcon = image.icon != null
        image.icon = icon
        image.setBounds(0, 0, icon.iconWidth, icon.iconHeight)
        window.title = "Bot($id) 扫码登陆 (关闭窗口取消登录)"
        window.size = Dimension((icon.iconWidth * 1.8).toInt(), (icon.iconHeight * 1.8).toInt())
        if (!hasIcon) window.setLocationRelativeTo(null)
        window.isVisible = true
    }
    override fun onFetchQRCode(bot: Bot, data: ByteArray) {
        tempBot = bot
        updateQRCode(bot.id, data)
        try {
            val tempFile: File
            if (tmpFile == null) {
                tempFile = File.createTempFile(
                    "mirai-qrcode-${bot.id}-${System.currentTimeMillis() / 1000L}",
                    ".png"
                ).apply { deleteOnExit() }

                tempFile.createNewFile()

                tmpFile = tempFile
            } else {
                tempFile = tmpFile!!
            }

            tempFile.writeBytes(data)
            logger.info { "将会在弹出窗口显示二维码图片，请在相似网络环境下使用手机QQ扫码登录。若看不清图片，请查看文件 ${tempFile.absolutePath}" }
        } catch (e: Exception) {
            logger.warning("无法写出二维码图片.", e)
        }
    }

    override fun onStateChanged(bot: Bot, state: QRCodeLoginListener.State) {
        tempBot = bot
        logger.info {
            buildString {
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
        if (state == QRCodeLoginListener.State.CONFIRMED) {
            kotlin.runCatching { tmpFile?.delete() }.onFailure { logger.warning(it) }
            tempBot = null
            window.isVisible = false
            window.dispose()
        }
    }
}
class UserCancelledLoginException : CustomLoginFailedException(true) {
    override val message: String
        get() = "用户主动取消登录"
}