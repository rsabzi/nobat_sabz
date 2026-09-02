package ir.sabztime.app.core

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

/**
 * سرور PHP (با PDO) مقدارهای عددی بسیاری را به‌صورت رشته‌ی JSON برمی‌گرداند
 * (مثلاً "total_earned": "10" به‌جای "total_earned": 10).
 * این سریالی‌ساز «مستحکم» هم عدد و هم رشته‌ی عددی را می‌پذیرد؛
 * مقدار نامعتبر به ۰ نگاشت می‌شود تا کل پاسخ از دست نرود.
 */
object LenientInt : KSerializer<Int> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LenientInt", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Int) = encoder.encodeInt(value)

    override fun deserialize(decoder: Decoder): Int {
        val element = (decoder as? JsonDecoder)?.decodeJsonElement() ?: return decoder.decodeInt()
        val primitive = element as? JsonPrimitive ?: return 0
        return primitive.intOrNull ?: primitive.contentOrNull?.trim()?.toIntOrNull() ?: 0
    }
}

/** همان [LenientInt] برای مقدارهای ۶۴ بیتی (telegram_id و ...) */
object LenientLong : KSerializer<Long> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LenientLong", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Long) = encoder.encodeLong(value)

    override fun deserialize(decoder: Decoder): Long {
        val element = (decoder as? JsonDecoder)?.decodeJsonElement() ?: return decoder.decodeLong()
        val primitive = element as? JsonPrimitive ?: return 0L
        return primitive.longOrNull ?: primitive.contentOrNull?.trim()?.toLongOrNull() ?: 0L
    }
}

/**
 * عدد یا رشته‌ی عددی به Double (برای فیلدهای اعشاری که ممکن است
 * به‌صورت "45.5" یا 45.5 برسند).
 */
object LenientDouble : KSerializer<Double> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LenientDouble", PrimitiveKind.DOUBLE)

    override fun serialize(encoder: Encoder, value: Double) = encoder.encodeDouble(value)

    override fun deserialize(decoder: Decoder): Double {
        val element = (decoder as? JsonDecoder)?.decodeJsonElement() ?: return decoder.decodeDouble()
        val primitive = element as? JsonPrimitive ?: return 0.0
        return primitive.doubleOrNull ?: primitive.contentOrNull?.trim()?.toDoubleOrNull() ?: 0.0
    }
}
