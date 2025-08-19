package core.krpcManyInterfaces.node

import core.krpcManyInterfaces.examples.ImageConverterInterface
import core.krpcManyInterfaces.examples.ImageConverterInterfaceImpl
import io.ktor.client.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.logging.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.rpc.krpc.ktor.client.KtorRpcClient
import kotlinx.rpc.krpc.ktor.client.installKrpc
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.ktor.server.Krpc
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.json.json

data class NodeContext(val nodeServer: EmbeddedServer<*, *>, val linkedNodes: List<KtorRpcClient> = listOf())

data class RemoteNodeConfig(val host: String, val port: Int, val path: String)

fun runNode(path: String, port: Int, linkedNodes: List<RemoteNodeConfig> = listOf()): NodeContext {
    val server = embeddedServer(Netty, port = port) {
        module(path)
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
        client
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

            val actorsPool = ActorsPool()
            registerService<ImageConverterInterface> { ImageConverterInterfaceImpl(actorsPool) }
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