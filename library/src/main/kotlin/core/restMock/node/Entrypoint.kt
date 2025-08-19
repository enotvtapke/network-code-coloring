package core.restMock.node

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.logging.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.reflect.TypeInfo
import io.mockk.coEvery
import io.mockk.mockkClass
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KVisibility
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

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

data class NodeContext(val nodeServer: EmbeddedServer<*, *>, val linkedNodes: List<NodeClient> = listOf())

data class RemoteNodeConfig(val host: String, val port: Int, val path: String)

fun node(path: String, port: Int, linkedNodes: List<RemoteNodeConfig> = listOf()): NodeContext {
    val server = embeddedServer(Netty, port = port) {
        module(path)
    }

    val remoteNodes = linkedNodes.map { linkedNode ->
        val client = HttpClient {
            defaultRequest {
                url {
                    host = linkedNode.host
                    this.port = linkedNode.port
                    val normalized = linkedNode.path.trim('/')
                    encodedPath = if (normalized.isEmpty()) "/" else "/$normalized/"
                }
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
            }
            install(ClientContentNegotiation) {
                json()
            }
            install(Logging) {
                level = LogLevel.BODY
            }
        }
        NodeClient(client)
    }

    return NodeContext(server, remoteNodes)
}

fun Application.module(path: String) {
    configureMonitoring()
    install(ServerContentNegotiation) {
        json()
    }

    val nodeServer = NodeServer(ActorsPool())
    routing {
        route(path) {
            post("/call") {
                call.respond(nodeServer.call(call.receive()))
            }
            post("/spawn") {
                val methodCall = call.receive<MethodCall>()
                nodeServer.spawn(methodCall)
                call.respond(OK)
            }
        }
    }
}

private fun Application.configureMonitoring() {
    install(CallId) {
        header(HttpHeaders.XRequestId)
        verify { callId: String ->
            callId.isNotEmpty()
        }
    }
    install(CallLogging) {
        callIdMdc("call-id")
        format { call ->
            val status = call.response.status() ?: "Unhandled"
            "${status}: ${call.request.toLogString()} ${call.request.queryString()}"
        }
    }
}