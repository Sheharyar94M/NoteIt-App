package playaxis.appinn.note_it.repository.model.converter

import androidx.room.TypeConverter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import playaxis.appinn.note_it.repository.model.entities.NoteMetadata
import playaxis.appinn.note_it.repository.model.utils.BadDataException

object NoteMetadataConverter : KSerializer<NoteMetadata> {

    private val json = Json

    @TypeConverter
    @JvmStatic
    fun toMetadata(str: String) = try {
        json.decodeFromString(NoteMetadata.serializer(), str)
    } catch (e: SerializationException) {
        throw BadDataException(cause = e)
    }

    @TypeConverter
    @JvmStatic
    fun toString(metadata: NoteMetadata) = json.encodeToString(NoteMetadata.serializer(), metadata)

    override val descriptor = PrimitiveSerialDescriptor("NoteMetadata", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: NoteMetadata) = encoder.encodeString(toString(value))

    override fun deserialize(decoder: Decoder) = toMetadata(decoder.decodeString())
}
