package playaxis.appinn.note_it.repository.model.utils

data class SortSettings(val field: SortField, val direction: SortDirection)

enum class SortField(override val value: String) : ValueEnum<String> {
    ADDED_DATE("added_date"),
    MODIFIED_DATE("modified_date"),
    TITLE("title");

    companion object {
        fun fromValue(value: String): SortField = findValueEnum(value)
    }
}

enum class SortDirection(override val value: String) : ValueEnum<String> {
    ASCENDING("ascending"),
    DESCENDING("descending");

    companion object {
        fun fromValue(value: String): SortDirection = findValueEnum(value)
    }
}
