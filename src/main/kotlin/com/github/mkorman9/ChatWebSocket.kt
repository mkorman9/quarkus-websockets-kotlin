package com.github.mkorman9

import io.vertx.core.json.JsonObject
import jakarta.enterprise.context.ApplicationScoped
import jakarta.websocket.CloseReason
import jakarta.websocket.OnClose
import jakarta.websocket.OnMessage
import jakarta.websocket.OnOpen
import jakarta.websocket.Session
import jakarta.websocket.server.ServerEndpoint
import org.jboss.logging.Logger

@ServerEndpoint("/ws")
@ApplicationScoped
class ChatWebSocket(
    private val log: Logger,
    private val packetParser: PacketParser,
    private val store: WebSocketClientStore
) {
    @OnOpen
    fun onOpen(session: Session) {
    }

    @OnClose
    fun onClose(session: Session, reason: CloseReason) {
        val client = store.findClient(session)
        if (client != null) {
            store.findClients()
                .filter { c -> client.session.id != c.session.id }
                .forEach { c ->
                    c.send(
                        "USER_LEFT",
                        JsonObject.of()
                            .put("username", client.username)
                    )
                }

            if (reason.reasonPhrase == "leaving") {
                log.info("${client.username} left")
            } else {
                log.info("${client.username} timed out")
            }
        }

        store.unregister(session)
    }

    @OnMessage
    fun onMessage(session: Session, data: String) {
        try {
            val packet = packetParser.parse(data)
            onPacket(session, packet)
        } catch (e: PacketParsingException) {
            // ignore packet
        }
    }

    private fun onPacket(session: Session, packet: Packet) {
        when (packet.type) {
            PacketType.JOIN_REQUEST -> onJoinRequest(session, packet.data as JoinRequest)
            PacketType.LEAVE_REQUEST -> onLeaveRequest(session, packet.data as LeaveRequest)
            PacketType.CHAT_MESSAGE -> onChatMessage(session, packet.data as ChatMessage)
        }
    }

    private fun onJoinRequest(session: Session, joinRequest: JoinRequest) {
        val client = try {
            store.register(session, joinRequest.username)
        } catch (e: RegisterException) {
            WebsocketClient.send(
                session,
                "JOIN_REJECTION",
                JsonObject.of()
                    .put("reason", e.reason)
            )
            return
        }

        client.send(
            "JOIN_CONFIRMATION",
            JsonObject.of()
                .put("username", client.username)
                .put("users", store.findClients().map { c ->
                    JsonObject.of()
                        .put("username", c.username)
                })
        )

        store.findClients()
            .filter { c -> c.session.id != session.id }
            .forEach { c ->
                c.send(
                    "USER_JOINED",
                    JsonObject.of()
                        .put("username", c.username)
                )
            }

        log.info("${client.username} joined")
    }

    private fun onLeaveRequest(session: Session, leaveRequest: LeaveRequest) {
        session.close(
            CloseReason(
                CloseReason.CloseCodes.NORMAL_CLOSURE,
                "leaving"
            )
        )
    }

    private fun onChatMessage(session: Session, chatMessage: ChatMessage) {
        val client = store.findClient(session) ?: return

        log.info("[${client.username}] ${chatMessage.text}")

        store.findClients().forEach { c ->
            c.send(
                "CHAT_MESSAGE",
                JsonObject.of()
                    .put("username", client.username)
                    .put("text", chatMessage.text)
            )
        }
    }
}
