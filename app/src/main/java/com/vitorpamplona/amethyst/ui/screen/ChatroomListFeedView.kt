package com.vitorpamplona.amethyst.ui.screen

import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vitorpamplona.amethyst.service.model.PrivateDmEvent
import com.vitorpamplona.amethyst.ui.note.ChatroomHeaderCompose
import com.vitorpamplona.amethyst.ui.screen.loggedIn.AccountViewModel
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

@Composable
fun ChatroomListFeedView(
    viewModel: FeedViewModel,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit,
    markAsRead: MutableState<Boolean>
) {
    RefresheableView(viewModel, true) {
        CrossFadeState(viewModel, accountViewModel, nav, markAsRead)
    }
}

@Composable
private fun CrossFadeState(
    viewModel: FeedViewModel,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit,
    markAsRead: MutableState<Boolean>
) {
    val feedState by viewModel.feedContent.collectAsState()

    Crossfade(
        targetState = feedState,
        animationSpec = tween(durationMillis = 100)
    ) { state ->
        when (state) {
            is FeedState.Empty -> {
                FeedEmpty {
                    viewModel.invalidateData()
                }
            }

            is FeedState.FeedError -> {
                FeedError(state.errorMessage) {
                    viewModel.invalidateData()
                }
            }

            is FeedState.Loaded -> {
                FeedLoaded(state, accountViewModel, nav, markAsRead)
            }

            FeedState.Loading -> {
                LoadingFeed()
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun FeedLoaded(
    state: FeedState.Loaded,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit,
    markAsRead: MutableState<Boolean>
) {
    val listState = rememberLazyListState()

    LaunchedEffect(key1 = markAsRead.value) {
        if (markAsRead.value) {
            for (note in state.feed.value) {
                note.event?.let {
                    val roomUser = (note.event as? PrivateDmEvent)?.talkingWith(accountViewModel.account.userProfile().pubkeyHex)
                    val route = "Room/$roomUser"
                    accountViewModel.account.markAsRead(route, it.createdAt())
                }
            }
            markAsRead.value = false
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(
            top = 10.dp,
            bottom = 10.dp
        ),
        state = listState
    ) {
        itemsIndexed(
            state.feed.value,
            key = { index, item -> if (index == 0) index else item.idHex }
        ) { _, item ->
            Row(Modifier.fillMaxWidth()) {
                val (value, elapsed) = measureTimedValue {
                    ChatroomHeaderCompose(
                        item,
                        accountViewModel = accountViewModel,
                        nav = nav
                    )
                }
                Log.d("Rendering Metrics", "Chat Header Complete: ${item.event?.content()?.split("\n")?.getOrNull(0)?.take(15)}.. $elapsed")
            }
        }
    }
}
