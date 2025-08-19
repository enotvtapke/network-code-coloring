package core.rest.node

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.jvm.java
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KParameter

data class ActorsPool(val pool: MutableMap<String, Any> = mutableMapOf()) : MutableMap<String, Any> by pool

class NodeServer(val pool: ActorsPool) {
    fun <T> call(methodCall: MethodCall): T {
        val qualifiedName = methodCall.`class`.java.name
        val actor = pool[qualifiedName]
            ?: throw IllegalArgumentException("Actor `$qualifiedName` not found in the pool")
        @Suppress("UNCHECKED_CAST")
        return methodCall.ref.call(actor, *methodCall.args.toTypedArray()) as T
    }

    fun spawn(methodCall: MethodCall) {
        pool[methodCall.`class`.java.name] = methodCall.ref.call(*methodCall.args.toTypedArray())!!
    }
}

class NodeClient(val client: HttpClient) {
    suspend inline fun <reified T> call(methodCall: MethodCall): T {
        return client.post("call") {
            setBody(methodCall)
        }.body()
    }

    suspend fun spawn(methodCall: MethodCall) {
        return client.post("spawn") {
            setBody(methodCall)
        }.body()
    }
}
