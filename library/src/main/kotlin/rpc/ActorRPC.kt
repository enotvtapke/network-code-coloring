package rpc

import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.memberFunctions

data class MethodRef(val className: String, val signature: String)

interface ActorRPC {
    fun <T> call(ref: MethodRef, vararg args: Any?): T
    fun spawn(constructorRef: MethodRef, vararg args: Any?)
}

data class ActorsPool(val pool: MutableMap<String, Any> = mutableMapOf()): MutableMap<String, Any> by pool

class ActorRPCImpl(val pool: ActorsPool) : ActorRPC {
    override fun <T> call(ref: MethodRef, vararg args: Any?): T {
        val actor = pool[ref.className] ?: throw IllegalArgumentException("Actor `${ref.className}` not found in the pool")
        val method = actor::class.members.find { it.signature() == ref.signature } ?: throw IllegalArgumentException("Method `$ref` not found in the actor `$actor`")
        @Suppress("UNCHECKED_CAST")
        return method.call(actor, *args) as T
    }

    override fun spawn(constructorRef: MethodRef, vararg args: Any?) {
        val clazz = Class.forName(constructorRef.className).kotlin
        val ctor = clazz.constructors.find { it.signature() == constructorRef.signature } ?: throw IllegalArgumentException("Constructor `$constructorRef` not found")
        val instance = ctor.call(*args)
        pool[constructorRef.className] = instance
//        @Suppress("UNCHECKED_CAST")
//        return instance as T
    }
}

fun <T> ActorRPC.call(clazz: KClass<*>, ref: KCallable<*>, vararg args: Any?): T = call(MethodRef(clazz.qualifiedName!!, ref.signature()), *args)

fun ActorRPC.spawn(clazz: KClass<*>, constructorRef: KFunction<*>, vararg args: Any?) =
    spawn(MethodRef(clazz.qualifiedName!!, constructorRef.signature()), *args)

fun KCallable<*>.signature() = name + parameters.joinToString(",", prefix = "(", postfix = ")") { it.type.toString() }

val actorRPC = ActorRPCImpl(ActorsPool())

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
