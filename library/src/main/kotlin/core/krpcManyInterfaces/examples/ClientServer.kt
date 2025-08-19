package core.krpcManyInterfaces.examples

import core.krpcManyInterfaces.node.ActorId
import core.krpcManyInterfaces.node.ActorsPool
import core.krpcManyInterfaces.node.RemoteNodeConfig
import core.krpcManyInterfaces.node.runNode
import kotlinx.coroutines.runBlocking
import kotlinx.rpc.annotations.Rpc
import kotlinx.rpc.withService
import kotlin.random.Random
import core.krpcManyInterfaces.examples.Server.ImageConverter as ImageConverterServer

//class ImageConverter(initialCounter: Int) {
//    var imageCounter = initialCounter
//
//    private fun classifyImage(image: ByteArray): Int {
//        imageCounter += 1
//        return if (image[0] < 0.toByte()) 0 else 1
//    }
//
//    fun convertImage(image: ByteArray) {
//        val c = classifyImage(image)
//        if (c == 0) return image.set(0, image[0].inc())
//    }
//}
//
//fun main() = runBlocking {
//    val imageConverter = ImageConverter(6)
//
//    val image = byteArrayOf(-5, 0, 1)
//
//    repeat(10) {
//        imageConverter.convertImage(image)
//    }
//    println(image.toList())
//    println(imageConverter.imageCounter)
//}

class Server {
    class ImageConverter(initialCounter: Int) {
        var imageCounter = initialCounter

        fun classifyImage(image: ByteArray): Int {
            imageCounter += 1
            return if (image[0] < 0.toByte()) 0 else 1
        }

        fun convertImage(image: ByteArray) {
            val c = classifyImage(image)
            if (c == 0) return image.set(0, image[0].inc())
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>): Unit = runBlocking {
            runNode("/serverNode", 8080, listOf()).nodeServer.start(wait = true)
        }
    }
}

class Client {
    companion object {
        @JvmStatic
        fun main(args: Array<String>): Unit = runBlocking {
            val context = runNode("/clientNode", 8080, listOf(RemoteNodeConfig("0.0.0.0", 8080, "/serverNode")))
            val imageConverter = context.linkedNodes[0].withService<ImageConverterInterface>()
            val actorId = imageConverter.imageConverter1(6)

            val image = byteArrayOf(-5, 0, 1)

            repeat(10) {
                imageConverter.convertImage(actorId, image)
            }
            println(image.toList())
            println(imageConverter.imageCounter(actorId))
        }
    }
}

@Rpc
interface ImageConverterInterface {
    suspend fun imageConverter1(initialCounter: Int): ActorId
    suspend fun convertImage(actorId: ActorId, image: ByteArray)
    suspend fun classifyImage(actorId: ActorId, image: ByteArray): Int
    suspend fun imageCounter(actorId: ActorId): Int
}

class ImageConverterInterfaceImpl(val actorsPool: ActorsPool)  : ImageConverterInterface {
    override suspend fun imageConverter1(initialCounter: Int): ActorId {
        val actorId = ActorId("ImageConverter" + Random.nextInt()) // Random is a VERY bad approach
        actorsPool.put(actorId, ImageConverterServer(initialCounter))
        return actorId
    }

    override suspend fun convertImage(actorId: ActorId, image: ByteArray) {
        return (actorsPool[actorId] as ImageConverterServer).convertImage(image)
    }

    override suspend fun classifyImage(actorId: ActorId, image: ByteArray): Int {
        return (actorsPool[actorId] as ImageConverterServer).classifyImage(image)
    }

    override suspend fun imageCounter(actorId: ActorId): Int {
        return (actorsPool[actorId] as ImageConverterServer).imageCounter
    }
}