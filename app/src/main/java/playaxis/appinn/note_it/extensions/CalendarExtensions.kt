package playaxis.appinn.note_it.extensions

import java.util.Calendar

/**
 * Set [this] calendar to start of day (midnight).
 */
fun Calendar.setToStartOfDay() {
    this[Calendar.HOUR_OF_DAY] = 0
    this[Calendar.MINUTE] = 0
    this[Calendar.SECOND] = 0
    this[Calendar.MILLISECOND] = 0
}