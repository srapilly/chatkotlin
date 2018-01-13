package com.github.simonrply.model

import com.github.simonrply.service.*
import com.google.common.net.InetAddresses
import io.grpc.stub.StreamObserver

class RingService(private val node: Node): RingServiceGrpc.RingServiceImplBase() {

    override fun joinRing(request: IdentifierMessage, responseObserver: StreamObserver<JoinRingResponse>) {
        val nodeToJoin = JoinRingResponse.newBuilder().toJoinBuilder
                .setIp(InetAddresses.coerceToInteger(node.nextUID.ip))
                .setPort(node.nextUID.port)
                .build()

        val leaderNode = JoinRingResponse.newBuilder().leaderBuilder
                .setIp(InetAddresses.coerceToInteger(node.leaderUID.ip))
                .setPort(node.leaderUID.port)

        val response = JoinRingResponse.newBuilder()
                .setToJoin(nodeToJoin)
                .setLeader(leaderNode)
                .build()

        node.changeNextUID(request.ip, request.port)
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun leftRing(request: LeavingMessage, responseObserver: StreamObserver<LeftRingResponse>) {
        val nodeLeaving = Identifier.fromInteger(request.leaving.ip, request.leaving.port)

        if (node.nextUID == nodeLeaving) {
            //Change UID
            logger.info { "Node leaving his our next node : $nodeLeaving " }
            node.changeNextUID(request.following.ip, request.following.port)
            if (request.wasLeader) {
                node.startElection()
            }
        }
        else {
            val stub = RingServiceGrpc.newBlockingStub(node.channel)

            logger.info { "Broadcast leave request from : $nodeLeaving" }
            stub.leftRing(request)
        }
        //Empty response
        responseObserver.onNext(LeftRingResponse.getDefaultInstance())
        responseObserver.onCompleted()
    }
}



