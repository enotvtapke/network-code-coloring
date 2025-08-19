package core.restMock.examples.classColoring

import core.restMock.node.MethodCall
import core.restMock.node.NodeClient
import core.restMock.node.RemoteNodeConfig
import core.restMock.node.node
import io.ktor.util.reflect.*
import io.mockk.coEvery
import io.mockk.mockkClass
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KVisibility

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

suspend fun <T: Any> NodeClient.spawn(`class`: KClass<T>, constructorRef: KFunction<*>, vararg args: Any?): T {
    val mock = mockkClass(`class`)
    mock::class.members.filter { it.name != "hashCode" && it.visibility == KVisibility.PUBLIC }.forEach { func ->
        coEvery {
            func.call(*(listOf<Any?>(mock) + func.parameters.drop(1).map { any(it.type.classifier as KClass<Any>) }).toTypedArray())
        } coAnswers { mockCall ->
            call(MethodCall(`class`, func, mockCall.invocation.args), TypeInfo(func.returnType.classifier as KClass<*>))
        }
    }
    spawn(MethodCall(`class`, constructorRef, args.toList()))
    return mock
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
