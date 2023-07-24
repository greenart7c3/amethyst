package com.vitorpamplona.amethyst.ui.screen

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vitorpamplona.amethyst.model.Account
import com.vitorpamplona.amethyst.model.LocalCache
import com.vitorpamplona.amethyst.model.Note
import com.vitorpamplona.amethyst.model.User
import com.vitorpamplona.amethyst.service.checkNotInMainThread
import com.vitorpamplona.amethyst.service.model.GenericRepostEvent
import com.vitorpamplona.amethyst.service.model.LnZapEvent
import com.vitorpamplona.amethyst.service.model.PrivateDmEvent
import com.vitorpamplona.amethyst.service.model.ReactionEvent
import com.vitorpamplona.amethyst.service.model.RepostEvent
import com.vitorpamplona.amethyst.ui.components.BundledInsert
import com.vitorpamplona.amethyst.ui.components.BundledUpdate
import com.vitorpamplona.amethyst.ui.dal.AdditiveFeedFilter
import com.vitorpamplona.amethyst.ui.dal.FeedFilter
import com.vitorpamplona.amethyst.ui.dal.NotificationFeedFilter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

@Stable
class NotificationViewModel(val account: Account) : CardFeedViewModel(NotificationFeedFilter(account)) {
    class Factory(val account: Account) : ViewModelProvider.Factory {
        override fun <NotificationViewModel : ViewModel> create(modelClass: Class<NotificationViewModel>): NotificationViewModel {
            return NotificationViewModel(account) as NotificationViewModel
        }
    }
}

@Stable
open class CardFeedViewModel(val localFilter: FeedFilter<Note>) : ViewModel() {
    private val _feedContent = MutableStateFlow<CardFeedState>(CardFeedState.Loading)
    val feedContent = _feedContent.asStateFlow()

    // Simple counter that changes when it needs to invalidate everything
    private val _scrollToTop = MutableStateFlow<Int>(0)
    val scrollToTop = _scrollToTop.asStateFlow()
    var scrolltoTopPending = false

    private var lastFeedKey: String? = null

    fun sendToTop() {
        if (scrolltoTopPending) return

        scrolltoTopPending = true
        viewModelScope.launch(Dispatchers.IO) {
            _scrollToTop.emit(_scrollToTop.value + 1)
        }
    }

    suspend fun sentToTop() {
        scrolltoTopPending = false
    }

    private var lastAccount: Account? = null
    private var lastNotes: Set<Note>? = null

    fun refresh() {
        val scope = CoroutineScope(Job() + Dispatchers.Default)
        scope.launch {
            refreshSuspended()
        }
    }

    @Synchronized
    private fun refreshSuspended() {
        checkNotInMainThread()

        val notes = localFilter.feed()
        lastFeedKey = localFilter.feedKey()

        val thisAccount = (localFilter as? NotificationFeedFilter)?.account
        val lastNotesCopy = if (thisAccount == lastAccount) lastNotes else null

        val oldNotesState = _feedContent.value
        if (lastNotesCopy != null && oldNotesState is CardFeedState.Loaded) {
            val newCards = convertToCard(notes.minus(lastNotesCopy))
            if (newCards.isNotEmpty()) {
                lastNotes = notes.toSet()
                lastAccount = (localFilter as? NotificationFeedFilter)?.account

                val updatedCards = (oldNotesState.feed.value + newCards)
                    .distinctBy { it.id() }
                    .sortedWith(compareBy({ it.createdAt() }, { it.id() }))
                    .reversed()
                    .take(localFilter.limit())
                    .toImmutableList()

                if (!equalImmutableLists(oldNotesState.feed.value, updatedCards)) {
                    updateFeed(updatedCards)
                }
            }
        } else {
            lastNotes = notes.toSet()
            lastAccount = (localFilter as? NotificationFeedFilter)?.account

            val cards = convertToCard(notes)
                .sortedWith(compareBy({ it.createdAt() }, { it.id() }))
                .reversed()
                .take(localFilter.limit())
                .toImmutableList()

            updateFeed(cards)
        }
    }

    private fun convertToCard(notes: Collection<Note>): List<Card> {
        checkNotInMainThread()

        val reactionsPerEvent = mutableMapOf<Note, MutableList<Note>>()
        notes
            .filter { it.event is ReactionEvent }
            .forEach {
                val reactedPost = it.replyTo?.lastOrNull()
                if (reactedPost != null) {
                    reactionsPerEvent.getOrPut(reactedPost, { mutableListOf() }).add(it)
                }
            }

        // val reactionCards = reactionsPerEvent.map { LikeSetCard(it.key, it.value) }
        val zapsPerUser = mutableMapOf<User, MutableList<CombinedZap>>()
        val zapsPerEvent = mutableMapOf<Note, MutableList<CombinedZap>>()
        notes
            .filter { it.event is LnZapEvent }
            .forEach { zapEvent ->
                val zappedPost = zapEvent.replyTo?.lastOrNull()
                if (zappedPost != null) {
                    val zapRequest = zappedPost.zaps.filter { it.value == zapEvent }.keys.firstOrNull()
                    if (zapRequest != null) {
                        // var newZapRequestEvent = LocalCache.checkPrivateZap(zapRequest.event as Event)
                        // zapRequest.event = newZapRequestEvent
                        zapsPerEvent.getOrPut(zappedPost, { mutableListOf() }).add(CombinedZap(zapRequest, zapEvent))
                    }
                } else {
                    val event = (zapEvent.event as LnZapEvent)
                    val author = event.zappedAuthor().firstNotNullOfOrNull {
                        LocalCache.users[it] // don't create user if it doesn't exist
                    }
                    if (author != null) {
                        val zapRequest = author.zaps.filter { it.value == zapEvent }.keys.firstOrNull()
                        if (zapRequest != null) {
                            zapsPerUser.getOrPut(author, { mutableListOf() })
                                .add(CombinedZap(zapRequest, zapEvent))
                        }
                    }
                }
            }

        val boostsPerEvent = mutableMapOf<Note, MutableList<Note>>()
        notes
            .filter { it.event is RepostEvent || it.event is GenericRepostEvent }
            .forEach {
                val boostedPost = it.replyTo?.lastOrNull()
                if (boostedPost != null) {
                    boostsPerEvent.getOrPut(boostedPost, { mutableListOf() }).add(it)
                }
            }

        val allBaseNotes = zapsPerEvent.keys + boostsPerEvent.keys + reactionsPerEvent.keys
        val multiCards = allBaseNotes.map { baseNote ->
            val boostsInCard = boostsPerEvent[baseNote] ?: emptyList()
            val reactionsInCard = reactionsPerEvent[baseNote] ?: emptyList()
            val zapsInCard = zapsPerEvent[baseNote] ?: emptyList()

            val singleList = (boostsInCard + zapsInCard.map { it.response } + reactionsInCard)
                .sortedWith(compareBy({ it.createdAt() }, { it.idHex }))
                .reversed()

            singleList.chunked(30).map { chunk ->
                MultiSetCard(
                    baseNote,
                    boostsInCard.filter { it in chunk }.toImmutableList(),
                    reactionsInCard.filter { it in chunk }.toImmutableList(),
                    zapsInCard.filter { it.response in chunk }.toImmutableList()
                )
            }
        }.flatten()

        val userZaps = zapsPerUser.map {
            ZapUserSetCard(
                it.key,
                it.value.toImmutableList()
            )
        }

        val textNoteCards = notes.filter { it.event !is ReactionEvent && it.event !is RepostEvent && it.event !is GenericRepostEvent && it.event !is LnZapEvent }.map {
            if (it.event is PrivateDmEvent) {
                MessageSetCard(it)
            } else {
                NoteCard(it)
            }
        }

        return (multiCards + textNoteCards + userZaps).sortedWith(compareBy({ it.createdAt() }, { it.id() })).reversed()
    }

    private fun updateFeed(notes: ImmutableList<Card>) {
        val scope = CoroutineScope(Job() + Dispatchers.Main)
        scope.launch {
            val currentState = _feedContent.value

            if (notes.isEmpty()) {
                _feedContent.update { CardFeedState.Empty }
            } else if (currentState is CardFeedState.Loaded) {
                currentState.showHidden.value = localFilter.showHiddenKey()
                currentState.feed.value = notes
            } else {
                _feedContent.update { CardFeedState.Loaded(mutableStateOf(notes), mutableStateOf(localFilter.showHiddenKey())) }
            }
        }
    }

    private fun refreshFromOldState(newItems: Set<Note>) {
        val oldNotesState = _feedContent.value

        val thisAccount = (localFilter as? NotificationFeedFilter)?.account
        val lastNotesCopy = if (thisAccount == lastAccount) lastNotes else null

        if (lastNotesCopy != null && localFilter is AdditiveFeedFilter && oldNotesState is CardFeedState.Loaded && lastFeedKey == localFilter.feedKey()) {
            val filteredNewList = localFilter.applyFilter(newItems)

            if (filteredNewList.isEmpty()) return

            val actuallyNew = filteredNewList.minus(lastNotesCopy)

            if (actuallyNew.isEmpty()) return

            val newCards = convertToCard(actuallyNew)

            if (newCards.isNotEmpty()) {
                lastNotes = lastNotesCopy + actuallyNew
                lastAccount = (localFilter as? NotificationFeedFilter)?.account

                val updatedCards = (oldNotesState.feed.value + newCards)
                    .distinctBy { it.id() }
                    .sortedWith(compareBy({ it.createdAt() }, { it.id() }))
                    .reversed()
                    .take(localFilter.limit())
                    .toImmutableList()

                if (!equalImmutableLists(oldNotesState.feed.value, updatedCards)) {
                    updateFeed(updatedCards)
                }
            }
        } else {
            // Refresh Everything
            refreshSuspended()
        }
    }

    private val bundler = BundledUpdate(1000, Dispatchers.IO)
    private val bundlerInsert = BundledInsert<Set<Note>>(1000, Dispatchers.IO)

    @OptIn(ExperimentalTime::class)
    fun invalidateData(ignoreIfDoing: Boolean = false) {
        bundler.invalidate(ignoreIfDoing) {
            // adds the time to perform the refresh into this delay
            // holding off new updates in case of heavy refresh routines.
            val (value, elapsed) = measureTimedValue {
                refreshSuspended()
            }
            Log.d("Time", "${this.javaClass.simpleName} Card update $elapsed")
        }
    }

    @OptIn(ExperimentalTime::class)
    fun invalidateDataAndSendToTop() {
        clear()
        bundler.invalidate(false) {
            // adds the time to perform the refresh into this delay
            // holding off new updates in case of heavy refresh routines.
            val (value, elapsed) = measureTimedValue {
                refreshSuspended()
                sendToTop()
            }
            Log.d("Time", "${this.javaClass.simpleName} Card update $elapsed")
        }
    }

    @OptIn(ExperimentalTime::class)
    fun checkKeysInvalidateDataAndSendToTop() {
        if (lastFeedKey != localFilter.feedKey()) {
            clear()
            bundler.invalidate(false) {
                // adds the time to perform the refresh into this delay
                // holding off new updates in case of heavy refresh routines.
                val (value, elapsed) = measureTimedValue {
                    refreshSuspended()
                    sendToTop()
                }
                Log.d("Time", "${this.javaClass.simpleName} Card update $elapsed")
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    fun invalidateInsertData(newItems: Set<Note>) {
        bundlerInsert.invalidateList(newItems) {
            val newObjects = it.flatten().toSet()
            val (value, elapsed) = measureTimedValue {
                if (newObjects.isNotEmpty()) {
                    refreshFromOldState(newObjects)
                }
            }
            Log.d("Time", "${this.javaClass.simpleName} Card additive update $elapsed. ${newObjects.size}")
        }
    }

    var collectorJob: Job? = null

    init {
        Log.d("Init", "${this.javaClass.simpleName}")
        collectorJob = viewModelScope.launch(Dispatchers.IO) {
            LocalCache.live.newEventBundles.collect { newNotes ->
                checkNotInMainThread()

                if (localFilter is AdditiveFeedFilter && _feedContent.value is CardFeedState.Loaded) {
                    invalidateInsertData(newNotes)
                } else {
                    // Refresh Everything
                    invalidateData()
                }
            }
        }
    }

    fun clear() {
        lastAccount = null
        lastNotes = null
    }

    override fun onCleared() {
        clear()
        collectorJob?.cancel()
        super.onCleared()
    }
}

fun <T> equalImmutableLists(list1: ImmutableList<T>, list2: ImmutableList<T>): Boolean {
    if (list1 === list2) return true
    if (list1.size != list2.size) return false
    for (i in 0 until list1.size) {
        if (list1[i] !== list2[i]) {
            return false
        }
    }
    return true
}

@Immutable
data class CombinedZap(val request: Note, val response: Note) {
    fun createdAt() = response.createdAt()
    fun idHex() = response.idHex
}
