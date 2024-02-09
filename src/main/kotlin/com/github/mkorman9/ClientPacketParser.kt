package com.github.mkorman9

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.context.ApplicationScoped
import jakarta.validation.Validator

@ApplicationScoped
class ClientPacketParser(
    private val objectMapper: ObjectMapper,
    private val validator: Validator
) {
    fun parse(data: String): ClientPacket {
        val rawPacket = try {
            objectMapper.readValue(data, RawClientPacket::class.java)
        } catch (e: JacksonException) {
            throw PacketParsingException("Invalid JSON ${e.message}")
        }
        if (rawPacket.type == null || rawPacket.data == null) {
            throw PacketParsingException("Malformed packet")
        }

        val packet = try {
            objectMapper.convertValue(rawPacket.data, rawPacket.type.payload.java)
        } catch (e: IllegalArgumentException) {
            throw PacketParsingException("Conversion error ${e.message}")
        }

        val constraintViolations = validator.validate(packet)
        if (constraintViolations.isNotEmpty()) {
            val messages = constraintViolations.map { "${it.propertyPath} -> ${it.message}" }
            throw PacketParsingException("Validation error: [${messages.joinToString(", ")}]")
        }

        return packet
    }
}

private data class RawClientPacket(
    val type: ClientPacketType?,
    val data: Map<String, Any>?
)

class PacketParsingException(message: String) : RuntimeException(message)
