package core.restMock.examples.classColoring

import core.restMock.node.RemoteNodeConfig
import core.restMock.node.node
import core.restMock.node.spawn
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
            val imageConverter = context.linkedNodes[0].spawn(ImageConverter::class, ::ImageConverter, 6)
            var image = byteArrayOf(-5, 0, 1)
            repeat(10) {
                image = imageConverter.convertImage(image)
            }
            println(image.toList())
        }
    }
}
