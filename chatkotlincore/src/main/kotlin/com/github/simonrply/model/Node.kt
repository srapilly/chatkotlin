package com.github.simonrply.model

import com.github.simonrply.service.*
import com.google.common.net.InetAddresses
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.ServerBuilder
import kotlinx.coroutines.experimental.launch
import mu.KotlinLogging
import tornadofx.observable
import java.net.InetAddress
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal val logger = KotlinLogging.logger {}

data class Identifier(val ip: InetAddress, val port: Int) {
    companion object {
        fun fromInteger(ip: Int , port: Int): Identifier {
            return Identifier(InetAddresses.fromInteger(ip), port)
        }
    }
}

data class ChatMessage(val pseudo: String, val message: String, val time: String) {
    companion object {
        fun fromEpoch(pseudo: String, message: String, epoch: Long): ChatMessage {
            val formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            val time = Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()).format(formatter)
            return ChatMessage(pseudo, message, time)
        }
    }

    fun format(): String {
        return "$time | $pseudo: $message"
    }
}

data class Node(val pseudo: String, val selfUID: Identifier,
                var nextUID: Identifier, var leaderUID: Identifier, val gui: Boolean = false) {
    var isLeader = false
    var isParticipant = false
    val uid = selfUID.hashCode()
    var channel = updateChannel()
    val messages = mutableListOf<ChatMessage>().observable()

    init {
        launchServer()
    }

    private fun launchServer() {
        val server = ServerBuilder.forPort(selfUID.port)
                .addService(RingService(this@Node))
                .addService(ElectionService(this@Node))
                .addService(ChatService(this@Node))
                .build()
        launch {
            server.start()
            logger.info { "Server started from: ${this@Node.toString()}" }
            server.awaitTermination()
        }
    }

    fun changeNextUID(ip: Int, port: Int) {
        logger.info { "next node: ${this.nextUID.port} --> $port" }
        nextUID = Identifier(InetAddresses.fromInteger(ip), port)
        channel.shutdown()
        channel = updateChannel()

    }
    private fun updateChannel() : ManagedChannel {
        return ManagedChannelBuilder
                .forAddress(InetAddresses.toAddrString(nextUID.ip), nextUID.port)
                .usePlaintext(true)
                .build()
    }

    fun startElection() {
        val stub = ElectionServiceGrpc.newBlockingStub(channel)
        val message = ElectionMessage.newBuilder().setUid(uid).build()
        logger.info { "Start election" }
        isParticipant = true
        stub.startElection(message)
    }

    fun sendMessage(input: String) {
        val stub = ChatServiceGrpc.newBlockingStub(channel)
        val message = Message.newBuilder()
                .setName(pseudo)
                .setData(input)
                .build()
        stub.sendChatToLeader(message)
    }

    fun leave() {
        val stub     = RingServiceGrpc.newBlockingStub(channel)
        val leaving = IdentifierMessage.newBuilder()
                .setIp(InetAddresses.coerceToInteger(selfUID.ip))
                .setPort(selfUID.port)
        val next =  IdentifierMessage.newBuilder()
                .setIp(InetAddresses.coerceToInteger(nextUID.ip))
                .setPort(nextUID.port)
        val message = LeavingMessage.newBuilder()
                .setLeaving(leaving)
                .setFollowing(next)
                .setWasLeader(isLeader)
                .build()
        logger.info { "Leaving ring" }
        stub.leftRing(message)
    }


    override fun toString(): String {
        return selfUID.toString()
    }

    companion object {

        fun fromNode(pseudo: String, selfUID: Identifier, ringUID: Identifier, gui: Boolean = false): Node {
            val channel = ManagedChannelBuilder
                    .forAddress(InetAddresses.toAddrString(ringUID.ip), ringUID.port)
                    .usePlaintext(true)
                    .build()

            val request = IdentifierMessage.newBuilder()
                    .setIp(InetAddresses.coerceToInteger(selfUID.ip))
                    .setPort(selfUID.port)
                    .build()

            val stub = RingServiceGrpc.newBlockingStub(channel)
            val response = stub.joinRing(request)

            with(response) {
                val toJoin = Identifier.fromInteger(toJoin.ip, toJoin.port)
                val leader = Identifier.fromInteger(leader.ip, leader.port)
                logger.info { "next node: $toJoin" }
                return Node(pseudo, selfUID, toJoin, leader, gui)
            }
        }

        fun createFirstNode(pseudo: String, selfUID: Identifier, gui: Boolean = false): Node {
            val node = Node(pseudo, selfUID, selfUID, selfUID, gui)
            node.isLeader = true
            return node
        }
    }
}
