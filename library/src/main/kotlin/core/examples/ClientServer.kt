package core.examples

import core.node.ActorsPool
import core.node.NodeImpl
import core.node.call
import core.node.spawn

val actorRPC = NodeImpl(ActorsPool())

class ImageConverterClient {
    constructor(initialCounter: Int) {
        actorRPC.spawn(ImageConverterServer::class, ::ImageConverterServer, initialCounter)
    }

    var imageCounter: Int = 0
        get() = actorRPC.call(ImageConverterServer::class, ImageConverterServer::imageCounter)

    private fun classifyImage(image: ByteArray): Int {
        return actorRPC.call(ImageConverterServer::class, ImageConverterServer::classifyImage, image)
    }

    fun convertImage(image: ByteArray) {
        val c = classifyImage(image)
        if (c == 0) return image.set(0, image[0].inc())
    }
}

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

fun main() {
    val imageConverterClient = ImageConverterClient(6)
    val image = byteArrayOf(-5, 0, 1)

//    repeat(10) {
        imageConverterClient.convertImage(image)
//    }
    println(image.toList())
    println("Number of processed images: ${imageConverterClient.imageCounter}")
}
