package playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.todo.adapter

import android.text.Editable
import android.text.TextWatcher
import playaxis.appinn.note_it.repository.model.entities.Note

/**
 * A [TextWatcher] used for adding new bullet points to a text list when user inserts a line break.
 */
class BulletTextWatcher : TextWatcher {

    private var bulletChange: ((text: Editable) -> Unit)? = null

    override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) {
//        Log.d(TAG, "beforeTextChanged(\"${text?.replace("\n".toRegex(), "\\\\n")}\", $start, $count, $after)")
    }

    override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
//        Log.d(TAG, "onTextChanged(\"${text?.replace("\n".toRegex(), "\\\\n")}\", $start, $before, $count)")
        val end = start + count - 1
        if (text != null && count - before == 1 && text[end] == '\n') {
            // User inserted a single char, a line break.
            // Find start position of current line.
            var pos = start - 1
            while (pos >= 0) {
                if (text[pos] == '\n') {
                    break
                }
                pos--
            }
            val lineStart = pos + 1

            // Check if current line used a list bullet.
            val lineText = text.substring(lineStart, end)
            val bulletMatch = BULLET_REGEX.find(lineText)
            if (bulletMatch != null) {
                val bulletText = bulletMatch.value
                bulletChange = if (lineStart + bulletText.length == end) {
                    // Last item was blank, create no new bullet and delete current one (also delete newline).
                    { it.delete(lineStart, end + 1) }
                } else {
                    // Last item wasn't blank, insert bullet text on new line.
                    { it.insert(end + 1, bulletText, 0, bulletText.length) }
                }
            }
        }
    }

    override fun afterTextChanged(text: Editable?) {
        // Apply bullet change, but set the change to null first, since applying it
        // will also result in call to afterTextChanged.
//        Log.d(TAG, "afterTextChanged(\"${text?.replace("\n".toRegex(), "\\\\n")}\")")
        val change = bulletChange
        bulletChange = null
        change?.invoke(text ?: return)
    }

    companion object {
        val BULLET_REGEX = """^\s*[${Note.BULLET_CHARS}]\s*""".toRegex()
    }
}
