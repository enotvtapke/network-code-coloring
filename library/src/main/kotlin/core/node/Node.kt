package core.node

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

fun Node.spawn(clazz: KClass<*>, constructorRef: KFunction<*>, vararg args: Any?) =
    spawn(MethodRef(clazz.qualifiedName!!, constructorRef.signature()), *args)

fun KCallable<*>.signature() = name + parameters.joinToString(",", prefix = "(", postfix = ")") { it.type.toString() }
