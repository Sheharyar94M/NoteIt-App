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

object StringListConverter : KSerializer<List<String>> {

    private val gson = Gson()

    @TypeConverter
    fun toImagesString(images: List<String>): String {
        return gson.toJson(images)
    }

    @TypeConverter
    fun fromImagesString(json: String): List<String>{
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, listType)
    }

    override val descriptor: SerialDescriptor get() = PrimitiveSerialDescriptor("ImagesList", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): List<String> = fromImagesString(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: List<String>) = encoder.encodeString(toImagesString(value))
}