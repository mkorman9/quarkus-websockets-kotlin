package com.github.mkorman9

import io.vertx.core.json.JsonObject
import jakarta.enterprise.context.ApplicationScoped
import jakarta.websocket.Session
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

data class ChatUser(
    val session: Session,
    val username: String
) {
    fun send(type: String, data: JsonObject): Boolean = send(session, type, data)

    companion object {
        fun send(session: Session, type: String, data: JsonObject): Boolean {
            val packet = JsonObject.of()
                .put("type", type)
                .put("data", data)
            return try {
                session.basicRemote.sendText(packet.encode())
                true
            } catch (_: IOException) {
                false
            }
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
    private val users = ConcurrentHashMap<String, ChatUser>()

    fun register(session: Session, username: String): ChatUser {
        val user = ChatUser(
            session = session,
            username = username
        )

        users.compute(session.id) { _, _ ->
            if (users.values.any { c -> c.username == username && c.session.id != session.id }) {
                throw DuplicateUsernameException()
            }

            user
        }

        return user
    }

    fun unregister(session: Session) {
        users.remove(session.id)
    }

    fun findBySession(session: Session): ChatUser? {
        return users[session.id]
    }

    fun findByUsername(username: String): ChatUser? {
        return users.values.find { c -> c.username == username }
    }

    val all get() = ChatUsersList(users.values)
}

class DuplicateUsernameException : RuntimeException()
