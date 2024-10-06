package playaxis.appinn.note_it.repository.model

import javax.crypto.SecretKey

interface JsonManager {

    /**
     * Export all the app data to a JSON string.
     */
    suspend fun exportJsonData(): String

    /**
     * Import notes data from JSON, merging with existing data.
     * Returns true if import was successful, false otherwise.
     */
    suspend fun importJsonData(data: String, importKey: SecretKey? = null): ImportResult

    enum class ImportResult {
        SUCCESS,
        BAD_FORMAT,
        BAD_DATA,
        FUTURE_VERSION,
        KEY_MISSING_OR_INCORRECT,
    }
}
