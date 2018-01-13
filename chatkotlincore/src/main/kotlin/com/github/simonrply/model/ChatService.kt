package com.github.simonrply.model

import com.github.simonrply.service.*
import io.grpc.stub.StreamObserver
import javafx.application.Platform
import tornadofx.runAsync
import tornadofx.runLater
import tornadofx.ui

class ChatService(private val node: Node): ChatServiceGrpc.ChatServiceImplBase() {

    override fun sendChatToLeader(request: Message, responseObserver: StreamObserver<MessageResponse>) {
        val stub = ChatServiceGrpc.newBlockingStub(node.channel)

        if (!node.isLeader) {
            logger.info { "Broadcast message to leader" }
            stub.sendChatToLeader(request)
        }
        else {
            logger.info { "new message recorded from ${request.name}: ${request.data} "}
            val message = MessageTimeStamp.newBuilder()
                    .setTimestamp(System.currentTimeMillis())
                    .setMessage(request)
                    .build()
            stub.sendChatToAll(message)
        }
        responseObserver.onNext(MessageResponse.getDefaultInstance())
        responseObserver.onCompleted()
    }

    override fun sendChatToAll(request: MessageTimeStamp, responseObserver: StreamObserver<MessageResponse>) {
        if (!node.isLeader) {
            val stub = ChatServiceGrpc.newBlockingStub(node.channel)
            stub.sendChatToAll(request)
        }
        val message = ChatMessage.fromEpoch(request.message.name, request.message.data, request.timestamp)
        logger.info { message.toString() }

        if (node.gui) {
            Platform.runLater({ node.messages.add(message) })
        }
        else {
            node.messages.add(message)
            println(message.format())
        }
        responseObserver.onNext(MessageResponse.getDefaultInstance())
        responseObserver.onCompleted()
    }

}