package com.github.mkorman9

import io.vertx.core.json.JsonObject
import jakarta.enterprise.context.ApplicationScoped
import jakarta.websocket.Session
import java.util.concurrent.ConcurrentHashMap

data class WebsocketClient(
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

data class WebsocketClientList(
    private val clients: Collection<WebsocketClient>
) {
    fun except(toExclude: WebsocketClient) = WebsocketClientList(
        clients.filter { c ->
            c.session.id != toExclude.session.id
        }
    )

    fun <R> map(func: (WebsocketClient) -> R): Collection<R> {
        return clients.map(func)
    }

    fun broadcast(type: String, data: JsonObject) {
        clients.forEach { c ->
            c.send(type, data)
        }
    }
}

@ApplicationScoped
class WebSocketClientStore {
    private val clients = ConcurrentHashMap<String, WebsocketClient>()

    fun register(session: Session, username: String): WebsocketClient {
        val client = WebsocketClient(
            session = session,
            username = username
        )

        if (clients.values.any { c -> c.username == username && c.session.id != session.id }) {
            throw RegisterException("duplicate_username")
        }
        if (clients.putIfAbsent(session.id, client) != null) {
            throw RegisterException("already_joined")
        }

        return client
    }

    fun unregister(session: Session) {
        clients.remove(session.id)
    }

    fun findClient(session: Session): WebsocketClient? {
        return clients[session.id]
    }

    fun listClients(): WebsocketClientList {
        return WebsocketClientList(clients.values)
    }
}

class RegisterException(val reason: String) : RuntimeException()
