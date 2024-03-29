package com.github.mkorman9.packets

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.context.ApplicationScoped
import jakarta.websocket.Session

@ApplicationScoped
class ServerPacketSender(
   private val objectMapper: ObjectMapper
) {
    fun send(session: Session, packet: ServerPacket) {
        val rawPacket = RawServerPacket(
            type = packet.packetType(),
            data = packet
        )
        session.asyncRemote.sendText(
            objectMapper.writeValueAsString(rawPacket)
        )
    }
}

private data class RawServerPacket(
    val type: ServerPacketType,
    val data: ServerPacket
)
