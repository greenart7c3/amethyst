package com.vitorpamplona.amethyst.ui.note

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomStart
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import coil.compose.AsyncImage
import com.vitorpamplona.amethyst.model.Channel
import com.vitorpamplona.amethyst.model.KIND3_FOLLOWS
import com.vitorpamplona.amethyst.model.LocalCache
import com.vitorpamplona.amethyst.model.Note
import com.vitorpamplona.amethyst.model.ParticipantListBuilder
import com.vitorpamplona.amethyst.model.User
import com.vitorpamplona.amethyst.service.model.ChannelCreateEvent
import com.vitorpamplona.amethyst.service.model.CommunityDefinitionEvent
import com.vitorpamplona.amethyst.ui.components.SensitivityWarning
import com.vitorpamplona.amethyst.ui.screen.equalImmutableLists
import com.vitorpamplona.amethyst.ui.screen.loggedIn.AccountViewModel
import com.vitorpamplona.amethyst.ui.theme.DoubleHorzSpacer
import com.vitorpamplona.amethyst.ui.theme.QuoteBorder
import com.vitorpamplona.amethyst.ui.theme.Size35dp
import com.vitorpamplona.amethyst.ui.theme.StdHorzSpacer
import com.vitorpamplona.amethyst.ui.theme.StdPadding
import com.vitorpamplona.amethyst.ui.theme.StdVertSpacer
import com.vitorpamplona.amethyst.ui.theme.newItemBackgroundColor
import com.vitorpamplona.amethyst.ui.theme.placeholderText
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChannelCardCompose(
    baseNote: Note,
    routeForLastRead: String? = null,
    modifier: Modifier = Modifier,
    parentBackgroundColor: MutableState<Color>? = null,
    forceEventKind: Int?,
    showHidden: Boolean = false,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    val isBlank by baseNote.live().metadata.map {
        it.note.event == null
    }.distinctUntilChanged().observeAsState(baseNote.event == null)

    Crossfade(targetState = isBlank) {
        if (it) {
            LongPressToQuickAction(baseNote = baseNote, accountViewModel = accountViewModel) { showPopup ->
                BlankNote(
                    remember {
                        modifier.combinedClickable(
                            onClick = { },
                            onLongClick = showPopup
                        )
                    },
                    false
                )
            }
        } else {
            if (forceEventKind == null || baseNote.event?.kind() == forceEventKind) {
                CheckHiddenChannelCardCompose(
                    baseNote,
                    routeForLastRead,
                    modifier,
                    parentBackgroundColor,
                    showHidden,
                    accountViewModel,
                    nav
                )
            }
        }
    }
}

@Composable
fun CheckHiddenChannelCardCompose(
    note: Note,
    routeForLastRead: String? = null,
    modifier: Modifier = Modifier,
    parentBackgroundColor: MutableState<Color>? = null,
    showHidden: Boolean,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    if (showHidden) {
        var state by remember {
            mutableStateOf(
                NoteComposeReportState(
                    isAcceptable = true,
                    canPreview = true,
                    relevantReports = persistentSetOf()
                )
            )
        }

        RenderChannelCardReportState(
            state = state,
            note = note,
            routeForLastRead = routeForLastRead,
            modifier = modifier,
            parentBackgroundColor = parentBackgroundColor,
            accountViewModel = accountViewModel,
            nav = nav
        )
    } else {
        val isHidden by accountViewModel.account.liveHiddenUsers.map {
            note.isHiddenFor(it)
        }.distinctUntilChanged().observeAsState(accountViewModel.isNoteHidden(note))

        Crossfade(targetState = isHidden) {
            if (!it) {
                LoadedChannelCardCompose(
                    note,
                    routeForLastRead,
                    modifier,
                    parentBackgroundColor,
                    accountViewModel,
                    nav
                )
            }
        }
    }
}

@Composable
fun LoadedChannelCardCompose(
    note: Note,
    routeForLastRead: String? = null,
    modifier: Modifier = Modifier,
    parentBackgroundColor: MutableState<Color>? = null,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    var state by remember {
        mutableStateOf(
            NoteComposeReportState(
                isAcceptable = true,
                canPreview = true,
                relevantReports = persistentSetOf()
            )
        )
    }

    val scope = rememberCoroutineScope()

    WatchForReports(note, accountViewModel) { newIsAcceptable, newCanPreview, newRelevantReports ->
        if (newIsAcceptable != state.isAcceptable || newCanPreview != state.canPreview) {
            val newState = NoteComposeReportState(newIsAcceptable, newCanPreview, newRelevantReports)
            scope.launch(Dispatchers.Main) {
                state = newState
            }
        }
    }

    Crossfade(targetState = state) {
        RenderChannelCardReportState(
            it,
            note,
            routeForLastRead,
            modifier,
            parentBackgroundColor,
            accountViewModel,
            nav
        )
    }
}

@Composable
fun RenderChannelCardReportState(
    state: NoteComposeReportState,
    note: Note,
    routeForLastRead: String? = null,
    modifier: Modifier = Modifier,
    parentBackgroundColor: MutableState<Color>? = null,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    var showReportedNote by remember { mutableStateOf(false) }

    Crossfade(targetState = !state.isAcceptable && !showReportedNote) { showHiddenNote ->
        if (showHiddenNote) {
            HiddenNote(
                state.relevantReports,
                accountViewModel,
                modifier,
                false,
                nav,
                onClick = { showReportedNote = true }
            )
        } else {
            NormalChannelCard(
                note,
                routeForLastRead,
                modifier,
                parentBackgroundColor,
                accountViewModel,
                nav
            )
        }
    }
}

@Composable
fun NormalChannelCard(
    baseNote: Note,
    routeForLastRead: String? = null,
    modifier: Modifier = Modifier,
    parentBackgroundColor: MutableState<Color>? = null,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    LongPressToQuickAction(baseNote = baseNote, accountViewModel = accountViewModel) { showPopup ->
        CheckNewAndRenderChannelCard(
            baseNote,
            routeForLastRead,
            modifier,
            parentBackgroundColor,
            accountViewModel,
            showPopup,
            nav
        )
    }
}

@Composable
private fun CheckNewAndRenderChannelCard(
    baseNote: Note,
    routeForLastRead: String? = null,
    modifier: Modifier = Modifier,
    parentBackgroundColor: MutableState<Color>? = null,
    accountViewModel: AccountViewModel,
    showPopup: () -> Unit,
    nav: (String) -> Unit
) {
    val newItemColor = MaterialTheme.colors.newItemBackgroundColor
    val defaultBackgroundColor = MaterialTheme.colors.background
    val backgroundColor = remember { mutableStateOf<Color>(defaultBackgroundColor) }

    LaunchedEffect(key1 = routeForLastRead, key2 = parentBackgroundColor?.value) {
        launch(Dispatchers.IO) {
            routeForLastRead?.let {
                val lastTime = accountViewModel.account.loadLastRead(it)

                val createdAt = baseNote.createdAt()
                if (createdAt != null) {
                    accountViewModel.account.markAsRead(it, createdAt)

                    val isNew = createdAt > lastTime

                    val newBackgroundColor = if (isNew) {
                        if (parentBackgroundColor != null) {
                            newItemColor.compositeOver(parentBackgroundColor.value)
                        } else {
                            newItemColor.compositeOver(defaultBackgroundColor)
                        }
                    } else {
                        parentBackgroundColor?.value ?: defaultBackgroundColor
                    }

                    if (newBackgroundColor != backgroundColor.value) {
                        launch(Dispatchers.Main) {
                            backgroundColor.value = newBackgroundColor
                        }
                    }
                }
            } ?: run {
                val newBackgroundColor = parentBackgroundColor?.value ?: defaultBackgroundColor

                if (newBackgroundColor != backgroundColor.value) {
                    launch(Dispatchers.Main) {
                        backgroundColor.value = newBackgroundColor
                    }
                }
            }
        }
    }

    ClickableNote(
        baseNote = baseNote,
        backgroundColor = backgroundColor,
        modifier = modifier,
        accountViewModel = accountViewModel,
        showPopup = showPopup,
        nav = nav
    ) {
        InnerChannelCardWithReactions(
            baseNote = baseNote,
            accountViewModel = accountViewModel,
            nav = nav
        )
    }
}

@Composable
fun InnerChannelCardWithReactions(
    baseNote: Note,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    Column(StdPadding) {
        SensitivityWarning(
            note = baseNote,
            accountViewModel = accountViewModel
        ) {
            RenderNoteRow(
                baseNote,
                accountViewModel,
                nav
            )
        }
    }
}

@Composable
private fun RenderNoteRow(
    baseNote: Note,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    when (remember { baseNote.event }) {
        is CommunityDefinitionEvent -> {
            RenderCommunitiesThumb(baseNote, accountViewModel, nav)
        }
        is ChannelCreateEvent -> {
            RenderChannelThumb(baseNote, accountViewModel, nav)
        }
    }
}

@Composable
fun RenderCommunitiesThumb(baseNote: Note, accountViewModel: AccountViewModel, nav: (String) -> Unit) {
    val noteEvent = baseNote.event as? CommunityDefinitionEvent ?: return

    val eventUpdates by baseNote.live().metadata.observeAsState()

    val name = remember(eventUpdates) { noteEvent.dTag() }
    val description = remember(eventUpdates) { noteEvent.description() }
    val cover by remember(eventUpdates) {
        derivedStateOf {
            noteEvent.image()?.ifBlank { null }
        }
    }
    val moderators = remember(eventUpdates) { noteEvent.moderators() }

    var participantUsers by remember {
        mutableStateOf<ImmutableList<User>>(
            persistentListOf()
        )
    }

    LaunchedEffect(key1 = eventUpdates) {
        launch(Dispatchers.IO) {
            val hosts = moderators.mapNotNull { part ->
                if (part.key != baseNote.author?.pubkeyHex) {
                    LocalCache.checkGetOrCreateUser(part.key)
                } else {
                    null
                }
            }

            val followingKeySet = accountViewModel.account.selectedUsersFollowList(accountViewModel.account.defaultDiscoveryFollowList)
            val allParticipants = ParticipantListBuilder().followsThatParticipateOn(baseNote, followingKeySet).minus(hosts)

            val newParticipantUsers = if (followingKeySet == null) {
                val allFollows = accountViewModel.account.selectedUsersFollowList(KIND3_FOLLOWS)
                val followingParticipants = ParticipantListBuilder().followsThatParticipateOn(baseNote, allFollows).minus(hosts)

                (hosts + followingParticipants + (allParticipants - followingParticipants)).toImmutableList()
            } else {
                (hosts + allParticipants).toImmutableList()
            }

            if (!equalImmutableLists(newParticipantUsers, participantUsers)) {
                participantUsers = newParticipantUsers
            }
        }
    }

    Row(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.3f)
                .aspectRatio(ratio = 1f)
        ) {
            cover?.let {
                Box(contentAlignment = BottomStart) {
                    AsyncImage(
                        model = it,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(QuoteBorder)
                    )
                }
            } ?: run {
                baseNote.author?.let {
                    DisplayAuthorBanner(it)
                }
            }
        }

        Spacer(modifier = DoubleHorzSpacer)

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = StdHorzSpacer)
                LikeReaction(baseNote = baseNote, grayTint = MaterialTheme.colors.onSurface, accountViewModel = accountViewModel, nav)
                Spacer(modifier = StdHorzSpacer)
                ZapReaction(baseNote = baseNote, grayTint = MaterialTheme.colors.onSurface, accountViewModel = accountViewModel)
            }

            description?.let {
                Spacer(modifier = StdVertSpacer)
                Row() {
                    Text(
                        text = it,
                        color = MaterialTheme.colors.placeholderText,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 14.sp
                    )
                }
            }

            if (participantUsers.isNotEmpty()) {
                Spacer(modifier = StdVertSpacer)
                Row(modifier = Modifier.fillMaxWidth()) {
                    Gallery(participantUsers, accountViewModel)
                }
            }
        }
    }
}

@Composable
fun RenderChannelThumb(baseNote: Note, accountViewModel: AccountViewModel, nav: (String) -> Unit) {
    val noteEvent = baseNote.event as? ChannelCreateEvent ?: return

    LoadChannel(baseChannelHex = baseNote.idHex) {
        RenderChannelThumb(baseNote = baseNote, channel = it, accountViewModel, nav)
    }
}

@Composable
fun RenderChannelThumb(baseNote: Note, channel: Channel, accountViewModel: AccountViewModel, nav: (String) -> Unit) {
    val channelUpdates by channel.live.observeAsState()

    val name = remember(channelUpdates) { channelUpdates?.channel?.toBestDisplayName() ?: "" }
    val description = remember(channelUpdates) { channelUpdates?.channel?.summary() }
    val cover by remember(channelUpdates) {
        derivedStateOf {
            channelUpdates?.channel?.profilePicture()?.ifBlank { null }
        }
    }

    var participantUsers by remember(baseNote) {
        mutableStateOf<ImmutableList<User>>(
            persistentListOf()
        )
    }

    LaunchedEffect(key1 = channelUpdates) {
        launch(Dispatchers.IO) {
            val followingKeySet = accountViewModel.account.selectedUsersFollowList(accountViewModel.account.defaultDiscoveryFollowList)
            val allParticipants = ParticipantListBuilder().followsThatParticipateOn(baseNote, followingKeySet).toImmutableList()

            val newParticipantUsers = if (followingKeySet == null) {
                val allFollows = accountViewModel.account.selectedUsersFollowList(KIND3_FOLLOWS)
                val followingParticipants = ParticipantListBuilder().followsThatParticipateOn(baseNote, allFollows).toList()

                (followingParticipants + (allParticipants - followingParticipants)).toImmutableList()
            } else {
                allParticipants.toImmutableList()
            }

            if (!equalImmutableLists(newParticipantUsers, participantUsers)) {
                participantUsers = newParticipantUsers
            }
        }
    }

    Row(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.3f)
                .aspectRatio(ratio = 1f)
        ) {
            cover?.let {
                Box(contentAlignment = BottomStart) {
                    AsyncImage(
                        model = it,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(QuoteBorder)
                    )
                }
            } ?: run {
                baseNote.author?.let {
                    DisplayAuthorBanner(it)
                }
            }
        }

        Spacer(modifier = DoubleHorzSpacer)

        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = StdHorzSpacer)
                LikeReaction(baseNote = baseNote, grayTint = MaterialTheme.colors.onSurface, accountViewModel = accountViewModel, nav)
                Spacer(modifier = StdHorzSpacer)
                ZapReaction(baseNote = baseNote, grayTint = MaterialTheme.colors.onSurface, accountViewModel = accountViewModel)
            }

            description?.let {
                Spacer(modifier = StdVertSpacer)
                Row() {
                    Text(
                        text = it,
                        color = MaterialTheme.colors.placeholderText,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 14.sp
                    )
                }
            }

            if (participantUsers.isNotEmpty()) {
                Spacer(modifier = StdVertSpacer)
                Row() {
                    Gallery(participantUsers, accountViewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Gallery(users: List<User>, accountViewModel: AccountViewModel) {
    FlowRow(verticalAlignment = CenterVertically) {
        users.take(6).forEach {
            ClickableUserPicture(it, Size35dp, accountViewModel)
        }

        if (users.size > 6) {
            Text(
                text = remember(users) { " + " + (showCount(users.size - 6)).toString() },
                fontSize = 13.sp,
                color = MaterialTheme.colors.onSurface
            )
        }
    }
}

@Composable
fun DisplayAuthorBanner(author: User) {
    val picture by author.live().metadata.map {
        it.user.info?.banner?.ifBlank { null } ?: it.user.info?.picture?.ifBlank { null }
    }.observeAsState()

    AsyncImage(
        model = picture,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .clip(QuoteBorder)
    )
}
