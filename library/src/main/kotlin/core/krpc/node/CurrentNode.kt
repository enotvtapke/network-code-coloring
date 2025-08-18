package core.krpc.node

import io.ktor.client.HttpClient
import io.ktor.http.HttpHeaders
import io.ktor.http.encodedPath
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.logging.toLogString
import io.ktor.server.netty.*
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.request.queryString
import io.ktor.server.routing.*
import kotlinx.rpc.krpc.ktor.client.KtorRpcClient
import kotlinx.rpc.krpc.ktor.client.installKrpc
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.ktor.server.Krpc
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.withService

data class NodeContext(val nodeServer: EmbeddedServer<*, *>, val linkedNodes: List<Node> = listOf())

data class RemoteNodeConfig(val host: String, val port: Int, val path: String)

fun nodeContext(path: String, port: Int, linkedNodes: List<RemoteNodeConfig> = listOf()): NodeContext {
    val server = embeddedServer(Netty, port = port) {
        module(path)
        println("Server running")
    }

    val ktorClient = HttpClient {
        installKrpc {
            waitForServices = true
        }
    }

    val remoteNodes = linkedNodes.map { linkedNode ->
        val client: KtorRpcClient = ktorClient.rpc {
            url {
                host = linkedNode.host
                this.port = linkedNode.port
                encodedPath = linkedNode.path
            }

            rpcConfig {
                serialization {
                    json()
                }
            }
        }
        client.withService<Node>()
    }

    return NodeContext(server, remoteNodes)
}

fun Application.module(path: String) {
    configureMonitoring()
    install(Krpc)

    routing {
        rpc(path) {
            rpcConfig {
                serialization {
                    json()
                }
            }

            registerService<Node> { NodeImpl(ActorsPool()) }
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