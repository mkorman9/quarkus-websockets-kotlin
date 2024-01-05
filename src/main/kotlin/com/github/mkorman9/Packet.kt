package com.github.mkorman9

import jakarta.validation.constraints.NotBlank
import kotlin.reflect.KClass

data class ChatMessage(
    @field:NotBlank val text: String
)

data class JoinRequest(
    val username: String
)

data class LeaveRequest(
    val placeholder: String?
)


enum class PacketType(
    val payload: KClass<*>
) {
    JOIN_REQUEST(JoinRequest::class),
    LEAVE_REQUEST(LeaveRequest::class),
    CHAT_MESSAGE(ChatMessage::class)
}

data class Packet(
    val type: PacketType,
    val data: Any
)
