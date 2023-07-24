package com.vitorpamplona.amethyst.ui.note

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.map
import com.vitorpamplona.amethyst.R
import com.vitorpamplona.amethyst.model.LocalCache
import com.vitorpamplona.amethyst.model.Note
import com.vitorpamplona.amethyst.model.User
import com.vitorpamplona.amethyst.service.model.PrivateDmEvent
import com.vitorpamplona.amethyst.ui.screen.loggedIn.AccountViewModel
import com.vitorpamplona.amethyst.ui.theme.ChatHeadlineBorders
import com.vitorpamplona.amethyst.ui.theme.DividerThickness
import com.vitorpamplona.amethyst.ui.theme.DoubleHorzSpacer
import com.vitorpamplona.amethyst.ui.theme.Height4dpModifier
import com.vitorpamplona.amethyst.ui.theme.Size55Modifier
import com.vitorpamplona.amethyst.ui.theme.Size55dp
import com.vitorpamplona.amethyst.ui.theme.Size75dp
import com.vitorpamplona.amethyst.ui.theme.StdTopPadding
import com.vitorpamplona.amethyst.ui.theme.grayText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ChatroomHeaderCompose(
    baseNote: Note,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    val isBlank by baseNote.live().metadata.map {
        it.note.event == null
    }.observeAsState(baseNote.event == null)

    if (isBlank) {
        BlankNote(Modifier)
    } else {
        ChatroomComposeChannelOrUser(baseNote, accountViewModel, nav)
    }
}

@Composable
fun ChatroomComposeChannelOrUser(
    baseNote: Note,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    ChatroomDirectMessage(baseNote, accountViewModel, nav)
}

@Composable
private fun ChatroomDirectMessage(
    baseNote: Note,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    val userRoomHex by remember(baseNote) {
        derivedStateOf {
            (baseNote.event as? PrivateDmEvent)?.talkingWith(accountViewModel.userProfile().pubkeyHex)
        }
    }

    userRoomHex?.let {
        LoadUser(it) { baseUser ->
            Crossfade(baseUser) { user ->
                if (user != null) {
                    UserRoomCompose(baseNote, user, accountViewModel, nav)
                } else {
                    Box(Modifier.height(Size75dp).fillMaxWidth()) {
                        // Makes sure just a max amount of objects are loaded.
                    }
                }
            }
        }
    }
}

@Composable
private fun UserRoomCompose(
    note: Note,
    user: User,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    val hasNewMessages = remember { mutableStateOf<Boolean>(false) }

    val route = remember(user) {
        "Room/${user.pubkeyHex}"
    }

    val createAt by remember(note) {
        derivedStateOf {
            note.createdAt()
        }
    }

    val content by remember(note) {
        derivedStateOf {
            accountViewModel.decrypt(note)
        }
    }

    WatchNotificationChanges(note, route, accountViewModel) { newHasNewMessages ->
        if (hasNewMessages.value != newHasNewMessages) {
            hasNewMessages.value = newHasNewMessages
        }
    }

    ChannelName(
        channelPicture = {
            NonClickableUserPicture(
                baseUser = user,
                accountViewModel = accountViewModel,
                size = Size55dp
            )
        },
        channelTitle = { UsernameDisplay(user, it) },
        channelLastTime = createAt,
        channelLastContent = content,
        hasNewMessages = hasNewMessages,
        onClick = { nav(route) }
    )
}

@Composable
private fun WatchNotificationChanges(
    note: Note,
    route: String,
    accountViewModel: AccountViewModel,
    onNewStatus: (Boolean) -> Unit
) {
    val cacheState by accountViewModel.accountLastReadLiveData.observeAsState()

    LaunchedEffect(key1 = note, cacheState) {
        launch(Dispatchers.IO) {
            note.event?.createdAt()?.let {
                val lastTime = accountViewModel.account.loadLastRead(route)
                onNewStatus(it > lastTime)
            }
        }
    }
}

@Composable
fun LoadUser(baseUserHex: String, content: @Composable (User?) -> Unit) {
    var user by remember(baseUserHex) {
        mutableStateOf(LocalCache.getUserIfExists(baseUserHex))
    }

    if (user == null) {
        LaunchedEffect(key1 = baseUserHex) {
            launch(Dispatchers.IO) {
                val newUser = LocalCache.checkGetOrCreateUser(baseUserHex)
                if (user != newUser) {
                    user = newUser
                }
            }
        }
    }

    content(user)
}

@Composable
fun ChannelName(
    channelPicture: @Composable () -> Unit,
    channelTitle: @Composable (Modifier) -> Unit,
    channelLastTime: Long?,
    channelLastContent: String?,
    hasNewMessages: MutableState<Boolean>,
    onClick: () -> Unit
) {
    Column(modifier = remember { Modifier.clickable(onClick = onClick) }) {
        Row(modifier = ChatHeadlineBorders) {
            Column(Size55Modifier) {
                channelPicture()
            }

            Spacer(modifier = DoubleHorzSpacer)

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.SpaceAround
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FirstRow(channelTitle, channelLastTime, remember { Modifier.weight(1f) })
                }

                Spacer(modifier = Height4dpModifier)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SecondRow(channelLastContent, hasNewMessages, remember { Modifier.weight(1f) })
                }
            }
        }

        Divider(
            modifier = StdTopPadding,
            thickness = DividerThickness
        )
    }
}

@Composable
private fun SecondRow(channelLastContent: String?, hasNewMessages: MutableState<Boolean>, modifier: Modifier) {
    if (channelLastContent != null) {
        Text(
            channelLastContent,
            color = MaterialTheme.colors.grayText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = LocalTextStyle.current.copy(textDirection = TextDirection.Content),
            modifier = modifier
        )
    } else {
        Text(
            stringResource(R.string.referenced_event_not_found),
            color = MaterialTheme.colors.grayText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
        )
    }

    if (hasNewMessages.value) {
        NewItemsBubble()
    }
}

@Composable
private fun FirstRow(
    channelTitle: @Composable (Modifier) -> Unit,
    channelLastTime: Long?,
    modifier: Modifier
) {
    channelTitle(modifier)
    TimeAgo(channelLastTime)
}

@Composable
private fun TimeAgo(channelLastTime: Long?) {
    if (channelLastTime == null) return

    val context = LocalContext.current
    val timeAgo by remember(channelLastTime) {
        derivedStateOf {
            timeAgo(channelLastTime, context)
        }
    }
    Text(
        text = timeAgo,
        color = MaterialTheme.colors.grayText,
        maxLines = 1
    )
}

@Composable
fun NewItemsBubble() {
    Box(
        modifier = Modifier
            .padding(start = 3.dp)
            .width(10.dp)
            .height(10.dp)
            .clip(shape = CircleShape)
            .background(MaterialTheme.colors.primary),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "",
            color = Color.White,
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            maxLines = 1,
            modifier = Modifier
                .wrapContentHeight()
                .align(Alignment.Center)
        )
    }
}
