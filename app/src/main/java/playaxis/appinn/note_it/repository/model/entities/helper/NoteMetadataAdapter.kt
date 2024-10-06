package playaxis.appinn.note_it.repository.model.entities.helper

import android.util.Log
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.Json.Default.decodeFromString
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import playaxis.appinn.note_it.repository.model.entities.BlankNoteMetadata
import playaxis.appinn.note_it.repository.model.entities.ListNoteMetadata
import playaxis.appinn.note_it.repository.model.entities.NoteMetadata

class NoteMetadataAdapter : JsonSerializer<NoteMetadata>, JsonDeserializer<NoteMetadata> {

    private val json = Json {
        serializersModule = SerializersModule {
            polymorphic(NoteMetadata::class) {
                subclass(BlankNoteMetadata::class, BlankNoteMetadata.serializer())
                subclass(ListNoteMetadata::class, ListNoteMetadata.serializer())
            }
        }
        classDiscriminator = "NoteMetaData" // Change to a unique name
    }

    override fun serialize(
        src: NoteMetadata,
        typeOfSrc: java.lang.reflect.Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        val jsonString = json.encodeToString(NoteMetadata.serializer(), src)
        return JsonPrimitive(jsonString)
    }

    override fun deserialize(
        json: JsonElement?,
        typeOfT: java.lang.reflect.Type?,
        context: JsonDeserializationContext?
    ): NoteMetadata {

        val jsonString = json!!.isJsonPrimitive.toString()
        Log.i("deserialize: ",jsonString)
        return jsonString.let {
            try {
                decodeFromString(NoteMetadata.serializer(), it)
            } catch (e: Exception) {
                BlankNoteMetadata
            }
        }
    }
}