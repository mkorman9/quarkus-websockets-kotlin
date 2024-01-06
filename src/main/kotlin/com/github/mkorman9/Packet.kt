package com.github.mkorman9

import jakarta.validation.constraints.NotBlank
import kotlin.reflect.KClass

data class ChatMessage(
    @field:NotBlank val text: String
)

data class DirectMessage(
    @field:NotBlank val to: String,
    @field:NotBlank val text: String
)

data class JoinRequest(
    @field:NotBlank val username: String
)

data class LeaveRequest(
    val placeholder: String?
)


enum class PacketType(
    val payload: KClass<*>
) {
    JOIN_REQUEST(JoinRequest::class),
    LEAVE_REQUEST(LeaveRequest::class),
    CHAT_MESSAGE(ChatMessage::class),
    DIRECT_MESSAGE(DirectMessage::class)
}

data class Packet(
    val type: PacketType,
    val data: Any
)
