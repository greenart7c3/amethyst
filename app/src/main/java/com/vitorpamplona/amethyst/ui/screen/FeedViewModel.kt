package com.vitorpamplona.amethyst.ui.screen

import android.util.Log
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vitorpamplona.amethyst.model.Account
import com.vitorpamplona.amethyst.model.AddressableNote
import com.vitorpamplona.amethyst.model.Channel
import com.vitorpamplona.amethyst.model.LocalCache
import com.vitorpamplona.amethyst.model.Note
import com.vitorpamplona.amethyst.model.User
import com.vitorpamplona.amethyst.service.checkNotInMainThread
import com.vitorpamplona.amethyst.ui.components.BundledInsert
import com.vitorpamplona.amethyst.ui.components.BundledUpdate
import com.vitorpamplona.amethyst.ui.dal.AdditiveFeedFilter
import com.vitorpamplona.amethyst.ui.dal.BookmarkPrivateFeedFilter
import com.vitorpamplona.amethyst.ui.dal.BookmarkPublicFeedFilter
import com.vitorpamplona.amethyst.ui.dal.ChannelFeedFilter
import com.vitorpamplona.amethyst.ui.dal.ChatroomFeedFilter
import com.vitorpamplona.amethyst.ui.dal.ChatroomListKnownFeedFilter
import com.vitorpamplona.amethyst.ui.dal.ChatroomListNewFeedFilter
import com.vitorpamplona.amethyst.ui.dal.CommunityFeedFilter
import com.vitorpamplona.amethyst.ui.dal.DiscoverChatFeedFilter
import com.vitorpamplona.amethyst.ui.dal.DiscoverCommunityFeedFilter
import com.vitorpamplona.amethyst.ui.dal.FeedFilter
import com.vitorpamplona.amethyst.ui.dal.HashtagFeedFilter
import com.vitorpamplona.amethyst.ui.dal.HomeConversationsFeedFilter
import com.vitorpamplona.amethyst.ui.dal.HomeNewThreadFeedFilter
import com.vitorpamplona.amethyst.ui.dal.ThreadFeedFilter
import com.vitorpamplona.amethyst.ui.dal.UserProfileAppRecommendationsFeedFilter
import com.vitorpamplona.amethyst.ui.dal.UserProfileBookmarksFeedFilter
import com.vitorpamplona.amethyst.ui.dal.UserProfileConversationsFeedFilter
import com.vitorpamplona.amethyst.ui.dal.UserProfileNewThreadFeedFilter
import com.vitorpamplona.amethyst.ui.dal.UserProfileReportsFeedFilter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NostrChannelFeedViewModel(val channel: Channel, val account: Account) : FeedViewModel(ChannelFeedFilter(channel, account)) {
    class Factory(val channel: Channel, val account: Account) : ViewModelProvider.Factory {
        override fun <NostrChannelFeedViewModel : ViewModel> create(modelClass: Class<NostrChannelFeedViewModel>): NostrChannelFeedViewModel {
            return NostrChannelFeedViewModel(channel, account) as NostrChannelFeedViewModel
        }
    }
}
class NostrChatroomFeedViewModel(val user: User, val account: Account) : FeedViewModel(ChatroomFeedFilter(user, account)) {
    class Factory(val user: User, val account: Account) : ViewModelProvider.Factory {
        override fun <NostrChatRoomFeedViewModel : ViewModel> create(modelClass: Class<NostrChatRoomFeedViewModel>): NostrChatRoomFeedViewModel {
            return NostrChatroomFeedViewModel(user, account) as NostrChatRoomFeedViewModel
        }
    }
}

class NostrDiscoverCommunityFeedViewModel(val account: Account) : FeedViewModel(DiscoverCommunityFeedFilter(account)) {
    class Factory(val account: Account) : ViewModelProvider.Factory {
        override fun <NostrDiscoverCommunityFeedViewModel : ViewModel> create(modelClass: Class<NostrDiscoverCommunityFeedViewModel>): NostrDiscoverCommunityFeedViewModel {
            return NostrDiscoverCommunityFeedViewModel(account) as NostrDiscoverCommunityFeedViewModel
        }
    }
}

class NostrDiscoverChatFeedViewModel(val account: Account) : FeedViewModel(DiscoverChatFeedFilter(account)) {
    class Factory(val account: Account) : ViewModelProvider.Factory {
        override fun <NostrDiscoverChatFeedViewModel : ViewModel> create(modelClass: Class<NostrDiscoverChatFeedViewModel>): NostrDiscoverChatFeedViewModel {
            return NostrDiscoverChatFeedViewModel(account) as NostrDiscoverChatFeedViewModel
        }
    }
}

class NostrThreadFeedViewModel(val noteId: String) : FeedViewModel(ThreadFeedFilter(noteId)) {
    class Factory(val noteId: String) : ViewModelProvider.Factory {
        override fun <NostrThreadFeedViewModel : ViewModel> create(modelClass: Class<NostrThreadFeedViewModel>): NostrThreadFeedViewModel {
            return NostrThreadFeedViewModel(noteId) as NostrThreadFeedViewModel
        }
    }
}

class NostrUserProfileNewThreadsFeedViewModel(val user: User, val account: Account) : FeedViewModel(UserProfileNewThreadFeedFilter(user, account)) {
    class Factory(val user: User, val account: Account) : ViewModelProvider.Factory {
        override fun <NostrUserProfileNewThreadsFeedViewModel : ViewModel> create(modelClass: Class<NostrUserProfileNewThreadsFeedViewModel>): NostrUserProfileNewThreadsFeedViewModel {
            return NostrUserProfileNewThreadsFeedViewModel(user, account) as NostrUserProfileNewThreadsFeedViewModel
        }
    }
}
class NostrUserProfileConversationsFeedViewModel(val user: User, val account: Account) : FeedViewModel(UserProfileConversationsFeedFilter(user, account)) {
    class Factory(val user: User, val account: Account) : ViewModelProvider.Factory {
        override fun <NostrUserProfileConversationsFeedViewModel : ViewModel> create(modelClass: Class<NostrUserProfileConversationsFeedViewModel>): NostrUserProfileConversationsFeedViewModel {
            return NostrUserProfileConversationsFeedViewModel(user, account) as NostrUserProfileConversationsFeedViewModel
        }
    }
}

class NostrHashtagFeedViewModel(val hashtag: String, val account: Account) : FeedViewModel(HashtagFeedFilter(hashtag, account)) {
    class Factory(val hashtag: String, val account: Account) : ViewModelProvider.Factory {
        override fun <NostrHashtagFeedViewModel : ViewModel> create(modelClass: Class<NostrHashtagFeedViewModel>): NostrHashtagFeedViewModel {
            return NostrHashtagFeedViewModel(hashtag, account) as NostrHashtagFeedViewModel
        }
    }
}

class NostrCommunityFeedViewModel(val note: AddressableNote, val account: Account) : FeedViewModel(CommunityFeedFilter(note, account)) {
    class Factory(val note: AddressableNote, val account: Account) : ViewModelProvider.Factory {
        override fun <NostrCommunityFeedViewModel : ViewModel> create(modelClass: Class<NostrCommunityFeedViewModel>): NostrCommunityFeedViewModel {
            return NostrCommunityFeedViewModel(note, account) as NostrCommunityFeedViewModel
        }
    }
}

class NostrUserProfileReportFeedViewModel(val user: User) : FeedViewModel(UserProfileReportsFeedFilter(user)) {
    class Factory(val user: User) : ViewModelProvider.Factory {
        override fun <NostrUserProfileReportFeedViewModel : ViewModel> create(modelClass: Class<NostrUserProfileReportFeedViewModel>): NostrUserProfileReportFeedViewModel {
            return NostrUserProfileReportFeedViewModel(user) as NostrUserProfileReportFeedViewModel
        }
    }
}
class NostrUserProfileBookmarksFeedViewModel(val user: User, val account: Account) : FeedViewModel(UserProfileBookmarksFeedFilter(user, account)) {
    class Factory(val user: User, val account: Account) : ViewModelProvider.Factory {
        override fun <NostrUserProfileBookmarksFeedViewModel : ViewModel> create(modelClass: Class<NostrUserProfileBookmarksFeedViewModel>): NostrUserProfileBookmarksFeedViewModel {
            return NostrUserProfileBookmarksFeedViewModel(user, account) as NostrUserProfileBookmarksFeedViewModel
        }
    }
}

class NostrChatroomListKnownFeedViewModel(val account: Account) : FeedViewModel(ChatroomListKnownFeedFilter(account)) {
    class Factory(val account: Account) : ViewModelProvider.Factory {
        override fun <NostrChatroomListKnownFeedViewModel : ViewModel> create(modelClass: Class<NostrChatroomListKnownFeedViewModel>): NostrChatroomListKnownFeedViewModel {
            return NostrChatroomListKnownFeedViewModel(account) as NostrChatroomListKnownFeedViewModel
        }
    }
}
class NostrChatroomListNewFeedViewModel(val account: Account) : FeedViewModel(ChatroomListNewFeedFilter(account)) {
    class Factory(val account: Account) : ViewModelProvider.Factory {
        override fun <NostrChatroomListNewFeedViewModel : ViewModel> create(modelClass: Class<NostrChatroomListNewFeedViewModel>): NostrChatroomListNewFeedViewModel {
            return NostrChatroomListNewFeedViewModel(account) as NostrChatroomListNewFeedViewModel
        }
    }
}

@Stable
class NostrHomeFeedViewModel(val account: Account) : FeedViewModel(HomeNewThreadFeedFilter(account)) {
    class Factory(val account: Account) : ViewModelProvider.Factory {
        override fun <NostrHomeFeedViewModel : ViewModel> create(modelClass: Class<NostrHomeFeedViewModel>): NostrHomeFeedViewModel {
            return NostrHomeFeedViewModel(account) as NostrHomeFeedViewModel
        }
    }
}

@Stable
class NostrHomeRepliesFeedViewModel(val account: Account) : FeedViewModel(HomeConversationsFeedFilter(account)) {
    class Factory(val account: Account) : ViewModelProvider.Factory {
        override fun <NostrHomeRepliesFeedViewModel : ViewModel> create(modelClass: Class<NostrHomeRepliesFeedViewModel>): NostrHomeRepliesFeedViewModel {
            return NostrHomeRepliesFeedViewModel(account) as NostrHomeRepliesFeedViewModel
        }
    }
}

class NostrBookmarkPublicFeedViewModel : FeedViewModel(BookmarkPublicFeedFilter)
class NostrBookmarkPrivateFeedViewModel : FeedViewModel(BookmarkPrivateFeedFilter)

class NostrUserAppRecommendationsFeedViewModel(val user: User) : FeedViewModel(UserProfileAppRecommendationsFeedFilter(user)) {
    class Factory(val user: User) : ViewModelProvider.Factory {
        override fun <NostrUserAppRecommendationsFeedViewModel : ViewModel> create(modelClass: Class<NostrUserAppRecommendationsFeedViewModel>): NostrUserAppRecommendationsFeedViewModel {
            return NostrUserAppRecommendationsFeedViewModel(user) as NostrUserAppRecommendationsFeedViewModel
        }
    }
}

@Stable
abstract class FeedViewModel(val localFilter: FeedFilter<Note>) : ViewModel(), InvalidatableViewModel {
    private val _feedContent = MutableStateFlow<FeedState>(FeedState.Loading)
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

    private fun refresh() {
        val scope = CoroutineScope(Job() + Dispatchers.Default)
        scope.launch {
            refreshSuspended()
        }
    }

    fun refreshSuspended() {
        checkNotInMainThread()

        lastFeedKey = localFilter.feedKey()
        val notes = localFilter.loadTop().toImmutableList()

        val oldNotesState = _feedContent.value
        if (oldNotesState is FeedState.Loaded) {
            if (!equalImmutableLists(notes, oldNotesState.feed.value)) {
                updateFeed(notes)
            }
        } else {
            updateFeed(notes)
        }
    }

    private fun updateFeed(notes: ImmutableList<Note>) {
        val scope = CoroutineScope(Job() + Dispatchers.Main)
        scope.launch {
            val currentState = _feedContent.value
            if (notes.isEmpty()) {
                _feedContent.update { FeedState.Empty }
            } else if (currentState is FeedState.Loaded) {
                // updates the current list
                currentState.showHidden.value = localFilter.showHiddenKey()
                currentState.feed.value = notes
            } else {
                _feedContent.update { FeedState.Loaded(mutableStateOf(notes), mutableStateOf(localFilter.showHiddenKey())) }
            }
        }
    }

    fun refreshFromOldState(newItems: Set<Note>) {
        val oldNotesState = _feedContent.value
        if (localFilter is AdditiveFeedFilter && lastFeedKey == localFilter.feedKey()) {
            if (oldNotesState is FeedState.Loaded) {
                val newList = localFilter.updateListWith(oldNotesState.feed.value, newItems.toSet()).distinctBy { it.idHex }.toImmutableList()
                if (!equalImmutableLists(newList, oldNotesState.feed.value)) {
                    updateFeed(newList)
                }
            } else if (oldNotesState is FeedState.Empty) {
                val newList = localFilter.updateListWith(emptyList(), newItems.toSet()).distinctBy { it.idHex }.toImmutableList()
                if (newList.isNotEmpty()) {
                    updateFeed(newList)
                }
            } else {
                // Refresh Everything
                refreshSuspended()
            }
        } else {
            // Refresh Everything
            refreshSuspended()
        }
    }

    private val bundler = BundledUpdate(250, Dispatchers.IO)
    private val bundlerInsert = BundledInsert<Set<Note>>(250, Dispatchers.IO)

    override fun invalidateData(ignoreIfDoing: Boolean) {
        bundler.invalidate(ignoreIfDoing) {
            // adds the time to perform the refresh into this delay
            // holding off new updates in case of heavy refresh routines.
            refreshSuspended()
        }
    }

    fun checkKeysInvalidateDataAndSendToTop() {
        if (lastFeedKey != localFilter.feedKey()) {
            bundler.invalidate(false) {
                // adds the time to perform the refresh into this delay
                // holding off new updates in case of heavy refresh routines.
                refreshSuspended()
                sendToTop()
            }
        }
    }

    fun invalidateInsertData(newItems: Set<Note>) {
        bundlerInsert.invalidateList(newItems) {
            refreshFromOldState(it.flatten().toSet())
        }
    }

    private var collectorJob: Job? = null

    init {
        Log.d("Init", "${this.javaClass.simpleName}")
        collectorJob = viewModelScope.launch(Dispatchers.IO) {
            LocalCache.live.newEventBundles.collect { newNotes ->
                checkNotInMainThread()

                if (localFilter is AdditiveFeedFilter &&
                    (_feedContent.value is FeedState.Loaded || _feedContent.value is FeedState.Empty)
                ) {
                    invalidateInsertData(newNotes)
                } else {
                    // Refresh Everything
                    invalidateData()
                }
            }
        }
    }

    override fun onCleared() {
        collectorJob?.cancel()
        super.onCleared()
    }
}
