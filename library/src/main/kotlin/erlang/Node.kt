package erlang

import io.mockk.every
import io.mockk.mockkClass
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

data class MethodRef(val className: String, val signature: String)

interface Node {
    fun <T> call(ref: MethodRef, vararg args: Any?): T
    fun spawn(constructorRef: MethodRef, vararg args: Any?)
}

data class ActorsPool(val pool: MutableMap<String, Any> = mutableMapOf()): MutableMap<String, Any> by pool

class NodeImpl(val pool: ActorsPool) : Node {
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
    }
}

fun <T> Node.call(clazz: KClass<*>, ref: KCallable<*>, vararg args: Any?): T = call(MethodRef(clazz.qualifiedName!!, ref.signature()), *args)

fun <T: Any> Node.spawn(clazz: KClass<T>, constructorRef: KFunction<*>, vararg args: Any?): T {
    val mock = mockkClass(clazz)
    mock::class.members.filter { it.name != "hashCode" }.forEach { func ->
        every {
            func.call(*(listOf<Any?>(mock) + func.parameters.drop(1).map { any(it.type.classifier as KClass<Any>) }).toTypedArray())
        } answers { mockCall ->
            actorRPC.call(clazz, func, *mockCall.invocation.args.toTypedArray())
        }
    }
    spawn(MethodRef(clazz.qualifiedName!!, constructorRef.signature()), *args)
    return mock
}

fun KCallable<*>.signature() = name + parameters.joinToString(",", prefix = "(", postfix = ")") { it.type.toString() }

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

val actorRPC = NodeImpl(ActorsPool())

fun main() {
    val imageConverter = actorRPC.spawn(ImageConverter::class, ::ImageConverter, 6)
    val image = byteArrayOf(-5, 0, 1)
    repeat(10) {
        imageConverter.convertImage(image)
    }
    println(image.toList())
    println("Number of processed images: ${imageConverter.imageCounter}")
}
