import com.github.simonrply.model.Identifier
import com.github.simonrply.model.Node
import com.google.common.net.InetAddresses
import kotlin.system.exitProcess

fun main(args: Array<String>) {

    val port = args[0].toInt()
    val pseudo = args[1]
    val ip = InetAddresses.forString("127.0.0.1")

    val node: Node;
    if (args.size == 2) {
        node = Node.createFirstNode(pseudo, Identifier(ip, port))
    }
    else {
        val portToJoin = args[2].toInt()
        node = Node.fromNode(pseudo,  Identifier(ip, port), Identifier(ip, portToJoin))
    }

    while (true) {
        val input= readLine()!!
        when(input) {
            ":election" -> node.startElection()
            ":leave" ->  {
                node.leave()
                exitProcess(1)
            }
            else -> node.sendMessage(input)
        }
    }
}

