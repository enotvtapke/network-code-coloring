package core.krpc.examples

import core.krpc.node.ActorId
import core.krpc.node.ActorsPool
import core.krpc.node.RemoteNodeConfig
import core.krpc.node.runNode
import kotlinx.coroutines.runBlocking
import kotlinx.rpc.annotations.Rpc
import kotlin.random.Random
import core.krpc.examples.Server.ImageConverter as ImageConverterServer

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
            throw IllegalStateException("Cannot call client function from server")
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
    class ImageConverter(val actorId: ActorId, val invocator: Invocator) {
        val imageCounter: Int = runBlocking { invocator.imageCounter(actorId) } // Getter cannot be suspendable function

        private suspend fun classifyImage(image: ByteArray): Int {
            return invocator.classifyImage(actorId, image)
        }

        suspend fun convertImage(image: ByteArray) {
            val c = classifyImage(image)
            if (c == 0) return image.set(0, image[0].inc())
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>): Unit = runBlocking {
            val context = runNode("/clientNode", 8080, listOf(RemoteNodeConfig("0.0.0.0", 8080, "/serverNode")))
            val imageConverter = ImageConverter(context.linkedNodes[0].imageConverter1(6), context.linkedNodes[0])

            val image = byteArrayOf(-5, 0, 1)

            repeat(10) {
                imageConverter.convertImage(image)
            }
            println(image.toList())
            println(imageConverter.imageCounter)
        }
    }
}

@Rpc
interface Spawner {
    suspend fun imageConverter1(initialCounter: Int): ActorId
}

@Rpc
interface Caller {
    suspend fun classifyImage(actorId: ActorId, image: ByteArray): Int
    suspend fun imageCounter(actorId: ActorId): Int
}

class Invocator(spawner: Spawner, caller: Caller): Spawner by spawner, Caller by caller

class CallerImpl(val actorsPool: ActorsPool) : Caller {
    override suspend fun classifyImage(actorId: ActorId, image: ByteArray): Int {
        return (actorsPool[actorId] as ImageConverterServer).classifyImage(image)
    }

    override suspend fun imageCounter(actorId: ActorId): Int {
        return (actorsPool[actorId] as ImageConverterServer).imageCounter
    }
}

class SpawnerImpl(val actorsPool: ActorsPool) : Spawner {
    override suspend fun imageConverter1(initialCounter: Int): ActorId {
        val actorId = ActorId("ImageConverter" + Random.nextInt()) // Random is a VERY bad approach
        actorsPool.put(actorId, ImageConverterServer(initialCounter))
        return actorId
    }
}
