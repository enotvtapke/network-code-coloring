package core.examples

import core.examples.Server.ImageConverterServer
import core.node.*
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
            nodeContext("/serverNode", 8080, listOf()).nodeServer.start(wait = true)
        }
    }
}

class Client {
    class ImageConverterClient(val node: Node) {
        val imageCounter: Int
            get() = TODO() // Getter cannot be suspendable function // actorRPC.call(ImageConverterServer::class, ImageConverterServer::imageCounter)

        private suspend fun classifyImage(image: ByteArray): Int {
            return node.call(ImageConverterServer::class, ImageConverterServer::classifyImage, image)
        }

        suspend fun convertImage(image: ByteArray) {
            val c = classifyImage(image)
            if (c == 0) return image.set(0, image[0].inc())
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>): Unit = runBlocking {
            val context = nodeContext("/clientNode", 8080, listOf(RemoteNodeConfig("0.0.0.0", 8080, "/serverNode")))
            val imageConverterClient = ImageConverterClient(context.linkedNodes[0]).also {
                context.linkedNodes[0].spawn(ImageConverterServer::class, ::ImageConverterServer, 6)
            }
            val image = byteArrayOf(-5, 0, 1)

            repeat(10) {
                imageConverterClient.convertImage(image)
            }
            println(image.toList())
        }
    }
}

//fun main() = runBlocking {
//    val imageConverterClient = ImageConverterClient(6).also {
//        actorRPC.spawn(ImageConverterServer::class, ::ImageConverterServer, 6)
//    }
//    val image = byteArrayOf(-5, 0, 1)
//
////    repeat(10) {
//        imageConverterClient.convertImage(image)
////    }
//    println(image.toList())
////    println("Number of processed images: ${imageConverterClient.imageCounter}")
//}
