package com.github.mkorman9

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.context.ApplicationScoped
import jakarta.websocket.Session
import java.io.IOException

@ApplicationScoped
class ServerPacketSender(
   private val objectMapper: ObjectMapper
) {
    fun send(session: Session, packet: ServerPacket): Boolean {
        val rawPacket = RawServerPacket(
            type = packet.getType(),
            data = packet
        )
        return try {
            session.basicRemote.sendText(
                objectMapper.writeValueAsString(rawPacket)
            )
            true
        } catch (_: IOException) {
            false
        }
    }
}

private data class RawServerPacket(
    val type: String,
    val data: Any
)
