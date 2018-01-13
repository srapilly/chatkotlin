import com.github.simonrply.model.ChatMessage
import com.github.simonrply.model.Identifier
import com.github.simonrply.model.Node
import com.google.common.net.InetAddresses
import javafx.application.Application
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.Parent
import javafx.scene.control.TextField
import javafx.scene.layout.VBox
import tornadofx.*
import java.util.*
import kotlin.system.exitProcess

class MyApp: App(MyView::class) {

}

class MyView: View() {
    val ip = InetAddresses.forString("127.0.0.1")
    var pseudo: TextField by singleAssign()
    var port: TextField by singleAssign()
    var toJoin: TextField by singleAssign()

    override val root = vbox {
        hbox {
            label("pseudo")
            pseudo = textfield()
        }
        hbox {
            label("port")
            port = textfield()
        }
        hbox {
            label("port to join (0 for first node")
            toJoin = textfield()
        }
        button("LOGIN") {
            useMaxWidth = true
            action {
                val node: Node

                if (toJoin.text.toInt() != 0) {
                    node = Node.fromNode(pseudo.text, Identifier(ip, port.text.toInt()),Identifier(ip, toJoin.text.toInt()), true)
                }
                else {
                    node = Node.createFirstNode(pseudo.text, Identifier(ip, port.text.toInt()), true)
                }
                replaceWith(OtherView(node))
            }
        }
    }
}

class OtherView(val node: Node) : View() {
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

fun main(args: Array<String>) {
    Application.launch(MyApp::class.java, *args)
}

