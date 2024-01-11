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
    private val clientPacketParser: ClientPacketParser,
    private val chatUsersStore: ChatUsersStore
) {
    @OnOpen
    fun onOpen(session: Session) {
    }

    @OnClose
    fun onClose(session: Session, reason: CloseReason) {
        chatUsersStore.findBySession(session)?.let { userToClose ->
            chatUsersStore.all
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
        val packet = clientPacketParser.parse(data) ?: return
        val user = chatUsersStore.findBySession(session)

        if (user == null) {
            when (packet) {
                is JoinRequest -> onJoinRequest(session, packet)
            }
        } else {
            when (packet) {
                is LeaveRequest -> onLeaveRequest(user, packet)
                is ChatMessage -> onChatMessage(user, packet)
                is DirectMessage -> onDirectMessage(user, packet)
            }
        }
    }

    private fun onJoinRequest(session: Session, joinRequest: JoinRequest) {
        val joiningUser = try {
            chatUsersStore.register(session, joinRequest.username)
        } catch (e: DuplicateUsernameException) {
            ChatUser.send(
                session,
                "JOIN_REJECTION",
                JsonObject.of()
                    .put("reason", "duplicate_username")
            )
            return
        }

        joiningUser.send(
            "JOIN_CONFIRMATION",
            JsonObject.of()
                .put("username", joiningUser.username)
                .put("users",
                    chatUsersStore.all.list().map { c ->
                        JsonObject.of()
                            .put("username", c.username)
                    }
                )
        )

        chatUsersStore.all
            .except(joiningUser)
            .broadcast(
                "USER_JOINED",
                JsonObject.of()
                    .put("username", joiningUser.username)
            )

        log.info("${joiningUser.username} joined")
    }

    private fun onLeaveRequest(user: ChatUser, leaveRequest: LeaveRequest) {
        user.session.close(
            CloseReason(
                CloseReason.CloseCodes.NORMAL_CLOSURE,
                "leaving"
            )
        )
    }

    private fun onChatMessage(user: ChatUser, chatMessage: ChatMessage) {
        log.info("[${user.username}] ${chatMessage.text}")

        chatUsersStore.all
            .broadcast(
                "CHAT_MESSAGE",
                JsonObject.of()
                    .put("username", user.username)
                    .put("text", chatMessage.text)
            )
    }

    private fun onDirectMessage(user: ChatUser, directMessage: DirectMessage) {
        val targetUser = chatUsersStore.findByUsername(directMessage.to)
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
