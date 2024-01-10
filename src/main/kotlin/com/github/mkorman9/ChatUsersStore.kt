package com.github.mkorman9

import io.vertx.core.json.JsonObject
import jakarta.enterprise.context.ApplicationScoped
import jakarta.websocket.Session
import java.util.concurrent.ConcurrentHashMap

data class ChatUser(
    val session: Session,
    val username: String
) {
    fun send(type: String, data: JsonObject) {
        send(session, type, data)
    }

    companion object {
        fun send(session: Session, type: String, data: JsonObject) {
            val packet = JsonObject.of()
                .put("type", type)
                .put("data", data)
            session.basicRemote.sendText(packet.encode())
        }
    }
}

data class ChatUsersList(
    private val users: Collection<ChatUser>
) {
    fun except(toExclude: ChatUser) = ChatUsersList(
        users.filter { c ->
            c.session.id != toExclude.session.id
        }
    )

    fun list(): List<ChatUser> = users.toList()

    fun broadcast(type: String, data: JsonObject) {
        users.forEach { c ->
            c.send(type, data)
        }
    }
}

@ApplicationScoped
class ChatUsersStore {
    private val activeUsers = ConcurrentHashMap<String, ChatUser>()

    fun register(session: Session, username: String): ChatUser {
        val user = ChatUser(
            session = session,
            username = username
        )

        if (activeUsers.values.any { c -> c.username == username && c.session.id != session.id }) {
            throw DuplicateUsernameException()
        }

        activeUsers[session.id] = user
        return user
    }

    fun unregister(session: Session) {
        activeUsers.remove(session.id)
    }

    fun findUser(session: Session): ChatUser? {
        return activeUsers[session.id]
    }

    fun findUserByUsername(username: String): ChatUser? {
        return activeUsers.values.find { c -> c.username == username }
    }

    val users get() = ChatUsersList(activeUsers.values)
}

class DuplicateUsernameException : RuntimeException()
