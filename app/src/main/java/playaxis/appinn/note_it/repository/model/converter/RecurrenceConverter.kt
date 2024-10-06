package playaxis.appinn.note_it.repository.model.converter

import androidx.room.TypeConverter
import com.maltaisn.recurpicker.Recurrence
import com.maltaisn.recurpicker.format.RRuleFormatter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializer(forClass = Recurrence::class)
object RecurrenceConverter : KSerializer<Recurrence> {

    private val rruleFormatter = RRuleFormatter()

    @TypeConverter
    @JvmStatic
    fun toRecurrence(rrule: String?) = rrule?.let { rruleFormatter.parse(it) }

    @TypeConverter
    @JvmStatic
    fun toRRule(recurrence: Recurrence?) = recurrence?.let { rruleFormatter.format(it) }

    override val descriptor = PrimitiveSerialDescriptor("Recurrence", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Recurrence) =
        encoder.encodeString(toRRule(value)!!)

    override fun deserialize(decoder: Decoder) = toRecurrence(decoder.decodeString())!!
}
