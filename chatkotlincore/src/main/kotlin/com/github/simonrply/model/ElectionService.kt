package com.github.simonrply.model

import com.github.simonrply.service.ElectedMessage
import com.github.simonrply.service.ElectionMessage
import com.github.simonrply.service.ElectionServiceGrpc
import com.github.simonrply.service.Nothing
import com.google.common.net.InetAddresses
import io.grpc.stub.StreamObserver

class ElectionService(private val node: Node): ElectionServiceGrpc.ElectionServiceImplBase() {

    override fun startElection(request: ElectionMessage, responseObserver: StreamObserver<Nothing>) {
        val stub = ElectionServiceGrpc.newBlockingStub(node.channel)
        node.isLeader = false //for testing purpose
        if (node.uid < request.uid) {
            logger.info { "Forward election message" }
            node.isParticipant = true
            stub.startElection(request)
        } else if (node.uid > request.uid && node.isParticipant) {
            logger.info { "Discard election message" }
            node.isParticipant = true
        } else if (node.uid > request.uid && !node.isParticipant) {
            logger.info { "Replace and forward election message" }
            node.isParticipant = true
            val newUID = ElectionMessage.newBuilder().setUid(node.uid).build()
            stub.startElection(newUID)
        } else if (node.uid == request.uid) {
            val elected = ElectedMessage.newBuilder()
                    .setIp(InetAddresses.coerceToInteger(node.selfUID.ip))
                    .setPort(node.selfUID.port)
                    .build()
            node.isLeader = true
            node.isParticipant = false
            logger.info { "Elected, start elected message" }
            stub.sendElected(elected)
        }
        responseObserver.onNext(Nothing.getDefaultInstance())
        responseObserver.onCompleted()
    }

    override fun sendElected(request: ElectedMessage, responseObserver: StreamObserver<Nothing>) {
        val leader = Identifier.fromInteger(request.ip, request.port)
        if (leader != node.selfUID) {
            node.isParticipant = false
            node.leaderUID = leader
            logger.info { "new leader: ${node.leaderUID}" }
            val stub = ElectionServiceGrpc.newBlockingStub(node.channel)
            stub.sendElected(request)
        }
        responseObserver.onNext(Nothing.getDefaultInstance())
        responseObserver.onCompleted()
    }
}

