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
    private val chatUsersStore: ChatUsersStore
) {
    @OnOpen
    fun onOpen(session: Session) {
    }

    @OnClose
    fun onClose(session: Session, reason: CloseReason) {
        chatUsersStore.findUser(session)?.let { userToClose ->
            chatUsersStore.users
                .except(userToClose)
                .broadcast(
                    "USER_LEFT",
                    JsonObject.of()
                        .put("username", userToClose.username)
                )

            if (reason.reasonPhrase == "leaving") {
                log.info("${userToClose.username} left")
            } else {
                log.info("${userToClose.username} timed out")
            }
        }

        chatUsersStore.unregister(session)
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
            PacketType.DIRECT_MESSAGE -> onDirectMessage(session, packet.data as DirectMessage)
        }
    }

    private fun onJoinRequest(session: Session, joinRequest: JoinRequest) {
        val joiningUser = try {
            chatUsersStore.register(session, joinRequest.username)
        } catch (e: RegisterException) {
            ChatUser.send(
                session,
                "JOIN_REJECTION",
                JsonObject.of()
                    .put("reason", e.reason)
            )
            return
        }

        joiningUser.send(
            "JOIN_CONFIRMATION",
            JsonObject.of()
                .put("username", joiningUser.username)
                .put("users",
                    chatUsersStore.users.list().map { c ->
                        JsonObject.of()
                            .put("username", c.username)
                    }
                )
        )

        chatUsersStore.users
            .except(joiningUser)
            .broadcast(
                "USER_JOINED",
                JsonObject.of()
                    .put("username", joiningUser.username)
            )

        log.info("${joiningUser.username} joined")
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
        val user = chatUsersStore.findUser(session) ?: return

        log.info("[${user.username}] ${chatMessage.text}")

        chatUsersStore.users
            .broadcast(
                "CHAT_MESSAGE",
                JsonObject.of()
                    .put("username", user.username)
                    .put("text", chatMessage.text)
            )
    }

    private fun onDirectMessage(session: Session, directMessage: DirectMessage) {
        val user = chatUsersStore.findUser(session) ?: return
        val targetUser = chatUsersStore.findUserByUsername(directMessage.to)
        if (targetUser == null) {
            user.send(
                "DIRECT_MESSAGE_NO_USER",
                JsonObject.of()
                    .put("username", directMessage.to)
            )
            return
        }

        log.info("[${user.username} -> ${targetUser.username}] ${directMessage.text}")

        targetUser.send(
            "DIRECT_MESSAGE",
            JsonObject.of()
                .put("from", user.username)
                .put("text", directMessage.text)
        )
    }
}
