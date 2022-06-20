package me.arasple.mc.trchat

import net.kyori.adventure.platform.AudienceProvider
import net.kyori.adventure.text.Component
import taboolib.common.platform.ProxyCommandSender
import java.util.*

/**
 * @author wlys
 * @since 2022/6/18 15:16
 */
interface ComponentManager {

    fun getAudienceProvider(): AudienceProvider

    fun init()

    fun release()

    /**
     * 发送玩家聊天Component
     *
     * @param receiver 接收者
     * @param component 内容
     * @param sender 发送者UUID
     */
    fun sendChatComponent(receiver: ProxyCommandSender, component: Component, sender: UUID = UUID.randomUUID())

    /**
     * 过滤Component
     *
     * @param component 原内容
     * @param maxLength 最大长度 (负数为不验证)
     * @return 过滤后内容
     */
    fun filterComponent(component: Component?, maxLength: Int = -1): Component?

}