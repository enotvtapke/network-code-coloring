package core.rest.examples

import core.rest.examples.Server.ImageConverterServer
import core.rest.node.MethodCall
import core.rest.node.NodeClient
import core.rest.node.RemoteNodeConfig
import core.rest.node.node
import kotlinx.coroutines.runBlocking

class Server {
    class ImageConverterServer(initialCounter: Int) {
        var imageCounter = initialCounter
            get() = field

        fun classifyImage(image: ByteArray): Int {
            imageCounter += 1
            return if (image[0] < 0.toByte()) 0 else 1
        }

        fun convertImage(image: ByteArray) {
            throw IllegalStateException("Cannot call client function from server")
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>): Unit = runBlocking {
            node("/serverNode", 8080, listOf()).nodeServer.start(wait = true)
        }
    }
}

class Client {
    class ImageConverterClient(val node: NodeClient) {
        val imageCounter: Int
            get() = TODO() // Getter cannot be suspendable function

        private suspend fun classifyImage(image: ByteArray): Int {
            return node.call(MethodCall(ImageConverterServer::class, ImageConverterServer::classifyImage, listOf(image)))
        }

        suspend fun convertImage(image: ByteArray) {
            val c = classifyImage(image)
            if (c == 0) return image.set(0, image[0].inc())
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>): Unit = runBlocking {
            val context = node("/clientNode", 8080, listOf(RemoteNodeConfig("0.0.0.0", 8080, "serverNode")))
            val imageConverterClient = ImageConverterClient(context.linkedNodes[0]).also {
                context.linkedNodes[0].spawn(MethodCall(ImageConverterServer::class, ::ImageConverterServer, listOf(6)))
            }
            val image = byteArrayOf(-5, 0, 1)

            repeat(10) {
                imageConverterClient.convertImage(image)
            }
            println(image.toList())
        }
    }
}
