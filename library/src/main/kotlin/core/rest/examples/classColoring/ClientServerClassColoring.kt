package core.rest.examples.classColoring

import core.rest.node.MethodCall
import core.rest.node.RemoteNodeConfig
import core.rest.node.node
import kotlinx.coroutines.runBlocking

class ImageConverter(initialCounter: Int) {
    var imageCounter = initialCounter

    private fun classifyImage(image: ByteArray): Int {
        imageCounter += 1
        return if (image[0] < 0.toByte()) 0 else 1
    }

    fun convertImage(image: ByteArray): ByteArray {
        val c = classifyImage(image)
        if (c == 0) image[0] = image[0].inc()
        return image
    }
}

class Server {
    companion object {
        @JvmStatic
        fun main(args: Array<String>): Unit = runBlocking {
            node("/serverNode", 8080, listOf()).nodeServer.start(wait = true)
        }
    }
}

class Client {
    companion object {
        @JvmStatic
        fun main(args: Array<String>): Unit = runBlocking {
            val context = node("/clientNode", 8080, listOf(RemoteNodeConfig("0.0.0.0", 8080, "serverNode")))
            context.linkedNodes[0].spawn(MethodCall(ImageConverter::class, ::ImageConverter, listOf(6)))

            var image = byteArrayOf(-5, 0, 1)

            repeat(10) {
                image = context.linkedNodes[0].call(MethodCall(ImageConverter::class, ImageConverter::convertImage, listOf(image)))
            }
            println(image.toList())
            println(context.linkedNodes[0].call<Int>(MethodCall(ImageConverter::class, ImageConverter::imageCounter, listOf())))
        }
    }
}
