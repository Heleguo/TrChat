package me.arasple.mc.trchat.module.internal.listener

import me.arasple.mc.trchat.TrChat
import me.arasple.mc.trchat.module.conf.file.Filters
import me.arasple.mc.trchat.module.conf.file.Settings
import me.arasple.mc.trchat.util.color.MessageColors
import me.arasple.mc.trchat.util.parseSimple
import net.md_5.bungee.chat.ComponentSerializer
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.inventory.meta.ItemMeta
import taboolib.common.platform.Platform
import taboolib.common.platform.PlatformSide
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.adaptPlayer
import taboolib.platform.util.isAir
import taboolib.platform.util.modifyMeta

/**
 * @author ItsFlicker
 * @date 2019/8/15 21:18
 */
@PlatformSide([Platform.BUKKIT])
object ListenerAnvilChange {

    @Suppress("Deprecation")
    @SubscribeEvent(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onAnvilCraft(e: PrepareAnvilEvent) {
        val p = e.view.player
        val result = e.result

        if (e.inventory.type != InventoryType.ANVIL || result.isAir()) {
            return
        }
        result.modifyMeta<ItemMeta> {
            if (!hasDisplayName()) {
                return@modifyMeta
            }
            if (Filters.conf.getBoolean("Enable.Anvil")) {
                setDisplayName(TrChat.api().getFilterManager().filter(displayName, player = adaptPlayer(p)).filtered)
            }
            if (Settings.conf.getBoolean("Color.Anvil")) {
                if (p.hasPermission("trchat.color.simple")) {
                    setDisplayNameComponent(ComponentSerializer.parse(displayName.parseSimple().toRawMessage()))
                } else {
                    setDisplayName(MessageColors.replaceWithPermission(p, displayName, MessageColors.Type.ANVIL))
                }
            }
        }
        e.result = result
    }
}