package com.github.mkorman9

import com.github.mkorman9.packets.ServerPacket
import com.github.mkorman9.packets.ServerPacketSender
import jakarta.enterprise.context.ApplicationScoped
import jakarta.websocket.Session
import java.util.concurrent.ConcurrentHashMap

data class ChatUser(
    val session: Session,
    val username: String,
    private val packetSender: ServerPacketSender
) {
    fun send(packet: ServerPacket) = packetSender.send(session, packet)
}

data class ChatUsersSnapshot(
    private val users: List<ChatUser>
) {
    fun except(toExclude: ChatUser) = ChatUsersSnapshot(
        users.filter { c ->
            c.session.id != toExclude.session.id
        }
    )

    fun broadcast(packet: ServerPacket) {
        users.forEach { c ->
            c.send(packet)
        }
    }

    val list get(): List<ChatUser> = users.toList()
}

@ApplicationScoped
class ChatUsersStore(
    private val packetSender: ServerPacketSender
) {
    private val users = ConcurrentHashMap<String, ChatUser>()

    fun register(session: Session, username: String): ChatUser {
        val user = ChatUser(
            session = session,
            username = username,
            packetSender = packetSender
        )

        users.compute(session.id) { _, _ ->
            if (users.values.any { c -> c.username == username }) {
                throw DuplicateUsernameException()
            }

            user
        }

        return user
    }

    fun unregister(session: Session) = users.remove(session.id)

    fun findBySession(session: Session): ChatUser? = users[session.id]

    fun findByUsername(username: String): ChatUser? = users.values.find { c -> c.username == username }

    val snapshot get() = ChatUsersSnapshot(users.values.toList())
}

class DuplicateUsernameException : RuntimeException()
