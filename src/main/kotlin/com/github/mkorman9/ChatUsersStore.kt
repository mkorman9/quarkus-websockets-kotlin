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

    fun <R> map(func: (ChatUser) -> R): Collection<R> {
        return users.map(func)
    }

    fun broadcast(type: String, data: JsonObject) {
        users.forEach { c ->
            c.send(type, data)
        }
    }
}

@ApplicationScoped
class ChatUsersStore {
    private val users = ConcurrentHashMap<String, ChatUser>()

    fun register(session: Session, username: String): ChatUser {
        val user = ChatUser(
            session = session,
            username = username
        )

        if (users.values.any { c -> c.username == username && c.session.id != session.id }) {
            throw RegisterException("duplicate_username")
        }
        if (users.putIfAbsent(session.id, user) != null) {
            throw RegisterException("already_joined")
        }

        return user
    }

    fun unregister(session: Session) {
        users.remove(session.id)
    }

    fun findUser(session: Session): ChatUser? {
        return users[session.id]
    }

    fun listUsers(): ChatUsersList {
        return ChatUsersList(users.values)
    }
}

class RegisterException(val reason: String) : RuntimeException()
