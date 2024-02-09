package com.github.mkorman9

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
    private val chatUsersStore: ChatUsersStore,
    private val packetSender: ServerPacketSender
) {
    @OnOpen
    fun onOpen(session: Session) {
    }

    @OnClose
    fun onClose(session: Session, reason: CloseReason) {
        chatUsersStore.findBySession(session)?.let { userToClose ->
            chatUsersStore.snapshot
                .except(userToClose)
                .broadcast(
                    UserLeft(
                        username = userToClose.username
                    )
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
            clientPacketParser.parse(data)
        } catch (e: PacketParsingException) {
            log.warn("Failed to parse packet: ${e.message}")
            return
        }

        val user = chatUsersStore.findBySession(session)

        if (user == null) {
            when (packet) {
                is JoinRequest -> onJoinRequest(session, packet)
                else -> log.warn("Unrecognized packet for JOINING state: ${packet::class.simpleName}")
            }
        } else {
            when (packet) {
                is LeaveRequest -> onLeaveRequest(user, packet)
                is ChatMessage -> onChatMessage(user, packet)
                is DirectMessage -> onDirectMessage(user, packet)
                else -> log.warn("Unrecognized packet for ACTIVE state: ${packet::class.simpleName}")
            }
        }
    }

    private fun onJoinRequest(session: Session, joinRequest: JoinRequest) {
        val joiningUser = try {
            chatUsersStore.register(session, joinRequest.username)
        } catch (e: DuplicateUsernameException) {
            packetSender.send(
                session,
                JoinRejection(
                    reason = "duplicate_username"
                )
            )
            return
        }

        joiningUser.send(
            JoinConfirmation(
                username = joiningUser.username,
                users = chatUsersStore.snapshot.list.map { c ->
                    JoinConfirmation.User(
                        username = c.username
                    )
                }
            )
        )

        chatUsersStore.snapshot
            .except(joiningUser)
            .broadcast(
                UserJoined(
                    username = joiningUser.username
                )
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

        chatUsersStore.snapshot
            .broadcast(
                ChatMessageDelivery(
                    username = user.username,
                    text = chatMessage.text
                )
            )
    }

    private fun onDirectMessage(user: ChatUser, directMessage: DirectMessage) {
        val targetUser = chatUsersStore.findByUsername(directMessage.to)
        if (targetUser == null) {
            user.send(
                DirectMessageError(
                    username = directMessage.to,
                    reason = "no_user"
                )
            )
            return
        }

        log.info("[${user.username} -> ${targetUser.username}] ${directMessage.text}")

        targetUser.send(
            DirectMessageDelivery(
                from = user.username,
                text = directMessage.text
            )
        )
    }
}
