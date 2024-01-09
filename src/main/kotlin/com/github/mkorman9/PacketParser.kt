package com.github.mkorman9

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.context.ApplicationScoped
import jakarta.validation.Validator

@ApplicationScoped
class PacketParser(
    private val objectMapper: ObjectMapper,
    private val validator: Validator
) {
    fun parse(data: String): Packet? {
        try {
            val packet = objectMapper.readValue(data, RawPacket::class.java)
            if (packet.type == null || packet.data == null) {
                return null
            }

            val payload = objectMapper.convertValue(packet.data, packet.type.payload.java)
            if (validator.validate(payload).isNotEmpty()) {
                return null
            }

            return Packet(
                type = packet.type,
                data = payload
            )
        } catch (_: Exception) {
            return null
        }
    }
}

private data class RawPacket(
    val type: PacketType?,
    val data: Map<String, Any>?
)
