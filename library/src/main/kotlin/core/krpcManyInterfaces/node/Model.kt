package core.krpcManyInterfaces.node

import kotlinx.serialization.Serializable

data class ActorsPool(val pool: MutableMap<ActorId, Any> = mutableMapOf()): MutableMap<ActorId, Any> by pool

@Serializable
data class ActorId(val id: String)
