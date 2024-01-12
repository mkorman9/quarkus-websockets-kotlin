package com.github.mkorman9

import com.fasterxml.jackson.annotation.JsonIgnore

enum class ServerPacketType {
    USER_LEFT,
    JOIN_REJECTION,
    JOIN_CONFIRMATION,
    USER_JOINED,
    CHAT_MESSAGE_DELIVERY,
    DIRECT_MESSAGE_DELIVERY,
    DIRECT_MESSAGE_ERROR
}

interface ServerPacket {
    @JsonIgnore fun getType(): ServerPacketType
}

data class UserLeft(
    val username: String
) : ServerPacket {
    override fun getType() = ServerPacketType.USER_LEFT
}

data class JoinRejection(
    val reason: String
) : ServerPacket {
    override fun getType() = ServerPacketType.JOIN_REJECTION
}

data class JoinConfirmation(
    val username: String,
    val users: List<User>
) : ServerPacket {
    override fun getType() = ServerPacketType.JOIN_CONFIRMATION

    data class User(
        val username: String
    )
}

data class UserJoined(
    val username: String
) : ServerPacket {
    override fun getType() = ServerPacketType.USER_JOINED
}

data class ChatMessageDelivery(
    val username: String,
    val text: String
) : ServerPacket {
    override fun getType() = ServerPacketType.CHAT_MESSAGE_DELIVERY
}

data class DirectMessageDelivery(
    val from: String,
    val text: String
) : ServerPacket {
    override fun getType() = ServerPacketType.DIRECT_MESSAGE_DELIVERY
}

data class DirectMessageError(
    val username: String,
    val reason: String
) : ServerPacket {
    override fun getType() = ServerPacketType.DIRECT_MESSAGE_ERROR
}
