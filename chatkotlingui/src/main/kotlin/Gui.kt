import com.github.simonrply.model.Identifier
import com.github.simonrply.model.Node
import com.google.common.net.InetAddresses
import javafx.application.Application
import javafx.scene.control.TextField
import tornadofx.*
import kotlin.system.exitProcess

class LaunchView : View() {
    val ip = InetAddresses.forString("127.0.0.1")
    var pseudo: TextField by singleAssign()
    var port: TextField by singleAssign()
    var toJoin: TextField by singleAssign()

    override val root = vbox {
        hbox {
            label("Pseudo")
            pseudo = textfield()
        }
        hbox {
            label("Port")
            port = textfield()
        }
        hbox {
            label("Port to join (0 for first node)")
            toJoin = textfield()
        }
        button("Login") {
            useMaxWidth = true
            action {
                val node = if (toJoin.text.toInt() != 0) {
                    Node.fromNode(
                            pseudo.text,
                            Identifier(ip, port.text.toInt()),
                            Identifier(ip, toJoin.text.toInt()),
                            true)
                } else {
                    Node.createFirstNode(
                            pseudo.text,
                            Identifier(ip, port.text.toInt()),
                            true)
                }
                replaceWith(ChatView(node))
            }
        }
    }
}

class ChatView(val node: Node) : View() {
    override val root = vbox {
        vbox {
            listview(node.messages) {
                cellFormat { text = it.format() }
            }
        }
        var message: TextField by singleAssign()
        vbox {
            vbox {
                message = textfield()
            }
            vbox {
                button("send") {
                    action {
                        node.sendMessage(message.text)
                    }
                }
            }
            vbox {
                button("leave") {
                    action {
                        node.leave()
                        exitProcess(1)
                    }
                }
            }
        }
    }
}

class ChatApp : App(LaunchView::class)


fun main(args: Array<String>) {
    Application.launch(ChatApp::class.java, *args)

}

