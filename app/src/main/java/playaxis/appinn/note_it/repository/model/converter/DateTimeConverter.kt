package playaxis.appinn.note_it.repository.model.converter

import androidx.room.TypeConverter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateTimeConverter : KSerializer<Date> {

    private val threadLocalDateFormat = ThreadLocal<SimpleDateFormat>()

    val dateFormat: SimpleDateFormat
        get() {
            // SimpleDateFormat is not thread-safe therefore a ThreadLocal is used here. It's only
            // used for serialization so it technically doesn't have to be thread-safe, but whatever.
            var dateFormat = threadLocalDateFormat.get()
            if (dateFormat == null) {
                dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT)
                dateFormat.timeZone = TimeZone.getTimeZone("GMT")
                threadLocalDateFormat.set(dateFormat)
            }
            return dateFormat
        }

    @TypeConverter
    @JvmStatic
    fun toDate(date: Long) = Date(date)

    @TypeConverter
    @JvmStatic
    fun toLong(date: Date) = date.time

    override val descriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Date) =
        encoder.encodeString(dateFormat.format(value))

    override fun deserialize(decoder: Decoder) = dateFormat.parse(decoder.decodeString())!!
}
