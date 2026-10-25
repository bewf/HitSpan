package me.bewf.hitspan.util

import me.bewf.hitspan.HitSpanHooks
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket
import org.polyfrost.oneconfig.api.event.v1.eventHandler
import org.polyfrost.oneconfig.api.event.v1.events.PacketEvent

object HitSpanPackets {

    private var initialized = false

    @JvmStatic
    fun init() {
        if (initialized) return
        initialized = true
        eventHandler { event: PacketEvent.Receive -> onPacket(event.getPacket<Any>()) }
    }

    private fun onPacket(packet: Any?) {
        when (packet) {
            is ClientboundDamageEventPacket -> {
                val id = packet.entityId()
                val cause = packet.sourceCauseId()
                Minecraft.getInstance().execute { HitSpanHooks.onDamageEvent(id, cause) }
            }
            is ClientboundHurtAnimationPacket -> {
                val id = packet.id()
                Minecraft.getInstance().execute { HitSpanHooks.onHurtAnimation(id) }
            }
        }
    }
}
