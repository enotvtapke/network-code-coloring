package core.node

import kotlinx.rpc.annotations.Rpc
import kotlinx.serialization.ContextualSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

@Serializable
data class MethodRef(val className: String, val signature: String)

@Rpc
interface Node {
    suspend fun call(ref: MethodRef, vararg args: Any?): Any?
    suspend fun spawn(constructorRef: MethodRef, vararg args: Any?)
}

data class ActorsPool(val pool: MutableMap<String, Any> = mutableMapOf()): MutableMap<String, Any> by pool

class NodeImpl(val pool: ActorsPool) : Node {
    override suspend fun call(ref: MethodRef, vararg args: Any?): Any? {
        val actor = pool[ref.className] ?: throw IllegalArgumentException("Actor `${ref.className}` not found in the pool")
        val method = actor::class.members.find { it.signature() == ref.signature } ?: throw IllegalArgumentException("Method `$ref` not found in the actor `$actor`")
        @Suppress("UNCHECKED_CAST")
        return method.call(actor, *args)
    }

    override suspend fun spawn(constructorRef: MethodRef, vararg args: Any?) {
        val clazz = Class.forName(constructorRef.className).kotlin
        val ctor = clazz.constructors.find { it.signature() == constructorRef.signature } ?: throw IllegalArgumentException("Constructor `$constructorRef` not found")
        val instance = ctor.call(*args)
        pool[constructorRef.className] = instance
    }
}

suspend fun <T> Node.call(clazz: KClass<*>, ref: KCallable<*>, vararg args: Any?): T = call(MethodRef(clazz.qualifiedName!!, ref.signature()), *args) as T

suspend fun Node.spawn(clazz: KClass<*>, constructorRef: KFunction<*>, vararg args: Any?) =
    spawn(MethodRef(clazz.qualifiedName!!, constructorRef.signature()), *args)

fun KCallable<*>.signature() = name + parameters.joinToString(",", prefix = "(", postfix = ")") { it.type.toString() }
