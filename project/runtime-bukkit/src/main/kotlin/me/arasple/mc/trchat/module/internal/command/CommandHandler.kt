package me.arasple.mc.trchat.module.internal.command

import me.arasple.mc.trchat.module.display.menu.MenuFilterControl
import me.arasple.mc.trchat.module.internal.TrChatBukkit
import me.arasple.mc.trchat.module.internal.command.sub.CommandColor
import me.arasple.mc.trchat.module.internal.command.sub.CommandRecallMessage
import me.arasple.mc.trchat.util.data
import org.bukkit.entity.Player
import taboolib.common.platform.Platform
import taboolib.common.platform.PlatformSide
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand
import taboolib.expansion.createHelper
import taboolib.module.lang.sendLang
import taboolib.platform.util.sendLang

/**
 * CommandHandler
 * me.arasple.mc.trchat.module.internal.command
 *
 * @author ItsFlicker
 * @since 2021/8/21 12:23
 */
@PlatformSide([Platform.BUKKIT])
@CommandHeader("trchat", ["trc"], "TrChat main command", permission = "trchat.access")
object CommandHandler {

    @CommandBody(permission = "trchat.command.reload", optional = true)
    val reload = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            TrChatBukkit.reload(sender)
        }
    }

    @CommandBody(permission = "trchat.command.chatfilter", optional = true)
    val chatFilter = subCommand {
        execute { sender, _, _ ->
            MenuFilterControl.displayFor(sender)
        }
    }

    @CommandBody(permission = "trchat.command.spy", optional = true)
    val spy = subCommand {
        execute<Player> { sender, _, _ ->
            val state = sender.data.switchSpy()
            sender.sendLang(if (state) "Private-Message-Spy-On" else "Private-Message-Spy-Off")
        }
    }

    @CommandBody(permission = "trchat.command.vanish", optional = true)
    val vanish = subCommand {
        execute<Player> { sender, _, _ ->
            sender.data.switchVanish()
        }
    }

    @CommandBody(permission = "trchat.command.recallmessage", optional = true)
    val recallMessage = CommandRecallMessage.command

    @CommandBody(permission = "trchat.command.color", optional = true)
    val color = CommandColor.command

    @CommandBody
    val help = subCommand {
        createHelper()
    }

    @CommandBody
    val main = mainCommand {
        createHelper()
        incorrectSender { sender, _ ->
            sender.sendLang("Command-Not-Player")
        }
        incorrectCommand { _, _, _, _ ->
            createHelper()
        }
    }

}