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
        store.findClient(session)?.let { clientToClose ->
            store.listClients()
                .except(clientToClose)
                .broadcast(
                    "USER_LEFT",
                    JsonObject.of()
                        .put("username", clientToClose.username)
                )

            if (reason.reasonPhrase == "leaving") {
                log.info("${clientToClose.username} left")
            } else {
                log.info("${clientToClose.username} timed out")
            }
        }

        store.unregister(session)
    }

    @OnMessage
    fun onMessage(session: Session, data: String) {
        val packet = try {
            packetParser.parse(data)
        } catch (e: PacketParsingException) {
            // ignore packet
            return
        }

        when (packet.type) {
            PacketType.JOIN_REQUEST -> onJoinRequest(session, packet.data as JoinRequest)
            PacketType.LEAVE_REQUEST -> onLeaveRequest(session, packet.data as LeaveRequest)
            PacketType.CHAT_MESSAGE -> onChatMessage(session, packet.data as ChatMessage)
        }
    }

    private fun onJoinRequest(session: Session, joinRequest: JoinRequest) {
        val joiningClient = try {
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

        joiningClient.send(
            "JOIN_CONFIRMATION",
            JsonObject.of()
                .put("username", joiningClient.username)
                .put("users", store.listClients().map { c ->
                    JsonObject.of()
                        .put("username", c.username)
                })
        )

        store.listClients()
            .except(joiningClient)
            .broadcast(
                "USER_JOINED",
                JsonObject.of()
                    .put("username", joiningClient.username)
            )

        log.info("${joiningClient.username} joined")
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

        store.listClients()
            .broadcast(
                "CHAT_MESSAGE",
                JsonObject.of()
                    .put("username", client.username)
                    .put("text", chatMessage.text)
            )
    }
}
