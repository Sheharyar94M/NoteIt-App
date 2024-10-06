package playaxis.appinn.note_it.main.search

/**
 * Helper class to clean search queries.
 */
object SearchQueryCleaner {

    /**
     * Clean a search [query] to disable all unwanted FTS features, in order to
     * prevent crashes on malformed queries, like when quotes are missing.
     * Also adds a prefix to each search term to widen the search.
     *
     * Only balanced quotes and NOT operator using a minus `-` are allowed.
     */
    fun clean(query: String) = buildString {
        var inQuotes = false
        var terms = 0
        var inSearchTerm = false
        for (c in query) {
            when (c) {
                '"' -> {
                    inQuotes = !inQuotes
                    inSearchTerm = false
                    append('"')
                }
                '^', ':', '*', '(', ')', '\\' -> {
                    // Disable column start, column name separator, wildcard,
                    // boolean operators grouping parenthesis, escapes.
                    continue
                }
                '-' -> {
                    // equivalent to NOT operator, allowed.
                    append('-')
                }
                ' ', ',', ';' -> {
                    // Last search term has ended, add wildcard and separator
                    if (inSearchTerm) {
                        if (!inQuotes) {
                            append('*')
                        }
                        append(' ')
                        inSearchTerm = false
                        terms++
                    }
                }
                else -> {
                    // Lowercase char to disable MATCH keywords.
                    append(c.lowercaseChar())
                    inSearchTerm = true
                }
            }
        }
        if (inQuotes) {
            // Add missing quote
            append('"')
        } else if (inSearchTerm) {
            append("*")
            terms++
        }
        if (isNotEmpty() && first() == '-' && terms == 1) {
            // Negative term but no other, ignore negative.
            deleteCharAt(0)
        }
        if (length > 0 && this[lastIndex] == ' ') {
            deleteCharAt(lastIndex)
        }
    }
}
