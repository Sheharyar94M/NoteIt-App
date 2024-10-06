package playaxis.appinn.note_it.repository.model.converter

import androidx.room.TypeConverter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import playaxis.appinn.note_it.repository.model.entities.NoteTextFormat
import playaxis.appinn.note_it.repository.model.utils.BadDataException

object NoteTextFormatConverter : KSerializer<NoteTextFormat> {

    private val json = Json

    @TypeConverter
    @JvmStatic
    fun toTextFormat(str: String) = try {

        json.decodeFromString(NoteTextFormat.serializer(), str)
    } catch (e: SerializationException) {
        throw BadDataException(cause = e)
    }

    @TypeConverter
    @JvmStatic
    fun toString(metadata: NoteTextFormat) = json.encodeToString(NoteTextFormat.serializer(), metadata)
    override val descriptor = PrimitiveSerialDescriptor("NoteTextFormat", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: NoteTextFormat) = encoder.encodeString(toString(value))
    override fun deserialize(decoder: Decoder) = toTextFormat(decoder.decodeString())
}