# chat-kotlin

## Project spec

Techonlogies used :
- kotlin : https://kotlinlang.org/
- grpc : https://grpc.io/
- maven : https://maven.apache.org/
- tornadofx : https://github.com/edvin/tornadofx
- kotlin-logging : https://github.com/MicroUtils/kotlin-logging

## How to use

Jar with all the dependencies are provided. One for the comand line interface and one for a basic graphical interface. The application is a standard Maven project, The command maven install can be used to build the entire project

### CLI

For the first node: 
java -jar cli.jar [name] [portToUse]
Example : java -jar cli.jar Simon 20001

For the subsequent node:
java -jar cli.jar [name] [portToUse] [portToJoin]
Example : java -jar cli.jar Simon 20002 20001

Message to leave the ring:
:leave
Message to start an election for testing purpose:
:election

### GUI

The gui will ask the port to use
java -jar gui.jar

## Architecture

### Ring 

Unidirectional ring : Each node know his clockwise neighbour

We can enter the ring with specifying the IP and the PORT of a node already in the ring expect for the node to enter the ring.
A node that enter the ring will get the neighbour of the node specified to enter the ring. The node that was specified to enter the ring will change his neighbours, it is now the node that just joined the ring.

When someone left the ring, it broadcast a message with his own IP:PORT and the IP:PORT of his neighbour. This message is propraged to the ring until the node that receives the message has the leaving node has a neighbour. This node will change his neighbour to the neighbour of the leaving node.


### Election

Algoriths used for the leader election : https://en.wikipedia.org/wiki/Chang_and_Roberts_algorithm
The first node of the ring is by default the leader. If the leader left the ring, a new leader will be designed.

### Message

Each message is send to the leader, then the leader send the message to each node and the timestamp providing full ordering.


## Missing requirements

A crash of a node is not implemented. 
