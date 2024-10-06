package playaxis.appinn.note_it.utils

/**
 * For debugging, provides a log tag in every class.
 */
inline fun debugCheck(value: Boolean, message: () -> String = { "Check failed" }) {
    check(value, message)
}

inline fun debugRequire(value: Boolean, message: () -> String = { "Failed requirement" }) {
    require(value, message)
}

