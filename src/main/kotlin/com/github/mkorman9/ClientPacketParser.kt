package com.github.mkorman9

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.context.ApplicationScoped
import jakarta.validation.Validator

@ApplicationScoped
class ClientPacketParser(
    private val objectMapper: ObjectMapper,
    private val validator: Validator
) {
    fun parse(data: String): ClientPacket? {
        try {
            val rawPacket = objectMapper.readValue(data, RawClientPacket::class.java)
            if (rawPacket.type == null || rawPacket.data == null) {
                return null
            }

            val packet = objectMapper.convertValue(rawPacket.data, rawPacket.type.payload.java)
            if (validator.validate(packet).isNotEmpty()) {
                return null
            }

            return packet
        } catch (_: Exception) {
            return null
        }
    }
}

private data class RawClientPacket(
    val type: ClientPacketType?,
    val data: Map<String, Any>?
)
