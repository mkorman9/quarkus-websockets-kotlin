package com.github.mkorman9

import com.fasterxml.jackson.annotation.JsonIgnore

interface ServerPacket {
    @JsonIgnore fun getType(): String
}

data class UserLeft(
    val username: String
) : ServerPacket {
    override fun getType() = "USER_LEFT"
}

data class JoinRejection(
    val reason: String
) : ServerPacket {
    override fun getType() = "JOIN_REJECTION"
}

data class JoinConfirmation(
    val username: String,
    val users: List<User>
) : ServerPacket {
    override fun getType() = "JOIN_CONFIRMATION"

    data class User(
        val username: String
    )
}

data class UserJoined(
    val username: String
) : ServerPacket {
    override fun getType() = "USER_JOINED"
}

data class ChatMessageDelivery(
    val username: String,
    val text: String
) : ServerPacket {
    override fun getType() = "CHAT_MESSAGE_DELIVERY"
}

data class DirectMessageDelivery(
    val from: String,
    val text: String
) : ServerPacket {
    override fun getType() = "DIRECT_MESSAGE_DELIVERY"
}

data class DirectMessageError(
    val username: String,
    val reason: String
) : ServerPacket {
    override fun getType() = "DIRECT_MESSAGE_ERROR"
}
