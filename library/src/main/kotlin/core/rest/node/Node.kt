package core.rest.node

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

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
