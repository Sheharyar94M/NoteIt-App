package playaxis.appinn.note_it.repository.model.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import playaxis.appinn.note_it.repository.model.entities.ColorNote

object ColorNoteConverter : KSerializer<ColorNote> {

    private val gson = Gson()

    @TypeConverter
    fun fromColorNote(colorNote: ColorNote): String {
        return gson.toJson(colorNote)
    }

    @TypeConverter
    fun toColorNote(json: String): ColorNote{
        val listType = object : TypeToken<ColorNote>() {}.type
        return gson.fromJson(json, listType)
    }

    override val descriptor: SerialDescriptor get() = PrimitiveSerialDescriptor("NoteType", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ColorNote = toColorNote(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: ColorNote) = encoder.encodeString(fromColorNote(value))
}