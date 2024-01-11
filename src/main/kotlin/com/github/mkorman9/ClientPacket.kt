package com.github.mkorman9

import jakarta.validation.constraints.NotBlank
import kotlin.reflect.KClass

interface ClientPacket

data class ChatMessage(
    @field:NotBlank val text: String
) : ClientPacket

data class DirectMessage(
    @field:NotBlank val to: String,
    @field:NotBlank val text: String
) : ClientPacket

data class JoinRequest(
    @field:NotBlank val username: String
) : ClientPacket

class LeaveRequest : ClientPacket


enum class ClientPacketType(
    val payload: KClass<out ClientPacket>
) {
    JOIN_REQUEST(JoinRequest::class),
    LEAVE_REQUEST(LeaveRequest::class),
    CHAT_MESSAGE(ChatMessage::class),
    DIRECT_MESSAGE(DirectMessage::class)
}
