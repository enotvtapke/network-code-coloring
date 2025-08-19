package core.restMock.node

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
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KParameter

@Serializable(with = MethodCallSerializer::class)
data class MethodCall(val `class`: KClass<*>, val ref: KCallable<*>, val args: List<Any?>)

object MethodCallSerializer : KSerializer<MethodCall> {

    @Serializable
    @SerialName("MethodCall")
    private class MethodCallSurrogate(val `class`: String, val signature: String, val args: List<String>)

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("MethodCall", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): MethodCall {
        val surrogate = decoder.decodeSerializableValue(MethodCallSurrogate.serializer())
        val kClass = Class.forName(surrogate.`class`).kotlin
        val method = (kClass.constructors + kClass.members).find { it.signature() == surrogate.signature }
            ?: throw IllegalArgumentException("Method `${surrogate.signature}` not found in the `$kClass`")
        val args =
            surrogate.args.zip(method.parameters.filter { it.kind != KParameter.Kind.INSTANCE }) { arg, argParam ->
                Json.decodeFromString(serializer(argParam.type), arg)
            }
        return MethodCall(kClass, method, args)
    }

    override fun serialize(encoder: Encoder, value: MethodCall) {
        val encodedArgs =
            value.args.zip(value.ref.parameters.filter { it.kind != KParameter.Kind.INSTANCE }) { arg, argParam ->
                Json.encodeToString(serializer(argParam.type), arg)
            }
        encoder.encodeSerializableValue(
            MethodCallSurrogate.serializer(),
            MethodCallSurrogate(value.`class`.java.name, value.ref.signature(), encodedArgs)
        )
    }
}

private fun KCallable<*>.signature() = name + parameters.joinToString(",", prefix = "(", postfix = ")") { it.type.toString() }

