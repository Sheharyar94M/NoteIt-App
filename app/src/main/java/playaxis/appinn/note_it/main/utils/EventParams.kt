package playaxis.appinn.note_it.main.utils

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer

class EventParams<out T, out U>(private val contentFirst: T, private val contentSecond: U) {

    var hasBeenHandled = false
        private set

    /**
     * Returns the content if not handled, otherwise throws an exception.
     */
    fun requireUnhandledContent(): Pair<T, U> {
        check(!hasBeenHandled)
        hasBeenHandled = true
        return Pair(contentFirst, contentSecond)
    }
}

/**
 * An [Observer] for [Event]s, simplifying the pattern of checking if the [Event]'s content has
 * already been handled.
 *
 * [onEventUnhandledContent] is *only* called if the [Event]'s contents have not been handled.
 */
class EventObserverParams<T, U>(private val onEventUnhandledContent: (T, U) -> Unit) : Observer<EventParams<T, U>> {
    override fun onChanged(value: EventParams<T, U>) {
        if (!value.hasBeenHandled) {
            val (contentFirst, contentSecond) = value.requireUnhandledContent()
            onEventUnhandledContent(contentFirst, contentSecond)
        }
    }
}

fun <T, U> LiveData<EventParams<T, U>>.observeEvent(owner: LifecycleOwner, observer: (T, U) -> Unit) {
    this.observe(owner, EventObserverParams(observer))
}

fun <T, U> MutableLiveData<EventParams<T, U>>.send(valueFirst: T, valueSecond: U) {
    this.value = EventParams(valueFirst, valueSecond)
}
