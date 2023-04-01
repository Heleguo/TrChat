package me.arasple.mc.trchat.module.display.channel

import me.arasple.mc.trchat.TrChat
import me.arasple.mc.trchat.api.event.TrChatEvent
import me.arasple.mc.trchat.api.impl.BukkitProxyManager
import me.arasple.mc.trchat.module.display.channel.obj.ChannelBindings
import me.arasple.mc.trchat.module.display.channel.obj.ChannelEvents
import me.arasple.mc.trchat.module.display.channel.obj.ChannelSettings
import me.arasple.mc.trchat.module.display.format.Format
import me.arasple.mc.trchat.module.internal.command.main.CommandReply
import me.arasple.mc.trchat.module.internal.data.ChatLogs
import me.arasple.mc.trchat.module.internal.data.PlayerData
import me.arasple.mc.trchat.module.internal.proxy.BukkitPlayers
import me.arasple.mc.trchat.module.internal.redis.RedisManager
import me.arasple.mc.trchat.module.internal.redis.TrRedisMessage
import me.arasple.mc.trchat.module.internal.service.Metrics
import me.arasple.mc.trchat.util.*
import org.bukkit.entity.Player
import taboolib.common.platform.command.command
import taboolib.common.platform.function.console
import taboolib.common.platform.function.getProxyPlayer
import taboolib.common.util.subList
import taboolib.module.chat.ComponentText
import taboolib.module.chat.Components
import taboolib.module.lang.sendLang
import taboolib.platform.util.onlinePlayers
import taboolib.platform.util.sendLang

/**
 * @author ItsFlicker
 * @since 2022/2/8 11:03
 */
class PrivateChannel(
    id: String,
    settings: ChannelSettings,
    bindings: ChannelBindings,
    events: ChannelEvents,
    val sender: List<Format>,
    val receiver: List<Format>
) : Channel(id, settings, bindings, events, emptyList()) {

    init {
        init()
    }

    override fun init() {
        onlinePlayers.filter { it.session.channel == id }.forEach {
            join(it, id, hint = false)
        }
        if (bindings.command.isNullOrEmpty()) {
            return
        }
        command(bindings.command[0], subList(bindings.command, 1), "Channel $id command", permission = settings.joinPermission) {
            execute<Player> { sender, _, _ ->
                if (sender.session.channel == this@PrivateChannel.id) {
                    quit(sender)
                } else {
                    sender.sendLang("Private-Message-No-Player")
                }
            }
            dynamic("player", optional = true) {
                suggestion<Player>(uncheck = RedisManager.enabled) { _, _ ->
                    BukkitPlayers.getPlayers().filter { !PlayerData.vanishing.contains(it) }
                }
                execute<Player> { sender, _, argument ->
                    sender.session.lastPrivateTo = BukkitPlayers.getPlayerFullName(argument)
                        ?: return@execute sender.sendLang("Command-Player-Not-Exist")
                    join(sender, this@PrivateChannel)
                }
                dynamic("message", optional = true) {
                    execute<Player> { sender, ctx, argument ->
                        BukkitPlayers.getPlayerFullName(ctx["player"])?.let {
                            sender.session.lastPrivateTo = it
                            execute(sender, argument)
                        } ?: sender.sendLang("Command-Player-Not-Exist")
                    }
                }
            }
            incorrectSender { sender, _ ->
                sender.sendLang("Command-Not-Player")
            }
        }
    }

    override fun execute(player: Player, message: String, forward: Boolean): Pair<ComponentText, ComponentText?>? {
        if (!player.checkMute()) {
            return null
        }
        if (!settings.speakCondition.pass(player)) {
            player.sendLang("Channel-No-Speak-Permission")
            return null
        }
        if (settings.filterBeforeSending && TrChat.api().getFilterManager().filter(message).sensitiveWords > 0) {
            player.sendLang("Channel-Bad-Language")
            return null
        }
        val session = player.session
        val event = TrChatEvent(this, session, message, forward)
        if (!event.call()) {
            return null
        }
        val msg = events.process(player, event.message)?.replace("{{", "\\{{") ?: return null

        val send = Components.empty()
        sender.firstOrNull { it.condition.pass(player) }?.let { format ->
            format.prefix
                .mapNotNull { prefix -> prefix.value.firstOrNull { it.condition.pass(player) }?.content?.toTextComponent(player) }
                .forEach { prefix -> send.append(prefix) }
            send.append(format.msg.createComponent(player, msg, settings.disabledFunctions, forward))
            format.suffix
                .mapNotNull { suffix -> suffix.value.firstOrNull { it.condition.pass(player) }?.content?.toTextComponent(player) }
                .forEach { suffix -> send.append(suffix) }
        } ?: return null

        val receive = Components.empty()
        receiver.firstOrNull { it.condition.pass(player) }?.let { format ->
            format.prefix
                .mapNotNull { prefix -> prefix.value.firstOrNull { it.condition.pass(player) }?.content?.toTextComponent(player) }
                .forEach { prefix -> receive.append(prefix) }
            receive.append(format.msg.createComponent(player, msg, settings.disabledFunctions, forward))
            format.suffix
                .mapNotNull { suffix -> suffix.value.firstOrNull { it.condition.pass(player) }?.content?.toTextComponent(player) }
                .forEach { suffix -> receive.append(suffix) }
        } ?: return null

        // Chat preview
        if (!forward) {
            return send to receive
        }

        // Channel event
        if (!events.send(player, session.lastPrivateTo, msg)) {
            return null
        }

        player.sendComponent(player, send)

        PlayerData.data.filterValues { it.isSpying }.entries.forEach { (_, v) ->
            v.player.player?.sendLang("Private-Message-Spy-Format", player.name, session.lastPrivateTo, msg)
        }
        console().sendLang("Private-Message-Spy-Format", player.name, session.lastPrivateTo, msg)

        CommandReply.lastMessageFrom[session.lastPrivateTo] = player.name
        ChatLogs.logPrivate(player.name, session.lastPrivateTo, message)
        Metrics.increase(0)

        if (settings.proxy && RedisManager.enabled) {
            // Redis
            RedisManager.sendMessage(TrRedisMessage(receive, player.uniqueId, target = session.lastPrivateTo))
            return send to receive
        } else if (settings.proxy && BukkitProxyManager.processor != null) {
            // BungeeCord / Velocity
            player.sendTrChatMessage(
                "SendRaw",
                session.lastPrivateTo,
                receive.toRawMessage(),
                settings.doubleTransfer.toString()
            )
            player.sendProxyLang("Private-Message-Receive", player.name)
        } else {
            getProxyPlayer(session.lastPrivateTo)?.let {
                it.sendComponent(player, receive)
                it.sendLang("Private-Message-Receive", player.name)
            }
        }

        return send to receive
    }
}