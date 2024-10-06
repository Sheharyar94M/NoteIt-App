
package playaxis.appinn.note_it.repository.model.converter

import androidx.room.TypeConverter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import playaxis.appinn.note_it.repository.model.entities.NoteType

object NoteTypeConverter : KSerializer<NoteType> {

    @TypeConverter
    @JvmStatic
    fun toInt(type: NoteType) = type.value

    @TypeConverter
    @JvmStatic
    fun toType(value: Int) = NoteType.fromValue(value)

    override val descriptor = PrimitiveSerialDescriptor("NoteType", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: NoteType) = encoder.encodeInt(toInt(value))

    override fun deserialize(decoder: Decoder) = toType(decoder.decodeInt())
}
