# mirai-console-dev-qrlogin

mirai-console 的扫码登录命令 临时实现。

## 注意事项

扫码登录暂无预发行版本或稳定版本支持，仅有开发版支持，可能会有较明显的 bug。

## 这是什么

最近二维码登录的 PR 合并了，但是我并没有找到登录命令，所以我打算写一份暂时用着。为了较快地能写出一套登录命令，大部分代码是抄的mirai原来的自动登录系统。

## 用法

在 mirai 版本大于或等于 `2.15.0-dev-27` 的控制台中安装该插件，如果你不会下载开发版本，可在本文末尾加群下载一键包。

安装插件后启动，在控制台输入命令
```
qrlogin <QQ号>
```
即可进行二维码登录，第一次进行二维码登录要求使用手机QQ扫码二维码确认，第二次及以后可自动使用登录会话来登录而无需扫码。建议在第一次登录后添加自动登录。

**如果看不清控制台输出的二维码图片，往上翻可以找到二维码图片文件保存位置。**

## 帮助命令
和 mirai-console 自带命令基本一致，只是命令前缀多了 `qr`，选项少了密码。
```
/qrLogin <qq> [protocol]    # 扫码登录，协议可用 ANDROID_WATCH 和 MACOS，默认 MACOS
/qrAutoLogin add <account>    # 添加(扫码登录)自动登录
/qrAutoLogin clear    # 清除(扫码登录)自动登录的所有配置
/qrAutoLogin list    # 查看(扫码登录)自动登录账号列表
/qrAutoLogin remove <account>    # 删除一个(扫码登录)自动登录账号
/qrAutoLogin removeConfig <account> <configKey>    # 删除一个账号(扫码登录)自动登录的一个配置项
/qrAutoLogin setConfig <account> <configKey> <value>    # 设置一个账号(扫码登录)自动登录的一个配置项
```

## 构建
将 `mirai-core-all`, `mirai-console` (2.15.0-dev-27 或以上版本) 放入 libs 文件夹中，然后执行
```
./gradlew build
```
## 2.15.0-dev-27 扫码登录版本下载

加入群 1047497524 在群文件 `Mirai 一键包` 文件夹下载。
