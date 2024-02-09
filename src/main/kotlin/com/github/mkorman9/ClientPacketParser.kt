package com.github.mkorman9

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.context.ApplicationScoped
import jakarta.validation.Validator

@ApplicationScoped
class ClientPacketParser(
    private val objectMapper: ObjectMapper,
    private val validator: Validator
) {
    fun parse(data: String): ClientPacket {
        try {
            val rawPacket = objectMapper.readValue(data, RawClientPacket::class.java)
            if (rawPacket.type == null || rawPacket.data == null) {
                throw PacketParsingException("Malformed packet")
            }

            val packet = objectMapper.convertValue(rawPacket.data, rawPacket.type.payload.java)

            val constraintViolations = validator.validate(packet)
            if (constraintViolations.isNotEmpty()) {
                val messages = constraintViolations.map { "${it.propertyPath} -> ${it.message}" }
                throw PacketParsingException("Validation error: [${messages.joinToString(", ")}]")
            }

            return packet
        } catch (e: Exception) {
            throw PacketParsingException("Invalid JSON ${e.message}")
        }
    }
}

private data class RawClientPacket(
    val type: ClientPacketType?,
    val data: Map<String, Any>?
)

class PacketParsingException(message: String) : RuntimeException(message)
